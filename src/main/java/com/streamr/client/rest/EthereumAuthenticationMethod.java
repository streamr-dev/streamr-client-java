package com.streamr.client.rest;

import com.squareup.moshi.JsonAdapter;
import com.streamr.client.utils.KeyUtil;
import com.streamr.client.utils.SigningUtil;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.web3j.crypto.ECKeyPair;

public class EthereumAuthenticationMethod extends AuthenticationMethod {
  private final ECKeyPair account;
  // address is prefixed with "0x"
  private final String address;
  private JsonAdapter<Challenge> challengeAdapter = Json.newMoshiBuilder().build().adapter(Challenge.class);
  private JsonAdapter<ChallengeResponse> challengeResponseAdapter =
      Json.newMoshiBuilder().build().adapter(ChallengeResponse.class);

  public EthereumAuthenticationMethod(String ethereumPrivateKey) {
    super();
    String withoutPrefix = privateKeyWithoutPrefix(ethereumPrivateKey);
    this.account = ECKeyPair.create(new BigInteger(withoutPrefix, 16));
    this.address = KeyUtil.toHex(this.account.getPublicKey());
  }

  private String privateKeyWithoutPrefix(String ethereumPrivateKey) {
    if (ethereumPrivateKey.startsWith("0x")) {
      return ethereumPrivateKey.substring(2);
    }
    return ethereumPrivateKey;
  }

  @Override
  protected LoginResponse login(String restApiUrl) throws IOException {
    Challenge challenge = getChallenge(restApiUrl);
    String signature = signChallenge(challenge.challenge);
    ChallengeResponse response = new ChallengeResponse(challenge, signature, address);
    Response resp = null;
    ResponseBody body = null;
    BufferedSource source = null;
    try {
      resp = post(restApiUrl + "/login/response", challengeResponseAdapter.toJson(response));
      body = resp.body();
      source = body.source();
      LoginResponse result = parse(source);
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

  public Challenge getChallenge(String restApiUrl) throws IOException {
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

  public ECKeyPair getAccount() {
    return account;
  }

  private String signChallenge(String challengeToSign) {
    return SigningUtil.sign(challengeToSign, account);
  }

  static class Challenge {
    String id;
    String challenge;
    Date expires;
  }

  static class ChallengeResponse {
    Challenge challenge;
    String signature;
    String address;

    public ChallengeResponse(Challenge challenge, String signature, String address) {
      this.challenge = challenge;
      this.signature = signature;
      this.address = address;
    }
  }
}
