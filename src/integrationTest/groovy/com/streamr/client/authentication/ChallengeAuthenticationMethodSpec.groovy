package com.streamr.client.authentication

import com.streamr.client.StreamrIntegrationSpecification

class ChallengeAuthenticationMethodSpec extends StreamrIntegrationSpecification {

    void "newSessionToken() fetches a new sessionToken by signing a challenge"() {
        ChallengeAuthenticationMethod auth = new ChallengeAuthenticationMethod(generatePrivateKey())
        auth.setRestApiUrl(DEFAULT_REST_URL)

        when:
        String sessionToken = auth.newSessionToken()

        then:
        sessionToken != null
    }
}
