package com.example.screenshottestapp.helpers

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EmittingObject {

    private val _destroyService = MutableSharedFlow<String>() // private mutable shared flow
    val eventsDestroyService = _destroyService.asSharedFlow()

    suspend fun produceEvent(event: String) {
        _destroyService.emit(event) // suspends until all subscribers receive it
    }

}