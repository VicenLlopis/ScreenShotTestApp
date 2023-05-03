package com.example.screenshottestapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.*
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.provider.Settings
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.jraska.falcon.Falcon
import eu.bolt.screenshotty.*
import eu.bolt.screenshotty.util.ScreenshotFileSaver
import java.io.File
import java.io.FileOutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var drawingView : DrawingView
    private lateinit var screenshotBitmap : Bitmap
    private lateinit var canvasBitmap : Canvas
    private val IMAGE_WIDTH = 720
    private val IMAGE_HEIGHT = 1280
    private  lateinit var mediaProjection : MediaProjection
    val displayMetrics = Resources.getSystem().displayMetrics
    private lateinit var imageReader: ImageReader
    private lateinit var mediaProjectionManager:MediaProjectionManager
    lateinit var startMediaProjection: ActivityResultLauncher<Intent>
    private lateinit var screenshotManager : ScreenshotManager

    companion object {
        private const val REQUEST_SCREENSHOT_PERMISSION = 888
        private const val SCREEN_CAPTURE_REQUEST_CODE = 1
        private const val DRAW_OVERLAYS_PERMISSION_REQUEST_CODE = 666
        private const val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams", "MissingInflatedId",
        "ResourceType", "UseCompatLoadingForDrawables"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
        }

        Log.d("Prueba 2","Mensaje para ver si va")

        drawingView = findViewById(R.id.imgView)

        // Obtener las referencias a los botones y configurar sus listeners
        val drawButton = findViewById<ImageView>(R.id.button_draw)
        val clearButton = findViewById<ImageView>(R.id.button_clear)
        val saveButton = findViewById<ImageView>(R.id.button_save)
        val minimizeButton = findViewById<ImageView>(R.id.button_widget)
        val exitButton = findViewById<ImageView>(R.id.button_exit)


        drawButton.setOnClickListener {
            drawingView.enableDrawing(true)
        }

        clearButton.setOnClickListener {
            drawingView.clearDrawing()
        }

        saveButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Guardar")
                .setMessage("¿Está seguro de que desea Guardar la imagen?")
                .setPositiveButton("Sí") { _, _ ->
                    guardarPantalla()
                }
                .setNegativeButton("No", null)
                .show()
        }

        minimizeButton.setOnClickListener {
            startFloatingWidgetMaybe()
            moveTaskToBack(true)
        }

        exitButton.setOnClickListener{
            AlertDialog.Builder(this)
                .setTitle("Salir")
                .setMessage("¿Está seguro de que desea salir de la aplicación?")
                .setPositiveButton("Sí") { _, _ ->
                    closeApp()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    @SuppressLint("WrongConstant")
    private fun guardarPantalla() {

        val file = File(Environment.getExternalStorageDirectory(), "Pictures/screenshot.png")
        Falcon.takeScreenshot(this, file)
        // Take bitmap and do whatever you want
        screenshotBitmap = Falcon.takeScreenshotBitmap(this)

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


    }
    fun writeToFile(screenshot: Screenshot): File {

        val fileSaver = ScreenshotFileSaver.create(Bitmap.CompressFormat.PNG)
        val targetFile = File( Environment.getExternalStorageDirectory(), "Pictures/screenshot.jpg")
        fileSaver.saveToFile(targetFile, screenshot)
        return targetFile
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        screenshotManager.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DRAW_OVERLAYS_PERMISSION_REQUEST_CODE && isDrawOverlaysAllowed()) {
            Toast.makeText(this, "Granted permissions for drawing over apps", Toast.LENGTH_SHORT).show()
            startFloatingWidgetMaybe()
        }


    }

    override fun onStart() {
        stopService(Intent(this, FloatingWidgetService::class.java))
        super.onStart()
    }

    override fun onDestroy() {
        stopService(Intent(this, FloatingWidgetService::class.java))
        Toast.makeText(this, "Stopped floating widget", Toast.LENGTH_SHORT).show()
        super.onDestroy()
    }

    private fun startFloatingWidgetMaybe() {
        if (isDrawOverlaysAllowed()) {
            startService(Intent(this@MainActivity, FloatingWidgetService::class.java))
            Toast.makeText(this, "Widget On", Toast.LENGTH_SHORT).show()
            return
        }
        requestForDrawingOverAppsPermission()
    }

    private fun closeApp(){
        this.finishAffinity()
    }

    private fun requestForDrawingOverAppsPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        startActivityForResult(intent, DRAW_OVERLAYS_PERMISSION_REQUEST_CODE)
    }

    private fun isDrawOverlaysAllowed(): Boolean =
        Settings.canDrawOverlays(this)
}

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {
        private var path = Path()
        private val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 10f
        }
        private var canDraw = false

        fun enableDrawing(enable: Boolean) {
            canDraw = enable
        }

        fun clearDrawing() {
            path.reset()
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawPath(path, paint)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (!canDraw) {
                return false
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    path.moveTo(event.x, event.y)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    path.lineTo(event.x, event.y)
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    return true
                }
            }
            return false
        }
}



