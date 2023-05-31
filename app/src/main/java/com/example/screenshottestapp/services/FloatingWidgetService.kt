package com.example.screenshottestapp.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.WindowManager
import com.example.screenshottestapp.views.FloatingWidgetView

class FloatingWidgetService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingWidgetView: FloatingWidgetView

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingWidgetView = FloatingWidgetView(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::windowManager.isInitialized) windowManager.removeView(floatingWidgetView)
    }
}