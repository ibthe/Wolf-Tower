package com.wolftower.app

/**
 * Имена стратегий из profiles.json -> аргументы ByeDPI (ciadpi).
 * Флаги сверены с `ciadpi --help` (коммит ba53229). "+s" = смещение от начала SNI.
 */
object StrategyArgs {
    /** Запасные описания на случай, если в profiles.json нет раздела strategy_args. */
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
        "multisplit-short" to listOf("-d1", "-s1+s", "-s3+s", "-s6+s"),
        "multisplit" to listOf("-d1", "-s1+s", "-s3+s", "-s6+s", "-s9+s", "-s12+s", "-s15+s", "-s20+s", "-s30+s", "-a1"),
        "multidisorder" to listOf("-d1", "-d3+s", "-d6+s", "-d9+s", "-d12+s", "-d15+s", "-d20+s", "-d30+s"),
    )

    @Volatile
    private var registered: Map<String, List<String>> = emptyMap()

    @Volatile
    private var fakeSni: String = "www.iana.org"

    /** Подключает описания из profiles.json. {sni} в аргументах заменяется на имя для фальшивых пакетов. */
    fun register(args: Map<String, List<String>>, sni: String) {
        registered = args
        if (sni.isNotBlank()) fakeSni = sni
    }

    /** "none" и неизвестные имена = без обхода. */
    fun forName(name: String): List<String> {
        val raw = registered[name] ?: library[name] ?: return emptyList()
        return raw.map { it.replace("{sni}", fakeSni) }
    }

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
