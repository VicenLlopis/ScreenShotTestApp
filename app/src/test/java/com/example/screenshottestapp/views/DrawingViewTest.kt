package com.example.screenshottestapp.views

import android.graphics.Color
import android.graphics.Path
import com.example.screenshottestapp.data.model.MyPath
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
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


}