package com.streamr.client.testing;

import com.streamr.client.MessageHandler;
import com.streamr.client.StreamrClient;
import com.streamr.client.java.util.Objects;
import com.streamr.client.options.ResendOption;
import com.streamr.client.options.StreamrClientOptions;
import com.streamr.client.protocol.control_layer.ControlMessage;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.rest.Stream;
import com.streamr.client.rest.StreamrRestClient;
import com.streamr.client.subs.Subscription;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.web3j.crypto.Credentials;

public class TestingStreamrClient extends StreamrClient {
  private static StreamrRestClient createStreamrRestClient(final BigInteger privateKey) {
    return new StreamrRestClient.Builder()
        .withRestApiUrl(TestingMeta.REST_URL)
        .withConnectTimeout(60000L)
        .withReadTimeout(60000L)
        .withWriteTimeout(60000L)
        .withPrivateKey(privateKey)
        .createStreamrRestClient();
  }

  public static StreamrClient createUnauthenticatedClient() {
    return new StreamrClient(
        new StreamrClientOptions(TestingMeta.WEBSOCKET_URL), createStreamrRestClient(null));
  }

  public static StreamrClient createClientWithPrivateKey(final Credentials credentials) {
    Objects.requireNonNull(credentials);
    return createClientWithPrivateKey(credentials.getEcKeyPair().getPrivateKey());
  }

  public static StreamrClient createClientWithPrivateKey(final String privateKey) {
    Objects.requireNonNull(privateKey);
    return createClientWithPrivateKey(new BigInteger(privateKey, 16));
  }

  public static StreamrClient createClientWithPrivateKey(final BigInteger privateKey) {
    Objects.requireNonNull(privateKey);
    return new StreamrClient(createOptions(), createStreamrRestClient(privateKey));
  }

  private static StreamrClientOptions createOptions() {
    return new StreamrClientOptions(TestingMeta.WEBSOCKET_URL);
  }

  List<StreamMessage> receivedStreamMessages = new ArrayList<>();

  public TestingStreamrClient(
      final StreamrClientOptions options, final StreamrRestClient restClient) {
    super(options, restClient);
  }

  public void receiveMessage(ControlMessage msg) {
    handleMessage(msg.toJson());
  }

  public List<StreamMessage> getReceivedStreamMessages() {
    return receivedStreamMessages;
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
}
