package com.wolftower.app

import android.content.Context
import android.os.ParcelFileDescriptor
import hev.htproxy.TProxyService
import java.io.File

/**
 * tun2socks на базе hev-socks5-tunnel (MIT).
 * DNS: приложения спрашивают адрес у hev (mapdns), тот отдаёт фейковый IP и передаёт в SOCKS5
 * имя домена. ByeDPI сам резолвит домен и видит имя для десинхронизации.
 */
class HevTun2Socks(private val ctx: Context) : Tun2Socks {

    companion object {
        const val MAPDNS_ADDRESS = "198.18.0.2"
        const val TUN_ADDRESS = "10.10.10.10"
        const val MTU = 1500
    }

    override fun start(
        tun: ParcelFileDescriptor,
        socksHost: String,
        socksPort: Int,
        onLog: (String) -> Unit,
    ) {
        val cfg = File(ctx.filesDir, "hev.yml")
        cfg.writeText(
            """
            tunnel:
              mtu: $MTU
              ipv4: $TUN_ADDRESS
            socks5:
              port: $socksPort
              address: $socksHost
              udp: 'udp'
            mapdns:
              address: $MAPDNS_ADDRESS
              port: 53
              network: 100.64.0.0
              netmask: 255.192.0.0
              cache-size: 10000
            misc:
              task-stack-size: 81920
              connect-timeout: 5000
              log-level: warn
            """.trimIndent() + "\n"
        )
        try {
            val ok = TProxyService.TProxyStartService(cfg.absolutePath, tun.fd)
            onLog(if (ok) "tun2socks запущен" else "tun2socks: не удалось запуститься")
        } catch (t: Throwable) {
            onLog("tun2socks: ошибка загрузки библиотеки: ${t.message}")
        }
    }

    override fun stop() {
        try {
            if (TProxyService.TProxyIsRunning()) TProxyService.TProxyStopService()
        } catch (_: Throwable) {
        }
    }
}
