package com.streamr.client.utils

import spock.lang.Specification

import java.util.function.BiFunction
import java.util.function.Function

class AddressValidityUtilSpec extends Specification {
    void "isValidSubscriber()"() {
        int getSubscribersFunctionCallCount = 0
        Function<String, List<String>> getSubscribersFunction = new Function<String, List<String>>() {
            @Override
            List<String> apply(String streamId) {
                getSubscribersFunctionCallCount++
                return streamId == "streamId" ? ["subscriberId1", "subscriberId2"] : null
            }
        }
        int isSubscriberFunctionCallCount = 0
        BiFunction<String, String, Boolean> isSubscriberFunction = new BiFunction<String, String, Boolean>() {
            @Override
            Boolean apply(String streamId, String subscriber) {
                isSubscriberFunctionCallCount++;
                return streamId == "streamId" && subscriber == "subscriberId3"
            }
        }
        AddressValidityUtil util = new AddressValidityUtil(getSubscribersFunction, isSubscriberFunction, null, null)
        when:
        // cache miss --> getting all addresses
        boolean res1 = util.isValidSubscriber("streamId", "subscriberId1")
        // cache hit
        boolean res2 = util.isValidSubscriber("streamId", "subscriberId2")
        // cache miss --> get only this address
        boolean res3 = util.isValidSubscriber("streamId", "subscriberId3")
        // cache miss --> get only this address
        boolean res4 = util.isValidSubscriber("streamId", "subscriberId4")
        // cache hit
        boolean res5 = util.isValidSubscriber("streamId", "subscriberId1")
        // cache hit
        boolean res6 = util.isValidSubscriber("streamId", "subscriberId3")
        // cache hit
        boolean res7 = util.isValidSubscriber("streamId", "subscriberId4")
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
        Function<String, List<String>> getPublishersFunction = new Function<String, List<String>>() {
            @Override
            List<String> apply(String streamId) {
                getPublishersFunctionCallCount++
                return streamId == "streamId" ? ["publisherId1", "publisherId2"] : null
            }
        }
        int isPublisherFunctionCallCount = 0
        BiFunction<String, String, Boolean> isPublisherFunction = new BiFunction<String, String, Boolean>() {
            @Override
            Boolean apply(String streamId, String publisher) {
                isPublisherFunctionCallCount++;
                return streamId == "streamId" && publisher == "publisherId3"
            }
        }
        AddressValidityUtil util = new AddressValidityUtil(null, null, getPublishersFunction, isPublisherFunction)
        when:
        // cache miss --> getting all addresses
        boolean res1 = util.isValidPublisher("streamId", "publisherId1")
        // cache hit
        boolean res2 = util.isValidPublisher("streamId", "publisherId2")
        // cache miss --> get only this address
        boolean res3 = util.isValidPublisher("streamId", "publisherId3")
        // cache miss --> get only this address
        boolean res4 = util.isValidPublisher("streamId", "publisherId4")
        // cache hit
        boolean res5 = util.isValidPublisher("streamId", "publisherId1")
        // cache hit
        boolean res6 = util.isValidPublisher("streamId", "publisherId3")
        // cache hit
        boolean res7 = util.isValidPublisher("streamId", "publisherId4")
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
        Function<String, List<String>> getSubscribersFunction = new Function<String, List<String>>() {
            @Override
            List<String> apply(String streamId) {
                if (streamId == "streamId1") {
                    streamId1CallCount++
                    if (streamId1CallCount == 1) {
                        return ["subscriberId1", "subscriberId2"]
                    } else if (streamId1CallCount == 2) {
                        return ["subscriberId1", "subscriberId3"]
                    } else if (streamId1CallCount == 3) {
                        return ["subscriberId1", "subscriberId3", "subscriberId8"]
                    } else if (streamId1CallCount == 4) {
                        return ["subscriberId4", "subscriberId3", "subscriberId2"]
                    }
                } else if (streamId == "streamId2") {
                    streamId2CallCount++
                    if (streamId2CallCount == 1) {
                        return ["subscriberId1", "subscriberId2"]
                    } else if (streamId2CallCount == 2) {
                        return ["subscriberId1", "subscriberId2"]
                    } else if (streamId2CallCount == 3) {
                        return ["subscriberId5", "subscriberId3", "subscriberId8"]
                    } else if (streamId2CallCount == 4) {
                        return ["subscriberId9", "subscriberId10", "subscriberId11"]
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
