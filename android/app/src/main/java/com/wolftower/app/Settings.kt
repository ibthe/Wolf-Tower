package com.wolftower.app

import android.content.Context

/** Простое хранилище выбора пользователя. */
object Settings {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("dpi", Context.MODE_PRIVATE)

    fun enabledIds(ctx: Context): Set<String> =
        prefs(ctx).getStringSet("enabled", emptySet()) ?: emptySet()

    fun setEnabledIds(ctx: Context, ids: Set<String>) =
        prefs(ctx).edit().putStringSet("enabled", ids).apply()

    /** Стратегия для конкретного сервиса. "none" = без обхода. */
    fun strategyFor(ctx: Context, profileId: String, default: String): String =
        prefs(ctx).getString("strategy_$profileId", default) ?: default

    fun setStrategyFor(ctx: Context, profileId: String, strategy: String) =
        prefs(ctx).edit().putString("strategy_$profileId", strategy).apply()

    /** null = автоматически (по block_quic выбранных сервисов). */
    fun blockQuicPref(ctx: Context): Boolean? {
        val p = prefs(ctx)
        return if (p.contains("block_quic")) p.getBoolean("block_quic", false) else null
    }

    fun setBlockQuic(ctx: Context, value: Boolean) =
        prefs(ctx).edit().putBoolean("block_quic", value).apply()
}
