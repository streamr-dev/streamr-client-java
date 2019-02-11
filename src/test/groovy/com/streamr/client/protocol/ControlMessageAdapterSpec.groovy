package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.streamr.client.protocol.control_layer.BroadcastMessage
import com.streamr.client.protocol.control_layer.ControlMessage
import com.streamr.client.protocol.control_layer.ControlMessageAdapter
import com.streamr.client.protocol.control_layer.ErrorResponse
import com.streamr.client.protocol.control_layer.PublishRequest
import com.streamr.client.protocol.control_layer.ResendFromRequest
import com.streamr.client.protocol.control_layer.ResendLastRequest
import com.streamr.client.protocol.control_layer.ResendRangeRequest
import com.streamr.client.protocol.control_layer.ResendResponseNoResend
import com.streamr.client.protocol.control_layer.ResendResponseResending
import com.streamr.client.protocol.control_layer.ResendResponseResent
import com.streamr.client.protocol.control_layer.SubscribeRequest
import com.streamr.client.protocol.control_layer.SubscribeResponse
import com.streamr.client.protocol.control_layer.UnicastMessage
import com.streamr.client.protocol.control_layer.UnsubscribeRequest
import com.streamr.client.protocol.control_layer.UnsubscribeResponse
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class ControlMessageAdapterSpec extends Specification {

    private static ControlMessage fromJson(ControlMessageAdapter adapter, String json) {
        JsonReader reader = JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
        return adapter.fromJson(reader)
    }

    ControlMessageAdapter adapter

    void setup() {
        adapter = new ControlMessageAdapter()
    }

    def "BroadcastMessage"() {
        String msgJson = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\"],[1528228170000,0],27,\"{\\\"hello\\\":\\\"world\\\"}\",1,\"signature\"]"
        String json = '[1,0,'+msgJson+']'
        when:
        ControlMessage msg = fromJson(adapter, json)
        then:
        msg instanceof BroadcastMessage
        msg.toJson() == json
    }
    def "UnicastMessage"() {
        String msgJson = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\"],[1528228170000,0],27,\"{\\\"hello\\\":\\\"world\\\"}\",1,\"signature\"]"
        String json = '[1,1,"subId",'+msgJson+']'
        when:
        ControlMessage msg = fromJson(adapter, json)
        then:
        msg instanceof UnicastMessage
        msg.toJson() == json
    }
    def "SubscribeResponse"() {
        String json = '[1,2,"streamId",0]'
        when:
        ControlMessage msg = fromJson(adapter, json)
        then:
        msg instanceof SubscribeResponse
        msg.toJson() == json
    }
    def "UnsubscribeResponse"() {
        String json = '[1,3,"streamId",0]'
        when:
        ControlMessage msg = fromJson(adapter, json)
        then:
        msg instanceof UnsubscribeResponse
        msg.toJson() == json
    }
    def "ResendResponseResending"() {
        String json = '[1,4,"streamId",0,"subId"]'
        when:
        ControlMessage msg = fromJson(adapter, json)
        then:
        msg instanceof ResendResponseResending
        msg.toJson() == json
    }
    def "ResendResponseResent"() {
        String json = '[1,5,"streamId",0,"subId"]'
        when:
        ControlMessage msg = fromJson(adapter, json)
        then:
        msg instanceof ResendResponseResent
        msg.toJson() == json
    }
    def "ResendResponseNoResend"() {
        String json = '[1,6,"streamId",0,"subId"]'
        when:
        ControlMessage msg = fromJson(adapter, json)
        then:
        msg instanceof ResendResponseNoResend
        msg.toJson() == json
    }
    def "ErrorResponse"() {
        String json = '[1,7,"error"]'
        when:
        ControlMessage msg = fromJson(adapter, json)
        then:
        msg instanceof ErrorResponse
        msg.toJson() == json
    }
    def "PublishRequest"() {
        String msgJson = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\"],[1528228170000,0],27,\"{\\\"hello\\\":\\\"world\\\"}\",1,\"signature\"]"
        String json = '[1,8,'+msgJson+',"sessionToken"]'
        when:
        ControlMessage msg = fromJson(adapter, json)
        then:
        msg instanceof PublishRequest
        msg.toJson() == json
    }
    def "SubscribeRequest"() {
        String json = '[1,9,"streamId",0,"sessionToken"]'
        when:
        ControlMessage msg = fromJson(adapter, json)
        then:
        msg instanceof SubscribeRequest
        msg.toJson() == json
    }
    def "UnsubscribeRequest"() {
        String json = '[1,10,"streamId",0]'
        when:
        ControlMessage msg = fromJson(adapter, json)
        then:
        msg instanceof UnsubscribeRequest
        msg.toJson() == json
    }
    def "ResendLastRequest"() {
        String json = '[1,11,"streamId",0,"subId",4]'
        when:
        ControlMessage msg = fromJson(adapter, json)
        then:
        msg instanceof ResendLastRequest
        msg.toJson() == json
    }
    def "ResendFromRequest"() {
        String json = '[1,12,"streamId",0,"subId",[143415425455,0],"publisherId"]'
        when:
        ControlMessage msg = fromJson(adapter, json)
        then:
        msg instanceof ResendFromRequest
        msg.toJson() == json
    }
    def "ResendRangeRequest"() {
        String json = '[1,13,"streamId",0,"subId",[143415425455,0],[14341542564555,7],"publisherId"]'
        when:
        ControlMessage msg = fromJson(adapter, json)
        then:
        msg instanceof ResendRangeRequest
        msg.toJson() == json
    }
}
