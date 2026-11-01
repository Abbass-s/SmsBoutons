package com.example.smsbuttons

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.text.TextUtils
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    companion object {
        const val ACTION_SENT = "com.example.smsbuttons.SENT"
        const val REQ_SMS = 1
    }

    private lateinit var info: TextView
    private val buttons = ArrayList<Button>()
    private var pending = -1

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (resultCode == Activity.RESULT_OK) toast("SMS envoyé ✓")
            else toast("Échec de l'envoi (code $resultCode)")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "SMS Boutons"

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        info = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        }
        col.addView(info)

        val settingsBtn = Button(this).apply {
            text = "Paramètres"
            isAllCaps = false
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        }
        col.addView(settingsBtn)

        for (r in 0 until Prefs.COUNT / 2) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (c in 0..1) {
                val idx = r * 2 + c
                val b = Button(this).apply {
                    isAllCaps = false
                    textSize = 15f
                    maxLines = 3
                    ellipsize = TextUtils.TruncateAt.END
                    setOnClickListener { send(idx) }
                }
                val lp = LinearLayout.LayoutParams(0, dp(96), 1f)
                lp.setMargins(dp(4), dp(4), dp(4), dp(4))
                row.addView(b, lp)
                buttons.add(b)
            }
            col.addView(row)
        }

        setContentView(ScrollView(this).apply { addView(col) })
    }

    override fun onStart() {
        super.onStart()
        val f = IntentFilter(ACTION_SENT)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, f, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(receiver, f)
    }

    override fun onStop() {
        super.onStop()
        try { unregisterReceiver(receiver) } catch (e: IllegalArgumentException) { }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val p = Prefs.phone(this)
        info.text = if (p.isBlank()) "Aucun numéro défini — ouvrez Paramètres" else "Destinataire : $p"
        buttons.forEachIndexed { i, b ->
            val m = Prefs.message(this, i)
            b.text = "${i + 1}\n" + if (m.isBlank()) "(vide)" else m.take(40)
        }
    }

    private fun send(i: Int) {
        val phone = Prefs.phone(this).replace(Regex("[^+0-9]"), "")
        val msg = Prefs.message(this, i)
        if (phone.isEmpty()) {
            toast("Définissez d'abord le numéro dans Paramètres")
            return
        }
        if (msg.isBlank()) {
            toast("Le message ${i + 1} est vide")
            return
        }
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            pending = i
            requestPermissions(arrayOf(Manifest.permission.SEND_SMS), REQ_SMS)
            return
        }
        doSend(phone, msg)
    }

    private fun doSend(phone: String, msg: String) {
        try {
            val sm: SmsManager =
                if (Build.VERSION.SDK_INT >= 31) getSystemService(SmsManager::class.java)
                else @Suppress("DEPRECATION") SmsManager.getDefault()

            val sentPI = PendingIntent.getBroadcast(
                this, 0,
                Intent(ACTION_SENT).setPackage(packageName),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val parts = sm.divideMessage(msg)
            if (parts.size == 1) {
                sm.sendTextMessage(phone, null, msg, sentPI, null)
            } else {
                // callback only on the last part to avoid one toast per part
                val sent = ArrayList<PendingIntent?>()
                for (k in parts.indices) sent.add(if (k == parts.size - 1) sentPI else null)
                sm.sendMultipartTextMessage(phone, null, parts, sent, null)
            }
        } catch (e: Exception) {
            toast("Erreur : ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_SMS) return
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (pending >= 0) send(pending)
        } else {
            toast("Permission SMS refusée. Android 13+ : Infos appli > ⋮ > Autoriser les paramètres restreints, puis Autorisations.")
        }
        pending = -1
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
