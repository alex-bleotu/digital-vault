package com.digitalvault.core.vpn

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.digitalvault.R
import com.digitalvault.core.data.DnsRepository
import com.digitalvault.core.data.DomainMatcher
import com.digitalvault.core.data.vaultDataStore
import com.digitalvault.core.service.VaultNotifications
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TUN_ADDRESS = "10.111.222.1"
private const val TUN_PREFIX_LENGTH = 24
private const val UDP_PROTOCOL = 17
private const val DNS_PORT = 53
private const val VPN_NOTIFICATION_ID = 2001
private const val UPSTREAM_TIMEOUT_MILLIS = 4_000

class VaultVpnService : VpnService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val relayExecutor = Executors.newCachedThreadPool()

    private var tunFd: ParcelFileDescriptor? = null
    private var readThread: Thread? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    @Volatile
    private var isReadLoopActive: Boolean = false

    @Volatile
    private var blockedDomains: Set<String> = emptySet()

    @Volatile
    private var currentDnsServers: List<String> = emptyList()

    override fun onCreate() {
        super.onCreate()
        isRunning.value = true
        serviceScope.launch {
            DnsRepository(vaultDataStore).config.collectLatest { config ->
                blockedDomains = config.blockedDomains
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()

            return START_NOT_STICKY
        }
        startForeground(VPN_NOTIFICATION_ID, buildNotification())
        startVpn()

        return START_STICKY
    }

    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        relayExecutor.shutdownNow()
        isRunning.value = false
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        stopSelf()
        super.onRevoke()
    }

    private fun startVpn() {
        val dnsServers = discoverDnsServers()
        val builder = Builder()
            .setSession("Digital Vault")
            .addAddress(TUN_ADDRESS, TUN_PREFIX_LENGTH)
            .setMtu(1500)
            .setBlocking(true)

        if (dnsServers.isEmpty()) {
            stopSelf()

            return
        }
        for (server in dnsServers) {
            builder.addRoute(server, 32)
            builder.addDnsServer(server)
        }

        val establishedFd = builder.establish()
        if (establishedFd == null) {
            stopSelf()

            return
        }
        tunFd = establishedFd
        currentDnsServers = dnsServers
        isReadLoopActive = true

        val thread = Thread({ runReadLoop(establishedFd) }, "VaultVpnReadLoop")
        thread.start()
        readThread = thread

        registerNetworkCallback()
    }

    private fun stopVpn() {
        isReadLoopActive = false
        networkCallback?.let { callback ->
            getSystemService<ConnectivityManager>()?.unregisterNetworkCallback(callback)
        }
        networkCallback = null
        readThread = null
        tunFd?.let {
            runCatching { it.close() }
        }
        tunFd = null
    }

    private fun registerNetworkCallback() {
        val connectivityManager = getSystemService<ConnectivityManager>() ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLinkPropertiesChanged(
                network: Network,
                linkProperties: android.net.LinkProperties,
            ) {
                val newServers = linkProperties.dnsServers
                    .mapNotNull { it.hostAddress }
                    .filter { it.contains('.') && !it.contains(':') }
                if (newServers.isNotEmpty() && newServers.toSet() != currentDnsServers.toSet()) {
                    restartVpn()
                }
            }
        }
        networkCallback = callback
        connectivityManager.registerDefaultNetworkCallback(callback)
    }

    private fun restartVpn() {
        stopVpn()
        startVpn()
    }

    private fun discoverDnsServers(): List<String> {
        val connectivityManager = getSystemService<ConnectivityManager>() ?: return emptyList()
        val network = connectivityManager.activeNetwork ?: return emptyList()
        val linkProperties = connectivityManager.getLinkProperties(network) ?: return emptyList()

        return linkProperties.dnsServers
            .mapNotNull { it.hostAddress }
            .filter { it.contains('.') && !it.contains(':') }
    }

    private fun runReadLoop(fd: ParcelFileDescriptor) {
        val input = FileInputStream(fd.fileDescriptor)
        val output = FileOutputStream(fd.fileDescriptor)
        val buffer = ByteArray(32767)
        while (isReadLoopActive) {
            val length = try {
                input.read(buffer)
            } catch (_: Exception) {
                break
            }
            if (length <= 0) {
                break
            }
            try {
                handlePacket(buffer, length, output)
            } catch (_: Exception) {
            }
        }
    }

    private fun handlePacket(packet: ByteArray, length: Int, output: FileOutputStream) {
        if (length < 20) {
            return
        }
        val version = (packet[0].toInt() and 0xF0) shr 4
        if (version != 4) {
            return
        }
        val headerLength = (packet[0].toInt() and 0x0F) * 4
        if (headerLength < 20 || headerLength > length) {
            return
        }
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != UDP_PROTOCOL) {
            return
        }
        val udpOffset = headerLength
        if (udpOffset + 8 > length) {
            return
        }
        val destPort = ((packet[udpOffset + 2].toInt() and 0xFF) shl 8) or (packet[udpOffset + 3].toInt() and 0xFF)
        if (destPort != DNS_PORT) {
            return
        }
        val dnsOffset = udpOffset + 8
        val dnsLength = length - dnsOffset
        if (dnsLength <= 0) {
            return
        }

        val sourceAddress = packet.copyOfRange(12, 16)
        val destAddress = packet.copyOfRange(16, 20)
        val sourcePort = ((packet[udpOffset].toInt() and 0xFF) shl 8) or (packet[udpOffset + 1].toInt() and 0xFF)
        val queryBytes = packet.copyOfRange(dnsOffset, dnsOffset + dnsLength)

        val queriedDomain = DnsMessageParser.parseQueriedDomain(queryBytes, dnsLength)
        if (queriedDomain != null && DomainMatcher.matches(queriedDomain, blockedDomains)) {
            val blockedResponse = DnsMessageParser.buildBlockedResponse(queryBytes, dnsLength) ?: return
            val replyPacket = buildReplyPacket(destAddress, sourceAddress, DNS_PORT, sourcePort, blockedResponse)
            writePacket(output, replyPacket)

            return
        }

        relayExecutor.submit {
            relayToUpstream(queryBytes, sourceAddress, destAddress, sourcePort, output)
        }
    }

    private fun relayToUpstream(
        query: ByteArray,
        sourceAddress: ByteArray,
        destAddress: ByteArray,
        sourcePort: Int,
        output: FileOutputStream,
    ) {
        try {
            val upstreamAddress = InetAddress.getByAddress(destAddress)
            DatagramSocket().use { socket ->
                protect(socket)
                socket.soTimeout = UPSTREAM_TIMEOUT_MILLIS
                socket.send(DatagramPacket(query, query.size, InetSocketAddress(upstreamAddress, DNS_PORT)))
                val responseBuffer = ByteArray(2048)
                val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
                socket.receive(responsePacket)
                val response = responseBuffer.copyOfRange(0, responsePacket.length)
                val replyPacket = buildReplyPacket(destAddress, sourceAddress, DNS_PORT, sourcePort, response)
                writePacket(output, replyPacket)
            }
        } catch (_: Exception) {
        }
    }

    private fun buildReplyPacket(
        sourceAddress: ByteArray,
        destAddress: ByteArray,
        sourcePort: Int,
        destPort: Int,
        payload: ByteArray,
    ): ByteArray {
        val udpLength = 8 + payload.size
        val totalLength = 20 + udpLength
        val packet = ByteArray(totalLength)

        packet[0] = 0x45
        packet[1] = 0
        packet[2] = (totalLength shr 8).toByte()
        packet[3] = (totalLength and 0xFF).toByte()
        packet[4] = 0
        packet[5] = 0
        packet[6] = 0x40.toByte()
        packet[7] = 0
        packet[8] = 64
        packet[9] = UDP_PROTOCOL.toByte()
        System.arraycopy(sourceAddress, 0, packet, 12, 4)
        System.arraycopy(destAddress, 0, packet, 16, 4)
        val ipChecksum = ChecksumUtil.ipv4HeaderChecksum(packet, 0, 20)
        packet[10] = (ipChecksum shr 8).toByte()
        packet[11] = (ipChecksum and 0xFF).toByte()

        val udpOffset = 20
        packet[udpOffset] = (sourcePort shr 8).toByte()
        packet[udpOffset + 1] = (sourcePort and 0xFF).toByte()
        packet[udpOffset + 2] = (destPort shr 8).toByte()
        packet[udpOffset + 3] = (destPort and 0xFF).toByte()
        packet[udpOffset + 4] = (udpLength shr 8).toByte()
        packet[udpOffset + 5] = (udpLength and 0xFF).toByte()
        packet[udpOffset + 6] = 0
        packet[udpOffset + 7] = 0
        System.arraycopy(payload, 0, packet, udpOffset + 8, payload.size)

        val udpSegment = packet.copyOfRange(udpOffset, totalLength)
        val udpChecksum = ChecksumUtil.udpChecksum(sourceAddress, destAddress, udpSegment, udpLength)
        packet[udpOffset + 6] = (udpChecksum shr 8).toByte()
        packet[udpOffset + 7] = (udpChecksum and 0xFF).toByte()

        return packet
    }

    private val writeLock = Any()

    private fun writePacket(output: FileOutputStream, packet: ByteArray) {
        synchronized(writeLock) {
            try {
                output.write(packet)
            } catch (_: Exception) {
            }
        }
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, VaultNotifications.STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vault_notification)
            .setContentTitle("Digital Vault")
            .setContentText("Domain blocking is active")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    companion object {
        val isRunning = MutableStateFlow(false)

        private const val ACTION_STOP = "com.digitalvault.vpn.STOP"

        fun prepareIntent(context: Context): Intent? = prepare(context)

        fun start(context: Context) {
            context.startForegroundService(Intent(context, VaultVpnService::class.java))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, VaultVpnService::class.java).setAction(ACTION_STOP))
        }
    }
}
