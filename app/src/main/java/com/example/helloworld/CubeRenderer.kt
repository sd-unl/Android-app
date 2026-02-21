package com.example.helloworld

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CubeRenderer(private val onFrameDrawn: () -> Unit) : GLSurfaceView.Renderer {

code
Code
download
content_copy
expand_less
private val vPMatrix = FloatArray(16)
private val projectionMatrix = FloatArray(16)
private val viewMatrix = FloatArray(16)
private val rotationMatrix = FloatArray(16)
private val modelMatrix = FloatArray(16)

private lateinit var cube: Cube
private var angleX = 0f
private var angleY = 0f

override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
    GLES20.glClearColor(0.05f, 0.05f, 0.05f, 1.0f)
    GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    cube = Cube()
}

override fun onDrawFrame(unused: GL10) {
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

    // Set camera position
    Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -6f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)

    // Apply Rotations
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.rotateM(modelMatrix, 0, angleX, 1f, 0f, 0f)
    Matrix.rotateM(modelMatrix, 0, angleY, 0f, 1f, 0f)

    val scratch = FloatArray(16)
    Matrix.multiplyMM(scratch, 0, viewMatrix, 0, modelMatrix, 0)
    Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, scratch, 0)

    cube.draw(vPMatrix)

    // Increment rotation logic for animation
    angleX += 0.8f
    angleY += 1.2f
    onFrameDrawn()
}

override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
    GLES20.glViewport(0, 0, width, height)
    val ratio: Float = width.toFloat() / height.toFloat()
    Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 10f)
}

}

class Cube {
private val vertexShaderCode = """
uniform mat4 uMVPMatrix;
attribute vec4 vPosition;
attribute vec4 aColor;
varying vec4 vColor;
void main() {
gl_Position = uMVPMatrix * vPosition;
vColor = aColor;
}
""".trimIndent()

code
Code
download
content_copy
expand_less
private val fragmentShaderCode = """
    precision mediump float;
    varying vec4 vColor;
    void main() {
        gl_FragColor = vColor;
    }
""".trimIndent()

private val vertexBuffer: FloatBuffer
private val colorBuffer: FloatBuffer
private val program: Int

// 8 Vertices of the Cube
private val cubeCoords = floatArrayOf(
    -1f, -1f,  1f,   1f, -1f,  1f,   1f,  1f,  1f,  -1f,  1f,  1f,
    -1f, -1f, -1f,   1f, -1f, -1f,   1f,  1f, -1f,  -1f,  1f, -1f
)

// Colors assigned to each vertex
private val colors = floatArrayOf(
    1f, 0f, 0f, 1f,  0f, 1f, 0f, 1f,  0f, 0f, 1f, 1f,  1f, 1f, 0f, 1f,
    1f, 0f, 1f, 1f,  0f, 1f, 1f, 1f,  1f, 1f, 1f, 1f,  0f, 0f, 0f, 1f
)

// Triangles formation index map
private val drawOrder = shortArrayOf(
    0, 1, 2, 0, 2, 3, // Front
    1, 5, 6, 1, 6, 2, // Right
    5, 4, 7, 5, 7, 6, // Back
    4, 0, 3, 4, 3, 7, // Left
    3, 2, 6, 3, 6, 7, // Top
    4, 5, 1, 4, 1, 0  // Bottom
)

private val indexBuffer: java.nio.ShortBuffer

init {
    vertexBuffer = ByteBuffer.allocateDirect(cubeCoords.size * 4).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply { put(cubeCoords); position(0) }
    }

    colorBuffer = ByteBuffer.allocateDirect(colors.size * 4).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply { put(colors); position(0) }
    }

    indexBuffer = ByteBuffer.allocateDirect(drawOrder.size * 2).run {
        order(ByteOrder.nativeOrder())
        asShortBuffer().apply { put(drawOrder); position(0) }
    }

    val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
    val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

    program = GLES20.glCreateProgram().also {
        GLES20.glAttachShader(it, vertexShader)
        GLES20.glAttachShader(it, fragmentShader)
        GLES20.glLinkProgram(it)
    }
}

fun draw(mvpMatrix: FloatArray) {
    GLES20.glUseProgram(program)

    val positionHandle = GLES20.glGetAttribLocation(program, "vPosition").also {
        GLES20.glEnableVertexAttribArray(it)
        GLES20.glVertexAttribPointer(it, 3, GLES20.GL_FLOAT, false, 3 * 4, vertexBuffer)
    }

    val colorHandle = GLES20.glGetAttribLocation(program, "aColor").also {
        GLES20.glEnableVertexAttribArray(it)
        GLES20.glVertexAttribPointer(it, 4, GLES20.GL_FLOAT, false, 4 * 4, colorBuffer)
    }

    GLES20.glGetUniformLocation(program, "uMVPMatrix").also {
        GLES20.glUniformMatrix4fv(it, 1, false, mvpMatrix, 0)
    }

    GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

    GLES20.glDisableVertexAttribArray(positionHandle)
    GLES20.glDisableVertexAttribArray(colorHandle)
}

private fun loadShader(type: Int, shaderCode: String): Int {
    return GLES20.glCreateShader(type).also { shader ->
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
    }
}

}