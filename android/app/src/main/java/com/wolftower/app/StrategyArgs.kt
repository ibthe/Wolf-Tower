package com.wolftower.app

/**
 * Преобразует имя стратегии из profiles.json в аргументы ByeDPI (ciadpi).
 * ВАЖНО: сверь флаги с `ciadpi --help` вашей версии third_party/byedpi.
 * "+s" = смещение считается от начала SNI.
 */
object StrategyArgs {
    fun forName(name: String): List<String> = when (name) {
        "split" -> listOf("-s1+s")
        "disorder" -> listOf("-d1+s")
        "oob" -> listOf("-o1+s")
        "split+tlsrec" -> listOf("-s1+s", "-r1+s")
        "fake+disorder" -> listOf("-f-1", "-t", "8", "-d1+s")
        "fake+tlsrec" -> listOf("-f-1", "-t", "8", "-r1+s")
        else -> listOf("-s1+s")
    }
}
