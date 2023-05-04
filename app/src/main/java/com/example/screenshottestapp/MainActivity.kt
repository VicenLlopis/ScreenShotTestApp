package com.example.screenshottestapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import eu.bolt.screenshotty.*
import java.nio.ByteBuffer
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var drawingView : DrawingView



    companion object {
        private const val DRAW_OVERLAYS_PERMISSION_REQUEST_CODE = 666
        private const val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1
    }

    private val REQUEST_MEDIA_PROJECTION = 1



    @SuppressLint("ClickableViewAccessibility", "InflateParams", "MissingInflatedId",
        "ResourceType", "UseCompatLoadingForDrawables"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
        }



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


                }
                .setNegativeButton("No", null)
                .show()
        }

        minimizeButton.setOnClickListener {
            startFloatingWidgetMaybe()
            moveTaskToBack(true)
        }

        exitButton.setOnClickListener {
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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("Prueba guardar0","Guarda captura")



        if (requestCode == DRAW_OVERLAYS_PERMISSION_REQUEST_CODE && isDrawOverlaysAllowed()) {
            Log.d("Prueba guardar1","Guarda captura")

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



