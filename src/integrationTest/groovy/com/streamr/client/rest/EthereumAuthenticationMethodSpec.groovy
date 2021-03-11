package com.streamr.client.rest

import com.streamr.client.testing.TestingKeys
import com.streamr.client.testing.TestingMeta
import spock.lang.Specification

class EthereumAuthenticationMethodSpec extends Specification {

    void "newSessionToken() fetches a new sessionToken by signing a challenge"() {
        EthereumAuthenticationMethod auth = new EthereumAuthenticationMethod(TestingKeys.generatePrivateKey())

        when:
        String sessionToken = auth.newSessionToken(TestingMeta.REST_URL)

        then:
        sessionToken != null
    }
}
