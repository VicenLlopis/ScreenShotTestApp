package com.example.screenshottestapp.views

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Path
import androidx.lifecycle.lifecycleScope
import com.example.screenshottestapp.MainActivity
import com.example.screenshottestapp.data.model.MyPath
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class DrawingViewTest{

    lateinit var drawingView: DrawingView


    @Before
    fun onBefore(){
        drawingView = Mockito.mock(DrawingView::class.java)
    }

    @Test
    fun thePathIsClear(){

        drawingView.myPaths.add(MyPath(Path(),Color.BLACK,8))

        // Llamar a la función clearDrawing()
        drawingView.clearDrawing()

        // Verificar que la lista myPaths esté vacía después de llamar a clearDrawing()
        assertEquals(0, drawingView.myPaths.size)
    }

    @Test
    fun `when exit the path is clean`() {

       val path= drawingView.myPaths.add(MyPath(Path(),Color.RED,3))

        GlobalScope.launch(Dispatchers.IO) {
            drawingView.setPick(Color.BLACK, MainActivity.stroke_m)
        }

        assertEquals(path ,drawingView.myPaths.add(MyPath(Path(),Color.BLACK,8)))

    }



}