package com.example.helloworld

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
setContentView(R.layout.activity_main)

code
Code
download
content_copy
expand_less
findViewById<Button>(R.id.btn_opengl).setOnClickListener {
        startActivity(Intent(this, OpenGLActivity::class.java))
    }

    findViewById<Button>(R.id.btn_vulkan).setOnClickListener {
        startActivity(Intent(this, VulkanActivity::class.java))
    }
}

}