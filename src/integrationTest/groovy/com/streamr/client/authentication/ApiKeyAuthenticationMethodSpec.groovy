package com.streamr.client.authentication

import com.streamr.client.*
import com.streamr.client.exceptions.AuthenticationException

class ApiKeyAuthenticationMethodSpec extends StreamrIntegrationSpecification {

	void "newSessionToken() fetches a new sessionToken using the provided API key"() {
		ApiKeyAuthenticationMethod auth = new ApiKeyAuthenticationMethod(createOptions("tester1-api-key"))

		when:
		String sessionToken = auth.newSessionToken()

		then:
		sessionToken != null
	}

	void "newSessionToken() throws if the credentials are wrong"() {
		ApiKeyAuthenticationMethod auth = new ApiKeyAuthenticationMethod(createOptions("wrong"))

		when:
		auth.newSessionToken()

		then:
		thrown(AuthenticationException)
	}

}
