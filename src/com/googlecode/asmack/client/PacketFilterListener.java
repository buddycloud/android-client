package com.googlecode.asmack.client;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;

/**
 * A packet filter aware packet listener.
 */
class PacketFilterListener implements PacketListener {

    /**
     * The packet filter.
     */
    private final PacketFilter filter;

    /**
     * The packet listener.
     */
    private final PacketListener listener;

    /**
     * Create a new filter aware packet listener.
     * @param filter The packet filter.
     * @param listener The real listener.
     */
    public PacketFilterListener(PacketFilter filter, PacketListener listener) {
        this.filter = filter;
        this.listener = listener;
    }

    /**
     * Proces a packet, calling the underlying listener only if the packet
     * filter matches.
     * @param packet The smack packet.
     */
    @Override
    public void processPacket(Packet packet) {
        if (!getFilter().accept(packet)) {
            return;
        }
        getListener().processPacket(packet);
    }

    /**
     * Retrieve the current filter.
     * @return The current smack packet filter.
     */
    public PacketFilter getFilter() {
        return filter;
    }

    /**
     * Retrieve the underlying packet listener.
     * @return The underlying packet listener.
     */
    public PacketListener getListener() {
        return listener;
    }

}