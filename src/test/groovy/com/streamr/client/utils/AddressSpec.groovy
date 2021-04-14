package com.streamr.client.utils

import spock.lang.Specification

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
}
