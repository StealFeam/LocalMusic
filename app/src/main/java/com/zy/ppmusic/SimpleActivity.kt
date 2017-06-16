package com.zy.ppmusic

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView

/**
 * Created by ZhiTouPC on 2017/6/16.
 */

class SimpleActivity : AppCompatActivity() {

    private var simple: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        simple = findViewById(R.id.main_menu_title) as TextView
        simple!!.text = "simple"
    }
}
