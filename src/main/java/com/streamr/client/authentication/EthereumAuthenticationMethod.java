package com.streamr.client.authentication;

import com.squareup.moshi.JsonAdapter;
import com.streamr.client.utils.HttpUtils;
import com.streamr.client.utils.SigningUtil;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import okhttp3.Response;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

public class EthereumAuthenticationMethod extends AuthenticationMethod {

    private final ECKeyPair account;
    private final String address;
    private JsonAdapter<Challenge> challengeAdapter = HttpUtils.MOSHI.adapter(Challenge.class);
    private JsonAdapter<ChallengeResponse> challengeResponseAdapter = HttpUtils.MOSHI.adapter(ChallengeResponse.class);

    public EthereumAuthenticationMethod(String ethereumPrivateKey) {
        super();
        String withoutPrefix = Numeric.cleanHexPrefix(ethereumPrivateKey);
        this.account = ECKeyPair.create(new BigInteger(withoutPrefix, 16));
        this.address = Numeric.prependHexPrefix(Keys.getAddress(this.account.getPublicKey()));
    }

    @Override
    protected LoginResponse login(String restApiUrl) throws IOException {
        Challenge challenge = getChallenge(restApiUrl);
        String signature = signChallenge(challenge.challenge);
        ChallengeResponse response = new ChallengeResponse(challenge, signature, address);
        Response resp = null;
        try {
            resp = post(restApiUrl + "/login/response", challengeResponseAdapter.toJson(response));
            return parse(resp.body().source());
        } finally {
            if (resp != null) {
                resp.close();
            }
        }
    }

    public Challenge getChallenge(String restApiUrl) throws IOException {
        Response response = null;
        try {
            response = post(restApiUrl + "/login/challenge/"+address, "");
            return challengeAdapter.fromJson(response.body().source());
        } finally {
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
