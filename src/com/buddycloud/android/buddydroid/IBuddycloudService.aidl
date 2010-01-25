package com.buddycloud.android.buddydroid;

interface IBuddycloudService {

    boolean isConnected();

    String getJidWithResource();

    void send(String rawXml);

}
