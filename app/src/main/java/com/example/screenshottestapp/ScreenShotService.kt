package com.example.screenshottestapp

import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.widget.Toast
import eu.bolt.screenshotty.Screenshot
import eu.bolt.screenshotty.ScreenshotBitmap
import eu.bolt.screenshotty.util.ScreenshotFileSaver
import java.io.File


class ScreenShotService : Service() {

    override fun onCreate() {
        super.onCreate()


    }



    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}