package com.wolftower.app

import android.content.Intent
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.concurrent.thread

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
    private lateinit var tun2socks: Tun2Socks
    private var guardThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        tun2socks = HevTun2Socks(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAll()
            stopSelf()
            return START_NOT_STICKY
        }
        startAll()
        return START_NOT_STICKY
    }

    private fun startAll() {
        if (running.value) return
        if (!LicenseManager.canRun(this)) {
            appendLog("Нужен лицензионный ключ")
            stopSelf()
            return
        }
        val config = ProfilesRepository.load(this)
        val enabled = Settings.enabledIds(this)
        val chosen = config.profiles.filter { it.id in enabled }
        if (chosen.isEmpty()) {
            appendLog("Не выбран ни один сервис")
            stopSelf()
            return
        }

        val selection = chosen.map { it to Settings.strategyFor(this, it.id, config.defaultStrategy) }
        appendLog("Стратегии: " + selection.joinToString { "${it.first.name}=${it.second}" })

        // Запрет UDP (QUIC): по выбору пользователя, а если не задано, то когда его хотят все сервисы
        val blockQuic = Settings.blockQuicPref(this) ?: chosen.all { it.blockQuic }
        val noUdp = if (blockQuic) listOf("-U") else emptyList()
        val args = noUdp + StrategyArgs.buildGroups(selection)

        val proc = ByeDpiProcess(this)
        if (!proc.start(SOCKS_PORT, args, ::appendLog)) {
            stopSelf()
            return
        }
        byeDpi = proc

        val builder = Builder()
            .setSession("Wolf Tower")
            .addAddress(HevTun2Socks.TUN_ADDRESS, 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer(HevTun2Socks.MAPDNS_ADDRESS)
            .setMtu(HevTun2Socks.MTU)

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
        startClockGuard()
    }

    /** Раз в 30 минут сверяет часы с сетью; при откате времени деактивирует ключ и останавливает защиту. */
    private fun startClockGuard() {
        guardThread?.interrupt()
        guardThread = thread(isDaemon = true) {
            try {
                while (true) {
                    Thread.sleep(30L * 60 * 1000)
                    if (LicenseManager.networkCheck(this@DpiVpnService) == NetCheck.BEHIND) {
                        appendLog("Время отстаёт от сетевого, ключ деактивирован")
                        Handler(Looper.getMainLooper()).post {
                            stopAll()
                            stopSelf()
                        }
                        break
                    }
                }
            } catch (_: InterruptedException) {
            }
        }
    }

    /** Безопасно вызывать несколько раз: каждый шаг защищён, чтобы остановка не роняла приложение. */
    private fun stopAll() {
        guardThread?.interrupt()
        guardThread = null
        runCatching { tun2socks.stop() }
        runCatching { byeDpi?.stop() }
        byeDpi = null
        runCatching { tun?.close() }
        tun = null
        if (running.value) appendLog("Остановлено")
        running.value = false
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
