package ch.uepaa.quickstart;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
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

    private TextView logView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logView = (TextView) findViewById(R.id.textView);

        final int isP2PServicesAvailable = KitClient.isP2PServicesAvailable(this);
        if (isP2PServicesAvailable == ConnectionResult.SUCCESS) {

            //FIXME doc
            KitClient mP2pClient = new KitClient.Builder(this)
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
            //FIXME doc
            ConnectionResultHandling.showAlertDialogForConnectionError(this, isP2PServicesAvailable);
        }

    }


    /**
     * FIXME doc
     */
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
        }
    };

    /**
     * FIXME doc
     */
    private final P2pListener mP2pDiscoveryListener = new P2pListener() {
        @Override
        public void onPeerDiscovered(final UUID nodeId) {
            logToView("P2pListener | Peer discovered: " + nodeId);

            // sending a message to the peer
            KitClient.getMessageService().sendMessage(nodeId, "SimpleChatMessage", "From Android: Hello P2P!".getBytes());
        }

        @Override
        public void onPeerLost(final UUID nodeId) {
            logToView("P2pListener | Peer lost: " + nodeId);
        }
    };


    /**
     * FIXME doc
     */
    private final GeoListener mGeoDiscoveryListener = new GeoListener() {
        @Override
        public void onStateChanged(int state) {
            logToView("GeoListener | State changed: " + state);
        }

        @Override
        public void onPeerDiscovered(final UUID nodeId) {
            logToView("GeoListener | Peer discovered: " + nodeId);

            // sending a message to the peer
            KitClient.getMessageService().sendMessage(nodeId, "SimpleChatMessage", "From Android: Hello GEO!".getBytes());
        }

        @Override
        public void onPeerLost(final UUID nodeId) {
            logToView("GeoListener | Peer lost: " + nodeId);
        }
    };

    private final MessageListener mMessageListener = new MessageListener() {
        @Override
        public void onStateChanged(int state) {
            logToView("MessageListener | State changed: " + state);
        }

        @Override
        public void onMessageReceived(long timestamp, UUID origin, String type, byte[] message) {
            logToView("MessageListener | Message received: From=" + origin + " type=" + type + " message=" + new String(message));
        }
    };

    private void logToView(String message) {
        logView.setText(message + "\n" + logView.getText());
    }
}
