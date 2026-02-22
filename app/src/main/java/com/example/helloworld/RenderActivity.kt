package com.example.helloworld

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.Typeface

class RenderActivity : AppCompatActivity() {
    private lateinit var fpsTextView: TextView
    private var glView: GLSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val api = intent.getStringExtra("API") ?: "OPENGL"

        val frameLayout = FrameLayout(this)

        if (api == "OPENGL") {
            glView = GLSurfaceView(this).apply {
                setEGLContextClientVersion(2)
                // Request a 16-bit depth buffer to render the 3D cube correctly
                setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                setRenderer(CubeRenderer { fps ->
                    runOnUiThread { fpsTextView.text = "FPS: $fps | API: OpenGL" }
                })
            }
            frameLayout.addView(glView)
        } else {
            // Vulkan Stub
            val stubView = TextView(this).apply {
                text = "Vulkan API Selected\n\n(A pure Vulkan implementation requires the Android NDK, a C++ swapchain, and compiled SPIR-V shaders, which translates to ~2000 lines of boilerplate!\n\nPlease test the OpenGL option to see the rotating 3D cube and FPS counter.)"
                textSize = 18f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.DKGRAY)
                gravity = Gravity.CENTER
                setPadding(60, 60, 60, 60)
            }
            frameLayout.addView(stubView)
        }

        fpsTextView = TextView(this).apply {
            text = "FPS: ... | API: $api"
            textSize = 20f
            setTextColor(Color.GREEN)
            setTypeface(null, Typeface.BOLD)
            setPadding(40, 40, 40, 40)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }
        }
        frameLayout.addView(fpsTextView)

        setContentView(frameLayout)
    }

    override fun onPause() {
        super.onPause()
        glView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        glView?.onResume()
    }
}