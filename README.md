[![Build Status](https://travis-ci.com/streamr-dev/streamr-client-java.svg?branch=master)](https://travis-ci.com/streamr-dev/streamr-client-java)

# Java client library for Streamr

Using this library, you can easily interact with Streamr over HTTP and websocket APIs from Java-based applications.

This library is work-in-progress. It is currently in a MVP stage covering a very basic subset of functionality including:

- [Authentication](#authentication)
- [Data signing](#signing)
- [Creating Streams](#creating-streams)
- [Looking up Streams](#looking-up-streams)
- [Publishing events to Streams](#publishing)
- [Subscribing and unsubscribing to Streams](#subscribing-unsubscribing)

# Installation

This library is published to the Maven Central repository.

## Using Maven

In your `pom.xml`, add the repository:
```
<repositories>
  <repository>
    <url>https://dl.bintray.com/ethereum/maven</url>
  </repository>
  ...
</repositories>
```
And the artifact itself:
```
<dependencies>
  <dependency>
    <groupId>com.streamr</groupId>
    <artifactId>client</artifactId>
    <version>1.0.0</version>
  </dependency>
  ...
</dependencies>
```

## Using Gradle

In your `build.gradle`, add the repository:
```
repositories {
    maven {
        url "https://dl.bintray.com/ethereum/maven/"
    }
}
```
And the artifact itself:
```
dependencies {
    implementation 'com.streamr:client:1.0.0'
}
```

# Usage

Every interaction with the API is done through a `StreamrClient` instance. In the following sections, we will see how to:
- [Create a `StreamrClient` instance with different options](#options)
- [Create Streams](#creating-streams)
- [Look up Streams](#looking-up-streams)
- [Publish events to Streams](#publishing)
- [Subscribe and unsubscribe to Streams](#subscribing-unsubscribing)

<a name="options"></a>
## Instantiation and options

Every interaction with the API is done through a `StreamrClient` object which is constructed with a `StreamrClientOptions` object:

```java
StreamrClientOptions options = ...
StreamrClient client = new StreamrClient(options);
```

The complete constructor of the `StreamrClientOptions` has the following signature:

```java
StreamrClientOptions(
  AuthenticationMethod authenticationMethod,
  SigningOptions signingOptions,
  EncryptionOptions encryptionOptions,
  String websocketApiUrl,
  String restApiUrl,
  int gapFillTimeout,
  int retryResendAfter
)
```

All the classes listed above can be imported from `com.streamr.client.options.*`. Commonly, only the `AuthenticationMethod` has to be set and all the other parameters can be set to their default value, which is why there is a shorthand constructor for the `StreamrClient`:

```java
// See Authentication section
AuthenticationMethod auth = ...
StreamrClient client = new StreamrClient(auth); 
```

Nevertheless, the next subsections will cover every parameter of the `StreamrClientOptions` constructor:
- [Authentication options](#authentication)
- [Signing options](#signing)
- [Encryption options](#encryption)
- [Other options](#other-options)

<a name="authentication"></a>
### Authentication
To authenticate as a Streamr user, provide an `AuthenticationMethod` instance. We have two concrete classes that extend `AuthenticationMethod`:

- `ApiKeyAuthenticationMethod(String apiKey)`
- `EthereumAuthenticationMethod(String ethereumPrivateKey)`

To authenticate with an API key, create an `ApiKeyAuthenticationMethod` instance and pass it to the `StreamrClient` constructor:

```java
StreamrClient client = new StreamrClient(new ApiKeyAuthenticationMethod(myApiKey)); 
```

The library will automatically fetch a session token using the provided API key to allow authenticated requests to be made.

To authenticate with an Ethereum account, create an `EthereumAuthenticationMethod` instance and pass it to the `StreamrClient` constructor:

```java
StreamrClient client = new StreamrClient(new EthereumAuthenticationMethod(myEthereumPrivateKey)); 
```

The library will automatically initiate a challenge-response protocol to allow you to prove that you own the Ethereum private key without revealing it. You will be identified with the associated Ethereum public address. At the end of the protocol, the library will fetch a session token to allow authenticated requests to be made.

You can access public resources without authenticating. In this case you can create the instance without any arguments:

```java
StreamrClient client = new StreamrClient();
```

<a name="signing"></a>
### Signing

The events published to streams can be signed using an Ethereum private key and verified using the corresponding Ethereum public key. The signing options define two policies: one deciding when to sign, the other when to verify.

The `SigningOptions` instance can be constructed as follows: 

```java
SigningOptions.SignatureComputationPolicy signPol = SigningOptions.SignatureComputationPolicy.AUTO; // or ALWAYS or NEVER
SigningOptions.SignatureVerificationPolicy verPol = SigningOptions.SignatureVerificationPolicy.AUTO; // or ALWAYS or NEVER
SigningOptions signingOptions = new SigningOptions(signPol, verPol);
```

The following table describes the meaning of the different values for the `SignatureComputationPolicy` enum.

Option value | Description
------------ | -----------
AUTO | Default value. Published events will be signed if and only if the client is authenticated using the `EthereumAuthenticationMethod`.
ALWAYS | The constructor will throw if the authentication method is not `EthereumAuthenticationMethod`. Will sign events otherwise.
NEVER | Won't sign published events.

The following table describes the meaning of the different values for the `SignatureVerificationPolicy` enum. Note that every stream has a list of valid Ethereum addresses that are allowed to publish. Every stream also has a metadata boolean flag set by the creator of the stream that determines whether events on the stream are supposed to be signed or not.

In the following table, by "verify" we mean:
1) Extract the Ethereum address from the signature and check it's equal to the publisher's address (verify the signature itself)
2) Check that the set of valid publishers Ethereum addresses contains the publisher's address.

Option value | Description
------------ | -----------
AUTO | Default value. All signed events are verified. Unsigned events are accepted if and only if the stream does not require signed data according to the metadata boolean flag.
ALWAYS | Only signed and verified events are accepted.
NEVER | All signed events are verified. Unsigned events are always accepted.

<a name="encryption"></a>
### Encryption

Encryption is still work-in-progress. The documentation will be updated once the implementation is done.

Events published can be encrypted with AES-256 symmetric group keys. For now, they must be set at construction time for the publisher and the subscribers. Later, a secure key exchange protocol will allow the publisher to share the key with their subscribers.

The `EncryptionOptions` instance can be constructed as follows:

```java
// stream --> group key
HashMap<String, String> publisherGroupKeys = new HashMap<>();
// every AES-256 group key must be represented as a hex string ("0x" prefix is optional)
publisherGroupKeys.put("streamId", "0x...");

// streamId --> (publisherId --> groupKeyHex)
HashMap<String, HashMap<String, String>> subscriberGroupKeys = ...;

EncryptionOptions encryptionOptions = new EncryptionOptions(publisherGroupKeys, subscriberGroupKeys);

// can also be constructed using default values which are empty HashMaps
EncryptionOptions defaultOptions = EncryptionOptions.getDefault();
```

<a name="other-options"></a>
### Other options

The following table describes the other options of the `StreamrClientOptions` constructor and their default values.

Option | Default value | Description
------ | ------------- | -----------
websocketApiUrl | wss://www.streamr.com/api/v1/ws | Address of the Streamr websocket endpoint to connect to.
restApiUrl | https://www.streamr.com/api/v1 | Base URL of the Streamr REST API.
gapFillTimeout | 5 seconds | When a gap between two received events is detected, a resend request is sent periodically until the gap is resolved. This option determines that period. 
retryResendAfter | 5 seconds | When subscribing with a resend option (See [this](#subscribing-unsubscribing) section), the messages requested by a first resend request might not be available yet. This option determines after how much time, the resend must be requested a second time.

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
// Each 'Object' in the Map must be serializable to JSON.
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

By default the stream partition is 0, but you can publish using a specific partition key:
```java
client.publish(stream, msg, new Date(), "myPartitionKey");
```

The events can be encrypted using AES-256 in CTR mode. The group keys can be set at construction (See [encryption options](#encryption)). But in order to prevent new subscribers to eavesdrop and then decrypt past messages, the key can be updated at will when publishing:

```java
String newGroupKey = "0x..."
// The key is updated to be 'newGroupKey' and sent along with 'msg'
client.publish(stream, msg, new Date(), null, newGroupKey)
// next messages are encrypted with 'newGroupKey'
client.publish(stream, msg2)
```

<a name="subscribing-unsubscribing"></a>
## Subscribing and unsubscribing to Streams

By subscribing to Streams, your application gets immediately notified about new events in the Stream. You provide a `MessageHandler` which gets called with new events.

```java
Subscription sub = client.subscribe(stream, new MessageHandler() {
    @Override
    void onMessage(Subscription s, StreamMessage message) {
        // Here you can react to the latest message
        System.out.println(message.getContent().toString());
    }
});
```

You can also choose other options such as a specific partition to subscribe to or a resend option:

```java
int partition = 0;
MessageHandler handler = ...
ResendOption resendOption = ...
Subscription sub = client.subscribe(stream, partition, handler, resendOption);
```

Below are examples of ways to construct the `ResendOption`.

```java
// Resends the last 10 events
ResendOption opt = new ResendLastOption(10);
```

```java
// Resends the events from a specific timestamp (and sequence number) for a particular message chain of a publisher
Date from = new Date(341298709);
int sequenceNumber = 0;
ResendOption opt = new ResendFromOption(from, sequenceNumber, "publisherId", "msgChainId");
```

```java
// Resends the events between two timestamps for a particular message chain of a publisher
Date from = new Date(341298709);
Date to = new Date(341299000);
// the 0s are sequence numbers
ResendOption opt = new ResendRangeOption(from, 0, to, 0, "publisherId", "msgChainId");
```

To stop receiving events from a Stream, pass the `Subscription` object you got when subscribing to the `unsubscribe` method:

```java
client.unsubscribe(sub);
```

# TODO

This library is work in progress. At least the following will be done, probably sooner than later:

- Covering all of the Stream API
- Covering the API endpoints for other resources than Streams

This library is officially developed and maintained by the Streamr core dev team, but community contributions are very welcome!
