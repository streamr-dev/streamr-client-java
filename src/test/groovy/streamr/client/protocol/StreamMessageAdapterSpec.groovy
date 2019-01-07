package streamr.client.protocol

import com.squareup.moshi.JsonReader
import okio.Buffer
import spock.lang.Specification
import streamr.client.exceptions.MalformedMessageException
import streamr.client.exceptions.UnsupportedMessageException

import java.nio.charset.Charset

class StreamMessageAdapterSpec extends Specification {

	StreamMessageAdapter adapter

	void setup() {
		adapter = new StreamMessageAdapter()
	}

	private static JsonReader toReader(String json) {
		return JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
	}

	void "fromJson"() {
		String json = "[28,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,1871084061,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\"]"

		when:
		StreamMessage msg = adapter.fromJson(toReader(json))

		then:
		msg.getStreamId() == "7wa7APtlTq6EC5iTCBy6dw"
		msg.getPartition() == 0
		msg.getTimestamp() == 1528228173462L
		msg.getTimestampAsDate() == new Date(1528228173462L)
		msg.getTtl() == 0
		msg.getOffset() == 1871084066
		msg.getPreviousOffset() == 1871084061
		msg.getContentTypeCode() == 27
		msg.getPayload() instanceof Map
		msg.getPayload().desi == "2"
	}

	void "fromJson() with previousOffset null"() {
		String json = "[28,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,null,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\"]"

		when:
		StreamMessage msg = adapter.fromJson(toReader(json))

		then:
		msg.getStreamId() == "7wa7APtlTq6EC5iTCBy6dw"
		msg.getPartition() == 0
		msg.getTimestamp() == 1528228173462L
		msg.getTimestampAsDate() == new Date(1528228173462L)
		msg.getTtl() == 0
		msg.getOffset() == 1871084066
		msg.getPreviousOffset() == null
		msg.getContentTypeCode() == 27
		msg.getPayload() instanceof Map
		msg.getPayload().desi == "2"
	}

	void "fromJson throws for invalid version"() {
		String json = "[666,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,1871084061,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\"]"

		when:
		adapter.fromJson(toReader(json))

		then:
		thrown(UnsupportedMessageException)
	}

	void "fromJson throws for invalid content type"() {
		String json = "[28,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,1871084061,666,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\"]"

		when:
		adapter.fromJson(toReader(json))

		then:
		thrown(UnsupportedMessageException)
	}

	void "fromJson throws for invalid message structure"() {
		String json = "[28,0]"

		when:
		adapter.fromJson(toReader(json))

		then:
		thrown(MalformedMessageException)
	}

}
