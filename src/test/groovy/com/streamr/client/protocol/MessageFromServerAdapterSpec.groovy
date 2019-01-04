package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.streamr.client.exceptions.MalformedMessageException
import com.streamr.client.exceptions.UnsupportedMessageException
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class MessageFromServerAdapterSpec extends Specification {

	MessageFromServerAdapter adapter

	void setup() {
		adapter = new MessageFromServerAdapter()
	}

	private static JsonReader toReader(String json) {
		return JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
	}

	/**
	 * Broadcast message
	 */
	void "fromJson broadcast"() {
		String json = "[0,0,\"\",[28,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,1871084061,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\"]]"

		when:
		MessageFromServer msg = adapter.fromJson(toReader(json))

		then:
		msg.getMessageTypeCode() == 0
		msg.getType() == MessageFromServer.Type.Broadcast
		msg.getSubscriptionId() == ""
		msg.getPayload() instanceof StreamMessage
	}

	void "fromJson broadcast with null subId"() {
		String json = "[0,0,null,[28,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,1871084061,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\"]]"

		when:
		MessageFromServer msg = adapter.fromJson(toReader(json))

		then:
		msg.getMessageTypeCode() == 0
		msg.getType() == MessageFromServer.Type.Broadcast
		msg.getSubscriptionId() == null
		msg.getPayload() instanceof StreamMessage
	}

	/**
	 * Unicast message
	 */
	void "fromJson unicast"() {
		String json = "[0,1,\"subId\",[28,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,1871084061,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\"]]"

		when:
		MessageFromServer msg = adapter.fromJson(toReader(json))

		then:
		msg.getMessageTypeCode() == 1
		msg.getType() == MessageFromServer.Type.Unicast
		msg.getSubscriptionId() == "subId"
		msg.getPayload() instanceof StreamMessage
	}

	/**
	 * Subscribed
	 */
	void "fromJson subscribed"() {
		String json = "[0,2,\"\",{\"stream\":\"7wa7APtlTq6EC5iTCBy6dw\",\"partition\":0}]"

		when:
		MessageFromServer msg = adapter.fromJson(toReader(json))

		then:
		msg.getMessageTypeCode() == 2
		msg.getType() == MessageFromServer.Type.Subscribed
		msg.getSubscriptionId() == ""
		msg.getPayload() instanceof SubscribeResponse
		msg.getPayload().stream == "7wa7APtlTq6EC5iTCBy6dw"
		msg.getPayload().partition == 0
	}

	/**
	 * Unsubscribed
	 */
	void "fromJson unsubscribed"() {
		String json = "[0,3,\"\",{\"stream\":\"7wa7APtlTq6EC5iTCBy6dw\",\"partition\":0}]"

		when:
		MessageFromServer msg = adapter.fromJson(toReader(json))

		then:
		msg.getMessageTypeCode() == 3
		msg.getType() == MessageFromServer.Type.Unsubscribed
		msg.getSubscriptionId() == ""
		msg.getPayload() instanceof UnsubscribeResponse
		msg.getPayload().stream == "7wa7APtlTq6EC5iTCBy6dw"
		msg.getPayload().partition == 0
	}

	/**
	 * Resending
	 */
	void "fromJson resending"() {
		String json = "[0,4,\"\",{\"stream\":\"7wa7APtlTq6EC5iTCBy6dw\",\"partition\":0,\"sub\":\"0\"}]"

		when:
		MessageFromServer msg = adapter.fromJson(toReader(json))

		then:
		msg.getMessageTypeCode() == 4
		msg.getType() == MessageFromServer.Type.Resending
		msg.getSubscriptionId() == ""
		msg.getPayload() instanceof ResendingMessage
		msg.getPayload().stream == "7wa7APtlTq6EC5iTCBy6dw"
		msg.getPayload().partition == 0
		msg.getPayload().sub == "0"
	}

	/**
	 * Resent
	 */
	void "fromJson resent"() {
		String json = "[0,5,\"\",{\"stream\":\"7wa7APtlTq6EC5iTCBy6dw\",\"partition\":0,\"sub\":\"0\"}]"

		when:
		MessageFromServer msg = adapter.fromJson(toReader(json))

		then:
		msg.getMessageTypeCode() == 5
		msg.getType() == MessageFromServer.Type.Resent
		msg.getSubscriptionId() == ""
		msg.getPayload() instanceof ResentMessage
		msg.getPayload().stream == "7wa7APtlTq6EC5iTCBy6dw"
		msg.getPayload().partition == 0
		msg.getPayload().sub == "0"
	}

	/**
	 * NoResend
	 */
	void "fromJson no resend"() {
		String json = "[0,6,\"\",{\"stream\":\"7wa7APtlTq6EC5iTCBy6dw\",\"partition\":0,\"sub\":\"0\"}]"

		when:
		MessageFromServer msg = adapter.fromJson(toReader(json))

		then:
		msg.getMessageTypeCode() == 6
		msg.getType() == MessageFromServer.Type.NoResend
		msg.getSubscriptionId() == ""
		msg.getPayload() instanceof NoResendMessage
		msg.getPayload().stream == "7wa7APtlTq6EC5iTCBy6dw"
		msg.getPayload().partition == 0
		msg.getPayload().sub == "0"
	}

	/**
	 * Error
	 */
	void "fromJson error"() {
		String json = "[0,7,\"\",\"Not authorized to subscribe to stream 123123 and partition 0\"]"

		when:
		MessageFromServer msg = adapter.fromJson(toReader(json))

		then:
		msg.getMessageTypeCode() == 7
		msg.getType() == MessageFromServer.Type.Error
		msg.getSubscriptionId() == ""
		msg.getPayload().equals("Not authorized to subscribe to stream 123123 and partition 0")
	}

	/**
	 * Errors in parsing the header
	 */
	void "fromJson throws for invalid version"() {
		String json = "[666,0,\"\",[28,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,1871084061,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\"]]"

		when:
		adapter.fromJson(toReader(json))

		then:
		thrown(UnsupportedMessageException)
	}

	void "fromJson throws for invalid message type"() {
		String json = "[0,666,\"\",[28,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,1871084061,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\"]]"

		when:
		adapter.fromJson(toReader(json))

		then:
		thrown(UnsupportedMessageException)
	}

	void "fromJson throws for invalid message structure"() {
		String json = "[0,0]"

		when:
		adapter.fromJson(toReader(json))

		then:
		thrown(MalformedMessageException)
	}

}
