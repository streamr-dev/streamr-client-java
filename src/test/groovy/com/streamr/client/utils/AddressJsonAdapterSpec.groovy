package com.streamr.client.utils

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class TestWrapper {
    Address address

    TestWrapper(Address address) {
        this.address = address
    }
}

class AddressJsonAdapterSpec extends Specification {

	private static JsonReader toReader(String json) {
		return JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
	}

    def "Address.fromJson"(String json, Address address) {
        JsonAdapter<TestWrapper> adapter = HttpUtils.MOSHI.adapter(TestWrapper.class);
        expect:
        adapter.fromJson(toReader(json)).address == address
		where:
        json | address
        "{\"address\": \"0x1111111111111111111111111111111111111111\"}" | new Address("0x1111111111111111111111111111111111111111")
        "{\"address\": null}" | null
        "{}" | null
	}

	def "Address.toJson"(Address address, String json) {
        JsonAdapter<TestWrapper> adapter = HttpUtils.MOSHI.adapter(TestWrapper.class);
        expect:
        adapter.toJson(new TestWrapper(address)) == json
		where:
        address | json
        new Address("0x2222222222222222222222222222222222222222") | "{\"address\":\"0x2222222222222222222222222222222222222222\"}"
        null | "{}"
	}

}
