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
import android.widget.TextView;

public class LoginActivity extends Activity implements OnClickListener {

    private IBuddycloudService service;
    private ServiceConnection serviceConnection;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        View login_button = this.findViewById(R.id.login_button);
        login_button.setOnClickListener(this);

        bindBCService();
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.login_button:
            TextView jID_tv = (TextView) this.findViewById(R.id.e_mail_textbox);
            TextView password_tv = (TextView) this
                    .findViewById(R.id.password_textbox);

            String jid = jID_tv.getText().toString();
            String password = password_tv.getText().toString();

            jID_tv.setText("");
            password_tv.setText("");

            setVisible(false);

            try {
                service.login(jid, password);
            } catch (RemoteException e) {
                e.printStackTrace(System.err);
            }

            break;
        }
    }

    protected void onDestroy() {
        unbindBCService();
        super.onDestroy();
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
        if (serviceConnection == null) {
            serviceConnection = new ServiceConnection() {

                public void onServiceDisconnected(ComponentName name) {
                }

                public void onServiceConnected(ComponentName name, IBinder binder) {
                    service = IBuddycloudService.Stub.asInterface(binder);
                }
            };
        }
        bindService(
                new Intent(IBuddycloudService.class.getName()),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );
    }

}