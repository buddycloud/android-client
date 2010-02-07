package com.buddycloud.android.buddydroid;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.*;
import org.apache.harmony.javax.security.auth.callback.Callback;
import org.apache.harmony.javax.security.auth.callback.CallbackHandler;
import org.apache.harmony.javax.security.auth.callback.UnsupportedCallbackException;
import org.jivesoftware.*;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.sasl.SASLAnonymous;
import org.jivesoftware.smack.sasl.SASLPlainMechanism;



public class Registration {
	
	private XMPPConnection connection;
	public Registration() {
		ConnectionConfiguration connConfig = new ConnectionConfiguration("buddycloud.com");
		connConfig.setSASLAuthenticationEnabled(true);
		connConfig.setSelfSignedCertificateEnabled(true);
		connection = new XMPPConnection(connConfig);
		try {
			connection.connect();
		} catch (XMPPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
	public boolean logintToServer(String jID, String password) {
		Boolean isAuth = false;
		try {
			connection.login(jID, password);
			isAuth = connection.isAuthenticated();
		} catch (XMPPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return isAuth;
	}

	public boolean connecttoServer(String username, String password) {		
		AccountManager accMan = connection.getAccountManager();
//		SASLAuthentication saslAuth = connection.getSASLAuthentication();
//		SASLPlainMechanism plain = new SASLPlainMechanism(saslAuth);
		

		// Collection<String> attr = accMan.getAccountAttributes();
		String hostname = "buddycloud.com";
	
		boolean connected = false;
		try {
			 accMan.createAccount(username, password);
//			plain.authenticate(username, hostname, password);

			connected = true;
//		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		} catch (XMPPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return connected;
	}

	public String connectAnonymousToServer() {
		SASLAuthentication auth = connection.getSASLAuthentication();
		/*
		 * Available mechanism: [PLAIN, DIGEST-MD5] Check if authentication is
		 * available. List<Class> saslMech = auth.getRegisterSASLMechanisms();
		 * 
		 * for(int i = 0; i < saslMech.size(); i++){
		 * 
		 * }
		 */
		// SASLDigestMD5Mechanism md5 = new SASLDigestMD5Mechanism(auth);
		// thinking about callbacks or packet listener
//		AccountManager accMan = connection.getAccountManager();
//		Collection<String> attrib = accMan.getAccountAttributes();
		String authString = "";
		try {
			authString = auth.authenticateAnonymously();
		} catch (XMPPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		boolean secure = connection.isSecureConnection();
		boolean conAble = connection.isConnected();
		connection.disconnect();

		return authString;
	}
}
