package com.buddycloud.android.buddydroid.collector;

import java.util.List;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.buddycloud.android.buddydroid.BuddycloudService;
import com.buddycloud.jbuddycloud.packet.BeaconLog;

public class NetworkListener {

    private final WifiManager wifiManager;

    public NetworkListener(BuddycloudService service) {
        wifiManager = (WifiManager) service.getSystemService(
            Context.WIFI_SERVICE
        );
    }

    public void appendTo(BeaconLog log) {
        if (wifiManager.isWifiEnabled()) {
            List<ScanResult> scanResults = wifiManager.getScanResults();
            if (scanResults != null) {
                for (ScanResult scan: scanResults) {
                    log.add("wifi", scan.BSSID, scan.level);
                }
            }
            WifiInfo info = wifiManager.getConnectionInfo();
            if (info != null) {
                log.add("wifi", info.getBSSID(), 113 - 2 * info.getRssi());
            }
        }
    }

}
