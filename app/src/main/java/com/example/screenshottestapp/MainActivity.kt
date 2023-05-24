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
import android.opengl.Visibility
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorListener
import com.skydoves.colorpickerview.sliders.AlphaSlideBar
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar


class MainActivity : AppCompatActivity() {

    private lateinit var drawingView : DrawingView
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var one : LinearLayout
    private lateinit var drawButton : ImageView
    private lateinit var saveButton : ImageView
    private lateinit var minimizeButton : ImageView
    private lateinit var exitButton : ImageView
    private lateinit var clearButton : ImageView
    private lateinit var qrButton : ImageView
    private lateinit var drawOverlaysLauncher: ActivityResultLauncher<Intent>
    private lateinit var screenshotLauncher: ActivityResultLauncher<Intent>
    private lateinit var screenshot2Launcher: ActivityResultLauncher<Intent>
    private  var writePermisionBool :Boolean = false
    private lateinit var fileName :String
    private lateinit var imgView :ImageView
    private var colorSelected : Int =0
    private var alphaSlideBar: AlphaSlideBar? = null
    private var brightnessSlideBar: BrightnessSlideBar? = null

    private lateinit var bttnPicker :Button
    private lateinit var bttnStrokeS :ImageView
    private lateinit var bttnStrokeM :ImageView
    private lateinit var bttnStrokeL :ImageView



    companion object {

       private const val DRAW_OVERLAYS_PERMISSION_REQUEST_CODE = 666
        private const val REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 1
        private const val stroke_s = 3
        private const val stroke_m = 8
        private const val stroke_l = 18


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
        qrButton = findViewById(R.id.button_QR)



       drawButton.setOnClickListener {

           val colorPickerLayout = layoutInflater.inflate(R.layout.flag_layout, null)
           val colorPickerView = colorPickerLayout.findViewById<ColorPickerView>(R.id.colorPickerView)
           val alphaSlideBar = colorPickerLayout.findViewById<AlphaSlideBar>(R.id.alphaSlideBar)
           bttnStrokeL = colorPickerLayout.findViewById(R.id.bttn_stroke_L)
           bttnStrokeM= colorPickerLayout.findViewById(R.id.bttn_stroke_M)
           bttnStrokeS=colorPickerLayout.findViewById(R.id.bttn_stroke_S)

           bttnStrokeL.setOnClickListener {
               drawingView.setStroke(stroke_l)
           }
           bttnStrokeS.elevation
           bttnStrokeS.setOnClickListener {
               drawingView.setStroke(stroke_s)
           }
           bttnStrokeM.setOnClickListener {
               drawingView.setStroke(stroke_m)
           }

           bttnPicker = colorPickerLayout.findViewById(R.id.bttn_picker)
           val brightnessSlideBar = colorPickerLayout.findViewById<BrightnessSlideBar>(R.id.brightnessSlide)
           var dialog = Dialog(this)
           dialog.setContentView(colorPickerLayout)

            colorPickerView.setColorListener(ColorListener{ color,_ ->
                colorSelected = color
                drawingView.setColor(colorSelected)

            })
           bttnPicker.setOnClickListener {
               dialog.dismiss()

           }
           colorPickerView.setInitialColor(colorSelected)
           colorPickerView.attachBrightnessSlider(brightnessSlideBar)
           colorPickerView.attachAlphaSlider(alphaSlideBar)
           //colorPickerView.brightnessSlider
          // colorPickerView.alphaSlideBar

           dialog.show()

       }

       clearButton.setOnClickListener {
           drawingView.clearDrawing()
       }

       minimizeButton.setOnClickListener {
            startFloatingWidgetMaybe()
            moveTaskToBack(true)
       }
        qrButton.setOnClickListener {
            showDialogQrButton()
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

    private fun showDialogQrButton() {

        one.visibility = View.GONE

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        if (writePermisionBool) {
            val intent = mediaProjectionManager.createScreenCaptureIntent()
            screenshot2Launcher.launch(intent)
        }


        val dialog2 = Dialog(this)
        dialog2.setContentView(R.layout.dialog_qr)
        dialog2.setCancelable(false)
        imgView = dialog2.findViewById(R.id.qr_img)

        val url ="https://www.marca.com/"

          try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                url, BarcodeFormat.QR_CODE, 500, 500
            )

            val ancho = bitMatrix.width
            val alto = bitMatrix.height
            val pixels = IntArray(ancho * alto)

            for (y in 0 until alto) {
                val offset = y * ancho
                for (x in 0 until ancho) {
                    pixels[offset + x] = if (bitMatrix[x, y]) {
                        // Color del código QR
                        0xFF000000.toInt()
                    } else {
                        // Color de fondo
                        0xFFFFFFFF.toInt()
                    }
                }
            }

            val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, ancho, 0, 0, ancho, alto)

              imgView.setImageBitmap(bitmap)

        } catch (e: WriterException) {
            e.printStackTrace()
        }
        dialog2.show()


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
         screenshot2Launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
             val resultCode = result.resultCode
             val data = result.data
             if (resultCode == Activity.RESULT_OK) {
                 val i = Intent(this, ScreenShotQrService::class.java)
                     .putExtra(ScreenShotQrService.EXTRA_RESULT_CODE, resultCode)
                     .putExtra(ScreenShotQrService.EXTRA_RESULT_INTENT, data)
                 Log.d("TestMediaProjection", "$resultCode, $data")

                 startService(i)
             }
         }
         if (writePermisionBool) {
             val intentFilter = IntentFilter().apply {
                 addAction(ScreenShotQrService.ACTION_SCREENSHOT_SERVICE_DESTROYED)
             }
             registerReceiver(screenshotServiceDestroyedReceiver, intentFilter)
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

            if (intent.action == ScreenShotQrService.ACTION_SCREENSHOT_SERVICE_DESTROYED) {
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

    private val paths = mutableListOf<Path>()
    private val colors = mutableListOf<Int>()
    private val strokes = mutableListOf<Int>()

    private var currentPath: Path? = null
    private var currentColor: Int = Color.BLACK
    private var currentStroke : Int = 10

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    private var canDraw = true

    fun enableDrawing(enable: Boolean) {
        canDraw = enable
    }

    fun clearDrawing() {
        paths.clear()
        colors.clear()
        invalidate()
    }

    fun setColor(color: Int) {
        currentColor = color
    }
    fun setStroke(stroke: Int) {
        currentStroke = stroke
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (i in paths.indices) {
            paint.color = colors[i]
            paint.strokeWidth = strokes[i].toFloat()
            canvas.drawPath(paths[i], paint)
        }
        currentPath?.let {
            paint.color = currentColor
            paint.strokeWidth = currentStroke.toFloat()
            canvas.drawPath(it, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!canDraw) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path()
                currentPath?.moveTo(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath?.lineTo(event.x, event.y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                currentPath?.let {
                    paths.add(it)
                    colors.add(currentColor)
                    strokes.add(currentStroke)
                }
                currentPath = null
                return true
            }
        }
        return false
    }
}



