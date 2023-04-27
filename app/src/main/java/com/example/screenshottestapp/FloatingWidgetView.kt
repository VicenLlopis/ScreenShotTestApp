package com.example.screenshottestapp

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible

class FloatingWidgetView : ConstraintLayout, View.OnClickListener {

    constructor(context : Context) : super(context)
    constructor(context : Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context : Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )

    private var x : Int = 0
    private var y : Int = 315

    private val windowManager : WindowManager

    init {
        View.inflate(context, R.layout.floating_overlay_button, this)
        setOnClickListener(this)

        layoutParams.x = x
        layoutParams.y = y

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(this, layoutParams)
    }

    override fun onClick(p0: View?) {
        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName))

    }
}
