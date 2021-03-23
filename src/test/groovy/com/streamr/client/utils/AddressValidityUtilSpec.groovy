package com.streamr.client.utils

import com.streamr.client.protocol.StreamrSpecification

import java.util.function.BiFunction
import java.util.function.Function

class AddressValidityUtilSpec extends StreamrSpecification {

    void "isValidSubscriber()"() {
        int getSubscribersFunctionCallCount = 0
        Function<String, List<Address>> getSubscribersFunction = new Function<String, List<Address>>() {
            @Override
            List<Address> apply(String streamId) {
                getSubscribersFunctionCallCount++
                return streamId == "streamId" ? [getSubscriberId(1), getSubscriberId(2)] : null
            }
        }
        int isSubscriberFunctionCallCount = 0
        BiFunction<String, Address, Boolean> isSubscriberFunction = new BiFunction<String, Address, Boolean>() {
            @Override
            Boolean apply(String streamId, Address subscriber) {
                isSubscriberFunctionCallCount++;
                return streamId == "streamId" && subscriber == getSubscriberId(3)
            }
        }
        AddressValidityUtil util = new AddressValidityUtil(getSubscribersFunction, isSubscriberFunction, null, null)
        when:
        // cache miss --> getting all addresses
        boolean res1 = util.isValidSubscriber("streamId", getSubscriberId(1))
        // cache hit
        boolean res2 = util.isValidSubscriber("streamId", getSubscriberId(2))
        // cache miss --> get only this address
        boolean res3 = util.isValidSubscriber("streamId", getSubscriberId(3))
        // cache miss --> get only this address
        boolean res4 = util.isValidSubscriber("streamId", getSubscriberId(4))
        // cache hit
        boolean res5 = util.isValidSubscriber("streamId", getSubscriberId(1))
        // cache hit
        boolean res6 = util.isValidSubscriber("streamId", getSubscriberId(3))
        // cache hit
        boolean res7 = util.isValidSubscriber("streamId", getSubscriberId(4))
        then:
        getSubscribersFunctionCallCount == 1
        isSubscriberFunctionCallCount == 2
        res1
        res2
        res3
        !res4
        res5
        res6
        !res7
    }
    void "isValidPublisher()"() {
        int getPublishersFunctionCallCount = 0
        Function<String, List<Address>> getPublishersFunction = new Function<String, List<Address>>() {
            @Override
            List<String> apply(String streamId) {
                getPublishersFunctionCallCount++
                return streamId == "streamId" ? [getPublisherId(1), getPublisherId(2)] : null
            }
        }
        int isPublisherFunctionCallCount = 0
        BiFunction<String, Address, Boolean> isPublisherFunction = new BiFunction<String, Address, Boolean>() {
            @Override
            Boolean apply(String streamId, Address publisher) {
                isPublisherFunctionCallCount++;
                return streamId == "streamId" && publisher == getPublisherId(3)
            }
        }
        AddressValidityUtil util = new AddressValidityUtil(null, null, getPublishersFunction, isPublisherFunction)
        when:
        // cache miss --> getting all addresses
        boolean res1 = util.isValidPublisher("streamId", getPublisherId(1))
        // cache hit
        boolean res2 = util.isValidPublisher("streamId", getPublisherId(2))
        // cache miss --> get only this address
        boolean res3 = util.isValidPublisher("streamId", getPublisherId(3))
        // cache miss --> get only this address
        boolean res4 = util.isValidPublisher("streamId", getPublisherId(4))
        // cache hit
        boolean res5 = util.isValidPublisher("streamId", getPublisherId(1))
        // cache hit
        boolean res6 = util.isValidPublisher("streamId", getPublisherId(3))
        // cache hit
        boolean res7 = util.isValidPublisher("streamId", getPublisherId(4))
        then:
        getPublishersFunctionCallCount == 1
        isPublisherFunctionCallCount == 2
        res1
        res2
        res3
        !res4
        res5
        res6
        !res7
    }
    void "nbSubscribersToRevoke()"() {
        int streamId1CallCount = 0
        int streamId2CallCount = 0
        Function<String, List<Address>> getSubscribersFunction = new Function<String, List<Address>>() {
            @Override
            List<String> apply(String streamId) {
                if (streamId == "streamId1") {
                    streamId1CallCount++
                    if (streamId1CallCount == 1) {
                        return [getSubscriberId(1), getSubscriberId(2)]
                    } else if (streamId1CallCount == 2) {
                        return [getSubscriberId(1), getSubscriberId(3)]
                    } else if (streamId1CallCount == 3) {
                        return [getSubscriberId(1), getSubscriberId(3), getSubscriberId(8)]
                    } else if (streamId1CallCount == 4) {
                        return [getSubscriberId(4), getSubscriberId(3), getSubscriberId(2)]
                    }
                } else if (streamId == "streamId2") {
                    streamId2CallCount++
                    if (streamId2CallCount == 1) {
                        return [getSubscriberId(1), getSubscriberId(2)]
                    } else if (streamId2CallCount == 2) {
                        return [getSubscriberId(1), getSubscriberId(2)]
                    } else if (streamId2CallCount == 3) {
                        return [getSubscriberId(5), getSubscriberId(3), getSubscriberId(8)]
                    } else if (streamId2CallCount == 4) {
                        return [getSubscriberId(9), getSubscriberId(10), getSubscriberId(11)]
                    }
                }
                return null
            }
        }
        when:
        AddressValidityUtil util = new AddressValidityUtil(getSubscribersFunction, null, null, null)
        then:
        util.nbSubscribersToRevoke("streamId1") == 0
        util.nbSubscribersToRevoke("streamId2") == 0
        util.nbSubscribersToRevoke("streamId1") == 1
        util.nbSubscribersToRevoke("streamId2") == 0
        util.nbSubscribersToRevoke("streamId1") == 0
        util.nbSubscribersToRevoke("streamId2") == 2
        util.nbSubscribersToRevoke("streamId1") == 2
        util.nbSubscribersToRevoke("streamId2") == 3
    }
}
