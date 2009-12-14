package com.buddycloud.android.buddydroid.collector;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import com.buddycloud.android.buddydroid.BuddycloudService;
import com.buddycloud.jbuddycloud.packet.BeaconLog;

public class CellListener extends PhoneStateListener {

    private BuddycloudService service;
    private TelephonyManager telephonyManager;
    private String cell;
    private String newCell;
    private int power = -1;
    private BroadcastReceiver receiver;

    private ArrayList<NeighboringCellInfo> neighbours;
    private String catchedAt = null;

    public CellListener(final BuddycloudService service) {
        this.service = service;
    }

    public void start() {
        telephonyManager = (TelephonyManager) service
                .getSystemService(Context.TELEPHONY_SERVICE);
        CellLocation.requestLocationUpdate();
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    service.sendBeaconLog(10);
                } catch (InterruptedException e) {
                    // not important
                }
            }
        };
        service.registerReceiver(receiver, new IntentFilter(
                "android.intent.action.TIME_TICK"));
        telephonyManager.listen(this, PhoneStateListener.LISTEN_CELL_LOCATION
                | PhoneStateListener.LISTEN_SIGNAL_STRENGTH);
    }

    public void stop() {
        telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
        service.unregisterReceiver(receiver);
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
        newCell = operator.substring(0, 3) + ':' + operator.substring(3) + ':'
                + gsmCell.getLac() + ':' + gsmCell.getCid();
        if (cell == null) {
            cell = newCell;
        }
        try {
            service.sendBeaconLog(3);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void onSignalStrengthChanged(int asu) {
        boolean force = cell == null;
        cell = newCell;
        power = 113 - 2 * asu;
        List<NeighboringCellInfo> neighboringCellInfo = telephonyManager
                .getNeighboringCellInfo();
        if (neighboringCellInfo != null) {
            ArrayList<NeighboringCellInfo> n = new ArrayList<NeighboringCellInfo>(
                    neighboringCellInfo.size() + 2);
            for (NeighboringCellInfo info : neighboringCellInfo) {
                if (info.getCid() != -1) {
                    n.add(info);
                }
            }
            if (n.size() > 0) {
                neighbours = n;
                catchedAt = cell;
            }
        }
        try {
            service.sendBeaconLog(force ? 2 : 10);
        } catch (InterruptedException e) {
        }
    }

    public void appendTo(BeaconLog log) {
        log.add("cell", cell, power);
        if (catchedAt.equals(cell)) {
            Log.d("CellListener", "Neighbour update");
            for (NeighboringCellInfo info : neighbours) {
                log.add("cell", cell.substring(0, cell.lastIndexOf(':') + 1)
                        + info.getCid(), 113 - 2 * info.getRssi());
            }
        }
    }

}
