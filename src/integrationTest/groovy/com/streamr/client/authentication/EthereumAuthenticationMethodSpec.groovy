package com.streamr.client.authentication

import com.streamr.client.StreamrIntegrationSpecification

class EthereumAuthenticationMethodSpec extends StreamrIntegrationSpecification {

    void "newSessionToken() fetches a new sessionToken by signing a challenge"() {
        EthereumAuthenticationMethod auth = new EthereumAuthenticationMethod(generatePrivateKey())
        auth.setRestApiUrl(DEFAULT_REST_URL)

        when:
        String sessionToken = auth.newSessionToken()

        then:
        sessionToken != null
    }
}
