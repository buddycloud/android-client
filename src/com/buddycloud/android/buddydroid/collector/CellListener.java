package com.buddycloud.android.buddydroid.collector;

import android.content.Context;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

import com.buddycloud.android.buddydroid.BuddycloudService;
import com.buddycloud.jbuddycloud.packet.BeaconLog;

public class CellListener extends PhoneStateListener {

    private BuddycloudService service;
    private TelephonyManager telephonyManager;
    private String cell;
    private String newCell;
    private int power = -1;
    private long lastFullScan;

    public CellListener(BuddycloudService service) {
        this.service = service;
        telephonyManager = (TelephonyManager) service.getSystemService(
            Context.TELEPHONY_SERVICE
        );
        CellLocation.requestLocationUpdate();
    }

    @Override
    public void onCellLocationChanged(CellLocation location) {
        if (!(location instanceof GsmCellLocation)) {
            return;
        }
        GsmCellLocation gsmCell = (GsmCellLocation) location;
        String operator = telephonyManager.getNetworkOperator();
        if (operator == null || operator.length() < 4) {
            return;
        }
        newCell = operator.substring(0, 3) + ':'
                    + operator.substring(3) + ':'
                    + gsmCell.getLac() + ':' + gsmCell.getCid();
        if (cell == null) {
            cell = newCell;
        }
        service.sendBeaconLog(3);
        long now = System.currentTimeMillis();
        long delta = now - lastFullScan;
        if (delta > 300000) {
            lastFullScan = now;
            new Thread() {
                public void run() {
                    // TODO full neighbour scan
                }
            }.start();
        }
    }

    @Override
    public void onSignalStrengthChanged(int asu) {
        boolean force = cell == null;
        cell = newCell;
        power = 113 - 2*asu;
        service.sendBeaconLog(10);
    }

    public void appendTo(BeaconLog log) {
        log.add("cell", cell, power);
    }

}
