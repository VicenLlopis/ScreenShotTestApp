package com.example.screenshottestapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var drawingView : DrawingView
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var one : LinearLayout
    private lateinit var drawButton : ImageView
    private lateinit var saveButton : ImageView
    private lateinit var minimizeButton : ImageView
    private lateinit var exitButton : ImageView
    private lateinit var clearButton : ImageView
    private lateinit var drawOverlaysLauncher: ActivityResultLauncher<Intent>
    private lateinit var screenshotLauncher: ActivityResultLauncher<Intent>
    private  var writePermisionBool :Boolean = false
    private lateinit var fileName :String

    companion object {

       private const val DRAW_OVERLAYS_PERMISSION_REQUEST_CODE = 666
        private const val REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 1

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

       drawingView = findViewById(R.id.imgView)

       // Obtener las referencias a los botones y configurar sus listeners
       drawButton = findViewById(R.id.button_draw)
       clearButton = findViewById(R.id.button_clear)
       saveButton = findViewById(R.id.button_save)
       minimizeButton = findViewById(R.id.button_widget)
       exitButton = findViewById(R.id.button_exit)

       drawButton.setOnClickListener {
           drawingView.enableDrawing(true)
       }

       clearButton.setOnClickListener {
           drawingView.clearDrawing()
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
    @SuppressLint("ResourceType")
    fun showAlertDialogButtonClicked() {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.layout_dialog)
        dialog.setCancelable(false)

        val nameFile: EditText = dialog.findViewById(R.id.editText)
        val bttnCancel = dialog.findViewById<Button>(R.id.button)
        val bttnAccept = dialog.findViewById<Button>(R.id.button2)

        bttnAccept.setOnClickListener {
            fileName = nameFile.text.toString()

            try {
                one.visibility = View.GONE
            } catch (e: Exception) {
                Log.d("Error vistaOne", "Error")
            }

            mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

            if (writePermisionBool) {
                val intent = mediaProjectionManager.createScreenCaptureIntent()
                screenshotLauncher.launch(intent)
            }

            dialog.dismiss()
        }

        bttnCancel.setOnClickListener {
            one.visibility = View.VISIBLE
            dialog.dismiss()
        }

        dialog.show()

    }

     override fun onStart() {
        stopService(Intent(this, FloatingWidgetService::class.java))
        super.onStart()

         if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
             ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION)
             Log.d("Permisos Write", "Pedir permiso")

         }else{
             writePermisionBool = true
         }

         drawOverlaysLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
             val resultCode = result.resultCode

             Log.d("Prueba guardar0", "Guarda captura")

             if (resultCode == Activity.RESULT_OK) {
                 if (isDrawOverlaysAllowed()) {
                     startFloatingWidgetMaybe()
                 }
             }
         }

         // Verificar y solicitar permisos de dibujar sobre otras aplicaciones
         if (!Settings.canDrawOverlays(this)) {
             val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
             drawOverlaysLauncher.launch(intent)
         }

         // Solicitar permisos de captura de pantalla
         screenshotLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
             val resultCode = result.resultCode
             val data = result.data

             if (resultCode == Activity.RESULT_OK) {
                 val i = Intent(this, ScreenShotService::class.java)
                     .putExtra(ScreenShotService.EXTRA_RESULT_CODE, resultCode)
                     .putExtra(ScreenShotService.EXTRA_RESULT_INTENT, data)
                     .putExtra("fileName", fileName)
                 Log.d("TestMediaProjection","$resultCode, $data")

                 startService(i)
             }
         }

         if (writePermisionBool) {
             val intentFilter = IntentFilter().apply {
                addAction(ScreenShotService.ACTION_SCREENSHOT_SERVICE_DESTROYED)
             }
             registerReceiver(screenshotServiceDestroyedReceiver, intentFilter)
         }

         one = findViewById<View>(R.id.linearLayout) as LinearLayout
         Log.d("Error vistaOne","$one")
         saveButton.setOnClickListener {

             showAlertDialogButtonClicked()

         }
     }
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenshotServiceDestroyedReceiver)
        stopService(Intent(this, FloatingWidgetService::class.java))
        closeApp()
    }

    private fun closeApp(){
        this.finishAffinity()
    }

     private fun startFloatingWidgetMaybe() {
        if (isDrawOverlaysAllowed()) {
            startService(Intent(this@MainActivity, FloatingWidgetService::class.java))
            return
        }
        requestForDrawingOverAppsPermission()
     }

    private val screenshotServiceDestroyedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ScreenShotService.ACTION_SCREENSHOT_SERVICE_DESTROYED) {
                one.visibility = View.VISIBLE
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                writePermisionBool = true
            }
        }
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



