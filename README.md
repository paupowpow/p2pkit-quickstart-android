# p2pkit.io Android Quickstart

#### A hyperlocal interaction toolkit
p2pkit is an easy to use SDK that bundles together several discovery technologies kung-fu style! With just a few lines of code, p2pkit enables you to accurately discover and directly message users nearby.

### Table of Contents

**[Signup](#signup)**  
**[Setup Android Studio project](#setup-android-studio-project)**  
**[Initialization](#initialization)**  
**[P2P Discovery](#p2p-discovery)**  
**[GEO Discovery](#geo-discovery)**  
**[Online Messaging](#online-messaging)**  
**[Content Provider API](#content-provider-api)**  
**[Documentation](#documentation)**  
**[p2pkit License](#p2pkit-license)**

### Quickstart video
[![Get started video](https://i.ytimg.com/vi/kmFOgtEbFLM/mqdefault.jpg)](https://youtu.be/kmFOgtEbFLM)

[Watch video here](https://youtu.be/kmFOgtEbFLM)

### Signup

Request your personal application key: http://p2pkit.io/signup.html

### Setup Android Studio project

Include the p2pkit maven repository and p2pkit dependencies in your gradle build files.

```
repositories {
  ...
  maven {
    url "http://p2pkit.io/maven2"
  }
}
...
dependencies {
  ...
  compile 'ch.uepaa.p2p:p2pkit-android:1.0.3'
}
```

### Initialization

Initialize the `KitClient` by calling `connect()` using your personal application key.

```java
final int statusCode = KitClient.isP2PServicesAvailable(this);
if (statusCode == ConnectionResult.SUCCESS) {
    KitClient client = KitClient.getInstance(this);
    client.registerConnectionCallbacks(mConnectionCallbacks);

    if (client.isConnected()) {
        Log.d(TAG, "Client already initialized");
    } else {
        Log.d(TAG, "Connecting P2PKit client");
        client.connect(APP_KEY);
    }

} else {
    ConnectionResultHandling.showAlertDialogForConnectionError(this, statusCode);
}
```

Implement `ConnectionCallbacks` to receive service status callbacks.

```java
private final ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks() {
    @Override
    public void onConnected() {
        //ready to start discovery
    }

    @Override
    public void onConnectionSuspended() {
        //p2pkit is now disconnected
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //connection failed, handle connectionResult
    }
};
```

Once the `ConnectionCallbacks` are registered with the KitClient instance, updates to the connection state will be forwarded to the listener.
Note that if a listener is registered to a `KitClient` that's already connected, `onConnected()` will directly be called.

## API
An API is considered to be in use if it has one or more listeners registered. If no listeners are registered,
it is assumed that no one is interested in this API and it might be disabled for battery saving reasons.

### P2P Discovery

Implement `P2pListener` to receive P2P discovery and update events.

```java
private final P2pListener mP2pDiscoveryListener = new P2pListener() {
    @Override
    public void onStateChanged(final int state) {
        Log.d(TAG, "P2pListener | State changed: " + state);
    }

    @Override
    public void onPeerDiscovered(final Peer peer) {
        Log.d(TAG, "P2pListener | Peer discovered: " + peer.getNodeId() + " with info: " + new String(peer.getDiscoveryInfo()));
    }

    @Override
    public void onPeerLost(final Peer peer) {
        Log.d(TAG, "P2pListener | Peer lost: " + peer.getNodeId());
    }

    @Override
    public void onPeerUpdatedDiscoveryInfo(Peer peer) {
        Log.d(TAG, "P2pListener | Peer updated: " + peer.getNodeId() + " with new info: " + new String(peer.getDiscoveryInfo()));
    }
};
```

Set or update the discovery info, which other peers will receive. Note that the info cannot be longer than 440 bytes.

```java
try {
        KitClient.getInstance(this).getDiscoveryServices().setP2pDiscoveryInfo("Hello p2pkit".getBytes());
} catch (InfoTooLongException e) {
        logToView("P2pListener | The discovery info is too long");
}
```

Register the listener to get event updates and enable P2P Discovery.

```java
KitClient.getInstance(context).getDiscoveryServices().addListener(mP2pDiscoveryListener);
```

Note that the discovery info can be omitted and delivered with a later call to `onPeerUpdatedDiscoveryInfo(Peer peer)`. This depends on the technology and load.

### GEO Discovery

Implement `GeoListener` to receive GEO discovery events.

```java
private final GeoListener mGeoDiscoveryListener = new GeoListener() {
    @Override
    public void onStateChanged(int state) {
        Log.d(TAG, "GeoListener | State changed: " + state);
    }

    @Override
    public void onPeerDiscovered(final UUID nodeId) {
        Log.d(TAG, "GeoListener | Peer discovered: " + nodeId);
    }

    @Override
    public void onPeerLost(final UUID nodeId) {
        Log.d(TAG, "GeoListener | Peer lost: " + nodeId);
    }
};
```

Register the listener to get event updates and enable GEO Discovery.

```java
KitClient.getInstance(context).getDiscoveryServices().addListener(mGeoDiscoveryListener);
```

### Online Messaging

Implement `MessageListener` to receive messages from other peers

```java
private final MessageListener mMessageListener = new MessageListener() {
    @Override
    public void onStateChanged(int state) {
        Log.d(TAG, "MessageListener | State changed: " + state);
    }

    @Override
    public void onMessageReceived(long timestamp, UUID origin, String type, byte[] message) {
        Log.d(TAG, "MessageListener | Message received: From=" + origin + " type=" + type + " message=" + new String(message));
    }
};
```

Register the listener to get event updates/receive messages and enable Online Messaging.

```java
KitClient.getInstance(context).getMessageServices().addListener(mMessageListener);
```

You can send messages to previously discovered peers using the `MessageServices`.

```java
// sending a message to the peer
boolean forwarded = KitClient.getInstance(context).getMessageServices().sendMessage(nodeId, "text/plain", "Hello!".getBytes());
```
Note that the KitClient needs to be connected and Online Messaging must be enabled in order to forward a message.

### Content Provider

When the KitClient is successfully connected, information about discovered peers (P2P and GEO) is available by querying the 'Peers ContentProvider'. This returns data about all currently visible and historically discovered peers, i.e. the current state of all discovered peers.

```java
Uri peersContentUri = KitClient.getInstance(context).getPeerContentUri();
ContentResolver contentResolver = context.getContentResolver();

Cursor cursor = contentResolver.query(peersContentUri, null, null, null, null);

int nodeIdColumnIndex = cursor.getColumnIndex(PeersContract.NODE_ID);
int lastSeenColumnIndex = cursor.getColumnIndex(PeersContract.LAST_SEEN);

while (cursor.moveToNext()) {
    UUID nodeId = UUID.fromString(cursor.getString(nodeIdColumnIndex));
    long lastSeen = cursor.getLong(lastSeenColumnIndex);

    Log.d("TAG", "Peer: " + nodeId + " was last seen: " + SimpleDateFormat.getInstance().format(new Date(lastSeen)));
}
cursor.close();
```

For all available data columns please see the documentation for the 'PeersContract'.

## Documentation

For more details and further information, please refer to the Javadoc documentation:

http://p2pkit.io/javadoc/

### p2pkit License

* By using P2PKit you agree to abide by our License (which is included with P2PKit) and Terms Of Service available at http://www.p2pkit.io/policy.html.
* Please refer to "Third_party_licenses.txt" included with P2PKit Quickstart for 3rd party software that P2PKit Quickstart may be using - You will need to abide by their licenses as well.