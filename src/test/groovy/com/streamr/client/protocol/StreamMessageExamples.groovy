package com.streamr.client.protocol

import com.streamr.client.protocol.message_layer.MessageID
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.utils.Address

class StreamMessageExamples {
    public static class InvalidSignature {
        public static final StreamMessage helloWorld = new StreamMessage(
                new MessageID("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, new Address("publisherId"), "1"),
                new MessageRef(1528228170000L, 0),
                StreamMessage.MessageType.STREAM_MESSAGE, [hello: "world"], StreamMessage.EncryptionType.NONE, null,
                StreamMessage.SignatureType.ETH, "signature")

        public static final String helloWorldSerialized31 = "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherid\",\"1\"],[1528228170000,0],27,0,\"{\\\"hello\\\":\\\"world\\\"}\",2,\"signature\"]"
        public static final String helloWorldSerialized32 = "[32,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherid\",\"1\"],[1528228170000,0],27,0,0,null,\"{\\\"hello\\\":\\\"world\\\"}\",null,2,\"signature\"]"
    }
}
