package com.wolftower.app

import android.content.Context
import org.json.JSONObject

data class Profile(
    val id: String,
    val name: String,
    val packages: List<String>,
    val domains: List<String>,
    val strategies: List<String>,
    val blockQuic: Boolean,
)

object ProfilesRepository {
    fun load(ctx: Context): List<Profile> {
        val text = ctx.assets.open("profiles.json").bufferedReader().use { it.readText() }
        val arr = JSONObject(text).getJSONArray("profiles")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            fun list(key: String): List<String> {
                val a = o.optJSONArray(key) ?: return emptyList()
                return (0 until a.length()).map { a.getString(it) }
            }
            Profile(
                id = o.getString("id"),
                name = o.getString("name"),
                packages = list("android_packages"),
                domains = list("domains"),
                strategies = list("strategies"),
                blockQuic = o.optBoolean("block_quic", false),
            )
        }
    }
}
