package com.streamr.client

import com.streamr.client.subs.RealTimeSubscription
import com.streamr.client.subs.Subscription
import com.streamr.client.stream.GroupKeyStore
import com.streamr.client.stream.KeyExchangeUtil
import java.util.function.Consumer
import spock.lang.Specification

class SubscriptionsSpec extends Specification {

	private Subscriptions subs
	private GroupKeyStore keyStore
	private KeyExchangeUtil keyExchangeUtil

	private RealTimeSubscription createSub(String streamId="stream", int partition=0) {
		return new RealTimeSubscription(streamId, partition, null, keyStore, keyExchangeUtil,null)
	}

	void setup() {
		subs = new Subscriptions()
		keyStore = Mock(GroupKeyStore)
		keyExchangeUtil = Mock(KeyExchangeUtil)
	}

	void "subscriptions can be added and retrieved"() {
		Subscription sub = createSub()

		when:
		subs.add(sub)

		then: "the original instance is returned when querying"
		subs.get(sub.getStreamId(), sub.getPartition()).is(sub)
	}

	void "getting non-existent sub throws"() {
		Subscription sub = createSub()
		subs.add(sub)

		when:
		subs.get("other", 0)

		then:
		thrown(SubscriptionNotFoundException)
	}

	void "subscriptions can be added and removed"() {
		Subscription sub = createSub()

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
		Subscription sub0 = createSub("stream", 0)
		Subscription sub3 = createSub("stream", 3)
		Subscription sub4 = createSub("stream", 4)
		Subscription otherSub = createSub("otherStream", 1)
		when:
		subs.add(sub0)
		subs.add(sub3)
		subs.add(sub4)
		subs.add(otherSub)
		then:
		subs.getAllForStreamId("stream").toList() == [sub0, sub3, sub4]
	}

	void "forEach()"() {
		Subscription sub1 = createSub("stream1", 5)
		Subscription sub2 = createSub("stream2", 2)
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
