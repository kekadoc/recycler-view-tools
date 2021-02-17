package com.kekadoc.tools.android.view.recyclerview

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import com.kekadoc.tools.observer.Observing

// TODO: 15.02.2021 Обновить android-tools
fun Lifecycle.addObserving(observer: LifecycleObserver): Observing {
    addObserver(observer)
    return object : Observing {
        override fun remove() {
            removeObserver(observer)
        }
    }
}