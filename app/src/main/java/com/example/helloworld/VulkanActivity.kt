package com.example.helloworld

import android.os.Bundle
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class VulkanActivity : AppCompatActivity(), SurfaceHolder.Callback {

code
Code
download
content_copy
expand_less
private lateinit var surfaceView: SurfaceView
private lateinit var fpsText: TextView
private var isRendering = false
private var renderThread: Thread? = null

companion object {
    init {
        System.loadLibrary("vulkancube")
    }
}

private external fun initVulkan(surface: Surface): Boolean
private external fun renderFrame(): Boolean
private external fun cleanupVulkan()

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_render)

    val container = findViewById<FrameLayout>(R.id.render_container)
    fpsText = findViewById(R.id.tv_fps)

    surfaceView = SurfaceView(this)
    surfaceView.holder.addCallback(this)
    
    container.addView(surfaceView, 0, ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, 
        ViewGroup.LayoutParams.MATCH_PARENT
    ))
    
    fpsText.text = "Vulkan FPS: 0\n(Native C++ Shifting Color)"
}

override fun surfaceCreated(holder: SurfaceHolder) {
    if (initVulkan(holder.surface)) {
        isRendering = true
        renderThread = Thread {
            var frames = 0
            var lastTime = System.currentTimeMillis()
            while (isRendering) {
                if (renderFrame()) frames++
                val now = System.currentTimeMillis()
                if (now - lastTime >= 1000) {
                    val fps = frames
                    frames = 0
                    lastTime = now
                    runOnUiThread { 
                        fpsText.text = "Vulkan FPS: $fps\n(Native C++ View)" 
                    }
                }
            }
        }
        renderThread?.start()
    } else {
        runOnUiThread { fpsText.text = "Vulkan Init Failed! Does your device support it?" }
    }
}

override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

override fun surfaceDestroyed(holder: SurfaceHolder) {
    isRendering = false
    renderThread?.join()
    cleanupVulkan()
}

}