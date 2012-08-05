package com.buddycloud.content;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;

import android.util.Log;

import com.googlecode.asmack.XmppAccount;
import com.googlecode.asmack.client.AsmackClientService;

public final class ChannelSync implements Runnable, PacketListener {

    private AsmackClientService service;
    private XmppAccount account;
    private ArrayBlockingQueue<Packet> queue = new ArrayBlockingQueue<Packet>(100);

    public ChannelSync(AsmackClientService service, XmppAccount account) {
        this.service = service;
        this.account = account;
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            DiscoverItems disco = new DiscoverItems();
            disco.setTo(account.getDomain());
            service.sendWithCallback(disco, account.getJid(), this, 5 * 60 * 1000);
            Packet reply = queue.poll(300, TimeUnit.SECONDS);
            if (reply == null) {
                // failed
                return;
            }
            disco = (DiscoverItems)reply;
            Iterator<Item> items = disco.getItems();
            while (items.hasNext()) {
                Item item = items.next();
                DiscoverInfo info = new DiscoverInfo();
                info.setTo(item.getEntityID());
                service.sendWithCallback(info, account.getJid(), this, 5 * 60 * 1000);
            }
            reply = queue.poll(300, TimeUnit.SECONDS);
            while (reply != null) {
                if (reply instanceof DiscoverInfo) {
                    DiscoverInfo info = (DiscoverInfo) reply;
                    Iterator<Identity> identities = info.getIdentities();
                    while (identities.hasNext()) {
                        Identity identity = identities.next();
                        if (!"pubsub".equals(identity.getCategory())) {
                            continue;
                        }
                        if (!"inbox".equals(identity.getType())) {
                            continue;
                        }
                        new InboxSync(info.getFrom(), service, account);
                    }
                }
                reply = queue.poll(300, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        };
    }

    @Override
    public void processPacket(Packet packet) {
        queue.add(packet);
    }

}
