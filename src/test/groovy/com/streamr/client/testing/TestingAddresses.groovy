package com.streamr.client.testing

import com.streamr.client.utils.Address

final class TestingAddresses {
	private TestingAddresses() {}

	public static Address createSubscriberId(int number) {
		return new Address("subscriberId${number}")
	}

	public static Address createPublisherId(int number) {
		return new Address("publisherId${number}")
	}
}
