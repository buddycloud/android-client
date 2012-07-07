package com.googlecode.asmack.client;

import com.googlecode.asmack.connection.IXmppTransportService;

/**
 * Listener for common transport bind events.
 */
public interface TransportServiceBindListener {

    /**
     * Called whenever the transport service was bound.
     * @param service The service that connected.
     */
    void onTrasportServiceConnect(IXmppTransportService service);

    /**
     * Called when the transport server got disconnected.
     * @param service The service that failed.
     */
    void onTrasportServiceDisconnect(IXmppTransportService service);

}
