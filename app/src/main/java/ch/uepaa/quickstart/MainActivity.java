package ch.uepaa.quickstart;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.UUID;

import ch.uepaa.p2pkit.ConnectionCallbacks;
import ch.uepaa.p2pkit.ConnectionResult;
import ch.uepaa.p2pkit.ConnectionResultHandling;
import ch.uepaa.p2pkit.KitClient;
import ch.uepaa.p2pkit.discovery.GeoListener;
import ch.uepaa.p2pkit.discovery.P2pListener;
import ch.uepaa.p2pkit.messaging.MessageListener;

public class MainActivity extends AppCompatActivity {

    private static final String APP_KEY = "<YOUR APP KEY>";

    private final P2pListener mP2pDiscoveryListener = new P2pListener() {

        @Override
        public void onStateChanged(final int state) {
            logToView("P2pListener | State changed: " + state);
        }

        @Override
        public void onPeerDiscovered(final UUID nodeId) {
            logToView("P2pListener | Peer discovered: " + nodeId);
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

    private final ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks() {

        @Override
        public void onConnected() {
            logToView("Successfully connected to P2P Services, with id: " + KitClient.getInstance(MainActivity.this).getNodeId().toString());
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

    private TextView mLogView;
    private Switch mP2pSwitch;
    private Switch mGeoSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupUI();

        enableKit();
        startP2pDiscovery();
        startGeoDiscovery();
    }

    private void enableKit() {
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
            logToView("Cannot start P2PKit, status code: " + statusCode);
            ConnectionResultHandling.showAlertDialogForConnectionError(this, statusCode);
        }
    }

    private void disableKit(){
        KitClient.getInstance(this).disconnect();
    }

    private void startP2pDiscovery(){
        KitClient.getInstance(this).getDiscoveryServices().addListener(mP2pDiscoveryListener);
    }

    private void stopP2pDiscovery(){
        KitClient.getInstance(this).getDiscoveryServices().removeListener(mP2pDiscoveryListener);
        logToView("P2pListener removed");
    }

    private void startGeoDiscovery(){
        KitClient.getInstance(this).getMessageServices().addListener(mMessageListener);

        KitClient.getInstance(this).getDiscoveryServices().addListener(mGeoDiscoveryListener);
    }

    private void stopGeoDiscovery(){
        KitClient.getInstance(this).getMessageServices().removeListener(mMessageListener);
        logToView("MessageListener removed");

        KitClient.getInstance(this).getDiscoveryServices().removeListener(mGeoDiscoveryListener);
        logToView("GeoListener removed");
    }

    private void logToView(String message) {
        CharSequence currentTime = DateFormat.format("hh:mm:ss - ", System.currentTimeMillis());
        mLogView.setText(currentTime + message + "\n" + mLogView.getText());
    }

    private void clearLogs() {
        mLogView.setText("");
    }

    private void setupUI(){
        mLogView = (TextView) findViewById(R.id.textView);
        findViewById(R.id.clearTextView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLogs();
            }
        });

        Switch kitSwitch = (Switch) findViewById(R.id.kitSwitch);
        kitSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    mP2pSwitch.setEnabled(true);
                    mGeoSwitch.setEnabled(true);

                    enableKit();
                } else {
                    mP2pSwitch.setChecked(false);
                    mP2pSwitch.setEnabled(false);

                    mGeoSwitch.setChecked(false);
                    mGeoSwitch.setEnabled(false);

                    disableKit();
                }
            }
        });

        mP2pSwitch = (Switch) findViewById(R.id.p2pSwitch);
        mP2pSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startP2pDiscovery();
                } else {
                    stopP2pDiscovery();
                }
            }
        });

        mGeoSwitch = (Switch) findViewById(R.id.geoSwitch);
        mGeoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    startGeoDiscovery();
                } else {
                    stopGeoDiscovery();
                }
            }
        });
    }
}