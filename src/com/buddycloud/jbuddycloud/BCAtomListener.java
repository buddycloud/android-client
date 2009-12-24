package com.buddycloud.jbuddycloud;

import com.buddycloud.jbuddycloud.packet.BCAtom;

/**
 * An interface for receiving <entry> encapsuated geoloc informations as
 * published on buddycloud channels.
 */
public interface BCAtomListener {

    void receive(String node, BCAtom atom);

}
