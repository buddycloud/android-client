package com.buddycloud.view;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.buddycloud.IBuddycloudService;

/**
 * An Activity that handles the bind/unbinding of the BC service in the
 * the background.
 */
public class BCActivity extends Activity {

    /**
     * The binding connection to the 
     */
    protected ServiceConnection connection;

    /**
     * The RPC implementation.
     */
    protected IBuddycloudService service;

    /**
     * Remove the binding to the buddycloud service.
     */
    protected final synchronized void unbindBCService() {
        if (connection == null) {
            return;
        }
        try {
            unbindService(connection);
        } catch (IllegalArgumentException e) {
            /* How could we verify that there is a pending connection request?
             * 
             * Looks like this is impossible. Silence the exception is evil
             * but somehow works around the problem.
             */
        }
    }

    /**
     * Bind to the buddycloud service, if not yet bound or dead.
     */
    protected final synchronized void bindBCService() {
        Intent serviceIntent = new Intent(
                IBuddycloudService.class.getCanonicalName());
        startService(serviceIntent);
        if (connection == null) {
            connection = new ServiceConnection() {

                public void onServiceDisconnected(ComponentName name) {
                }

                public void onServiceConnected(
                        ComponentName name,
                        IBinder binder
                ) {
                    service =
                        IBuddycloudService.Stub.asInterface(binder);
                }
            };
        }
        if (service != null) {
            if (service.asBinder().isBinderAlive()) {
                return;
            } else {
                service = null;
            }
        }
        bindService(new Intent(
            IBuddycloudService.class.getName()),
            connection,
            Context.BIND_AUTO_CREATE
        );
    }

    /**
     * Unbind service on destroy.
     */
    @Override
    protected void onDestroy() {
        unbindBCService();
        super.onDestroy();
    }

    /**
     * Unbind service on pause.
     */
    @Override
    protected void onPause() {
        unbindBCService();
        super.onPause();
    }

    /**
     * Bind service on restart.
     */
    @Override
    protected void onRestart() {
        bindBCService();
        super.onRestart();
    }

    /**
     * Bind service on resume.
     */
    @Override
    protected void onResume() {
        bindBCService();
        super.onResume();
    }

    /**
     * Bind service on create.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindBCService();
    }

    /**
     * Bind service on start.
     */
    @Override
    protected void onStart() {
        bindBCService();
        super.onStart();
    }

    /**
     * Unbind/Bind the service on focus change.
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            bindBCService();
        } else {
            unbindBCService();
        }
        super.onWindowFocusChanged(hasFocus);
    }

    /**
     * Unbind service on stop.
     */
    @Override
    protected void onStop() {
        unbindBCService();
        super.onStop();
    }

}
