package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.streamr.client.protocol.control_layer.DeleteRequest
import com.streamr.client.protocol.control_layer.DeleteRequestAdapter
import com.streamr.client.protocol.control_layer.DeleteResponse
import com.streamr.client.protocol.control_layer.DeleteResponseAdapter
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class DeleteResponseAdapterSpec extends Specification {

    private static Charset utf8 = Charset.forName("UTF-8")

    DeleteResponseAdapter adapter
    Buffer buffer

    void setup() {
        adapter = new DeleteResponseAdapter()
        buffer = new Buffer()
    }

    private static DeleteResponse toMsg(DeleteResponseAdapter adapter, String json) {
        JsonReader reader = JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
        reader.beginArray()
        reader.nextInt()
        reader.nextInt()
        DeleteResponse msg = adapter.fromJson(reader)
        reader.endArray()
        return msg
    }

    void "fromJson"() {
        String json = '[1,15,"streamId",0,true]'
        when:
        DeleteResponse msg = toMsg(adapter, json)
        then:
        msg.streamId == "streamId"
        msg.streamPartition == 0
        msg.status
    }

    void "toJson"() {
        DeleteResponse response = new DeleteResponse("streamId", 0, true)
        when:
        adapter.toJson(buffer, response)

        then:
        buffer.readString(utf8) == '[1,15,"streamId",0,true]'
    }
}
