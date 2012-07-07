package com.buddycloud.content;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;

import com.googlecode.asmack.Attribute;
import com.googlecode.asmack.Stanza;
import com.googlecode.asmack.XmppAccount;
import com.googlecode.asmack.client.AsmackClientService;

public final class InboxSync implements Runnable, PacketListener {

    protected static AtomicInteger id = new AtomicInteger(new Random().nextInt());

    private String to;
    private AsmackClientService service;
    private XmppAccount account;
    private ArrayBlockingQueue<Packet> queue = new ArrayBlockingQueue<Packet>(1);

    public InboxSync(String to, AsmackClientService service,
            XmppAccount account) {
        this.to = to;
        this.service = service;
        this.account = account;
        new Thread(this).start();
    }

    @Override
    public void processPacket(Packet packet) {
        queue.add(packet);
    }

    @Override
    public void run() {
        Stanza request = new Stanza(
            "iq",
            "",
            account.getJid(),
            "<iq type='get'><query xmlns='urn:xmpp:archive#management' /></iq>",
            null
        );
        request.addAttribute(new Attribute("from", "", account.getJid()));
        request.addAttribute(new Attribute("to", "", to));
        request.addAttribute(new Attribute("id", "", "sync-" + Integer.toHexString(id.getAndIncrement())));
        service.send(request);
    }

}
