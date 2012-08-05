package com.buddycloud.view;

import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PublishItem;
import org.jivesoftware.smackx.pubsub.packet.PubSub;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.buddycloud.BuddycloudService;
import com.buddycloud.IBuddycloudService;
import com.buddycloud.R;
import com.buddycloud.content.BuddyCloud.ChannelData;
import com.buddycloud.jbuddycloud.packet.BCAtom;
import com.buddycloud.util.HumanTime;
import com.googlecode.asmack.Stanza;
import com.googlecode.asmack.client.AsmackClient;

/**
 * Posting window, handles service interaction, content provider fetch of the
 * original posting and user interaction.
 */
public class PostActivity extends BCActivity implements OnClickListener {

    /**
     * The internal logging tag.
     */
    private static final String TAG = PostActivity.class.getSimpleName();

    /**
     * The item to be replied to
     */
    private String itemId;

    /**
     * The text we will post
     */
    private String posting;

    /**
     * The channel node (e.g. /user/yourname@yourdomain.tld/channel)
     */
    private String node;
    
    /**
     * Our view to initialize
     */
    private TextView titleView;
    private TextView messageView;
    private TextView jidView;
    private TextView locationView;
    private Button abortButton;
    private Button postButton;
    
    /** Instanse of BuddyCloudService */
    private IBuddycloudService mService;
    
    /** a boolean indicating if the service is bound */
    private boolean mServiceBound = false;
    
    private boolean isAlreadySetuped = false;
    
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
        	mService = (IBuddycloudService.Stub) service;
            mServiceBound = true;
            setClickableButtons(true);
            
            try {
				String[] jids = mService.getAllAccountJids(true);
				if (jids != null && jids.length > 0) {					
					node = jids[0];
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mServiceBound = false;
        }
    };    

    /**
     * Create a new instance of the post window.
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.post_message);

        int width = getWindowManager().getDefaultDisplay().getWidth();
        int height = getWindowManager().getDefaultDisplay().getHeight();

        if (width <= height) {
            height = (55 * height) / 100;
        } else {
            height = (90 * height) / 100;
            width = (80 * width) / 100;
        }
        getWindow().setLayout(width, height);

        initializeViews();
        setup(getIntent());
    }
    
    /**
     * initialize all views
     */
    private void initializeViews()
    {
        // fetch all views
        titleView = (TextView) findViewById(R.id.title);
        messageView = (TextView) findViewById(R.id.message);
        jidView = (TextView) findViewById(R.id.jid);
        locationView = (TextView) findViewById(R.id.location);
        abortButton = (Button) findViewById(R.id.abort);
        postButton = (Button) findViewById(R.id.post);

        // listeners
        abortButton.setOnClickListener(this);
        postButton.setOnClickListener(this);        
    }
    
    /** activate / deactivate buttons 
     * 
     * @param pClickable if true activate buttons, deactivate buttons
     */
    private void setClickableButtons(final boolean pClickable)
    {
        abortButton.setClickable(pClickable);
        postButton.setClickable(pClickable);
    }

    /**
     * Called on new intents, will reset the internal state.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        bindBCService();
        setup(intent);
    }

    /**
     * Initialize the internal state from an intent. Called by
     * <i>onNewIntent</i> and <i>onCreate</i>.
     * @param intent The intent with basic metadata.
     */
    private void setup(Intent intent) {
    	
        // if this is from the share menu
        if (Intent.ACTION_SEND.equals(intent.getAction()))
        {
        	shareContent(intent);
        	return;
        }

        // Fetch intent metadata
        long id = intent.getLongExtra("id", -1);
        String name = intent.getCharSequenceExtra("name").toString();
        node = intent.getCharSequenceExtra("node").toString();

        if (id != -1) {
        	reply(id);
        } else {
        	postTo(name);
        }
    }
    
