package com.wolftower.app

import android.os.ParcelFileDescriptor

/** Мост TUN -> SOCKS5. Подключить hev-socks5-tunnel (MIT). */
interface Tun2Socks {
    fun start(tun: ParcelFileDescriptor, socksHost: String, socksPort: Int, onLog: (String) -> Unit)
    fun stop()
}

/**
 * ЗАГЛУШКА. Пока здесь ничего не пересылается, трафик выбранных приложений не пойдёт.
 * Следующий шаг: собрать hev-socks5-tunnel под Android и вызвать его с fd от TUN
 * и конфигом вида:
 *   socks5: { address: 127.0.0.1, port: 1080, udp: 'tcp' }
 */
class PlaceholderTun2Socks : Tun2Socks {
    override fun start(tun: ParcelFileDescriptor, socksHost: String, socksPort: Int, onLog: (String) -> Unit) {
        onLog("tun2socks: ЗАГЛУШКА, подключи hev-socks5-tunnel")
    }

    override fun stop() {}
}
