package com.streamr.client.protocol.control_layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.streamr.client.protocol.common.MessageRef;
import com.streamr.client.protocol.utils.Address;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ResendRangeRequestAdapterTest {
  public static final Address PUBLISHER_ID =
      new Address("0xBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB");

  @ParameterizedTest(name = "serialize and deserialize {0}")
  @MethodSource("testDataProvider")
  void serializationAndDeserialization(String serializedMessage, ControlMessage message)
      throws IOException {
    assertEquals(message, ControlMessage.fromJson(serializedMessage));
    assertEquals(serializedMessage, message.toJson());
  }

  static Stream<Arguments> testDataProvider() {
    return Stream.of(
        arguments(
            "[2,13,\"requestId\",\"streamId\",0,[143415425455,0],[14341542564555,7],\"0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\",\"msgChainId\",\"sessionToken\"]",
            new ResendRangeRequest(
                "requestId",
                "streamId",
                0,
                new MessageRef(143415425455L, 0L),
                new MessageRef(14341542564555L, 7L),
                PUBLISHER_ID,
                "msgChainId",
                "sessionToken")),
        arguments(
            "[2,13,\"requestId\",\"streamId\",0,[143415425455,0],[14341542564555,7],null,null,null]",
            new ResendRangeRequest(
                "requestId",
                "streamId",
                0,
                new MessageRef(143415425455L, 0L),
                new MessageRef(14341542564555L, 7L),
                null)));
  }

  @Test
  void fromJsonWhereFromIsGreaterThanTo() throws IOException {
    String serializedMessage =
        "[2,13,\"requestId\",\"streamId\",0,[143415425455,0],[143415425000,0],\"0xBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB\",\"msgChainId\",\"sessionToken\"]";
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          ControlMessage.fromJson(serializedMessage);
        });
  }
}
