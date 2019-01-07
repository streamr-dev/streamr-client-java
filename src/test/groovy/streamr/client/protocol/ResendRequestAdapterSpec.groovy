package streamr.client.protocol

import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class ResendRequestAdapterSpec extends Specification {

	private static Charset utf8 = Charset.forName("UTF-8")

	ResendRequestAdapter adapter
	Buffer buffer

	void setup() {
		adapter = new ResendRequestAdapter()
		buffer = new Buffer()
	}

	void "toJson (no resend)"() {
		ResendRequest request = new ResendRequest("streamId", 1, "subId", ResendOption.createNoResendOption());

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == "{\"stream\":\"streamId\",\"partition\":1,\"sub\":\"subId\"}"
	}

	void "toJson (resend all)"() {
		ResendRequest request = new ResendRequest("streamId", 1, "subId", ResendOption.createResendAllOption());

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == "{\"stream\":\"streamId\",\"partition\":1,\"sub\":\"subId\",\"resend_all\":true}"
	}

	void "toJson (resend last)"() {
		ResendRequest request = new ResendRequest("streamId", 1, "subId", ResendOption.createResendLastOption(10));

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == "{\"stream\":\"streamId\",\"partition\":1,\"sub\":\"subId\",\"resend_last\":10}"
	}

	void "toJson (resend from)"() {
		ResendRequest request = new ResendRequest("streamId", 1, "subId", ResendOption.createResendFromOption(12345));

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == "{\"stream\":\"streamId\",\"partition\":1,\"sub\":\"subId\",\"resend_from\":12345}"
	}

}
