package com.streamr.client.authentication

import com.streamr.client.StreamrIntegrationSpecification
import com.streamr.client.testing.TestingKeys
import com.streamr.client.testing.TestingMeta

class EthereumAuthenticationMethodSpec extends StreamrIntegrationSpecification {

    void "newSessionToken() fetches a new sessionToken by signing a challenge"() {
        EthereumAuthenticationMethod auth = new EthereumAuthenticationMethod(TestingKeys.generatePrivateKey())

        when:
        String sessionToken = auth.newSessionToken(TestingMeta.REST_URL)

        then:
        sessionToken != null
    }
}
