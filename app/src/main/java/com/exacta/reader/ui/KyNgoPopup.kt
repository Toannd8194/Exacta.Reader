package com.exacta.reader.ui

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.exacta.reader.data.Store

/** A kỳ ngộ met shows as a framed popup, while reading too; several wait their turn. */
object KyNgoPopup {
    private var open = false

    fun show(a: Activity) {
        if (open || !Store.kyNgoPopup || a.isFinishing) return
        val (name, text) = Store.takePopup() ?: return
        open = true
        AlertDialog.Builder(a)
            .setTitle("✦ Kỳ ngộ · $name")
            .setMessage(text)
            .setPositiveButton("Thu nhận cơ duyên") { _, _ -> }
            .setOnDismissListener {
                open = false
                show(a)
            }
            .show()
    }
}
