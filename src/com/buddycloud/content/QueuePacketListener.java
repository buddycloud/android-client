package com.buddycloud.content;

import java.util.Queue;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;

public class QueuePacketListener implements PacketListener {

    private Queue<Packet> queue;

    public QueuePacketListener(Queue<Packet> queue) {
        this.queue = queue;
    }

    @Override
    public void processPacket(Packet packet) {
        queue.add(packet);
    }

}
