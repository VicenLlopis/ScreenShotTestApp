package com.example.screenshottestapp.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.example.screenshottestapp.MainActivity
import com.example.screenshottestapp.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class ScreenShotQrService : Service() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjection: MediaProjection
    private var resultCode = 0
    private var resultData: Intent? = null


    companion object {

        const val ACTION_SCREENSHOT_SERVICE_DESTROYED =
            "com.example.ACTION_SCREENSHOT_SERVICE_DESTROYED"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_INTENT = "resultIntent"
        private const val ONGOING_NOTIFICATION_ID = 123
        private const val CHANNEL_DEFAULT_IMPORTANCE = "1"

    }

    override fun onCreate() {


        Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
            )
        }

        createNotification()

        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    }

    private fun createNotification() {

        val serviceChannel = NotificationChannel(
            CHANNEL_DEFAULT_IMPORTANCE, "2", NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = getSystemService(
            NotificationManager::class.java
        )

        manager.createNotificationChannel(serviceChannel)

    }

    private fun showNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 0
        )
        val notification: Notification = Notification.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
            .setContentTitle("Service Screenshot").setContentText("Service Screenshot")
            .setSmallIcon(R.drawable.ic_launcher_foreground).setContentIntent(pendingIntent).build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    @SuppressLint("WrongConstant")
    private fun takeScreenshot() {

        Log.d("TESTBase64Encode", " Ha entrado función encode")

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        val size = Point()
        display.getRealSize(size)
        val mWidth = size.x
        val mHeight = size.y
        val mDensity = metrics.densityDpi
        val mImageReader: ImageReader =
            ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2)
        val handler = Handler()

        val flags =
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC

        mediaProjection.createVirtualDisplay(
            "screen-mirror", mWidth, mHeight, mDensity, flags, mImageReader.surface, null, handler
        )

        var image: Image? = null
        var cont = 0
        while (image == null || cont == 5) {
            Log.d("PRUEBATEST", "cont: $cont")
            Thread.sleep(300)
            image = mImageReader.acquireLatestImage()
            cont++
        }

        if (image == null) {
            mImageReader.close()
            mediaProjection.stop()
            stopSelf()
            Toast.makeText(
                this, R.string.string_Error_screenshot, Toast.LENGTH_SHORT
            ).show()

        } else {
            val bitmap = Bitmap.createBitmap(
                image.width, image.height, Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(image.planes[0].buffer)



            image.close()
            mImageReader.close()
            encodeImage(bitmap)
            Toast.makeText(
                this, R.string.base64_qr_ok, Toast.LENGTH_SHORT
            ).show()

            mediaProjection.stop()
            stopSelf()
        }
    }


    private fun encodeImage(bm: Bitmap)  {

        mediaProjection.stop()
        val fileName = "base64Img"
        val file = File(applicationContext.filesDir, fileName)
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        val base64Img = Base64.encodeToString(b, Base64.DEFAULT)

        try {

            if(!file.exists()){
                file.createNewFile()
                file.writeText(base64Img, Charsets.UTF_8)

            }else {
                file.delete()
                file.createNewFile()
                file.writeText(base64Img, Charsets.UTF_8)
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        showNotification()
        if (intent.action == null) {

            resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, resultCode)
            resultData = intent.getParcelableExtra(EXTRA_RESULT_INTENT)

            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData!!)
            takeScreenshot()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onDestroy() {
        mediaProjection.stop()

        val broadcastIntent = Intent().apply {
            action = ACTION_SCREENSHOT_SERVICE_DESTROYED
        }
        sendBroadcast(broadcastIntent)
    }
}