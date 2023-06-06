package com.example.screenshottestapp.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.screenshottestapp.data.model.MyCurrentPath
import com.example.screenshottestapp.data.model.MyPath
import com.example.screenshottestapp.data.dataStore
import com.example.screenshottestapp.helpers.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val colorPref = intPreferencesKey(Util.keyDataStoreColor)
    private val strokePref = intPreferencesKey(Util.keyDataStoreStroke)

    val myPaths = mutableListOf<MyPath>()
    private var colorSelected: Int = 0
    private var selectedStroke: Int = 0
    private var savedPath: Path? = null


    private var currentPath: MyCurrentPath = if (myPaths.isNotEmpty()) {

        MyCurrentPath(Path(), Color.BLACK, 8)

    } else {
        GlobalScope.launch(Dispatchers.IO) {
            getDataPick().collect {
                withContext(Dispatchers.Main) {
                    colorSelected = it.color
                    selectedStroke = it.stroke
                    savedPath = it.path
                    setColor(colorSelected)
                    setStroke(selectedStroke)
                    setPath(savedPath!!)
                }
            }
        }

        MyCurrentPath(Path(), colorSelected, selectedStroke)
    }


    fun getDataPick() = context.dataStore.data.map { preferences ->
        MyPath(
            color = preferences[colorPref] ?: 0,
            stroke = preferences[strokePref] ?: 0,
            path = Path()
        )
    }

    suspend fun setPick(color: Int, stroke: Int) {
        context.dataStore.edit { pref ->
            pref[colorPref] = color
            pref[strokePref] = stroke
        }
    }

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private var canDraw = true

    fun clearDrawing() {
        myPaths.clear()
        invalidate()
    }

    fun setColor(color: Int) {
        currentPath.currenColor = color
    }

    fun setStroke(stroke: Int) {
        currentPath.currenStroke = stroke
    }

    private fun setPath(path: Path) {
        currentPath.path = path
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (i in myPaths.indices) {
            paint.color = myPaths[i].color
            paint.strokeWidth = myPaths[i].stroke.toFloat()
            canvas.drawPath(myPaths[i].path, paint)
        }

        currentPath.let {
            paint.color = currentPath.currenColor
            paint.strokeWidth = currentPath.currenStroke.toFloat()
            canvas.drawPath(currentPath.path, paint)
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!canDraw) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath.path = Path()
                currentPath.path.moveTo(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath.path.lineTo(event.x, event.y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                currentPath.let {
                    myPaths.add(
                        MyPath(
                            currentPath.path,
                            currentPath.currenColor,
                            currentPath.currenStroke
                        )
                    )
                }
                currentPath.path = Path()
                return true
            }
        }
        return false
    }
}