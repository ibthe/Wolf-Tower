package com.wolftower.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Profile(
    val id: String,
    val name: String,
    val packages: List<String>,
    val domains: List<String>,
    val ipRanges: List<String>,
    val probeHosts: List<String>,
    val blockQuic: Boolean,
)

data class ProfilesConfig(
    val profiles: List<Profile>,
    val strategies: List<String>,
    /** Расширенные наборы: в списке для ручного выбора не показываются, но проверяются автоподбором. */
    val extended: List<String> = emptyList(),
) {
    val allStrategies: List<String> get() = strategies + extended
    val defaultStrategy: String
        get() = if ("disorder" in strategies) "disorder" else strategies.first()
}

object ProfilesRepository {
    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }

    fun load(ctx: Context): ProfilesConfig {
        val text = ctx.assets.open("profiles.json").bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        val arr = root.getJSONArray("profiles")
        val profiles = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Profile(
                id = o.getString("id"),
                name = o.getString("name"),
                packages = o.optJSONArray("android_packages").strings(),
                domains = o.optJSONArray("domains").strings(),
                ipRanges = o.optJSONArray("ip_ranges").strings(),
                probeHosts = o.optJSONArray("probe_hosts").strings(),
                blockQuic = o.optBoolean("block_quic", false),
            )
        }
        // Описания стратегий (имя -> аргументы ByeDPI) лежат в profiles.json: новые наборы добавляются без правки кода
        val defs = root.optJSONObject("strategy_args")
        if (defs != null) {
            val map = HashMap<String, List<String>>()
            val names = defs.keys()
            while (names.hasNext()) {
                val name = names.next()
                map[name] = defs.getJSONArray(name).strings()
            }
            StrategyArgs.register(map, root.optString("fake_sni", ""))
        }
        return ProfilesConfig(
            profiles,
            root.optJSONArray("strategies").strings(),
            root.optJSONArray("strategies_extended").strings(),
        )
    }
}
