package ch.uepaa.quickstart;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.UUID;

import ch.uepaa.p2pkit.P2PKitClient;
import ch.uepaa.p2pkit.P2PKitStatusCallback;
import ch.uepaa.p2pkit.StatusResult;
import ch.uepaa.p2pkit.StatusResultHandling;
import ch.uepaa.p2pkit.discovery.GeoListener;
import ch.uepaa.p2pkit.discovery.InfoTooLongException;
import ch.uepaa.p2pkit.discovery.P2PListener;
import ch.uepaa.p2pkit.discovery.entity.Peer;
import ch.uepaa.p2pkit.discovery.entity.ProximityStrength;
import ch.uepaa.p2pkit.internal.messaging.MessageTooLargeException;
import ch.uepaa.p2pkit.messaging.MessageListener;
import ch.uepaa.quickstart.config.Config;
import ch.uepaa.quickstart.fragments.ColorPickerFragment;
import ch.uepaa.quickstart.fragments.ConsoleFragment;
import ch.uepaa.quickstart.graph.Graph;
import ch.uepaa.quickstart.graph.GraphView;
import ch.uepaa.quickstart.utils.ColorStorage;
import ch.uepaa.quickstart.utils.Logger;
import ch.uepaa.quickstart.utils.P2PKitEnabledCallback;

public class MainActivity extends AppCompatActivity implements ConsoleFragment.ConsoleListener, ColorPickerFragment.ColorPickerListener {

    private static final String APP_KEY = Config.APP_KEY;

    // Enabling (1/2) - Enable the P2P Services
    public void enableKit(final boolean startP2PDiscovery, P2PKitEnabledCallback p2PKitEnabledCallback) {

        mShouldStartP2PDiscovery = startP2PDiscovery;
        mP2PKitEnabledCallback = p2PKitEnabledCallback;

        StatusResult result = P2PKitClient.isP2PServicesAvailable(this);
        if (result.getStatusCode() == StatusResult.SUCCESS) {
            Logger.i("P2PKitClient", "Enable P2PKit");

            P2PKitClient client = P2PKitClient.getInstance(this);
            client.enableP2PKit(mStatusCallback, APP_KEY);
            mShouldEnable = false;

        } else {
            Logger.w("P2PKitClient", "Cannot start P2PKit, status code: " + result.getStatusCode());

            mShouldEnable = true;
            StatusResultHandling.showAlertDialogForStatusError(this, result);
        }
    }

    // Enabling (2/2) - Handle the status callbacks with the P2P Services
    private final P2PKitStatusCallback mStatusCallback = new P2PKitStatusCallback() {

        @Override
        public void onEnabled() {
            Logger.v("P2PKitStatusCallback", "Successfully enabled P2P Services, with node id: " + P2PKitClient.getInstance(MainActivity.this).getNodeId().toString());

            UUID ownNodeId = P2PKitClient.getInstance(MainActivity.this).getNodeId();
            setupPeers(ownNodeId);
            if (mP2PKitEnabledCallback != null) {
                mP2PKitEnabledCallback.onEnabled();
            }

            if (mShouldStartP2PDiscovery) {

                startP2pDiscovery();
            }
        }

        @Override
        public void onSuspended() {
            Logger.v("P2PKitStatusCallback", "P2P Services suspended");
        }

        @Override
        public void onResumed() {
            Logger.v("P2PKitStatusCallback", "P2P Services resumed");
        }

        @Override
        public void onDisabled() {
            Logger.v("P2PKitStatusCallback", "P2P Services disabled");

            teardownPeers();
        }

        @Override
        public void onError(StatusResult statusResult) {
            Logger.e("P2PKitStatusCallback", "Error in P2P Services with status: " + statusResult.getStatusCode());
            StatusResultHandling.showAlertDialogForStatusError(MainActivity.this, statusResult);
        }
    };

    public void disableKit() {
        Logger.i("P2PKitClient", "Disable P2PKit");

        P2PKitClient client = P2PKitClient.getInstance(this);
        client.getDiscoveryServices().removeGeoListener(mGeoDiscoveryListener);
        client.getDiscoveryServices().removeP2pListener(mP2pDiscoveryListener);
        client.getMessageServices().removeMessageListener(mMessageListener);

        client.disableP2PKit();

        mShouldEnable = false;
        mShouldStartP2PDiscovery = false;

        mP2PServiceStarted = false;
        mGeoServiceStarted = false;
    }

    public void startP2pDiscovery() {
        Logger.i("P2PKitClient", "Start discovery");
        mP2PServiceStarted = true;

        byte[] ownDiscoveryData = loadOwnDiscoveryData();
        publishP2pDiscoveryInfo(ownDiscoveryData);

        P2PKitClient.getInstance(this).getDiscoveryServices().addP2pListener(mP2pDiscoveryListener);
    }

