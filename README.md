<p align="center">
  <a href="https://streamr.network">
    <img alt="Streamr" src="https://raw.githubusercontent.com/streamr-dev/streamr-client-javascript/master/readme-header-img.png" width="1320" />
  </a>
</p>

# Java client library for Streamr

Using this library, you can easily interact with Streamr over HTTP and websocket APIs from Java-based applications.

This library is work-in-progress. It is currently in a MVP stage covering a very basic subset of functionality including:

- [Authentication](#authentication)
- [Data signing](#signing)
- [Handling Errors](#handling-errors)
- [Creating Streams](#creating-streams)
- [Looking up Streams](#looking-up-streams)
- [Publishing events to Streams](#publishing)
- [Subscribing and unsubscribing to Streams](#subscribing-unsubscribing)

[![Build Status](https://travis-ci.com/streamr-dev/streamr-client-java.svg?branch=master)](https://travis-ci.com/streamr-dev/streamr-client-java)

## Installation

This library is published to the Maven Central repository.

#### Using Maven

In your `pom.xml`, add the repository:
```
<repositories>
  <repository>
    <url>https://dl.bintray.com/ethereum/maven</url>
  </repository>
  ...
</repositories>
```
And the artifact itself (replace x.y.z with the [latest version](https://mvnrepository.com/artifact/com.streamr/client)):
```
<dependencies>
  <dependency>
    <groupId>com.streamr</groupId>
    <artifactId>client</artifactId>
    <version>x.y.z</version>
  </dependency>
  ...
</dependencies>
```

#### Using Gradle

In your `build.gradle`, add the repository:
```
repositories {
    maven {
        url "https://dl.bintray.com/ethereum/maven/"
    }
}
```
And the artifact itself (replace x.y.z with the [latest version](https://mvnrepository.com/artifact/com.streamr/client)):
```
dependencies {
    implementation 'com.streamr:client:x.y.z'
}
```

## Usage

Every interaction with Streamr is done through a `StreamrClient` instance. In the following sections, we will see how to:
- [Create a `StreamrClient` instance with different options](#options)
- [Create Streams](#creating-streams)
- [Look up Streams](#looking-up-streams)
- [Publish events to Streams](#publishing)
- [Subscribe and unsubscribe to Streams](#subscribing-unsubscribing)

<a name="options"></a>
#### Instantiation and options

Quickstart (unauthenticated):

```java
StreamrClient client = new StreamrClient();
```

Quickstart (authenticated):

```java
// An Ethereum private key to use for signing and identity
String myPrivateKey = "0x..."; 
StreamrClient client = new StreamrClient(new EthereumAuthenticationMethod(myPrivateKey));
```

For full configuration of the client's behavior, you can construct the client with `StreamrClientOptions`:

```java
StreamrClientOptions options = new StreamrClientOptions(...);
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
  int retryResendAfter,
  boolean skipGapsOnFullQueue
)
```

The next subsections will cover every parameter of the `StreamrClientOptions` constructor:
- [Authentication options](#authentication)
- [Signing options](#signing)
- [Encryption options](#encryption)
- [Other options](#other-options)

<a name="authentication"></a>
## Authentication
To authenticate as a Streamr user, provide an `AuthenticationMethod` instance. We have two concrete classes that extend `AuthenticationMethod`:

- `EthereumAuthenticationMethod(String ethereumPrivateKey)`
- `ApiKeyAuthenticationMethod(String apiKey)` (deprecated and will be removed in the future)

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
## Signing

The events published to streams can be signed using an Ethereum private key and verified using the corresponding Ethereum public key. The signing options define two policies: one deciding when to sign, the other when to verify.

The `SigningOptions` instance can be constructed as follows: 

```java
SigningOptions.SignatureComputationPolicy signPol = SigningOptions.SignatureComputationPolicy.AUTO; // or ALWAYS or NEVER
SigningOptions.SignatureVerificationPolicy verPol = SigningOptions.SignatureVerificationPolicy.AUTO; // or ALWAYS or NEVER
SigningOptions signingOptions = new SigningOptions(signPol, verPol);
```

The following table describes the meaning of the different values for the `SignatureComputationPolicy` enum.

Option value | Description
:------------ | :-----------
AUTO | Default value. Published events will be signed if and only if the client is authenticated using the `EthereumAuthenticationMethod`.
ALWAYS | The constructor will throw if the authentication method is not `EthereumAuthenticationMethod`. Will sign events otherwise.
NEVER | Won't sign published events.

The following table describes the meaning of the different values for the `SignatureVerificationPolicy` enum. Note that every stream has a list of valid Ethereum addresses that are allowed to publish. Every stream also has a metadata boolean flag set by the creator of the stream that determines whether events on the stream are supposed to be signed or not.

In the following table, by "verify" we mean:
1) Extract the Ethereum address from the signature and check it's equal to the publisher's address (verify the signature itself)
2) Check that the set of valid publishers Ethereum addresses contains the publisher's address.

Option value | Description
:------------ | :-----------
AUTO | Default value. All signed events are verified. Unsigned events are accepted if and only if the stream does not require signed data according to the metadata boolean flag.
ALWAYS | Only signed and verified events are accepted.
NEVER | All signed events are verified. Unsigned events are always accepted.

<a name="encryption"></a>
## Encryption

We first introduce the `GroupKey` class: it defines a symmetric AES-256 group key used by the publisher to encrypt data and by the subscriber to decrypt data. A new, random `GroupKey` can be generated as follows:

```java
GroupKey groupKey = GroupKey.generate();
```

The `GroupKey` can then be passed to the `publish` method to publish end-to-end encrypted messages:

```java
client.publish(stream, payload, groupKey);
```

To rotate the key, simply generate a new one. This will announce the new key to everyone who has the current key. Rotating the key every now and then establishes forward secrecy: compromised future `GroupKey`s will not reveal previous messages. 

```java
groupKey = GroupKey.generate(); // Generate new key
client.publish(stream, payload, groupKey); // Publish as usual
```

Subscribers normally obtain the `GroupKey` via an automatic key exchange mechanism, which is triggered if the subscriber receives messages for which they don't have the key. As an alternative, keys can also be pre-shared manually and configured on the client like this:

```
client.getKeyStore().add(streamId, new GroupKey(keyId, groupKeyHex));
```

We also need a way to revoke subscribers whose subscription has expired. This is accomplished with a rekey, which means that a new group key is chosen by the publisher and sent to the remaining valid subscribers but not to the revoked ones. The rekey is a fairly intensive operation which should be used only when necessary.

There are two ways to rekey (examples below):
- By using the automatic built-in revocation mechanism: it periodically checks how many subscribers should be revoked and rekey if the number reaches a threshold (5 subscribers).
- By explicitly calling the `client.rekey(stream)` method at any time.

```java
// autoRevoke determines whether the automatic revocation mechanism is to be used or not. 
// In this case, it is deactivated.
boolean autoRevoke = false; // default is true 
EncryptionOptions encryptionOptions = new EncryptionOptions(autoRevoke);

StreamrClient client = new StreamrClient(new StreamrClientOptions(...)); // passing the encryptionOptions here

GroupKey key = GroupKey.generate()
client.publish("streamId", payload, key); // publishing some message with an initial key

// You can trigger a rekey of the stream at any moment to revoke any expired subscribers from the next message.
key = client.rekey("streamId");

// Publish with the new key generated during the rekey.
client.publish("streamId", payload, key); 
```

<a name="other-options"></a>
## Other options

The following table describes the other options of the `StreamrClientOptions` constructor and their default values.

Option | Default value | Description
:------ | :------------- | :-----------
websocketApiUrl | wss://streamr.network/api/v1/ws | Address of the websocket endpoint to connect to.
restApiUrl | https://streamr.network/api/v1 | Base URL of the Streamr REST API.
gapFillTimeout | 5 seconds | When a gap between two received events is detected, a resend request is sent periodically until the gap is resolved. This option determines that period. 
retryResendAfter | 5 seconds | When subscribing with a resend option (See [this](#subscribing-unsubscribing) section), the messages requested by a first resend request might not be available yet. This option determines after how much time, the resend must be requested a second time.
skipGapsOnFullQueue | true | Determine behaviour in the case of gap filling failure. Default behaviour (`true`) is to clear the internal queue of messages and start immediately processing new incoming messages. This means that any queued messages are effectively ignored and skipped. If it is more important that messages be processed at the expense of latency, this should be set to `false`. This will mean that in the case of gap filling failure, the next messages (and potential gaps) in the queue will be processed in order. This comes at the expense of the real-time.

<a name="handling-errors"></a>
## Handling Errors

You can customize error handling by registering an error handler.
```java
client.setErrorMessageHandler({ ErrorResponse error ->
    // handle error
})
```
If no error message handler is register then the error is logged.

<a name="creating-streams"></a>
## Creating Streams

You create Streams via the `create(Stream)` method, passing in a prototype `Stream` object with fields set as you wish. The method returns the `Stream` object that was actually created.

```java
Stream created = client.createStream(new Stream("Stream name", "Stream description"));
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

By default streams have one partition. For streams with multiple partitions, you can map messages to partitions using a partition key. The same partition key always maps to the same partition:
```java
client.publish(stream, msg, new Date(), "myPartitionKey");
```

The events can be end-to-end encrypted by passing a `GroupKey` to the `publish` method:

```java
GroupKey key = GroupKey.generate();
client.publish(stream, msg, key);

// You can rotate the key at any time
GroupKey newKey = GroupKey.generate();
client.publish(stream, msg2, newKey); // message is encrypted with newKey instead of key
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

You can also choose other options such as a specific partition to subscribe to (for load balancing high-volume, partitioned streams), or specify a resend option:

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

## Contributions

This library is officially developed and maintained by the Streamr core dev team, but community contributions are very welcome!
