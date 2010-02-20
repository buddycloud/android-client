package com.buddycloud.android.buddydroid;

import com.buddycloud.android.buddydroid.IBuddycloudServiceListener;

interface IBuddycloudService {

    boolean isConnected();

    boolean isAuthenticated();

    boolean isAnonymous();

    String getJidWithResource();

    void send(String rawXml);

    void login(String jid, String password);

    void loginAnonymously();

    void createAccount(String username, String password);

    void addListener(IBuddycloudServiceListener listener);

    boolean follow(String channel);

}
