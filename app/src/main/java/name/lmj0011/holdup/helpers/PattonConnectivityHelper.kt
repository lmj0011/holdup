package name.lmj0011.holdup.helpers

import android.content.Context
import io.socket.client.IO
import io.socket.client.Socket
import java.lang.RuntimeException
import java.net.URISyntaxException

/**
 * A class that manages the client's connection to a "Patton" websocket server
 */
class PattonConnectivityHelper(val context: Context) {
    companion object {
        const val SERVER_URL = "https://patton-service.mo1s7ll9qdn64.us-east-1.cs.amazonlightsail.com"
    }

    val socket: Socket

    init {
        try {
            socket = IO.socket(SERVER_URL)
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }
}