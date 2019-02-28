package com.streamr.client.utils;

import com.streamr.client.rest.Stream;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class StreamCachingUtil {
    private HashMap<String, Stream> streamsPerStreamId = new HashMap<>();
    private Function<String,Stream> getStreamFunction;
    private HashMap<String, List<String>> publishersPerStreamId = new HashMap<>();
    private Function<String,List<String>> getPublishersFunction;

    public StreamCachingUtil(Function<String,Stream> getStreamFunction, Function<String,List<String>> getPublishersFunction) {
        this.getStreamFunction = getStreamFunction;
        this.getPublishersFunction = getPublishersFunction;
    }

    public Stream getStream(String streamId) {
        Stream s = streamsPerStreamId.get(streamId);
        if (s == null) {
            s = getStreamFunction.apply(streamId);
            streamsPerStreamId.put(streamId, s);
        }
        return s;
    }

    public List<String> getPublishers(String streamId) {
        List<String> publishers = publishersPerStreamId.get(streamId);
        if (publishers == null) {
            publishers = getPublishersFunction.apply(streamId);
            publishersPerStreamId.put(streamId, publishers);
        }
        return publishers;
    }
}
