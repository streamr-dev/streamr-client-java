package com.streamr.client.protocol.message_layer

import com.streamr.client.utils.Address

final class TestingAddresses {
	private TestingAddresses() {}

	public static Address createSubscriberId(int number) {
		return new Address("subscriberId${number}")
	}
}
