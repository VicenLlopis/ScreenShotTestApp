package com.example.screenshottestapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var drawingView: DrawingView

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Crear un objeto Bitmap vacío
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        // Crear un ImageView y agregarlo a la vista raíz
        imageView = ImageView(this)
        imageView.setImageBitmap(bitmap)
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        rootView.addView(imageView)

        // Crear un DrawingView y agregarlo encima del ImageView
        drawingView = createDrawingView(this, null)
        rootView.addView(drawingView)

    }

    fun createDrawingView(context: Context, attrs: AttributeSet?): DrawingView {
        return DrawingView(context, attrs)
    }

    inner class DrawingView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

        private val path = Path()
        private val paint = Paint()

        init {
            paint.color = Color.BLACK
            paint.strokeWidth = 10f
            paint.style = Paint.Style.STROKE
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeCap = Paint.Cap.ROUND
        }

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

            // Dibujar la captura de pantalla
            val rootView = window.decorView.findViewById<View>(android.R.id.content)
            val bitmap = Bitmap.createBitmap(
                rootView.width, rootView.height, Bitmap.Config.ARGB_8888
            )
            val canvasBitmap = Canvas(bitmap)
            rootView.draw(canvasBitmap)
            canvas.drawBitmap(bitmap, 0f, 0f, null)

            // Dibujar la línea
            canvas.drawPath(path, paint)
        }
    }
}