    private void reply(final long pId) {
        // fetch parent data
        Cursor cursor = getContentResolver().query(
            ChannelData.CONTENT_URI,
            ChannelData.PROJECTION_MAP,
            ChannelData._ID + "=" + pId,
            null,
            null
        );
        if (!cursor.moveToFirst()) {
            setResult(0);
            finish();
            return;
        }

        final long parent =
            cursor.getLong(cursor.getColumnIndex(ChannelData.PARENT));

        if (parent != 0) {
            cursor.close();
            return;
        }

        itemId =
            cursor.getString(cursor.getColumnIndex(ChannelData.ITEM_ID));
        final String originalText = cursor.getString(cursor.getColumnIndex(
                ChannelData.CONTENT));
        final long timestamp = cursor.getLong(cursor.getColumnIndex(
                ChannelData.ITEM_ID));
        final String town = cursor.getString(cursor.getColumnIndex(
                ChannelData.GEOLOC_LOCALITY));
        final String country = cursor.getString(cursor.getColumnIndex(
                ChannelData.GEOLOC_COUNTRY));
        final String jid =  cursor.getString(cursor.getColumnIndex(
                ChannelData.AUTHOR_JID));
        final String affiliation = cursor.getString(cursor.getColumnIndex(
                ChannelData.AUTHOR_AFFILIATION));

        cursor.close();

        // update UI

        titleView.setText(R.string.posting_reply);
        messageView.setText(originalText);

        String humanTime = HumanTime.humanReadableString(
                System.currentTimeMillis() - timestamp);

        jidView.setText(jid);

        if ("owner".equals(affiliation)) {
            jidView.setTextColor(Color.rgb(150, 15, 20));
        } else
        if ("moderator".equals(affiliation)) {
            jidView.setTextColor(Color.rgb(200, 130, 50));
        } else {
            jidView.setTextColor(Color.BLACK);
        }

        if (town != null && town.length() > 0) {
            if (country != null && country.length() > 0) {
                locationView.setText(town + ", " + country + ", " +
                        humanTime);
            } else {
                locationView.setText(town + ", " + humanTime);
            }
        } else {
            if (country != null && country.length() > 0) {
                locationView.setText(country + ", " + humanTime);
            } else {
                locationView.setText(humanTime);
            }
        }    	
    }
    
    private void postTo(final String pName) {
        itemId = null;
        titleView.setText(getString(R.string.posting_post_to, pName));
        messageView.setVisibility(View.GONE);    	
    }
    
    
    private void shareContent(final Intent pIntent)
    {
    	bindService(pIntent, mConnection, Context.BIND_AUTO_CREATE);
    	setClickableButtons(false);
    	
    	Bundle extras = pIntent.getExtras();
    	if (extras.containsKey(Intent.EXTRA_STREAM)) {
    		Uri contentUri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
    		shareMediaContent(contentUri, pIntent.getType());
        } 
    	else if (extras.containsKey(Intent.EXTRA_TEXT))
    	{
    		shareTextContent(extras.getCharSequence(Intent.EXTRA_TEXT));
    	}    	
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	if (mServiceBound) {
    		unbindService(mConnection);
    		mServiceBound = false;
    	}
    }
    
    private void shareMediaContent(final Uri pContentUri, final String pMediaType) {
    	// STILL DO NOTHING, BUT FOR FUTURE
    	// http://eggie5.com/8-hook-share-picture-via-menu-android
    }
    
    private void shareTextContent(final CharSequence pContent) {
    	messageView.setText(pContent);
    }
    
    /**
     * Handle Post/Abort onClick.
     */
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.abort:
            setResult(0);
            finish();
            break;
        case R.id.post:
            posting = ((EditText)findViewById(R.id.posting)).getText().toString();
            if (post()) {
                setResult(0);
                finish();
            }
            break;
        }
    }

    /**
     * Post a new message to the current node.
     * @return true on success.
     */
    private boolean post() {
        BCAtom atom = new BCAtom();

        atom.setContent(posting);
        atom.setParent(itemId);

        String jid = getApplicationContext()
                        .getSharedPreferences("buddycloud", 0)
                        .getString("main_jid", "");

        if (jid.length() == 0) {
            return false;
        }

        atom.setAuthorJid(jid);

        PayloadItem<BCAtom> item = new PayloadItem<BCAtom>(null, atom);
        PublishItem<PayloadItem<BCAtom>> publish =
                            new PublishItem<PayloadItem<BCAtom>>(node, item);

        PubSub pubSub = new PubSub();
        pubSub.setTo("broadcaster.buddycloud.com");
        pubSub.setType(PubSub.Type.SET);
        pubSub.addExtension(publish);

        for (int i = 0; i < 3; i++) {
            try {
                Stanza stanza = AsmackClient.toStanza(pubSub, null);
                stanza.setVia(jid);
                if (service.send(stanza)) {
                    return true;
                }
            } catch (RemoteException e) {
                Log.d(TAG, "post failed, try " + i);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }

        return false;
    }

}
