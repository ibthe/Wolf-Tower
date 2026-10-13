package com.wolftower.app

import android.content.Context
import java.io.File
import kotlin.concurrent.thread

/** Запускает ByeDPI (libciadpi.so) как отдельный процесс с SOCKS5 на localhost. */
class ByeDpiProcess(private val ctx: Context) {
    private var proc: Process? = null

    fun start(port: Int, strategyArgs: List<String>, onLog: (String) -> Unit): Boolean {
        val bin = File(ctx.applicationInfo.nativeLibraryDir, "libciadpi.so")
        if (!bin.exists()) {
            onLog("Не найден ${bin.path}. Проверь сборку CMake и third_party/byedpi.")
            return false
        }
        val cmd = listOf(bin.path, "-i", "127.0.0.1", "-p", port.toString()) + strategyArgs
        onLog("Запуск: ${cmd.joinToString(" ")}")
        return try {
            val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
            proc = p
            thread(isDaemon = true) {
                p.inputStream.bufferedReader().forEachLine { onLog("byedpi: $it") }
                onLog("byedpi завершился")
            }
            true
        } catch (e: Exception) {
            onLog("Ошибка запуска: ${e.message}")
            false
        }
    }

    fun stop() {
        proc?.destroy()
        proc = null
    }
}
