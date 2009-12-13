package com.buddycloud.android.buddydroid;

import com.buddycloud.jbuddycloud.packet.BeaconLog;

import android.content.Context;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

public class CellListener extends PhoneStateListener {

    private BuddycloudService service;
    private TelephonyManager telephonyManager;
    private String cell;
    private String newCell;
    private int power = -1;
    private long time;

    public CellListener(BuddycloudService service) {
        time = System.currentTimeMillis();
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
        newCell = operator.substring(0, 3) + ':'
                    + operator.substring(3) + ':'
                    + gsmCell.getLac() + ':' + gsmCell.getCid();
        if (cell == null) {
            cell = newCell;
        }
        service.sendBeaconLog(true);
    }

    @Override
    public void onSignalStrengthChanged(int asu) {
        boolean force = cell == null;
        cell = newCell;
        power = asu;
        service.sendBeaconLog(force);
    }

    public void appendTo(BeaconLog log) {
        log.add("cell", cell, power);
    }

}
