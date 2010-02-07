package com.buddycloud.android.buddydroid;

import com.buddycloud.android.buddydroid.R;

import android.app.Activity;
import android.content.DialogInterface;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class LoginActivity extends Activity implements OnClickListener {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);

		View login_button = this.findViewById(R.id.login_button);
		login_button.setOnClickListener(this);

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.login_button:
			 TextView jID_tv = (TextView) this.findViewById(R.id.e_mail_textbox);
			 TextView password_tv = (TextView) this.findViewById(R.id.password_textbox);
			 
			 Registration reg = new Registration();
			 reg.connecttoServer(jID_tv.getText().toString(),password_tv.getText().toString());
			 
//			 reg.logintToServer(jID_tv.getText().toString(), password_tv.getText().toString());
			 
			 jID_tv.setText("");
			 password_tv.setText("");
			 
			break;
		}
	}
}