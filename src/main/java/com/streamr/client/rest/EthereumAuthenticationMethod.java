package com.streamr.client.rest;

import com.squareup.moshi.JsonAdapter;
import com.streamr.client.utils.SigningUtil;
import java.io.IOException;
import java.math.BigInteger;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

public class EthereumAuthenticationMethod {
  private final ECKeyPair account;
  private final BigInteger privateKey;
  // address is prefixed with "0x"
  private final String address;
  private final JsonAdapter<Challenge> challengeAdapter =
      Json.newMoshiBuilder().build().adapter(Challenge.class);
  private final JsonAdapter<ChallengeResponse> challengeResponseAdapter =
      Json.newMoshiBuilder().build().adapter(ChallengeResponse.class);
  private final JsonAdapter<LoginResponse> loginResponseAdapter =
      Json.newMoshiBuilder().build().adapter(LoginResponse.class);

  public EthereumAuthenticationMethod(String ethereumPrivateKey) {
    String withoutPrefix = Numeric.cleanHexPrefix(ethereumPrivateKey);
    this.privateKey = new BigInteger(withoutPrefix, 16);
    this.account = ECKeyPair.create(privateKey);
    final String addr = Keys.getAddress(this.account.getPublicKey());
    this.address = Numeric.prependHexPrefix(addr);
  }

  public LoginResponse login(String restApiUrl) throws IOException {
    Challenge challenge = getChallenge(restApiUrl);
    String signature = SigningUtil.sign(privateKey, challenge.getChallenge());
    ChallengeResponse response = new ChallengeResponse(challenge, signature, address);
    Response resp = null;
    ResponseBody body = null;
    BufferedSource source = null;
    try {
      resp = post(restApiUrl + "/login/response", challengeResponseAdapter.toJson(response));
      body = resp.body();
      source = body.source();
      LoginResponse result = loginResponseAdapter.fromJson(source);
      return result;
    } finally {
      if (source != null) {
        source.close();
      }
      if (body != null) {
        body.close();
      }
      if (resp != null) {
        resp.close();
      }
    }
  }

  private Challenge getChallenge(String restApiUrl) throws IOException {
    Response response = null;
    ResponseBody body = null;
    BufferedSource source = null;
    try {
      response = post(restApiUrl + "/login/challenge/" + address, "");
      body = response.body();
      source = body.source();
      Challenge result = challengeAdapter.fromJson(source);
      return result;
    } finally {
      if (source != null) {
        source.close();
      }
      if (body != null) {
        body.close();
      }
      if (response != null) {
        response.close();
      }
    }
  }

  public String getAddress() {
    return address;
  }

  public BigInteger getPrivateKey() {
    return privateKey;
  }

  /**
   * Uses the credentials represented by this class to login and obtain a new, valid sessionToken.
   */
  public String newSessionToken(String restApiUrl) {
    try {
      LoginResponse loginResponse = login(restApiUrl);
      return loginResponse.getToken();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Response post(String endpoint, String requestBody) throws IOException {
    OkHttpClient client = new OkHttpClient();

    Request request =
        new Request.Builder()
            .url(endpoint)
            .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
            .build();

    // Execute the request and retrieve the response.
    Response response = client.newCall(request).execute();
    StreamrRestClient.assertSuccessful(response);
    return response;
  }
}
