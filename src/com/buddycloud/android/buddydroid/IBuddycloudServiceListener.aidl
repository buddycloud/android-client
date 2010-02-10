package com.buddycloud.android.buddydroid;

interface IBuddycloudServiceListener {

    void onBCConnected();

    void onBCDisconnected();

    void onBCLoginFailed();

}