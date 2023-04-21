package com.example.screenshottestapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var drawingView: DrawingView
    private lateinit var screenshotBitmap: Bitmap

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        // Crear un ImageView y agregarlo a la vista raíz
        imageView = ImageView(this)
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        rootView.addView(imageView)

        // Tomar una captura de pantalla y mostrarla en el ImageView
        rootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                screenshotBitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
                val canvasBitmap = Canvas(screenshotBitmap)
                rootView.draw(canvasBitmap)
                imageView.setImageBitmap(screenshotBitmap)
            }
        })

        // Crear un DrawingView y agregarlo encima del ImageView
        drawingView = DrawingView(this)
        rootView.addView(drawingView)
    }

    inner class DrawingView(context: Context) : View(context) {

        private val path = Path()
        private val paint = Paint()

        init {
            paint.color = Color.BLACK
            paint.strokeWidth = 10f
            paint.style = Paint.Style.STROKE
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeCap = Paint.Cap.ROUND
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    path.moveTo(event.x, event.y)
                }
                MotionEvent.ACTION_MOVE -> {
                    path.lineTo(event.x, event.y)
                }
            }

            invalidate()

            return true
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            canvas.drawBitmap(screenshotBitmap, 0f, 0f, null)

            canvas.drawPath(path, paint)
        }
    }
}
