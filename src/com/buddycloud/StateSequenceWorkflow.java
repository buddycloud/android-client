package com.buddycloud;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;

import android.os.RemoteException;
import android.util.Log;

import com.googlecode.asmack.client.AsmackClient;

public abstract class StateSequenceWorkflow implements PacketListener {

    private static final String TAG =
                                StateSequenceWorkflow.class.getSimpleName();
    protected final AsmackClient client;
    protected int sequence = 0;
    protected long ttl = 60*1000;
    protected final String via;

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
        try {
            client.sendWithCallback(packet, via, this, ttl);
        } catch (RemoteException e) {
            error(e);
        }
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
