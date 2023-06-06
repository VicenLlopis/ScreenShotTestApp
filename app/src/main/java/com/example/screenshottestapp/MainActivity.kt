package com.example.screenshottestapp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.*
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.screenshottestapp.helpers.EmittingObject
import com.example.screenshottestapp.helpers.Util
import com.example.screenshottestapp.services.FloatingWidgetService
import com.example.screenshottestapp.services.ScreenShotService
import com.example.screenshottestapp.views.DrawingView
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorListener
import com.skydoves.colorpickerview.sliders.AlphaSlideBar
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var one: LinearLayout
    private lateinit var drawButton: ImageView
    private lateinit var saveButton: ImageView
    private lateinit var minimizeButton: ImageView
    private lateinit var exitButton: ImageView
    private lateinit var clearButton: ImageView
    private lateinit var qrButton: ImageView
    private lateinit var drawOverlaysLauncher: ActivityResultLauncher<Intent>
    private lateinit var screenshotLauncher: ActivityResultLauncher<Intent>
    private var writePermisionBool: Boolean = false
    private  var fileName: String=""
    private lateinit var imgView: ImageView
    private var colorSelected: Int = Color.BLACK
    private lateinit var selectedButton: ImageButton
    private lateinit var bttnPickerOk: Button
    private lateinit var bttnPickerCancel: Button
    private lateinit var bttnStrokeS: ImageButton
    private lateinit var bttnStrokeM: ImageButton
    private lateinit var bttnStrokeL: ImageButton
    private lateinit var dialog2: Dialog
    private val selectedColor = Color.BLACK
    private var selectedStroke = stroke_m
    private val unselectedColor = Color.LTGRAY
    private var isWidget = false
    private var widgetOpen = false
    private var isQR= false
    private var isSave=false

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

        widgetOpen = intent.getBooleanExtra(Util.keyWidgetOpen, false)
        if (!widgetOpen) {
            lifecycleScope.launch(Dispatchers.IO) {
                drawingView.setPick(Color.BLACK, stroke_m)
            }
        }
        lifecycleScope.launch(Dispatchers.IO){
            EmittingObject.eventsDestroyService.collect{
                launch(Dispatchers.Main) {
                    if (it == ScreenShotService.ACTION_SCREENSHOT_SERVICE_DESTROYED) {
                        one.visibility = View.VISIBLE
                        if (isQR) {
                            dialog2.show()
                        }
                        isSave = false
                        isQR = false
                    }
                }
            }
        }
        //Obtener las referencias a los botones y configurar sus listeners
        drawButton = findViewById(R.id.button_draw)
        clearButton = findViewById(R.id.button_clear)
        saveButton = findViewById(R.id.button_save)
        minimizeButton = findViewById(R.id.button_widget)
        exitButton = findViewById(R.id.button_exit)
        qrButton = findViewById(R.id.button_QR)

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.flag_layout)
        dialog.setCancelable(false)

        bttnStrokeL = dialog.findViewById(R.id.bttn_stroke_L)
        bttnStrokeM = dialog.findViewById(R.id.bttn_stroke_M)
        bttnStrokeS = dialog.findViewById(R.id.bttn_stroke_S)

        lifecycleScope.launch(Dispatchers.IO) {
            drawingView.getDataPick().collect {
                withContext(Dispatchers.Main) {
                }
                selectedStroke = it.stroke
            }
        }

        drawButton.setOnClickListener {
            val colorInPicker = colorSelected
            val strokeInPicker = selectedStroke
            var buttonNow:ImageButton = bttnStrokeM

            val colorPickerView = dialog.findViewById<ColorPickerView>(R.id.colorPickerView)

            bttnPickerOk = dialog.findViewById(R.id.bttn_picker_OK)
            bttnPickerCancel = dialog.findViewById(R.id.bttn_picker_Cancel)
            val alphaSlideBar = dialog.findViewById<AlphaSlideBar>(R.id.alphaSlideBar)
            val brightnessSlideBar = dialog.findViewById<BrightnessSlideBar>(R.id.brightnessSlide)

            when (selectedStroke) {
                8 -> {
                    selectedButton = bttnStrokeM

                    selectButton(selectedButton, stroke_m)
                }
                3 -> {
                    selectedButton = bttnStrokeS

                    selectButton(selectedButton, stroke_s)
                }
                18 -> {
                    selectedButton = bttnStrokeL

                    selectButton(selectedButton, stroke_l)
                }
            }

            bttnStrokeL.setOnClickListener {
                selectedStroke = stroke_l
                buttonNow=bttnStrokeL
                selectButton(bttnStrokeL, selectedStroke)

            }

            bttnStrokeM.setOnClickListener {
                selectedStroke = stroke_m
                buttonNow=bttnStrokeM
                selectButton(bttnStrokeM, selectedStroke)

            }

            bttnStrokeS.setOnClickListener {
                selectedStroke = stroke_s
                buttonNow=bttnStrokeS
                selectButton(bttnStrokeS, selectedStroke)

            }

            lifecycleScope.launch(Dispatchers.IO) {
                drawingView.getDataPick().collect {
                    colorSelected = it.color
                    colorPickerView.setInitialColor(colorSelected)
                }
            }
            bttnPickerOk.setText(R.string.string_Acept)
            bttnPickerOk.setOnClickListener {

                lifecycleScope.launch(Dispatchers.IO) {
                    drawingView.setPick(colorSelected, selectedStroke)
                }
                dialog.dismiss()
            }

            colorPickerView.setColorListener(ColorListener { color, _ ->
                colorSelected = color
                drawingView.setColor(colorSelected)
            })
            bttnPickerCancel.setText(R.string.string_Cancel)
            bttnPickerCancel.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    drawingView.setPick(colorInPicker, strokeInPicker)
                    drawingView.setColor(colorInPicker)
                    drawingView.setStroke(strokeInPicker)
                    selectedStroke = strokeInPicker
                    colorSelected = colorInPicker
                    buttonNow.imageTintList = ColorStateList.valueOf(unselectedColor)

                }
                dialog.dismiss()
            }

            colorPickerView.attachBrightnessSlider(brightnessSlideBar)
            colorPickerView.attachAlphaSlider(alphaSlideBar)

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
            isQR=true
            showDialogQrButton()
        }

        exitButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.string_Exit_Title)
                .setMessage(R.string.string_Exit_Message)
                .setPositiveButton(R.string.string_Acept) { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        drawingView.setPick(Color.BLACK, stroke_m)
                    }
                    closeApp()
                }
                .setNegativeButton(R.string.string_Cancel, null)
                .show()
        }
    }

    private fun selectButton(button: ImageButton, strokeSize: Int) {
        selectedButton.isSelected = false
        selectedButton.imageTintList = ColorStateList.valueOf(unselectedColor)
        button.isSelected = true
        button.imageTintList = ColorStateList.valueOf(selectedColor)
        selectedButton = button
        drawingView.setStroke(strokeSize)
    }

    private fun showDialogQrButton() {
        one.visibility = View.GONE

        mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        if (writePermisionBool) {
            val intent = mediaProjectionManager.createScreenCaptureIntent()
            screenshotLauncher.launch(intent)
        }

        dialog2 = Dialog(this)
        dialog2.setContentView(R.layout.dialog_qr)
        dialog2.setCancelable(true)
        imgView = dialog2.findViewById(R.id.qr_img)

        val url = "https://www.marca.com/"

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
    }

    private fun showDialogSave() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.layout_dialog)
        dialog.setCancelable(false)

        val nameFile: EditText = dialog.findViewById(R.id.editText)
        val bttnCancel = dialog.findViewById<Button>(R.id.button)
        val bttnAccept = dialog.findViewById<Button>(R.id.button2)

        bttnAccept.setOnClickListener {
            fileName = nameFile.text.toString()
            isSave=true
            try {
                one.visibility = View.GONE
            } catch (_: Exception) {
            }
            mediaProjectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
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
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION
            )
        } else {
            writePermisionBool = true
        }

        drawOverlaysLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val resultCode = result.resultCode
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
        screenshotLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val resultCode = result.resultCode
                val data = result.data
                if (resultCode == Activity.RESULT_OK) {
                    val i = Intent(this, ScreenShotService::class.java)
                        .putExtra(ScreenShotService.EXTRA_RESULT_CODE, resultCode)
                        .putExtra(ScreenShotService.EXTRA_RESULT_INTENT, data)
                        .putExtra(Util.keyFileName, fileName)
                        .putExtra(Util.keyIsQr,isQR)
                        .putExtra(Util.keyIsSave,isSave)

                    startService(i)
                }
            }


        one = findViewById<View>(R.id.linearLayout) as LinearLayout

        saveButton.setOnClickListener {
            showDialogSave()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, FloatingWidgetService::class.java))
        closeApp()
    }

    private fun closeApp() {
        this.finishAffinity()
    }

    private fun startFloatingWidgetMaybe() {
        if (isDrawOverlaysAllowed()) {
            startService(Intent(this@MainActivity, FloatingWidgetService::class.java))
            return
        }
        isWidget = true
        requestForDrawingOverAppsPermission()
    }




    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                writePermisionBool = true
            }
        }
    }

    private fun requestForDrawingOverAppsPermission() {
        val intent =
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))

        startActivityForResult(intent, DRAW_OVERLAYS_PERMISSION_REQUEST_CODE)
    }

    private fun isDrawOverlaysAllowed(): Boolean =
        Settings.canDrawOverlays(this)
}






