package com.anwesh.uiprojects.linkedsiderectedgeview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.siderectedgeview.SideRectEdgeView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SideRectEdgeView.create(this)
    }
}
