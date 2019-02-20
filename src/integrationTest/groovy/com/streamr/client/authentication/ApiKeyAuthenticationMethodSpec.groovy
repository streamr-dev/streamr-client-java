package com.streamr.client.authentication

import com.streamr.client.StreamrIntegrationSpecification
import com.streamr.client.exceptions.AuthenticationException

class ApiKeyAuthenticationMethodSpec extends StreamrIntegrationSpecification {

	void "newSessionToken() fetches a new sessionToken using the provided API key"() {
		ApiKeyAuthenticationMethod auth = new ApiKeyAuthenticationMethod("tester1-api-key")
		auth.setRestApiUrl(DEFAULT_REST_URL)

		when:
		String sessionToken = auth.newSessionToken()

		then:
		sessionToken != null
	}

	void "newSessionToken() throws if the credentials are wrong"() {
		ApiKeyAuthenticationMethod auth = new ApiKeyAuthenticationMethod("wrong")
		auth.setRestApiUrl(DEFAULT_REST_URL)

		when:
		auth.newSessionToken()

		then:
		thrown(AuthenticationException)
	}

}
