package com.streamr.client.utils;

import com.streamr.client.options.SigningOptions.SignatureVerificationPolicy;
import com.streamr.client.exceptions.InvalidSignatureException;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.rest.Stream;
import org.cache2k.*;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

public class SubscribedStreamsUtil {
    private Cache<String, Stream> streamsPerStreamId = new Cache2kBuilder<String, Stream>() {}
        .expireAfterWrite(15, TimeUnit.MINUTES).build();
    private Function<String, Stream> getStreamFunction;
    private final AddressValidityUtil addressValidityUtil;
    private final SignatureVerificationPolicy verifySignatures;

    public SubscribedStreamsUtil(Function<String, Stream> getStreamFunction,
                                 AddressValidityUtil addressValidityUtil,
                                 SignatureVerificationPolicy verifySignatures) {
        this.getStreamFunction = getStreamFunction;
        this.addressValidityUtil = addressValidityUtil;
        this.verifySignatures = verifySignatures;
    }

    public void verifyStreamMessage(StreamMessage msg) throws InvalidSignatureException {
        SignatureVerificationResult result = isValid(msg);
        if (!result.isCorrect()) {
            throw new InvalidSignatureException(msg, result.isValidPublisher());
        }
    }

    private SignatureVerificationResult isValid(StreamMessage msg) {
        if (verifySignatures == SignatureVerificationPolicy.ALWAYS) {
            if (!addressValidityUtil.isValidPublisher(msg.getStreamId(), msg.getPublisherId())) {
                return SignatureVerificationResult.invalidPublisher();
            }
            return SignatureVerificationResult.withValidPublisher(SigningUtil.hasValidSignature(msg));
        } else if (verifySignatures == SignatureVerificationPolicy.NEVER) {
            return SignatureVerificationResult.fromBoolean(true);
        }
        // verifySignatures == AUTO
        if(msg.getSignature() != null) {
            if (!addressValidityUtil.isValidPublisher(msg.getStreamId(), msg.getPublisherId())) {
                return SignatureVerificationResult.invalidPublisher();
            }
            return SignatureVerificationResult.withValidPublisher(SigningUtil.hasValidSignature(msg));
        } else {
            Stream stream = getStream(msg.getStreamId());
            return SignatureVerificationResult.fromBoolean(!stream.requiresSignedData());
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

    public void clearAndClose() {
        streamsPerStreamId.clearAndClose();
        addressValidityUtil.clearAndClose();
    }
}
