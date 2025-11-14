package com.samsung.android.scan3d.http

import android.content.Context
import android.util.Log
import com.samsung.android.scan3d.util.SettingsManager
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import java.io.OutputStream

class HttpService(private val context: Context) {
    private lateinit var engine: NettyApplicationEngine
    private lateinit var imageChannel: Channel<ByteArray>

    private var currentPort: Int = 0

    /**
     * A suspendable lambda that consumes image data from a channel and writes it to an
     * OutputStream in MJPEG format. This continues until the client disconnects.
     */
    private fun producer(): suspend OutputStream.() -> Unit = {
        val outputStream = this

        try {
            imageChannel.consumeEach { frameData ->
                outputStream.write("--FRAME\r\nContent-Type: image/jpeg\r\n\r\n".toByteArray())
                outputStream.write(frameData)
                outputStream.flush()
            }
        } catch (_: Exception) {
            // This exception is expected when the client disconnects or the channel is closed.
            // No action is needed.
        }
    }

    /**
     * Initializes and starts the Ktor embedded server.
     */
    fun start() {
        imageChannel = Channel(Channel.CONFLATED)
        if (currentPort == 0) {
            currentPort = SettingsManager.loadPort(context)
        }

        engine = embeddedServer(Netty, port = currentPort) {
            routing {
                get("/cam") {
                    call.respondText("Ok")
                }
                get("/cam.mjpeg") {
                    call.respondOutputStream(
                        contentType = ContentType.parse("multipart/x-mixed-replace;boundary=FRAME"),
                        status = HttpStatusCode.OK,
                        producer = producer()
                    )
                }
            }
        }
        engine.start(wait = false)
    }

    /**
     * Gracefully stops the Ktor server engine.
     */
    fun stop() {
        if (::engine.isInitialized) {
            Log.i("HttpService", "Stopping server...")
            try {
                engine.stop(100, 100)
            } catch (e: Exception) {
                Log.w("HttpService", "Error while stopping server: ${e.message}")
            }
        }
    }

    /**
     * Stops the current server instance and starts a new one on the specified port.
     * Does nothing if the new port is the same as the current one.
     *
     * @param newPort The new port to run the server on.
     */
    fun restartServer(newPort: Int) {
        if (newPort == currentPort) return
        Log.i("HttpService", "Restarting server on port $newPort")
        stop() // Use the new stop function
        currentPort = newPort
        start()
    }

    /**
     * Sends a new image frame to the MJPEG stream.
     * It uses `trySend` to avoid blocking if the channel is full.
     *
     * @param bytes The ByteArray of the JPEG image frame.
     */
    fun sendFrame(bytes: ByteArray) {
        if (::imageChannel.isInitialized) {
            imageChannel.trySend(bytes)
        }
    }
}
