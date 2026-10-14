package com.wolftower.app

import android.os.ParcelFileDescriptor

/** Мост TUN -> SOCKS5. */
interface Tun2Socks {
    fun start(tun: ParcelFileDescriptor, socksHost: String, socksPort: Int, onLog: (String) -> Unit)
    fun stop()
}