    private void publishP2pDiscoveryInfo(byte[] data) {
        Logger.i("P2PKitClient", "Publish discovery info");
        try {
            P2PKitClient.getInstance(this).getDiscoveryServices().setP2pDiscoveryInfo(data);
        } catch (InfoTooLongException e) {
            Logger.e("P2PKitClient", "The discovery info is too long: " + ((data != null) ? data.length : "null") + " bytes");
        }
    }

    // Listener of P2P discovery events
    private final P2PListener mP2pDiscoveryListener = new P2PListener() {

        @Override
        public void onP2PStateChanged(final int state) {
            Logger.v("P2PListener", "State changed: " + state);
        }

        @Override
        public void onPeerDiscovered(final Peer peer) {
            if (peer.getProximityStrength() == ProximityStrength.WIFI_PEER){
                Logger.v("P2PListener", "WIFI Peer discovered: " + peer.getNodeId() + ".");
            }else{
                Logger.v("P2PListener", "Peer discovered: " + peer.getNodeId() + ". Proximity strength: " + peer.getProximityStrength());
            }

            handlePeerDiscovered(peer);
        }

        @Override
        public void onPeerLost(final Peer peer) {
            Logger.v("P2PListener", "Peer lost: " + peer.getNodeId());

            handlePeerLost(peer);
        }

        @Override
        public void onPeerUpdatedDiscoveryInfo(Peer peer) {
            Logger.v("P2PListener", "Peer updated discovery info: " + peer.getNodeId());

            handlePeerUpdatedDiscoveryInfo(peer);
        }

        @Override
        public void onProximityStrengthChanged(Peer peer) {
            Logger.v("P2PListener", "Peer changed proximity strength: " + peer.getNodeId() + ". Proximity strength: " + peer.getProximityStrength());
            handlePeerChangedProximityStrength(peer);
        }
    };

    public void stopP2pDiscovery() {
        Logger.i("P2PKitClient", "Stop discovery");

        P2PKitClient.getInstance(this).getDiscoveryServices().removeP2pListener(mP2pDiscoveryListener);

        mP2PServiceStarted = false;
    }

    public void startGeoDiscovery() {
        Logger.i("P2PKitClient", "Start geo discovery");
        mGeoServiceStarted = true;

        P2PKitClient.getInstance(this).getMessageServices().addMessageListener(mMessageListener);
        P2PKitClient.getInstance(this).getDiscoveryServices().addGeoListener(mGeoDiscoveryListener);
    }

    private final GeoListener mGeoDiscoveryListener = new GeoListener() {

        @Override
        public void onGeoStateChanged(final int state) {
            Logger.v("GeoListener", "State changed: " + state);
        }

        @Override
        public void onPeerDiscovered(final UUID nodeId) {
            Logger.v("GeoListener", "Peer discovered: " + nodeId);

            // sending a message to the peer
            try {
                P2PKitClient.getInstance(MainActivity.this).getMessageServices().sendMessage(nodeId, "SimpleChatMessage", "From Android: Hello GEO!".getBytes());
            } catch (MessageTooLargeException e) {
                Logger.e("GeoListener", "error: " + e.getMessage());
            }
        }

        @Override
        public void onPeerLost(final UUID nodeId) {
            Logger.v("GeoListener", "Peer lost: " + nodeId);
        }
    };

    private final MessageListener mMessageListener = new MessageListener() {

        @Override
        public void onMessageStateChanged(final int state) {
            Logger.v("MessageListener", "State changed: " + state);
        }

        @Override
        public void onMessageReceived(final long timestamp, final UUID origin, final String type, final byte[] message) {
            Logger.v("MessageListener", "MessageListener | Message received: From=" + origin + " type=" + type + " message=" + new String(message));
        }
    };

    public void stopGeoDiscovery() {
        Logger.i("P2PKitClient", "Stop geo discovery");

        P2PKitClient.getInstance(this).getMessageServices().removeMessageListener(mMessageListener);
        P2PKitClient.getInstance(this).getDiscoveryServices().removeGeoListener(mGeoDiscoveryListener);

        mGeoServiceStarted = false;
    }

    private boolean mShouldEnable;
    private boolean mShouldStartP2PDiscovery;
    private P2PKitEnabledCallback mP2PKitEnabledCallback;
    private boolean mP2PServiceStarted;
    private boolean mGeoServiceStarted;

    private ColorStorage storage;
    private int defaultColor;

    private GraphView graphView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        mShouldEnable = true;
        mShouldStartP2PDiscovery = false;

        mP2PServiceStarted = false;
        mGeoServiceStarted = false;

