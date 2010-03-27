package com.buddycloud.android.buddydroid;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;

public class RegisterActivity extends Activity implements OnClickListener {

    private IBuddycloudService service;
    private ServiceConnection serviceConnection;

    private final IBuddycloudServiceListener listener =
        new IBuddycloudServiceListener.Stub() {

            public void onBCLoginFailed() throws RemoteException {
            }

            public void onBCDisconnected() throws RemoteException {
            }

            public void onBCConnected() throws RemoteException {
                runOnUiThread(new Runnable() {
                    public void run() {
                        loggedIn();
                    }
                });
            }
        };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Intent backgroungService = new Intent(this, BuddycloudService.class);
        startService(backgroungService); // just in case...

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_start);

        // login anonymous
        View anonymousButton = this.findViewById(R.id.anonymousButton);
        anonymousButton.setOnClickListener(this);

        // button to log in to buddycloud
        View login_text = this.findViewById(R.id.login_text);
        login_text.setOnClickListener(this);

        // button to go to the login window
        View goto_login_button = this.findViewById(R.id.goto_login_button);
        goto_login_button.setOnClickListener(this);

        // exit Application
        View exit_button = this.findViewById(R.id.exit_button);
        exit_button.setOnClickListener(this);

    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.goto_login_button:
            Intent login = new Intent(this, LoginActivity.class);
            startActivity(login);
            break;
        case R.id.anonymousButton:
            try {
                service.loginAnonymously();
            } catch (RemoteException e) {
                e.printStackTrace(System.err);
            }
            break;

        case R.id.exit_button:
            finish();
            break;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            bindBCService();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        bindBCService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindBCService();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        bindBCService();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        bindBCService();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        bindBCService();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        bindBCService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindBCService();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        unbindBCService();
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        bindBCService();
        super.onStart();
    }

    @Override
    protected void onStop() {
        unbindBCService();
        super.onStop();
    }

    private void loggedIn() {
        Intent main = new Intent(this, MainActivity.class);
        main.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        main.addFlags(Intent.FLAG_FROM_BACKGROUND);
        startActivity(main);
        finish();
    }

    private final synchronized void unbindBCService() {
        synchronized (this) {
            if (serviceConnection != null) {
                try {
                    unbindService(serviceConnection);
                } catch (Exception e) { /* ignore */ }
                service = null;
            }
        }
    }

    private final synchronized void bindBCService() {
        if (serviceConnection != null) {
            if (service != null) {
                try {
                    if (service.isConnected()) {
                        try {
                            if (service.isAuthenticated()) {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        loggedIn();
                                    }
                                });
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace(System.err);
                        }
                        return;
                    }
                } catch (RemoteException e) { /* ignore */ }
            }
            try {
                unbindService(serviceConnection);
            } catch (Exception e) { /* ignore */ }
        }
        serviceConnection = new ServiceConnection() {

            public void onServiceDisconnected(ComponentName name) {
            }

            public void onServiceConnected(ComponentName name, IBinder binder) {
                service = IBuddycloudService.Stub.asInterface(binder);
                try {
                    service.addListener(listener);
                } catch (RemoteException e) {
                    e.printStackTrace(System.err);
                }
                try {
                    if (service.isAuthenticated()) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                loggedIn();
                            }
                        });
                    }
                } catch (RemoteException e) {
                    e.printStackTrace(System.err);
                }
            }
        };
        bindService(
                new Intent(IBuddycloudService.class.getName()),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );
    }

}