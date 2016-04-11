/**
 * ConsoleFragment.java
 * Kanka-quickstart-android
 * <p/>
 * Created by uepaa on 09/02/16.
 * <p/>
 * <p/>
 * Copyright (c) 2016 by Uepaa AG, Zürich, Switzerland.
 * All rights reserved.
 * <p/>
 * We reserve all rights in this document and in the information contained therein.
 * Reproduction, use, transmission, dissemination or disclosure of this document and/or
 * the information contained herein to third parties in part or in whole by any means
 * is strictly prohibited, unless prior written permission is obtained from Uepaa AG.
 */
package ch.uepaa.quickstart.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import ch.uepaa.quickstart.R;
import ch.uepaa.quickstart.utils.Logger;
import ch.uepaa.quickstart.utils.P2PKitEnabledCallback;

/**
 * Console fragment.
 * Created by uepaa on 09/02/16.
 */
public class ConsoleFragment extends DialogFragment implements Logger.LogHandler {

    public interface ConsoleListener {
        void enableKit(final boolean startP2PService, P2PKitEnabledCallback p2PKitEnabledCallback);

        void disableKit();

        void startP2pDiscovery();

        void stopP2pDiscovery();

        void startGeoDiscovery();

        void stopGeoDiscovery();
    }

    public static final String FRAGMENT_TAG = "console_fragment";

    private static final String KIT_ENABLED_KEY = "kit_enabled";
    private static final String P2P_ENABLED_KEY = "p2p_enabled";
    private static final String GEO_ENABLED_KEY = "geo_enabled";

    public static ConsoleFragment newInstance(final boolean kitEnabled, final boolean p2pEnabled, final boolean geoEnabled) {

        ConsoleFragment fragment = new ConsoleFragment();

        Bundle args = new Bundle();
        args.putBoolean(KIT_ENABLED_KEY, kitEnabled);
        args.putBoolean(P2P_ENABLED_KEY, p2pEnabled);
        args.putBoolean(GEO_ENABLED_KEY, geoEnabled);
        fragment.setArguments(args);

        return fragment;
    }

    private TextView mLogView;
    private ConsoleListener listener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.console_fragment, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {

        Bundle args = getArguments();

        getDialog().setTitle(R.string.console);

        final Switch mP2pSwitch = (Switch) view.findViewById(R.id.p2pSwitch);
        mP2pSwitch.setChecked(args.getBoolean(P2P_ENABLED_KEY, false));
        mP2pSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    listener.startP2pDiscovery();
                } else {
                    listener.stopP2pDiscovery();
                }
            }
        });

        final Switch mGeoSwitch = (Switch) view.findViewById(R.id.geoSwitch);
        mGeoSwitch.setChecked(args.getBoolean(GEO_ENABLED_KEY, false));
        mGeoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    listener.startGeoDiscovery();
                } else {
                    listener.stopGeoDiscovery();
                }
            }
        });

        Switch kitSwitch = (Switch) view.findViewById(R.id.kitSwitch);
        kitSwitch.setChecked(args.getBoolean(KIT_ENABLED_KEY, false));
        kitSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    listener.enableKit(false, new P2PKitEnabledCallback() {
                        @Override
                        public void onEnabled() {
                            mP2pSwitch.setEnabled(true);
                            mGeoSwitch.setEnabled(true);
                        }
                    });
                } else {
                    listener.disableKit();

                    mP2pSwitch.setEnabled(false);
                    mGeoSwitch.setEnabled(false);
                    mP2pSwitch.setChecked(false);
                    mGeoSwitch.setChecked(false);
                }
            }
        });

        mLogView = (TextView) view.findViewById(R.id.logTextView);

        TextView clearLogs = (TextView) view.findViewById(R.id.clearTextView);
        clearLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLogs();
            }
        });
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        try {
            listener = (ConsoleListener) activity;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(activity.toString() + " must implement ConsoleListener", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        String logs = Logger.getLogs();
        mLogView.setText(logs);

        Logger.addObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        Logger.removeObserver(this);

        mLogView.setText("");
    }

    public void handleLogMessage(String message) {
        String updated = message + "\n" + mLogView.getText();
        mLogView.setText(updated);
    }

    private void clearLogs() {
        Logger.clearLogs();
        mLogView.setText("");
    }

}
