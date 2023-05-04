package com.example.screenshottestapp

import android.annotation.SuppressLint
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat.startActivityForResult

class ScreenShotService : Service() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjection: MediaProjection
    private lateinit var screenshotBitmap : Bitmap


    companion object {
        private const val REQUEST_SCREENSHOT_PERMISSION = 888

    }

    override fun onCreate() {
        super.onCreate()

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val permissionIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(permissionIntent,
            REQUEST_SCREENSHOT_PERMISSION)


    }

    private fun startActivityForResult(permissionIntent: Intent?, requestScreenshotPermission: Int) {

        if (requestScreenshotPermission == REQUEST_SCREENSHOT_PERMISSION) {
            mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(requestScreenshotPermission, permissionIntent!!)
        }
    }

    @SuppressLint("WrongConstant")
    private fun guardarPantalla()
    {
        val display = windowManager.defaultDisplay
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        val size = Point()
        display.getRealSize(size)
        val mWidth = size.x
        val mHeight = size.y
        val mDensity = metrics.densityDpi
        val mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2)
        val handler = Handler()

        val flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC

        mediaProjection.createVirtualDisplay(
            "screen-mirror",
            mWidth,
            mHeight,
            mDensity,
            flags,
            mImageReader.surface,
            null,
            handler
        )

        mImageReader.setOnImageAvailableListener({ reader ->
            reader.setOnImageAvailableListener(null, handler)
            val image = reader.acquireLatestImage()
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * metrics.widthPixels
            // create bitmap

            val bmp = Bitmap.createBitmap(
                metrics.widthPixels + (rowPadding.toFloat() / pixelStride.toFloat()).toInt(),
                metrics.heightPixels,
                Bitmap.Config.ARGB_8888
            )

            bmp.copyPixelsFromBuffer(buffer)
            screenshotBitmap = Bitmap.createBitmap(bmp, 0, 0, metrics.widthPixels, bmp.height)
            Log.d("Prueba guardar0","Bystes de el bitmap ${screenshotBitmap.byteCount}")
            bmp.recycle()
            image.close()
            reader.close()

            val imageFileName = "my_image.png"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            }

            val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                contentResolver.openOutputStream(imageUri).use { outputStream ->
                    screenshotBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    Toast.makeText(this, "Fondo de pantalla guardado en la galería de fotos", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No se pudo guardar el fondo de pantalla", Toast.LENGTH_SHORT).show()
            }
        }, handler)
    }


    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}