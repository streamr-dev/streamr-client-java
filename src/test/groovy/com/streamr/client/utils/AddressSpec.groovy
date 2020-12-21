package com.streamr.client.utils

import org.web3j.utils.Numeric
import spock.lang.Specification

class AddressSpec extends Specification {
	void "creates address with given String"() {
		setup:
		String input = "0x0000000000000000000000000000000000000001"
		when:
		Address address = new Address(input)
		then:
		address.toString() == input
	}

	void "creates address with given byte array"() {
		setup:
		byte[] input = Numeric.hexStringToByteArray("0000000000000000000000000000000000000001")
		when:
		Address address = new Address(input)
		then:
		address.toString() == "0x0000000000000000000000000000000000000001"
	}
}
