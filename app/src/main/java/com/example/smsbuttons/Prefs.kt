package com.example.smsbuttons

import android.content.Context

object Prefs {
    const val COUNT = 12
    private const val FILE = "cfg"

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun phone(c: Context): String = sp(c).getString("phone", "") ?: ""
    fun setPhone(c: Context, v: String) = sp(c).edit().putString("phone", v).apply()

    fun message(c: Context, i: Int): String =
        sp(c).getString("msg_$i", "Message ${i + 1}") ?: ""

    fun setMessage(c: Context, i: Int, v: String) =
        sp(c).edit().putString("msg_$i", v).apply()
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
