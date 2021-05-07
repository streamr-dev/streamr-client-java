package com.streamr.client.rest

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import com.streamr.client.utils.Address
import java.nio.charset.StandardCharsets
import okio.Buffer
import spock.lang.Specification

class TestWrapper {
	Address address

	TestWrapper(Address address) {
		this.address = address
	}
}

class AddressJsonAdapterSpec extends Specification {
	private static JsonReader toReader(String json) {
		return JsonReader.of(new Buffer().writeString(json, StandardCharsets.UTF_8))
	}
	private JsonAdapter<TestWrapper> adapter = new Moshi.Builder()
			.add(Address, new AddressJsonAdapter().nullSafe())
			.build()
			.adapter(TestWrapper)

	def "Address.fromJson"(String json, Address address) {
		expect:
		adapter.fromJson(toReader(json)).address == address
		where:
		json | address
		"{\"address\": \"0x1111111111111111111111111111111111111111\"}" | new Address("0x1111111111111111111111111111111111111111")
		"{\"address\": null}" | null
		"{}" | null
	}

	def "Address.toJson"(Address address, String json) {
		expect:
		adapter.toJson(new TestWrapper(address)) == json
		where:
		address | json
		new Address("0x2222222222222222222222222222222222222222") | "{\"address\":\"0x2222222222222222222222222222222222222222\"}"
		null | "{}"
	}
}
