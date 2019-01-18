package com.streamr.client.utils


import com.streamr.client.exceptions.SubscriptionNotFoundException
import spock.lang.Specification

class SubscriptionsSpec extends Specification {

	private Subscriptions subs

	void setup() {
		subs = new Subscriptions()
	}

	void "subscriptions can be added and retrieved"() {
		com.streamr.client.Subscription sub = new com.streamr.client.Subscription("stream", 0, null)

		when:
		subs.add(sub)

		then: "the original instance is returned when querying"
		subs.get(sub.getStreamId(), sub.getPartition()).is(sub)
	}

	void "getting non-existent sub throws"() {
		com.streamr.client.Subscription sub = new com.streamr.client.Subscription("stream", 0, null)
		subs.add(sub)

		when:
		subs.get("other", 0)

		then:
		thrown(SubscriptionNotFoundException)
	}

	void "subscriptions can be added and removed"() {
		com.streamr.client.Subscription sub = new com.streamr.client.Subscription("stream", 0, null)

		when:
		subs.add(sub)

		then: "the original instance is returned when querying"
		subs.get(sub.getStreamId(), sub.getPartition()).is(sub)

		when:
		subs.remove(sub)
		subs.get(sub.getStreamId(), sub.getPartition()).is(sub)

		then:
		thrown(SubscriptionNotFoundException)
	}

}
