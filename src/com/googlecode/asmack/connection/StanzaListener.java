package com.googlecode.asmack.connection;

import com.googlecode.asmack.Stanza;

public interface StanzaListener {

    void receive(Stanza stanza);

}
