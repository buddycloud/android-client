package com.buddycloud.content;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.pubsub.GetItemsRequest;
import org.jivesoftware.smackx.pubsub.ItemsExtension;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.packet.PubSub;

import android.content.ContentValues;
import android.content.pm.PackageInfo;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.UrlQuerySanitizer.ValueSanitizer;
import android.test.IsolatedContext;
import android.util.Log;

import com.buddycloud.content.BuddyCloud.Roster;
import com.buddycloud.content.BuddyCloud.Sync;
import com.buddycloud.jbuddycloud.packet.DiscoItemsPacketExtension;
import com.buddycloud.jbuddycloud.packet.DiscoItemsPacketExtension.Item;
import com.buddycloud.jbuddycloud.packet.MessageArchiveManagement;
import com.buddycloud.jbuddycloud.packet.RSMSet;
import com.googlecode.asmack.XmppAccount;
import com.googlecode.asmack.client.AsmackClientService;

public final class InboxSync implements Runnable, PacketListener {

    private String to;
    private AsmackClientService service;
    private XmppAccount account;
    private ArrayBlockingQueue<Packet> queue = new ArrayBlockingQueue<Packet>(1);
    private String id;

    public InboxSync(String to, AsmackClientService service,
            XmppAccount account) {
        this.to = to;
        this.service = service;
        this.account = account;
        new Thread(this).start();
    }

    @Override
    public void processPacket(Packet packet) {
    }

    @Override
    public void run() {
        QueuePacketListener cb = new QueuePacketListener(queue);
        try {
            ContentValues values = new ContentValues();
            BuddycloudProvider provider = new BuddycloudProvider();
            provider.attachInfo(service.getApplicationContext(), new ProviderInfo());
            provider.getDatabase(service.getApplicationContext());
            String last = null;
            boolean repeat = false;
            do {
                PubSub pubsub = new PubSub();
                pubsub.setTo(to);
                RSMSet rsm = new RSMSet();
                rsm.max = 10;
                rsm.before = last;
                GetItemsRequest getitems = new GetItemsRequest("/user/" + account.getJid() + "/subscriptions");
                pubsub.addExtension(getitems);
                pubsub.addExtension(rsm);
                service.sendWithCallback(pubsub, account.getJid(), cb, 5 * 60 * 1000);
                Packet reply = queue.poll(300, TimeUnit.SECONDS);
                if (reply != null && reply instanceof PubSub) {
                    pubsub = (PubSub) reply;
                    for (PacketExtension extension : pubsub.getExtensions()) {
                        if (!(extension instanceof ItemsExtension)) {
                            Log.d("XXXX", "Reply " + extension.getClass());
                            Log.d("XXXX", "Reply " + extension.toXML());
                            continue;
                        }
                        ItemsExtension items = (ItemsExtension) extension;
                        for (Object o : items.getItems()) {
                            if (!(o instanceof PayloadItem<?>)) {
                                continue;
                            }
                            PayloadItem<?> payload = (PayloadItem<?>)o;
                            if (!(payload.getPayload() instanceof DiscoItemsPacketExtension))
                            {
                                continue;
                            }
                            DiscoItemsPacketExtension itemsExtension =
                                    (DiscoItemsPacketExtension)payload.getPayload();
                            List<Item> list = itemsExtension.getItems();
                            for (Item i : list) {
                                values.clear();
                                values.put(Roster.JID, payload.getId());
                                values.put(Roster.NAME, payload.getId());
                                if (i.node.endsWith("/posts")) {
                                    values.put(Roster.ENTRYTYPE, "channel");
                                }
                                provider.insert(BuddyCloud.Roster.CONTENT_URI, values);
                            }
                        }
                    }
                }
            } while (repeat);

            if (!repeat) { return; }

            MessageArchiveManagement mam = new MessageArchiveManagement();
            mam.setTo(to);
            mam.setFrom(account.getJid());
            Calendar now = Calendar.getInstance();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.HOUR_OF_DAY, 4);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            for (int i = 0; i < 30 * 24; i++) {
                mam.end =
                    cal.get(Calendar.YEAR) + "-" +
                    ((cal.get(Calendar.MONTH) < 10) ? "0" : "") +
                    cal.get(Calendar.MONTH) + "-" +
                    ((cal.get(Calendar.DAY_OF_MONTH) < 10) ? "0" : "") +
                    cal.get(Calendar.DAY_OF_MONTH) + "T" +
                    ((cal.get(Calendar.HOUR_OF_DAY) < 10) ? "0" : "") +
                    cal.get(Calendar.HOUR_OF_DAY) + ":00:00Z";
                Cursor c = service.getContentResolver().query(
                        Sync.CONTENT_URI,
                        Sync.PROJECTION_MAP,
                        Sync.SERVICE + "=? AND " + Sync.TIMESTAMP + "=?",
                        new String[]{to, Long.toString(cal.getTimeInMillis())},
                        null);
                boolean isUnqueried = c.isAfterLast();
                c.close();
                if (isUnqueried) {
                    Calendar end = (Calendar) cal.clone();
                    values.clear();
                    values.put(Sync.SERVICE, to);
                    values.put(Sync.TIMESTAMP, Long.toString(cal.getTimeInMillis()));
                    cal.add(Calendar.HOUR_OF_DAY, -1);
                    mam.start =
                        cal.get(Calendar.YEAR) + "-" +
                        ((cal.get(Calendar.MONTH) < 10) ? "0" : "") +
                        cal.get(Calendar.MONTH) + "-" +
                        ((cal.get(Calendar.DAY_OF_MONTH) < 10) ? "0" : "") +
                        cal.get(Calendar.DAY_OF_MONTH) + "T" +
                        ((cal.get(Calendar.HOUR_OF_DAY) < 10) ? "0" : "") +
                        cal.get(Calendar.HOUR_OF_DAY) + ":00:00Z";
                    id = service.sendWithCallback(
                        mam,
                        account.getJid(),
                        new QueuePacketListener(queue),
                        5 * 60 * 1000
                    );
                    Packet result = queue.poll(300, TimeUnit.SECONDS);
                    if (result instanceof IQ && "error".equals(((IQ)result).getType())) {
                        if (now.before(end)) {
                            service.getContentResolver().insert(Sync.CONTENT_URI, values);
                        }
                    }
                } else {
                    cal.add(Calendar.HOUR_OF_DAY, -1);
                    Thread.sleep(1);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            service.removeListener(this);
        }
    }

}
