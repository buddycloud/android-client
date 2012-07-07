package com.buddycloud.content;

import java.util.Calendar;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;

import android.util.Log;

import com.buddycloud.jbuddycloud.packet.MessageArchiveManagement;
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
        if (!(packet instanceof Message)) {
            return;
        }
        Message message = (Message) packet;
        Log.d("XXXXX", "Message");
        for (PacketExtension e : message.getExtensions()) {
            Log.d("XXXXX", "Extension[" + e.getClass() + "] " + e.toXML());
        }
    }

    @Override
    public void run() {
        service.registerListener(this);
        try {
            MessageArchiveManagement mam = new MessageArchiveManagement();
            mam.setTo(to);
            mam.setFrom(account.getJid());
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.add(Calendar.DATE, 2);
            Calendar end = (Calendar) cal.clone();
            for (int i = 0; i < 32; i++) {
                mam.limit = 1000;
                mam.end =
                    cal.get(Calendar.YEAR) + "-" +
                    cal.get(Calendar.MONTH) + "-" +
                    cal.get(Calendar.DAY_OF_MONTH) + "T" +
                    cal.get(Calendar.HOUR) + ":" +
                    cal.get(Calendar.MINUTE) + ":" +
                    cal.get(Calendar.SECOND) + "Z";
                cal.add(Calendar.DATE, -1);
                mam.start =
                    cal.get(Calendar.YEAR) + "-" +
                    cal.get(Calendar.MONTH) + "-" +
                    cal.get(Calendar.DAY_OF_MONTH) + "T" +
                    cal.get(Calendar.HOUR) + ":" +
                    cal.get(Calendar.MINUTE) + ":" +
                    cal.get(Calendar.SECOND) + "Z";
                id = service.sendWithCallback(
                    mam,
                    account.getJid(),
                    new QueuePacketListener(queue),
                    5 * 60 * 1000
                );
                Packet result = queue.poll(5, TimeUnit.MINUTES);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            service.removeListener(this);
        }
    }

}
