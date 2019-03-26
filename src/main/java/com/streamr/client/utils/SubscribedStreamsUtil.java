package com.streamr.client.utils;

import com.streamr.client.SigningOptions.SignatureVerificationPolicy;
import com.streamr.client.exceptions.InvalidSignatureException;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.rest.Stream;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

public class SubscribedStreamsUtil {
    private HashMap<String, Stream> streamsPerStreamId = new HashMap<>();
    private Function<String,Stream> getStreamFunction;
    private HashMap<String, HashSet<String>> publishersPerStreamId = new HashMap<>();
    private Function<String,List<String>> getPublishersFunction;
    private final SignatureVerificationPolicy verifySignatures;

    public SubscribedStreamsUtil(Function<String,Stream> getStreamFunction,
                                 Function<String,List<String>> getPublishersFunction,
                                 SignatureVerificationPolicy verifySignatures) {
        this.getStreamFunction = getStreamFunction;
        this.getPublishersFunction = getPublishersFunction;
        this.verifySignatures = verifySignatures;
    }

    public void verifyStreamMessage(StreamMessage msg) throws InvalidSignatureException {
        if (!isValid(msg)) {
            throw new InvalidSignatureException(msg);
        }
    }

    private boolean isValid(StreamMessage msg) {
        if (verifySignatures == SignatureVerificationPolicy.ALWAYS) {
            HashSet<String> publishers = getPublishers(msg.getStreamId());
            return SigningUtil.hasValidSignature(msg, publishers);
        } else if (verifySignatures == SignatureVerificationPolicy.NEVER) {
            return true;
        }
        // verifySignatures == AUTO
        if(msg.getSignature() != null) {
            HashSet<String> publishers = getPublishers(msg.getStreamId());
            return SigningUtil.hasValidSignature(msg, publishers);
        } else {
            Stream stream = getStream(msg.getStreamId());
            return !stream.requiresSignedData();
        }
    }

    private Stream getStream(String streamId) {
        Stream s = streamsPerStreamId.get(streamId);
        if (s == null) {
            s = getStreamFunction.apply(streamId);
            streamsPerStreamId.put(streamId, s);
        }
        return s;
    }

    private HashSet<String> getPublishers(String streamId) {
        HashSet<String> publishers = publishersPerStreamId.get(streamId);
        if (publishers == null) {
            publishers = new HashSet<>(getPublishersFunction.apply(streamId));
            publishersPerStreamId.put(streamId, publishers);
        }
        return publishers;
    }
}
