package com.example.helloworld

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        
        val title = TextView(this).apply {
            text = "Select Graphics API"
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 50)
        }
        
        val btnOpenGl = Button(this).apply {
            text = "OpenGL (Rotating Cube)"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, RenderActivity::class.java).apply {
                    putExtra("API", "OPENGL")
                })
            }
        }
        
        val btnVulkan = Button(this).apply {
            text = "Vulkan"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, RenderActivity::class.java).apply {
                    putExtra("API", "VULKAN")
                })
            }
        }
        
        layout.addView(title)
        layout.addView(btnOpenGl)
        layout.addView(btnVulkan)
        
        setContentView(layout)
    }
}