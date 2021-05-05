package com.streamr.client.utils

import org.web3j.utils.Numeric
import spock.lang.Specification
import spock.lang.Unroll

class AddressSpec extends Specification {

    def "Address.valid"() {
        when:
        Address address = new Address("0xabcdeabcde123456789012345678901234567890")
		then:
        address.toString() == "0xAbcdeabCDE123456789012345678901234567890"
	}

    def "Address.invalid"() {
        when:
        Address address = new Address("foobar")
		then:
        thrown IllegalArgumentException
	}

    def "Address.null"() {
        when:
        Address address = new Address((String) null)
		then:
        thrown IllegalArgumentException
    }

    def "Address.createRandom"() {
        when:
        Address address = Address.createRandom()
		then:
        address.toString().startsWith("0x")
        address.toString().length() == 42
    }

	@Unroll
	def "Address.equals"(String a, String b, Boolean result) {
		expect:
		new Address(Numeric.hexStringToByteArray(a)).equals(new Address(Numeric.hexStringToByteArray(b))) == result

		where:
		a | b | result
		"abcdeabcde123456789012345678901234567890" | "abcdeabcde123456789012345678901234567890" | true
		"0xa5374e3c19f15e1847881979dd0c6c9ffe846bd5" | "0xa5374e3C19f15E1847881979Dd0C6C9ffe846BD5" | true
		"0xabcdeabcde123456789012345678901234567890" | "0x999999bcde123456789012345678901234999999" | false
	}
}
