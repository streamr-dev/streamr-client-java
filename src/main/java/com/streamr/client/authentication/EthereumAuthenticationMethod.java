package com.streamr.client.authentication;

import com.squareup.moshi.JsonAdapter;
import com.streamr.client.utils.HttpUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;

import com.streamr.client.utils.SigningUtil;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.apache.commons.codec.DecoderException;
import org.ethereum.crypto.ECKey;
import org.apache.commons.codec.binary.Hex;

public class EthereumAuthenticationMethod extends AuthenticationMethod {

    private final ECKey account;
    private final String address;
    private JsonAdapter<Challenge> challengeAdapter = HttpUtils.MOSHI.adapter(Challenge.class);
    private JsonAdapter<ChallengeResponse> challengeResponseAdapter = HttpUtils.MOSHI.adapter(ChallengeResponse.class);

    public EthereumAuthenticationMethod(String ethereumPrivateKey) {
        super();
        String withoutPrefix = ethereumPrivateKey.startsWith("0x") ? ethereumPrivateKey.substring(2) : ethereumPrivateKey;
        this.account = ECKey.fromPrivate(new BigInteger(withoutPrefix, 16));
        this.address = "0x" + Hex.encodeHexString(this.account.getAddress());
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
            response = post(restApiUrl + "/login/challenge/"+address, "");
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

    public ECKey getAccount() {
        return account;
    }

    private String signChallenge(String challengeToSign){
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
