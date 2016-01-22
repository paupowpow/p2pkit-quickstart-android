# p2pkit.io Android Quickstart

#### A hyperlocal interaction toolkit
p2pkit is an easy to use SDK that bundles together several proximity technologies kung-fu style! With p2pkit apps immediately understand their proximity to nearby devices and users, 'verify' their identity, and exchange information with them.

### Table of Contents

**[Get started video](#get-started-video)**  
**[How it works](#how-it-works)**  
**[Signup](#signup)**  
**[Release notes](#release-notes)**  
**[Setup Android Studio project](#setup-android-studio-project)**  
**[Documentation](#documentation)**  
**[Initialization](#initialization)**  
**[P2P Discovery](#p2p-discovery)**  
**[GEO Discovery (beta)](#geo-discovery-beta)**  
**[Online Messaging (beta)](#online-messaging-beta)**  
**[Content Provider API](#content-provider)**  
**[p2pkit License](#p2pkit-license)**

### Get started video
[![Get started video](https://i.ytimg.com/vi/iId5n7lhJ5Y/mqdefault.jpg)](https://youtu.be/iId5n7lhJ5Y)

[Watch video here](https://youtu.be/iId5n7lhJ5Y)

### How it works

<img src="https://github.com/Uepaa-AG/p2pkit-quickstart-android/blob/master/p2pkit-android-explained.jpg" alt="how it works" width="600" height="359">

The Android P2PKit library, called `P2PKitClient`, is an API library. The functionality is provided by the P2P Services app available in the Android Play Store. This has the advantage that the users will benefit from any improvements made to P2P Services, without updating the app using P2PKit.
The P2P Services are similar in their workings to the Google Play Services.

### Signup

Request your evaluation/testing application key: http://p2pkit.io/signup.html

### Release notes

Release notes can be found at http://p2pkit.io/changelog.html.

### Setup Android Studio project
**P2PKit supports Android version 4.1 and above.**

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
  compile 'ch.uepaa.p2p:p2pkit-android:1.1.1'
}
```

## Documentation

For detailed information about the api calls, please refer to the Javadoc documentation: http://p2pkit.io/javadoc/

### Enabling

Enable the `P2PKitClient` by calling `enableP2PKit()` using your `P2PKitStatusCallback` and your personal application key.

```java
final StatusResult result = P2PKitClient.isP2PServicesAvailable(this);
if (result.getStatusCode() == StatusResult.SUCCESS) {
    P2PKitClient client = P2PKitClient.getInstance(this);
    client.enableP2PKit(mStatusCallback, APP_KEY);
} else {
    StatusResultHandling.showAlertDialogForStatusError(this, result);
}
```

Implement `P2PKitStatusCallback` to receive updates about the status of the P2P Services. `onEnabled()` is immediately called for every listener added, if `P2PKitClient` is already enabled.


```java
private final P2PKitStatusCallback mStatusCallback = new P2PKitStatusCallback() {
    @Override
    public void onEnabled() {
        // ready to start discovery
    }

    @Override
    public void onSuspended() {
        // p2pkit is now suspended
    }

    @Override
    public void onError(StatusResult statusResult) {
        // enabling failed, handle statusResult
    }
};
```

## API

An API (such as P2P Discovery, Geo Discovery and online Messaging) is considered to be in use if it has one or more listeners registered. If no listeners are registered,
it is assumed that no one is interested in this API and it might be disabled for battery saving reasons.
Using P2P Discovery nearby peers are discovered by P2P technologies only. Geo Discovery means that devices nearby discover each other using their position and the cloud.

### P2P Discovery

Implement `P2PListener` to receive P2P discovery and update events.

```java
private final P2PListener mP2PDiscoveryListener = new P2PListener() {
    @Override
    public void onP2PStateChanged(final int state) {
        Log.d(TAG, "P2PListener | State changed: " + state);
    }

    @Override
    public void onPeerDiscovered(final Peer peer) {
        Log.d(TAG, "P2PListener | Peer discovered: " + peer.getNodeId() + " with info: " + new String(peer.getDiscoveryInfo()));
    }

    @Override
    public void onPeerLost(final Peer peer) {
        Log.d(TAG, "P2PListener | Peer lost: " + peer.getNodeId());
    }

    @Override
    public void onPeerUpdatedDiscoveryInfo(Peer peer) {
        Log.d(TAG, "P2PListener | Peer updated: " + peer.getNodeId() + " with new info: " + new String(peer.getDiscoveryInfo()));
    }
};
```

Register the listener to enable P2P Discovery and to get event updates. The P2PKitClient must be enabled before the listeners can be added.

```java
P2PKitClient.getInstance(context).getDiscoveryServices().addP2PListener(mP2PDiscoveryListener);
```

Set or update the discovery info, that other peers will receive.

```java
try {
        P2PKitClient.getInstance(this).getDiscoveryServices().setP2pDiscoveryInfo("Hello p2pkit".getBytes());
} catch (InfoTooLongException e) {
        logToView("P2PListener | The discovery info is too long");
}
```
Note that on a discovery event the discovery info can be omitted and will be delivered later by calling `onPeerUpdatedDiscoveryInfo(Peer peer)`. This depends on the technology used to discover the peer and the number of peers around.

### GEO Discovery (beta)

Implement `GeoListener` to receive GEO discovery events.

```java
private final GeoListener mGeoDiscoveryListener = new GeoListener() {
    @Override
    public void onGeoStateChanged(int state) {
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

Register the listener to enable GEO Discovery and to receive event updates. The P2PKitClient must be enabled before the listeners can be added.

```java
P2PKitClient.getInstance(context).getDiscoveryServices().addGeoListener(mGeoDiscoveryListener);
```

### Online Messaging (beta)

Implement a `MessageListener` to receive messages from other peers

```java
private final MessageListener mMessageListener = new MessageListener() {
    @Override
    public void onMessageStateChanged(int state) {
        Log.d(TAG, "MessageListener | State changed: " + state);
    }

    @Override
    public void onMessageReceived(long timestamp, UUID origin, String type, byte[] message) {
        Log.d(TAG, "MessageListener | Message received: From=" + origin + " type=" + type + " message=" + new String(message));
    }
};
```

Register the listener to get updates/receive messages and enable Online Messaging.

```java
P2PKitClient.getInstance(context).getMessageServices().addMessageListener(mMessageListener);
```

You can send messages to previously discovered peers using the `MessageServices`.

```java
// sending a message to the peer
boolean forwarded = P2PKitClient.getInstance(context).getMessageServices().sendMessage(nodeId, "text/plain", "Hello!".getBytes());
```
Note that the P2PKitClient and Online Messaging need to be enabled on both peers in order to forward a message.

### Content Provider (Deprecated)
**Note: The content provider will be removed in one of the next releases.**

When the P2PKitClient is enabled information about discovered peers (P2P and GEO) is available by querying the 'Peers ContentProvider'. This returns data about all currently visible and historically discovered peers, i.e. the current state of all discovered peers.

```java
Uri peersContentUri = P2PKitClient.getInstance(context).getPeerContentUri();
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

### p2pkit License

* By using P2PKit you agree to abide by our Terms of Service, License Agreement and Policies which are available at the following address: http://p2pkit.io/policy.html