        defaultColor = ContextCompat.getColor(this, R.color.graph_node);
        storage = new ColorStorage(this);

        graphView = (GraphView) findViewById(R.id.graph);

        FloatingActionButton colorActionButton = (FloatingActionButton) findViewById(R.id.color_action);
        if (colorActionButton != null) {
            colorActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showColorPicker();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // When the user comes back from the play store after installing p2p services, try to enable p2pkit again
        if (mShouldEnable && !P2PKitClient.getInstance(this).isEnabled()) {
            enableKit(true, null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        disableKit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_console: {
                showConsole();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupPeers(final UUID ownNodeId) {

        byte[] ownDiscoveryData = loadOwnDiscoveryData();
        int ownColor = ColorStorage.getColorCode(ownDiscoveryData, ColorStorage.createRandomColor());
        if (ownDiscoveryData == null) {
            storage.saveColor(ownColor);
            updateOwnDiscoveryInfo();
        }

        Graph graph = graphView.getGraph();
        graph.setup(ownNodeId);
        graph.addNode(ownNodeId);
        graph.setNodeColor(ownNodeId, ownColor);
    }

    private void handlePeerDiscovered(final Peer peer) {

        UUID peerId = peer.getNodeId();
        byte[] peerDiscoveryInfo = peer.getDiscoveryInfo();
        int peerColor = ColorStorage.getColorCode(peerDiscoveryInfo, defaultColor);
        float proximityStrength = (peer.getProximityStrength() - 1f) / 4;
        boolean proximityStrengthImmediate = peer.getProximityStrength() == ProximityStrength.IMMEDIATE;

        Graph graph = graphView.getGraph();
        graph.addNode(peerId);
        graph.setNodeColor(peerId, peerColor);
        graph.setEdgeStrength(peerId, proximityStrength);
        graph.setHighlighted(peerId, proximityStrengthImmediate);
    }

    private void handlePeerLost(final Peer peer) {

        UUID peerId = peer.getNodeId();

        Graph graph = graphView.getGraph();
        graph.removeNode(peerId);
        graph.updateOwnNode();
    }

    private void handlePeerUpdatedDiscoveryInfo(final Peer peer) {

        UUID peerId = peer.getNodeId();
        byte[] peerDiscoveryInfo = peer.getDiscoveryInfo();

        int peerColor = ColorStorage.getColorCode(peerDiscoveryInfo, defaultColor);

        Graph graph = graphView.getGraph();
        graph.setNodeColor(peerId, peerColor);
    }

    private void handlePeerChangedProximityStrength(final Peer peer) {

        UUID peerId = peer.getNodeId();
        float proximityStrength = (peer.getProximityStrength() - 1f) / 4;
        boolean proximityStrengthImmediate = peer.getProximityStrength() == ProximityStrength.IMMEDIATE;

        Graph graph = graphView.getGraph();
        graph.setEdgeStrength(peerId, proximityStrength);
        graph.setHighlighted(peerId, proximityStrengthImmediate);
    }

    private void updateOwnDiscoveryInfo() {

        P2PKitClient client = P2PKitClient.getInstance(MainActivity.this);
        if (!client.isEnabled()) {
            Toast.makeText(this, R.string.p2pkit_not_enabled, Toast.LENGTH_LONG).show();
            return;
        }

        UUID ownNodeId = client.getNodeId();
        byte[] ownDiscoveryData = loadOwnDiscoveryData();
        int ownColor = ColorStorage.getColorCode(ownDiscoveryData, defaultColor);

        publishP2pDiscoveryInfo(ownDiscoveryData);

        Graph graph = graphView.getGraph();
        graph.setNodeColor(ownNodeId, ownColor);
    }

    private byte[] loadOwnDiscoveryData() {
        return storage.loadColor();
    }

    private void teardownPeers() {

        Graph graph = graphView.getGraph();
        graph.reset();
    }

    private void showColorPicker() {

        byte[] colorData = storage.loadColor();
        int colorCode = ColorStorage.getColorCode(colorData, defaultColor);

        ColorPickerFragment fragment = ColorPickerFragment.newInstance(colorCode);
        fragment.show(getFragmentManager(), ColorPickerFragment.FRAGMENT_TAG);
    }

    @Override
    public void onColorPicked(int colorCode) {

        storage.saveColor(colorCode);

        updateOwnDiscoveryInfo();
    }

    private void showConsole() {

        P2PKitClient client = P2PKitClient.getInstance(this);
        boolean kitEnabled = client.isEnabled();

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(ConsoleFragment.FRAGMENT_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        ConsoleFragment fragment = ConsoleFragment.newInstance(kitEnabled, mP2PServiceStarted, mGeoServiceStarted);
        fragment.show(ft, ConsoleFragment.FRAGMENT_TAG);
    }

}
