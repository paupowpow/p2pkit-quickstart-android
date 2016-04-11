package ch.uepaa.quickstart.utils;

/**
 * Simple Interface to forward the onEnabled callback of the P2PKitClient
 */
public interface P2PKitEnabledCallback {

    /**
     * called when the P2PKitClient was successfully enabled.
     */
    void onEnabled();
}
