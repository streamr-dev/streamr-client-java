[![Build Status](https://travis-ci.com/streamr-dev/streamr-java-client.svg?token=XGdFPZ3zEHKu8U8yLzz6&branch=master)](https://travis-ci.com/streamr-dev/streamr-java-client)

# Java client library for Streamr

Using this library, you can easily interact with Streamr over HTTP and websocket APIs from Java-based applications.

This library is work-in-progress. It is currently in a MVP stage covering a very basic subset of functionality including:

- [Authentication with an API key](#authentication)
- [Creating Streams](#creating-streams)
- [Looking up Streams](#looking-up-streams)
- [Publishing events to Streams](#publishing)
- [Subscribing to events from Streams](#subscribing)
- [Unsubscribing from Streams](#unsubscribing)

# Installation

This library will be published to Maven, but it's not there yet.

# Usage

<a name="authentication"></a>
## Authentication

To authenticate as a Streamr user, simply provide your API key to the `StreamrClient` constructor:

```java
StreamrClient client = new StreamrClient(myApiKey); 
```

The library will automatically fetch a session token using the provided API key to allow authenticated requests to be made.

You can access public resources without authenticating. In this case you can create the instance without any arguments:

```java
StreamrClient client = new StreamrClient(); 
```

<a name="creating-streams"></a>
## Creating Streams

You create Streams via the `create(Stream)` method, passing in a prototype `Stream` object with fields set as you wish. The method returns the `Stream` object that was actually created.

```java
Stream created = client.create(new Stream("Stream name", "Stream description"));
```

<a name="looking-up-streams"></a>
## Looking up Streams

You can look up Streams by `id`:

```java
Stream stream = client.getStream("id-of-the-stream");
```

Or by the name of the Stream (expects an unique result):

```java
Stream stream = client.getStreamByName("My Fancy Stream");
```

<a name="publishing"></a>
## Publishing events to Streams

Events in Streams are key-value pairs, represented in Java as `Map` objects. Below is an example of creating an event payload and publishing it into a Stream:

```java
// Create the message payload, which is represented as a Map
Map<String, Object> msg = new LinkedHashMap<>();
msg.put("foo", "bar");
msg.put("random", Math.random());

// Then publish it!
client.publish(stream, msg);
```

All events are timestamped. The above example assigns the current timestamp to the new event, but you can also provide a timestamp explicitly:

```java
client.publish(stream, msg, new Date());
```

<a name="subscribing"></a>
## Subscribing to events from Streams

By subscribing to Streams, your application gets immediately notified about new events in the Stream. You provide a `MessageHandler` which gets called with new events.

```java
Subscription sub = client.subscribe(stream, new MessageHandler() {
    @Override
    void onMessage(Subscription s, StreamMessage message) {
        // Here you can react to the latest message
        System.out.println(message.getPayload().toString());
    }
})
```

<a name="unsubscribing"></a>
## Unsubscribing from Streams

To stop receiving events from a Stream, pass the `Subscription` object you got when subscribing to the `unsubscribe` method:

```java
client.unsubscribe(sub);
```

# TODO

This library is work in progress. At least the following will be done, probably sooner than later:

- Publishing this library to Maven
- Authenticating with the Ethereum challenge/response protocol
- Signing published data
- Verifying signed data on reception
- Resends on subscription
- Detecting and filling-in gaps in data
- Covering all of the Stream API
- Covering the API endpoints for other resources than Streams

This library is officially developed and maintained by the Streamr core dev team, but community contributions are very welcome!
