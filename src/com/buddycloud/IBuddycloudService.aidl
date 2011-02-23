package com.buddycloud;

import com.googlecode.asmack.Stanza;

interface IBuddycloudService {

    boolean follow(String channel);
    void updateChannel(String channel);
    boolean send(in Stanza stanza);

}
