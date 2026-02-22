package com.example.helloworld

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CubeRenderer(private val onFpsUpdate: (Int) -> Unit) : GLSurfaceView.Renderer {

    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val rotationMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    private var cube: Cube? = null
    private var angle = 0f

    // FPS tracking variables
    private var frameCount = 0
    private var lastTime = System.currentTimeMillis()

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST) // Enable depth testing for 3D
        cube = Cube()
    }

    override fun onDrawFrame(unused: GL10) {
        // Clear color and depth buffers
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -5f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)

        // Reset the Model matrix and apply rotation on multiple axes
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, angle, 1.0f, 1.0f, 0.5f)

        // Calculate the projection and view transformation
        val tempMatrix = FloatArray(16)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        // Draw cube
        cube?.draw(vPMatrix)

        // Update rotation angle
        angle = (angle + 2.0f) % 360.0f

        // Compute FPS
        frameCount++
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTime >= 1000) {
            onFpsUpdate(frameCount)
            frameCount = 0
            lastTime = currentTime
        }
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio: Float = width.toFloat() / height.toFloat()
        // Adjust the frustum (Projection matrix) to screen ratio
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 10f)
    }
}

class Cube {
    private val vertexBuffer: FloatBuffer
    private val colorBuffer: FloatBuffer
    private val indexBuffer: ByteBuffer

    private val vertexShaderCode =
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "attribute vec4 aColor;" +
        "varying vec4 vColor;" +
        "void main() {" +
        "  gl_Position = uMVPMatrix * vPosition;" +
        "  vColor = aColor;" +
        "}"

    private val fragmentShaderCode =
        "precision mediump float;" +
        "varying vec4 vColor;" +
        "void main() {" +
        "  gl_FragColor = vColor;" +
        "}"

    private var mProgram: Int

    init {
        // 8 vertices for a 3D cube
        val vertices = floatArrayOf(
            -1f, -1f, -1f, // 0: Left Bottom Front
             1f, -1f, -1f, // 1: Right Bottom Front
             1f,  1f, -1f, // 2: Right Top Front
            -1f,  1f, -1f, // 3: Left Top Front
            -1f, -1f,  1f, // 4: Left Bottom Back
             1f, -1f,  1f, // 5: Right Bottom Back
             1f,  1f,  1f, // 6: Right Top Back
            -1f,  1f,  1f  // 7: Left Top Back
        )

        // 8 distinct colors for the vertices to create a colorful gradient map
        val colors = floatArrayOf(
            1f, 0f, 0f, 1f, // 0: Red
            0f, 1f, 0f, 1f, // 1: Green
            0f, 0f, 1f, 1f, // 2: Blue
            1f, 1f, 0f, 1f, // 3: Yellow
            1f, 0f, 1f, 1f, // 4: Magenta
            0f, 1f, 1f, 1f, // 5: Cyan
            1f, 1f, 1f, 1f, // 6: White
            0f, 0f, 0f, 1f  // 7: Black
        )

        // Map vertices into 12 triangles (6 square faces)
        val indices = byteArrayOf(
            0, 1, 2,  0, 2, 3,  // Front
            5, 4, 7,  5, 7, 6,  // Back
            4, 0, 3,  4, 3, 7,  // Left
            1, 5, 6,  1, 6, 2,  // Right
            3, 2, 6,  3, 6, 7,  // Top
            4, 5, 1,  4, 1, 0   // Bottom
        )

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply { put(vertices); position(0) }
        }

        colorBuffer = ByteBuffer.allocateDirect(colors.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply { put(colors); position(0) }
        }

        indexBuffer = ByteBuffer.allocateDirect(indices.size).apply {
            put(indices)
            position(0)
        }

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        mProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    fun draw(mvpMatrix: FloatArray) {
        GLES20.glUseProgram(mProgram)

        // Pass position data
        val positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)

        // Pass color data
        val colorHandle = GLES20.glGetAttribLocation(mProgram, "aColor")
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 16, colorBuffer)

        // Pass camera/projection matrix
        val mvpMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // Draw the mapped cube triangles
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 36, GLES20.GL_UNSIGNED_BYTE, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }
}