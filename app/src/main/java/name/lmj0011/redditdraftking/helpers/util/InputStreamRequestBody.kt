package name.lmj0011.redditdraftking.helpers.util

import android.content.ContentResolver
import android.net.Uri
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.*
import timber.log.Timber
import java.lang.Exception

// ref: https://commonsware.com/blog/2020/07/05/multipart-upload-okttp-uri.html
class InputStreamRequestBody (
    private val contentType: MediaType,
    private val contentLength: Long = -1L,
    private val contentResolver: ContentResolver,
    private val uri: Uri
) : RequestBody() {
    companion object {
        const val SEGMENT_SIZE = 2048L
    }
    override fun contentType() = contentType

    override fun contentLength() = contentLength

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val input = contentResolver.openInputStream(uri)
        val source = input?.source()

        val progressListener = object : ProgressListener {
            override fun onBytesSent(contentLength: Long, totalBytesSent: Long) {
                val totalProgress = totalBytesSent.toFloat().div(contentLength().toFloat())
            }
        }

        when (source) {
            is Source -> {
                // ref: https://stackoverflow.com/a/26376724/2445763
                try {
                    var total = 0L
                    var read: Long
                    while (source.read(sink.buffer, SEGMENT_SIZE).also { read = it } != -1L) {
                        total += read
                        sink.flush()
                        progressListener.onBytesSent(contentLength(), total)
                    }

                    Timber.d("file: $uri has finished uploading.")
                } catch (ex: Exception) {
                    Timber.e(ex)
                }

            }
            else -> throw IOException("Could not open $uri")
        }
    }

    interface ProgressListener {
        /**
         * Should be called after each sink.flush() in writeTo()
         */
        fun onBytesSent(contentLengthInBytes: Long, totalBytesSent: Long)
    }
}