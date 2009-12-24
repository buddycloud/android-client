package com.buddycloud.jbuddycloud;

import com.buddycloud.jbuddycloud.packet.GeoLoc;

public interface BCGeoLocListener {

    void receive(String from, GeoLoc geoloc);

}
