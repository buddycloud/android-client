package com.buddycloud;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * The time broadcast receiver handles time ticks and calls back to the
 * service.
 */
public class TimeBroadcastReceiver extends BroadcastReceiver {

    /**
     * The buddycloud service reference.
     */
    private final BuddycloudService service;

    /**
     * Create a new time tick receiver that will call the service on every
     * event.
     * @param service The buddycloud service.
     */
    public TimeBroadcastReceiver(BuddycloudService service) {
        this.service = service;
    }

    /**
     * Delegate time events to BuddycloudService.onTimeTick.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        service.onTimeTick();
    }

}
