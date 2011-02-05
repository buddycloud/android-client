package com.buddycloud.view;

import com.buddycloud.R;

import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class FollowActivity extends BCActivity implements OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.follow);

        Button abort = (Button) findViewById(R.id.abort);
        Button follow = (Button) findViewById(R.id.follow);

        abort.setOnClickListener(this);
        follow.setOnClickListener(this);

        setup(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setup(intent);
    }

    private void setup(Intent intent) {
        char value[] = intent.getCharArrayExtra("value");
        TextView textView = (TextView) findViewById(R.id.edit);
        if (value != null) {
            textView.setText(new String(value));
        } else {
            textView.setText("");
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.abort:
            finish();
            break;
        case R.id.follow:
            EditText edit = (EditText) findViewById(R.id.edit);
            String following = edit.getText().toString().trim();
            if (following.indexOf('@') == -1) {
                following = following.substring(following.lastIndexOf('@') + 1);
                for (int i = 0; i < 2; i++) {
                    try {
                        if (service.follow("/channel/" + following)) {
                            finish();
                            return;
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace(System.err);
                    }
                    unbindBCService();
                    bindBCService();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }
            break;
        }
    }

}
