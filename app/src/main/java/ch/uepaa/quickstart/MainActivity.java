package ch.uepaa.quickstart;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.TextView;

import java.util.UUID;

import ch.uepaa.p2pkit.ConnectionCallbacks;
import ch.uepaa.p2pkit.ConnectionResult;
import ch.uepaa.p2pkit.ConnectionResultHandling;
import ch.uepaa.p2pkit.KitClient;
import ch.uepaa.p2pkit.discovery.DiscoveryServices;
import ch.uepaa.p2pkit.discovery.GeoListener;
import ch.uepaa.p2pkit.discovery.P2pListener;
import ch.uepaa.p2pkit.messaging.MessageListener;
import ch.uepaa.p2pkit.messaging.MessageServices;

public class MainActivity extends ActionBarActivity {

    private static final String APP_KEY = "<YOUR APP KEY>";
    private final P2pListener mP2pDiscoveryListener = new P2pListener() {
        @Override
        public void onStateChanged(final int state) {
            logToView("P2pListener | State changed: " + state);
        }

        @Override
        public void onPeerDiscovered(final UUID nodeId) {
            logToView("P2pListener | Peer discovered: " + nodeId);

            // sending a message to the peer
            KitClient.getInstance(MainActivity.this).getMessageServices().sendMessage(nodeId, "SimpleChatMessage", "From Android: Hello P2P!".getBytes());
        }

        @Override
        public void onPeerLost(final UUID nodeId) {
            logToView("P2pListener | Peer lost: " + nodeId);
        }
    };
    private final GeoListener mGeoDiscoveryListener = new GeoListener() {
        @Override
        public void onStateChanged(final int state) {
            logToView("GeoListener | State changed: " + state);
        }

        @Override
        public void onPeerDiscovered(final UUID nodeId) {
            logToView("GeoListener | Peer discovered: " + nodeId);

            // sending a message to the peer
            KitClient.getInstance(MainActivity.this).getMessageServices().sendMessage(nodeId, "SimpleChatMessage", "From Android: Hello GEO!".getBytes());
        }

        @Override
        public void onPeerLost(final UUID nodeId) {
            logToView("GeoListener | Peer lost: " + nodeId);
        }
    };
    private final MessageListener mMessageListener = new MessageListener() {
        @Override
        public void onStateChanged(final int state) {
            logToView("MessageListener | State changed: " + state);
        }

        @Override
        public void onMessageReceived(final long timestamp, final UUID origin, final String type, final byte[] message) {
            logToView("MessageListener | Message received: From=" + origin + " type=" + type + " message=" + new String(message));
        }
    };
    private TextView mLogView;
    private final ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks() {
        @Override
        public void onConnected() {
            logToView("Successfully connected to P2P Services");
        }

        @Override
        public void onConnectionSuspended() {
            logToView("Connection to P2P Services suspended");
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            logToView("Connection to P2P Services failed with status: " + connectionResult.getStatusCode());
            ConnectionResultHandling.showAlertDialogForConnectionError(MainActivity.this, connectionResult.getStatusCode());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLogView = (TextView) findViewById(R.id.textView);

        findViewById(R.id.startButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAll();
            }
        });

        findViewById(R.id.stopButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopAll();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        final int statusCode = KitClient.isP2PServicesAvailable(this);
        if (statusCode == ConnectionResult.SUCCESS) {
            KitClient client = KitClient.getInstance(this);
            client.registerConnectionCallbacks(mConnectionCallbacks);

            if (client.isConnected()) {
                logToView("Client already connected");
            } else {
                logToView("Connecting P2PKit client");
                client.connect(APP_KEY);
            }

        } else {
            logToView("Cannot start everything yet, status code: " + statusCode);
            ConnectionResultHandling.showAlertDialogForConnectionError(this, statusCode);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        KitClient client = KitClient.getInstance(this);
        client.unregisterConnectionCallbacks(mConnectionCallbacks);
        client.disconnect();
    }

    private void startAll() {
        logToView("Start all called");
        MessageServices messageServices = KitClient.getInstance(this).getMessageServices();
        messageServices.addListener(mMessageListener);

        DiscoveryServices discoveryServices = KitClient.getInstance(this).getDiscoveryServices();
        discoveryServices.addListener(mP2pDiscoveryListener);
        discoveryServices.addListener(mGeoDiscoveryListener);
    }

    private void stopAll() {
        logToView("Stop all called");
        MessageServices messageServices = KitClient.getInstance(this).getMessageServices();
        messageServices.removeListener(mMessageListener);

        DiscoveryServices discoveryServices = KitClient.getInstance(this).getDiscoveryServices();
        discoveryServices.removeListener(mP2pDiscoveryListener);
        discoveryServices.removeListener(mGeoDiscoveryListener);
    }

    private void logToView(String message) {
        mLogView.setText(message + "\n" + mLogView.getText());
    }
}
