package com.wolftower.app

/**
 * Имена стратегий из profiles.json -> аргументы ByeDPI (ciadpi).
 * Флаги сверены с `ciadpi --help` (коммит ba53229). "+s" = смещение от начала SNI.
 */
object StrategyArgs {
    private val library: Map<String, List<String>> = mapOf(
        "split" to listOf("-s1+s"),
        "disorder" to listOf("-d1+s"),
        "oob" to listOf("-o1+s"),
        "disoob" to listOf("-q1+s"),
        "split+tlsrec" to listOf("-s1+s", "-r1+s"),
        "disorder+tlsrec" to listOf("-d1+s", "-r1+s"),
        "oob+tlsrec" to listOf("-o1+s", "-r1+s"),
        "fake+disorder" to listOf("-f-1", "-t", "8", "-d1+s"),
        "fake+oob" to listOf("-f-1", "-t", "8", "-o1+s"),
        "fake+split" to listOf("-f-1", "-t", "8", "-s1+s"),
        "fake+tlsrec" to listOf("-f-1", "-t", "8", "-r1+s"),
        "fake4+disorder" to listOf("-f-1", "-t", "4", "-d1+s"),
        "fake12+disorder" to listOf("-f-1", "-t", "12", "-d1+s"),
    )

    /** "none" и неизвестные имена = без обхода. */
    fun forName(name: String): List<String> = library[name] ?: emptyList()

    /**
     * Своя стратегия для каждого сервиса: группы опций ByeDPI, разделённые --auto=none.
     * Группа по доменам срабатывает по SNI. Для сервисов с ip_ranges (Telegram подключается по IP
     * без имени сайта) добавляется вторая группа с фильтром по IP-диапазонам: в ByeDPI фильтры
     * внутри одной группы работают по "И", поэтому домены и IP идут отдельными группами.
     * Остальной трафик идёт без изменений.
     */
    fun buildGroups(selection: List<Pair<Profile, String>>): List<String> {
        val out = ArrayList<String>()
        fun group(filterFlag: String, filterValue: String, strategy: String) {
            if (out.isNotEmpty()) out.add("--auto=none")
            out.add(filterFlag)
            out.add(":$filterValue")
            out.addAll(forName(strategy))
        }
        for ((profile, strategy) in selection) {
            if (strategy == "none") continue
            if (profile.domains.isNotEmpty()) group("--hosts", profile.domains.joinToString(" "), strategy)
            if (profile.ipRanges.isNotEmpty()) group("--ipset", profile.ipRanges.joinToString(" "), strategy)
        }
        return out
    }
}
