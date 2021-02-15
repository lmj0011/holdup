package name.lmj0011.holdup.helpers.adapters

import com.squareup.moshi.*
import okio.Buffer
import org.json.JSONObject

/**
 * A JsonAdapter to be added to a Moshi.Builder that
 * handles pesky properties in JSON responses that can be interpreted
 * as either a String or JSONObject
 */
object JSONObjectAdapter : JsonAdapter<Pair<JSONObject?, String?>?>() {

    @FromJson
    override fun fromJson(reader: JsonReader): Pair<JSONObject?, String?>? {
        var pair: Pair<JSONObject?, String?>?
        // Here we're expecting the JSON object, it is processed as Map<String, Any> by Moshi
        return if(reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
            (reader.readJsonValue() as? Map<String, Any>)?.let { data ->
                pair = Pair(JSONObject(data), null)
                pair
            }
        }else {
            pair = Pair(null, reader.nextString())
            pair
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: Pair<@JvmSuppressWildcards JSONObject?, @JvmSuppressWildcards String?>?) {
        value?.let {
            when {
                value.first != null -> writer.value(Buffer().writeUtf8(value.first.toString()))
                value.second != null -> writer.value(Buffer().writeUtf8(value.second.toString()))
                else -> {}
            }
        }
    }
}