package com.zy.ppmusic.extension

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*

fun View.addOnClickListener(listener: View.OnClickListener) {
    if (context is AppCompatActivity) {
        addOnClickListener((context as AppCompatActivity).lifecycle, listener)
    }
}

fun View.addOnClickListener(lifecycle: Lifecycle, listener: View.OnClickListener) {
    lifecycle.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            setOnClickListener(null)
            lifecycle.removeObserver(this)
        }
    })
    setOnClickListener(listener)
}
