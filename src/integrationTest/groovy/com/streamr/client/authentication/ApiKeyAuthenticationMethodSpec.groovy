package com.streamr.client.authentication

import com.streamr.client.StreamrIntegrationSpecification
import com.streamr.client.exceptions.AuthenticationException

class ApiKeyAuthenticationMethodSpec extends StreamrIntegrationSpecification {

	void "newSessionToken() fetches a new sessionToken using the provided API key"() {
		ApiKeyAuthenticationMethod auth = new ApiKeyAuthenticationMethod("tester1-api-key")

		when:
		String sessionToken = auth.newSessionToken(DEFAULT_REST_URL)

		then:
		sessionToken != null
	}

	void "newSessionToken() throws if the credentials are wrong"() {
		ApiKeyAuthenticationMethod auth = new ApiKeyAuthenticationMethod("wrong")

		when:
		auth.newSessionToken(DEFAULT_REST_URL)

		then:
		thrown(AuthenticationException)
	}

}
