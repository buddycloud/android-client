package com.buddycloud.android.buddydroid;


import com.buddycloud.android.buddydroid.R;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class RegisterActivity extends Activity implements OnClickListener{
   Registration m_reg;
   
   public RegisterActivity() {
	   m_reg = new Registration();
   }

   
	/** Called when the activity is first created. */
   @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_start);
        
        
        //login anonymous
		View anonymousButton = this.findViewById(R.id.anonymousButton);
		anonymousButton.setOnClickListener(this);
		
		//button to log in to buddycloud
		View login_text = this.findViewById(R.id.login_text);
		login_text.setOnClickListener(this);
		
		//button to go to the login window
		View goto_login_button = this.findViewById(R.id.goto_login_button);
		goto_login_button.setOnClickListener(this);
		
		
		
		//exit Application
		View exit_button = this.findViewById(R.id.exit_button);
		exit_button.setOnClickListener(this);
		
		
	
	
    }

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.goto_login_button:
			Intent login = new Intent(this, LoginActivity.class);
			startActivity(login);
			break;
		//anonymous bind
		case R.id.anonymousButton:
			String reg = m_reg.connectAnonymousToServer();
			
			// possibility to connect to the client, and try buddycloud
			break;
		
		
		case R.id.exit_button:
			finish();
			break;
		}
		
	}
}