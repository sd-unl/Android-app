package com.example.helloworld

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class OpenGLActivity : AppCompatActivity() {
private lateinit var glView: GLSurfaceView
private lateinit var fpsText: TextView
private var frameCount = 0
private var lastTime = System.currentTimeMillis()

code
Code
download
content_copy
expand_less
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_render)

    val container = findViewById<FrameLayout>(R.id.render_container)
    fpsText = findViewById(R.id.tv_fps)

    glView = GLSurfaceView(this)
    glView.setEGLContextClientVersion(2)
    
    val renderer = CubeRenderer {
        frameCount++
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTime >= 1000) {
            val fps = frameCount
            frameCount = 0
            lastTime = currentTime
            runOnUiThread { fpsText.text = "GL FPS: $fps" }
        }
    }
    
    glView.setRenderer(renderer)
    // Insert view behind the FPS text
    container.addView(glView, 0, ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, 
        ViewGroup.LayoutParams.MATCH_PARENT
    ))
}

override fun onPause() {
    super.onPause()
    glView.onPause()
}

override fun onResume() {
    super.onResume()
    glView.onResume()
}

}