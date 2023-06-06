package com.example.screenshottestapp.services

import android.annotation.SuppressLint
import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.provider.MediaStore
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.example.screenshottestapp.MainActivity
import com.example.screenshottestapp.R
import com.example.screenshottestapp.helpers.EmittingObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import com.example.screenshottestapp.helpers.Util
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class ScreenShotService : Service() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjection: MediaProjection
    private var resultCode = 0
    private var resultData: Intent? = null
    private lateinit var fileName : String
    private var isQR= false
    private var isSave=false

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
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private fun createNotification() {
        val serviceChannel = NotificationChannel(
            CHANNEL_DEFAULT_IMPORTANCE, "1", NotificationManager.IMPORTANCE_DEFAULT
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
        Log.d("Prueba Error Pantalla", "1")
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
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

                if (isQR) {
                    encodeImage(bitmap)
                } else if (isSave) {
                    saveScreenshotToGallery(this, bitmap)
                }

                isQR=false
                isSave=false

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

            Toast.makeText(
                this, R.string.string_Qr_Message_Ok, Toast.LENGTH_SHORT
            ).show()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun saveScreenshotToGallery(context: Context, screenshotBitmap: Bitmap) {
        mediaProjection.stop()
        val directory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val marcadorDirectory = File(directory, "Marcador")
        marcadorDirectory.mkdirs()

        val relativeLocation = Environment.DIRECTORY_PICTURES + File.separator + "Marcador"

            GlobalScope.launch(Dispatchers.IO) {
            Log.d("TESTSCREEN","Entra1")
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                put(MediaStore.Images.Media.RELATIVE_PATH, relativeLocation)
            }

            Log.d("TESTSCREEN","Entra2")

            val imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                Log.d("TESTIMAGEURI","${imageUri?.path}")

            if (imageUri != null) {
                Log.d("TESTSCREEN","Entra3")

                context.contentResolver.openOutputStream(imageUri).use { outputStream ->
                    screenshotBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context, R.string.string_Save_OK, Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context, R.string.string_Save_ERROR, Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        fileName = intent.getStringExtra(Util.keyFileName).toString()
        isQR=intent.getBooleanExtra(Util.keyIsQr,false)
        isSave=intent.getBooleanExtra(Util.keyIsSave,false)

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

        GlobalScope.launch(Dispatchers.IO) {
            EmittingObject.produceEvent(ACTION_SCREENSHOT_SERVICE_DESTROYED)
        }
    }
}