package com.buddycloud;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;

import android.os.RemoteException;
import android.util.Log;

import com.googlecode.asmack.client.AsmackClient;

/**
 * A simple state flow system to do short sequences of calls in a more-or-less
 * linear way.
 */
public abstract class StateSequenceWorkflow implements PacketListener {

    /**
     * The logging tag of this class.
     */
    private static final String TAG =
                                StateSequenceWorkflow.class.getSimpleName();

    /**
     * The asmack client.
     */
    protected final AsmackClient client;

    /**
     * The current sequence id.
     */
    protected int sequence = 0;

    /**
     * The callback ttl.
     */
    protected long ttl = 60*1000;

    /**
     * The jid to use during stanza sending.
     */
    protected final String via;

    /**
     * Create a new state sequence.
     * @param client The asmack client for sending.
     * @param via The jid used for sending.
     */
    public StateSequenceWorkflow(AsmackClient client, String via) {
        super();
        this.client = client;
        this.via = via;
    }

    public abstract void start();

    public void state1(Packet packet){}
    public void state2(Packet packet){}
    public void state3(Packet packet){}
    public void state4(Packet packet){}
    public void state5(Packet packet){}
    public void state6(Packet packet){}
    public void state7(Packet packet){}
    public void state8(Packet packet){}
    public void state9(Packet packet){}

    public void send(Packet packet) {
        send(packet, sequence + 1);
    }

    public void send(Packet packet, int i) {
        sequence = i;
        client.sendWithCallback(packet, via, this, ttl);
    }

    private void error(RemoteException e) {
        Log.w(TAG, "unhandled error", e);
    }

    @Override
    public void processPacket(Packet packet) {
        switch (sequence) {
        case 1: state1(packet); break;
        case 2: state2(packet); break;
        case 3: state3(packet); break;
        case 4: state4(packet); break;
        case 5: state5(packet); break;
        case 6: state6(packet); break;
        case 7: state7(packet); break;
        case 8: state8(packet); break;
        case 9: state9(packet); break;
        default: 
            try {
                Method method =
                        getClass().getMethod("state" + sequence, Packet.class);
                method.invoke(this, packet);
            } catch (SecurityException e) {
                Log.w(TAG, "unhandled state" + sequence, e);
            } catch (NoSuchMethodException e) {
                Log.w(TAG, "unhandled state" + sequence, e);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "unhandled state" + sequence, e);
            } catch (IllegalAccessException e) {
                Log.w(TAG, "unhandled state" + sequence, e);
            } catch (InvocationTargetException e) {
                Log.w(TAG, "unhandled state" + sequence, e);
            } catch (Exception e) {
                Log.w(TAG, "unhandled state" + sequence, e);
            }
            break;
        }
    }

}
