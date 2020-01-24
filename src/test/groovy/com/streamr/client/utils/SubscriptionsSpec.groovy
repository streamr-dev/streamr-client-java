package com.streamr.client.utils

import com.streamr.client.exceptions.SubscriptionNotFoundException
import com.streamr.client.subs.RealTimeSubscription
import com.streamr.client.subs.Subscription
import spock.lang.Specification

import java.util.function.Consumer

class SubscriptionsSpec extends Specification {

	private Subscriptions subs

	void setup() {
		subs = new Subscriptions()
	}

	void "subscriptions can be added and retrieved"() {
		Subscription sub = new RealTimeSubscription("stream", 0, null)

		when:
		subs.add(sub)

		then: "the original instance is returned when querying"
		subs.get(sub.getStreamId(), sub.getPartition()).is(sub)
	}

	void "getting non-existent sub throws"() {
		Subscription sub = new RealTimeSubscription("stream", 0, null)
		subs.add(sub)

		when:
		subs.get("other", 0)

		then:
		thrown(SubscriptionNotFoundException)
	}

	void "subscriptions can be added and removed"() {
		Subscription sub = new RealTimeSubscription("stream", 0, null)

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

	void "getAllForStreamId()"() {
		Subscription sub0 = new RealTimeSubscription("stream", 0, null)
		Subscription sub3 = new RealTimeSubscription("stream", 3, null)
		Subscription sub4 = new RealTimeSubscription("stream", 4, null)
		Subscription otherSub = new RealTimeSubscription("otherStream", 1, null)
		when:
		subs.add(sub0)
		subs.add(sub3)
		subs.add(sub4)
		subs.add(otherSub)
		then:
		subs.getAllForStreamId("stream").toList() == [sub0, sub3, sub4]
	}

	void "forEach()"() {
		Subscription sub1 = new RealTimeSubscription("stream1", 5, null)
		Subscription sub2 = new RealTimeSubscription("stream2", 2, null)
		ArrayList<Subscription> called = []
		Consumer<Subscription> f = new Consumer<Subscription>() {
			@Override
			void accept(Subscription subscription) {
				called.add(subscription)
			}
		}
		when:
		subs.add(sub1)
		subs.add(sub2)
		subs.forEach(f)
		then:
		called == [sub1, sub2]
	}

}
