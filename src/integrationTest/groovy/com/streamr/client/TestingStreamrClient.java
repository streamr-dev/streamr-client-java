package com.streamr.client;

import com.streamr.client.exceptions.ResourceNotFoundException;
import com.streamr.client.options.ResendOption;
import com.streamr.client.options.StreamrClientOptions;
import com.streamr.client.protocol.control_layer.ControlMessage;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.rest.Stream;
import com.streamr.client.rest.UserInfo;
import com.streamr.client.subs.Subscription;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestingStreamrClient extends StreamrClient {

  List<StreamMessage> receivedStreamMessages = new ArrayList<>();
  Map<String, Stream> mockStreams = new LinkedHashMap<>();

  public TestingStreamrClient(StreamrClientOptions options) {
    super(options);
  }

  @Override
  public Subscription subscribe(
      Stream stream,
      int partition,
      MessageHandler handler,
      ResendOption resendOption,
      boolean isExplicitResend) {
    // Capture received StreamMessages
    MessageHandler loggingHandler =
        (sub, message) -> {
          receivedStreamMessages.add(message);
          handler.onMessage(sub, message);
        };
    return super.subscribe(stream, partition, loggingHandler, resendOption, isExplicitResend);
  }

  @Override
  public UserInfo getUserInfo() {
    return new UserInfo("name", "username");
  }

  @Override
  public String getSessionToken() {
    return "sessionToken";
  }

  public void receiveMessage(ControlMessage msg) {
    handleMessage(msg.toJson());
  }

  public List<StreamMessage> getReceivedStreamMessages() {
    return receivedStreamMessages;
  }

  public void addMockStream(Stream stream) {
    mockStreams.put(stream.getId(), stream);
  }

  @Override
  public Stream getStream(String streamId) throws IOException, ResourceNotFoundException {
    if (mockStreams.containsKey(streamId)) {
      return mockStreams.get(streamId);
    } else {
      // Return a default mock
      Stream stream = new Stream("default mock stream from TestingStreamrClient", "");
      stream.setId(streamId);
      return stream;
    }
  }
}
