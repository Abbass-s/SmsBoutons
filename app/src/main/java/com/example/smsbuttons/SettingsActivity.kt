package com.example.smsbuttons

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Paramètres"
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }

        col.addView(label("Sauvegarde automatique à chaque modification."))

        col.addView(label("Numéro de téléphone (destinataire unique)"))
        val phone = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_PHONE
            hint = "+216 XX XXX XXX"
            setText(Prefs.phone(this@SettingsActivity))
        }
        watch(phone) { Prefs.setPhone(this, it) }
        col.addView(phone)

        for (i in 0 until Prefs.COUNT) {
            col.addView(label("Message ${i + 1}"))
            val e = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                minLines = 2
                hint = "Texte du message ${i + 1}"
                setText(Prefs.message(this@SettingsActivity, i))
            }
            watch(e) { Prefs.setMessage(this, i, it) }
            col.addView(e)
        }

        setContentView(ScrollView(this).apply { addView(col) })
    }

    private fun label(t: String) = TextView(this).apply {
        text = t
        textSize = 14f
        setPadding(0, dp(14), 0, 0)
    }

    private fun watch(e: EditText, save: (String) -> Unit) {
        e.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = save(s?.toString() ?: "")
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
