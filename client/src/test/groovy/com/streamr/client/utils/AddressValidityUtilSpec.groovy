package com.streamr.client.utils

import com.streamr.client.testing.TestingAddresses
import com.streamr.ethereum.common.Address
import java.util.function.BiFunction
import java.util.function.Function
import spock.lang.Specification

class AddressValidityUtilSpec extends Specification {

    void "isValidSubscriber()"() {
        int getSubscribersFunctionCallCount = 0
        Function<String, List<Address>> getSubscribersFunction = new Function<String, List<Address>>() {
            @Override
            List<Address> apply(String streamId) {
                getSubscribersFunctionCallCount++
                return streamId == "streamId" ? [TestingAddresses.createSubscriberId(1), TestingAddresses.createSubscriberId(2)] : null
            }
        }
        int isSubscriberFunctionCallCount = 0
        BiFunction<String, Address, Boolean> isSubscriberFunction = new BiFunction<String, Address, Boolean>() {
            @Override
            Boolean apply(String streamId, Address subscriber) {
                isSubscriberFunctionCallCount++;
                return streamId == "streamId" && subscriber == TestingAddresses.createSubscriberId(3)
            }
        }
        AddressValidityUtil util = new AddressValidityUtil(getSubscribersFunction, isSubscriberFunction, null, null)
        when:
        // cache miss --> getting all addresses
        boolean res1 = util.isValidSubscriber("streamId", TestingAddresses.createSubscriberId(1))
        // cache hit
        boolean res2 = util.isValidSubscriber("streamId", TestingAddresses.createSubscriberId(2))
        // cache miss --> get only this address
        boolean res3 = util.isValidSubscriber("streamId", TestingAddresses.createSubscriberId(3))
        // cache miss --> get only this address
        boolean res4 = util.isValidSubscriber("streamId", TestingAddresses.createSubscriberId(4))
        // cache hit
        boolean res5 = util.isValidSubscriber("streamId", TestingAddresses.createSubscriberId(1))
        // cache hit
        boolean res6 = util.isValidSubscriber("streamId", TestingAddresses.createSubscriberId(3))
        // cache hit
        boolean res7 = util.isValidSubscriber("streamId", TestingAddresses.createSubscriberId(4))
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
                return streamId == "streamId" ? [TestingAddresses.createPublisherId(1), TestingAddresses.createPublisherId(2)] : null
            }
        }
        int isPublisherFunctionCallCount = 0
        BiFunction<String, Address, Boolean> isPublisherFunction = new BiFunction<String, Address, Boolean>() {
            @Override
            Boolean apply(String streamId, Address publisher) {
                isPublisherFunctionCallCount++;
                return streamId == "streamId" && publisher == TestingAddresses.createPublisherId(3)
            }
        }
        AddressValidityUtil util = new AddressValidityUtil(null, null, getPublishersFunction, isPublisherFunction)
        when:
        // cache miss --> getting all addresses
        boolean res1 = util.isValidPublisher("streamId", TestingAddresses.createPublisherId(1))
        // cache hit
        boolean res2 = util.isValidPublisher("streamId", TestingAddresses.createPublisherId(2))
        // cache miss --> get only this address
        boolean res3 = util.isValidPublisher("streamId", TestingAddresses.createPublisherId(3))
        // cache miss --> get only this address
        boolean res4 = util.isValidPublisher("streamId", TestingAddresses.createPublisherId(4))
        // cache hit
        boolean res5 = util.isValidPublisher("streamId", TestingAddresses.createPublisherId(1))
        // cache hit
        boolean res6 = util.isValidPublisher("streamId", TestingAddresses.createPublisherId(3))
        // cache hit
        boolean res7 = util.isValidPublisher("streamId", TestingAddresses.createPublisherId(4))
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
                        return [TestingAddresses.createSubscriberId(1), TestingAddresses.createSubscriberId(2)]
                    } else if (streamId1CallCount == 2) {
                        return [TestingAddresses.createSubscriberId(1), TestingAddresses.createSubscriberId(3)]
                    } else if (streamId1CallCount == 3) {
                        return [TestingAddresses.createSubscriberId(1), TestingAddresses.createSubscriberId(3), TestingAddresses.createSubscriberId(8)]
                    } else if (streamId1CallCount == 4) {
                        return [TestingAddresses.createSubscriberId(4), TestingAddresses.createSubscriberId(3), TestingAddresses.createSubscriberId(2)]
                    }
                } else if (streamId == "streamId2") {
                    streamId2CallCount++
                    if (streamId2CallCount == 1) {
                        return [TestingAddresses.createSubscriberId(1), TestingAddresses.createSubscriberId(2)]
                    } else if (streamId2CallCount == 2) {
                        return [TestingAddresses.createSubscriberId(1), TestingAddresses.createSubscriberId(2)]
                    } else if (streamId2CallCount == 3) {
                        return [TestingAddresses.createSubscriberId(5), TestingAddresses.createSubscriberId(3), TestingAddresses.createSubscriberId(8)]
                    } else if (streamId2CallCount == 4) {
                        return [TestingAddresses.createSubscriberId(9), TestingAddresses.createSubscriberId(10), TestingAddresses.createSubscriberId(11)]
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
