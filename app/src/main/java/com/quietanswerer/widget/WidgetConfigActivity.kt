package com.quietanswerer.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.widget.Toast
import com.quietanswerer.R

class WidgetConfigActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id != null && id != AppWidgetManager.INVALID_APPWIDGET_ID) {
            Toast.makeText(this, R.string.done, Toast.LENGTH_SHORT).show()
            val result = android.content.Intent()
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            setResult(RESULT_OK, result)
        } else {
            setResult(RESULT_CANCELED)
        }
        finish()
    }
}