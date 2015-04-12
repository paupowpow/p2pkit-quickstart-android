# p2pkit.io Quickstart

Rocket science in candy wrap! With just a few lines of code, p2pkit packs a punch by enabling you to accurately discover and directly message users nearby!

### Table of Contents

**[Signup](#signup)**  
**[Setup Android Studio project](#setup-android-studio-project)**  
**[Initialization](#initialization)**  
**[P2P Discovery](#p2p-discovery)**  
**[GEO Discovery](#geo-discovery)**  
**[Online Messaging](#online-messaging)**  
**[Documentation](#documentation)**  


### Signup

Request your personal application key: http://p2pkit.io

### Setup Android Studio project

Include the p2pkit maven repository and p2pkit dependencies in your gradle build files

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
  compile 'ch.uepaa.p2p:p2pkit-android:0.1.10'
}
```

### Initialization

Initialize the `KitClient` by adding corresponding api / listeners and connect with your personal application key

```java
final int isP2PServicesAvailable = KitClient.isP2PServicesAvailable(context);
if (isP2PServicesAvailable == ConnectionResult.SUCCESS) {

    KitClient mP2pClient = new KitClient.Builder(context)
            .addConnectionCallbacks(mConnectionCallbacks)
            .addApi(KitClient.API.P2P_DISCOVERY)
            .addApi(KitClient.API.GEO_DISCOVERY)
            .addApi(KitClient.API.MESSAGING)
            .build();

    DiscoveryServices.addListener(mP2pDiscoveryListener);
    DiscoveryServices.addListener(mGeoDiscoveryListener);
    MessageServices.addListener(mMessageListener);

    mP2pClient.connect(APP_KEY);

} else {
    ConnectionResultHandling.showAlertDialogForConnectionError(context, isP2PServicesAvailable);
}
```

Implement `ConnectionCallbacks` to receive service status callbacks

```java
private final ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks() {
    @Override
    public void onConnected() {
    }

    @Override
    public void onConnectionSuspended() {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }
};
```

### P2P Discovery

Implement `P2pListener` to receive P2P discovery events

```java
private final P2pListener mP2pDiscoveryListener = new P2pListener() {
    @Override
    public void onStateChanged(int state) {
    }

    @Override
    public void onPeerDiscovered(final UUID nodeId) {
    }

    @Override
    public void onPeerLost(final UUID nodeId) {
    }
};
```

### GEO Discovery

Implement `GeoListener` to receive GEO discovery events

```java
private final GeoListener mGeoDiscoveryListener = new GeoListener() {
    @Override
    public void onStateChanged(int state) {
    }

    @Override
    public void onPeerDiscovered(final UUID nodeId) {
    }

    @Override
    public void onPeerLost(final UUID nodeId) {
    }
};
```

### Online Messaging

You can send messages to previously discovered peers using the `MessageServices`

```java
// sending a message to the peer
KitClient.getMessageService().sendMessage(nodeId, "SimpleChatMessage", "Hello!".getBytes());
```

Implement `MessageListener` to receive messages from other peers

```java
private final MessageListener mMessageListener = new MessageListener() {
    @Override
    public void onStateChanged(int state) {
    }

    @Override
    public void onMessageReceived(long timestamp, UUID origin, String type, byte[] message) {
    }
};
```

## Documentation

For more details and further information, please refer to the Javadoc documentation:

http://p2pkit.io/javadoc/
