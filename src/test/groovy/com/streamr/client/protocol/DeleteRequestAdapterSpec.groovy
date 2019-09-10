package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.streamr.client.protocol.control_layer.DeleteRequest
import com.streamr.client.protocol.control_layer.DeleteRequestAdapter
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class DeleteRequestAdapterSpec extends Specification {

    private static Charset utf8 = Charset.forName("UTF-8")

    DeleteRequestAdapter adapter
    Buffer buffer

    void setup() {
        adapter = new DeleteRequestAdapter()
        buffer = new Buffer()
    }

    private static DeleteRequest toMsg(DeleteRequestAdapter adapter, String json) {
        JsonReader reader = JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
        reader.beginArray()
        reader.nextInt()
        reader.nextInt()
        DeleteRequest msg = adapter.fromJson(reader)
        reader.endArray()
        return msg
    }

    void "fromJson"() {
        String json = '[1,14,"streamId",0,123,456]'
        when:
        DeleteRequest msg = toMsg(adapter, json)
        then:
        msg.streamId == "streamId"
        msg.streamPartition == 0
        msg.fromTimestamp == 123L
        msg.toTimestamp == 456L
    }

    void "toJson"() {
        DeleteRequest request = new DeleteRequest("streamId", 0, 123L, 456L)
        when:
        adapter.toJson(buffer, request)

        then:
        buffer.readString(utf8) == '[1,14,"streamId",0,123,456]'
    }
}
