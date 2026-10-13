package com.wolftower.app

import android.content.Context

/** Простое хранилище выбора пользователя. */
object Settings {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("dpi", Context.MODE_PRIVATE)

    fun enabledIds(ctx: Context): Set<String> =
        prefs(ctx).getStringSet("enabled", emptySet()) ?: emptySet()

    fun setEnabledIds(ctx: Context, ids: Set<String>) =
        prefs(ctx).edit().putStringSet("enabled", ids).apply()

    fun strategy(ctx: Context): String = prefs(ctx).getString("strategy", "split") ?: "split"

    fun setStrategy(ctx: Context, s: String) = prefs(ctx).edit().putString("strategy", s).apply()
}
