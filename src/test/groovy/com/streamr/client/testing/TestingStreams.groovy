package com.streamr.client.testing

final class TestingStreams {
    private TestingStreams() {}

    public static String generateName() {
        return "${this.getClass().getSimpleName()}-${System.currentTimeMillis()}"
    }
}
