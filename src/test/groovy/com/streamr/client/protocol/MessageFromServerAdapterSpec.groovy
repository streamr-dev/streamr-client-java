package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.streamr.client.MessageFromServer
import com.streamr.client.Subscription
import com.streamr.client.exceptions.MalformedMessageException
import com.streamr.client.exceptions.SubscriptionNotFoundException
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

	void "fromJson broadcast"() {
		String json = "[0,0,\"\",[28,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,1871084061,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\"]]"

		when:
		MessageFromServer msg = adapter.fromJson(toReader(json))

		then:
		msg.getMessageTypeCode() == 0
		msg.getType() == MessageFromServer.Type.Broadcast
		msg.getSubscriptionId() == ""
		// TODO msg.getPayload() instanceof BroadcastMessage
	}

	void "fromJson unicast"() {
		String json = "[0,1,\"subId\",[28,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,1871084061,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\"]]"

		when:
		MessageFromServer msg = adapter.fromJson(toReader(json))

		then:
		msg.getMessageTypeCode() == 1
		msg.getType() == MessageFromServer.Type.Unicast
		msg.getSubscriptionId() == "subId"
		// TODO msg.getPayload() instanceof UnicastMessage
	}

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
