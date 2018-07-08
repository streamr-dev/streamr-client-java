package com.streamr.client.utils

import com.streamr.client.Subscription
import com.streamr.client.exceptions.SubscriptionNotFoundException
import com.streamr.client.utils.Subscriptions
import spock.lang.Specification

class SubscriptionsSpec extends Specification {

	private final static url = "ws://localhost:8890/api/v1/ws";
	private Subscriptions subs;

	void setup() {
		subs = new Subscriptions()
	}

	void "subscriptions can be added and retrieved"() {
		Subscription sub = new Subscription("stream", 0)

		when:
		subs.add(sub)

		then: "the original instance is returned when querying"
		subs.get(sub).is(sub)
		subs.get(new Subscription("stream", 0)).is(sub)
		subs.get(sub.getId()).is(sub)
	}

	void "getting non-existent sub throws"() {
		Subscription sub = new Subscription("stream", 0)
		subs.add(sub)

		when:
		subs.get(new Subscription("other", 0))

		then:
		thrown(SubscriptionNotFoundException)
	}

	void "subscriptions can be added and removed"() {
		Subscription sub = new Subscription("stream", 0)

		when:
		subs.add(sub)

		then: "the original instance is returned when querying"
		subs.get(sub).is(sub)

		when:
		subs.remove(sub)
		subs.get(sub)

		then:
		thrown(SubscriptionNotFoundException)
	}

}
