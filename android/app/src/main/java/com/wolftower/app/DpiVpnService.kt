package com.wolftower.app

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.flow.MutableStateFlow

class DpiVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.wolftower.app.START"
        const val ACTION_STOP = "com.wolftower.app.STOP"
        const val SOCKS_PORT = 1080

        val running = MutableStateFlow(false)
        val log = MutableStateFlow<List<String>>(emptyList())

        fun appendLog(s: String) {
            log.value = (log.value + s).takeLast(200)
        }
    }

    private var tun: ParcelFileDescriptor? = null
    private var byeDpi: ByeDpiProcess? = null
    private val tun2socks: Tun2Socks = PlaceholderTun2Socks()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAll()
            stopSelf()
            return START_NOT_STICKY
        }
        startAll()
        return START_STICKY
    }

    private fun startAll() {
        if (running.value) return
        val profiles = ProfilesRepository.load(this)
        val enabled = Settings.enabledIds(this)
        val chosen = profiles.filter { it.id in enabled }
        if (chosen.isEmpty()) {
            appendLog("Не выбран ни один сервис")
            stopSelf()
            return
        }

        val proc = ByeDpiProcess(this)
        val args = StrategyArgs.forName(Settings.strategy(this))
        if (!proc.start(SOCKS_PORT, args, ::appendLog)) {
            stopSelf()
            return
        }
        byeDpi = proc

        val builder = Builder()
            .setSession("Wolf Tower")
            .addAddress("10.10.10.10", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            .setMtu(1500)

        // Только выбранные сервисы идут через туннель, остальной трафик напрямую
        for (pkg in chosen.flatMap { it.packages }.distinct()) {
            try {
                builder.addAllowedApplication(pkg)
            } catch (e: Exception) {
                appendLog("Пропущено (не установлено): $pkg")
            }
        }

        val fd = builder.establish()
        if (fd == null) {
            appendLog("Не удалось создать VPN-интерфейс")
            stopAll()
            stopSelf()
            return
        }
        tun = fd
        tun2socks.start(fd, "127.0.0.1", SOCKS_PORT, ::appendLog)
        running.value = true
        appendLog("Запущено. Сервисы: ${chosen.joinToString { it.name }}")
    }

    private fun stopAll() {
        tun2socks.stop()
        byeDpi?.stop()
        byeDpi = null
        tun?.close()
        tun = null
        running.value = false
        appendLog("Остановлено")
    }

    override fun onDestroy() {
        stopAll()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopAll()
        super.onRevoke()
    }
}
