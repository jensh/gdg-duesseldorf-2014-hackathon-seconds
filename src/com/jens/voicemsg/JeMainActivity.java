/* 
 * (c)2014 Jens Hauke <jens@4k2.de>
 */
package com.jens.voicemsg;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class JeMainActivity extends Activity {
    
	private static final int REC_IDLE = 1;
	private static final int REC_RECORDING = 2;
	private static final int REC_UPLOADING = 3;
	private static final int REC_RETRY_UPLOAD = 4;
	private static final String LOG_TAG = Configuration.LOG_TAG;
	protected static final int MESSAGE_NEW_INTENT = 0;

	// receive local broadcast messages from the GcmBroadcastReceiver for "updatechat"
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(LOG_TAG, "Got local broadcast message.");
			if (uiHandler == null) return;
			// Forward to the UI thread
			Message msg = uiHandler.obtainMessage();
		    msg.what = MESSAGE_NEW_INTENT;
		    msg.obj = intent;
		    uiHandler.sendMessage(msg);
		    // useIntent(intent);
		}
	};

	private static final class UiHandler extends Handler {
		private WeakReference<JeMainActivity> activity;

		public UiHandler(JeMainActivity jeMainActivity) {
			activity = new WeakReference<JeMainActivity>(jeMainActivity);
		}

		@Override
		public void handleMessage(Message msg) {
			if(msg.what== MESSAGE_NEW_INTENT){
				JeMainActivity jeMainActivity = activity.get();
				if (jeMainActivity != null) jeMainActivity.useIntent((Intent)msg.obj);
			}
			super.handleMessage(msg);
		}
	}
	
	private Button buttonRecord;
	private CharSequence buttonRecordDefaultText;
	private int recordState = REC_IDLE;
	private Recorder recorder;
	private String mUsername = "unknown";
	public String mRoom = "lobby";
	private String mUploadRetryFilename;
	private Player player;
	private GcmChat mGCMChat = null;
	public TextView display;
	private EditText editTextRoom;
	private Button buttonEnterRoom;
	private Button buttonLeaveRoom;
	private EditText editTextUser;
	private Handler uiHandler;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        loadPeristentState();
        gcmRegister();
        // initSocket();
        // jeMainView1 = (JeMainView) findViewById(R.id.jeMainView1);

        // Register to receive local broadcast.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
        		new IntentFilter("updatechat"));
        
        uiHandler = new UiHandler(this);
    	
        buttonRecord = (Button) findViewById(R.id.buttonRecord);
        buttonRecord.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				recordClicked();
			}
		});
        buttonRecordDefaultText = buttonRecord.getText();
        
        editTextRoom = (EditText) findViewById(R.id.editTextRoom);
        editTextRoom.setText(mRoom);
        buttonEnterRoom = (Button) findViewById(R.id.buttonEnterRoom);
        buttonEnterRoom.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				enterRoom(editTextRoom.getText().toString());
			}
		});
        buttonLeaveRoom = (Button) findViewById(R.id.buttonLeaveRoom);
        buttonLeaveRoom.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				leaveRoom();
			}
		});
        editTextUser = (EditText) findViewById(R.id.editTextUser);
        editTextUser.setText(mUsername);
        editTextUser.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				mUsername = editTextUser.getText().toString();
			}
		});
        display = (TextView) findViewById(R.id.display);
        
        if (savedInstanceState != null) {
        	// We are being restored
//        	Bundle map = savedInstanceState.getBundle("jeMainView1");
//        	if (map != null) jeMainView1.restoreState(map);
        }

    	display.append("Hallo!\n");
//    	display.append("Achtung! Die App saugt gerne am Akku und leakt auch Resourcen wenn sie läuft!\n"
//    			+ "Benutzung auf eigene Gefahr und ohne Gewähr :-).\n");
    	useIntent(getIntent());
    }

    @Override
	protected void onStop() {
    	savePeristentState();
		super.onStop();
    }

	private void loadPeristentState() {
    	final SharedPreferences prefs = getSharedPreferences(JeMainActivity.class.getSimpleName(),
				Context.MODE_PRIVATE);
    	mRoom = prefs.getString("room", "");
    	mUsername = prefs.getString("user", "");
    }
    
    private void savePeristentState() {
		final SharedPreferences prefs = getSharedPreferences(JeMainActivity.class.getSimpleName(),
				Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("room", mRoom);
        editor.putString("user", mUsername);
        editor.commit();
	}


	private void gcmRegister() {
		if (mGCMChat == null) mGCMChat = new GcmChat();
        mGCMChat.register(this, mRoom);
	}


	protected void leaveRoom() {
		editTextRoom.setText("");
		enterRoom("");
	}


	protected void enterRoom(String newRoom) {
		mRoom = newRoom;
		savePeristentState();
		gcmRegister();
	}


	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent); // Update existing intent
		// display.append("onNewIntent------ " + /*intent.toString() +*/ "\n");
		useIntent(intent);
	}

	private void useIntent(Intent intent) {
		String cmd = intent.getStringExtra("cmd");
		if (cmd == null) cmd = "";
		
		if (cmd.equals("record")) {
			useIntentRecord(intent);
		} else {
			useIntentUpdatechat(intent);
		}
	}
	
	private void useIntentRecord(Intent intent) {
		recordClicked();
	}

	private void useIntentUpdatechat(Intent intent) {
        String url_audio = intent.getStringExtra("url_audio");
        if (url_audio != null) {
        	// display.append("url_audio: " + url_audio + "\n");
        	playStart(url_audio);
        }
        String msg = intent.getStringExtra("msg");
        String user = intent.getStringExtra("user");
        if (user != null && user.length() > 0) {
        	if (msg != null && msg.length() > 0) {
        		display.append(user + ":");
        	} else {
        		display.append("Message from " + user + "\n");
        	}
    	}
        if (msg != null && msg.length() > 0) {
        	display.append(msg + "\n");
        }
        // display.append(intent.toString() + "\n");
        Bundle extras = intent.getExtras();
        if (extras != null) {
        	// display.append("Use:" + extras.toString() + "\n");
        }
	}


	public void updateChat(String username, Object object) {
		Log.d(LOG_TAG, "updateChat " + username +
				" : (" + object.getClass().toString() + ") "+ object.toString());
		if (object instanceof JSONObject) {
			try {
				JSONObject jobj = (JSONObject) object;
				String url_audio = jobj.getString("url_audio");
				if (url_audio.length() > 4 &&
						!username.equals(mUsername) /* do not eat my own foot */) {
					playStart(url_audio);
				}
			} catch (JSONException e) {
				// Ignore unknown type 
			}
		}
	}

	
	public void updateRoom(String roomname) {
		// Got an update_room message from the server.
		if (roomname==null) roomname = "<?>";
		// Tools.showInfoMessage(this, "#" + roomname + " joined.");
		// display.append("Enter #"+roomname + "\n");
	}


	public void updateUsername(String username) {
		// Ignore server suggested username.
		// mUsername = username;
	}

	protected void recordStart() {
		final JeMainActivity activity = this;
		if (mRoom.length() == 0) {
			Tools.showErrorMessage(activity, "Join a room first!");
			return;
		}
		if (mUsername.length() == 0) {
			Tools.showErrorMessage(activity, "Please set a username!");
			return;
		}

		setRecordState(REC_RECORDING);
    	
    	recorder = new Recorder();
    	recorder.setOnDoneListener(new Recorder.OnDoneListener() {

			@Override
			public void onDone(Recorder recorder, Boolean err) {
				if (err) {
					Tools.showErrorMessage(activity, "Recording failed.");
					setRecordState(REC_IDLE);
				} else {
					uploadMessage(recorder.getFilename());
				}
			}});
    	recorder.start();
	}

	private void playStart(String url_audio) {
		Log.d(LOG_TAG, "Try to play: " + url_audio);
		player = new Player();
		player.setUrl(url_audio);
		player.start();
	}

	protected void uploadMessage(String filename) {
		final JeMainActivity activity = this;
		mUploadRetryFilename = filename;
		setRecordState(REC_UPLOADING);
		
		RequestParams params = new RequestParams();
		params.put("username", mUsername );
		params.put("room", mRoom);
		params.put("msg", "");
		try {
			params.put("audio", new FileInputStream(filename), "message.mp4", "audio/mp4");
		} catch (FileNotFoundException e) {
			setRecordState(REC_IDLE);
			Tools.showInfoMessage(activity, "reading local messsage '" + filename + "' failed.");
			return;
		} 

		// final Notification n = createNotification(activity, decorator, task);

		AsyncHttpClient client = new AsyncHttpClient();
		client.post(Configuration.URL_CHAT_UPLOAD, params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String response) {
				final String message = "Send";
				Tools.showInfoMessage(activity, message);
				final int progress = 100;
				updateNotification(message, progress);
				done(false);
			}

			@Override
			public void onFailure(Throwable arg0, String content) {
				Tools.showErrorMessage(activity, arg0.toString());
				updateNotification("Fehler : " + arg0, 0);
				done(true);
			}

			/*
			@Override
			public void onProgress(int position, int length) {
				// Log.d("progress", "pos: " + position + " len: " + length);
				updateNotification(null, position * 100 / (length > 0 ? length : 1));
			}
*/
			private void done(boolean err) {
				if (!err) {
					activity.setRecordState(REC_IDLE);
				} else {
					activity.setRecordState(REC_RETRY_UPLOAD);
				}
			}

			private void updateNotification(final String message, final int progress) {
				// setProgressBar(R.id.progressBar, 100, progress, false);
				//NotificationManager ns = (NotificationManager)activity.getSystemService(Context.NOTIFICATION_SERVICE);
				//ns.notify(uploadId/*HELLO_ID*/, n);
			}
		});
	}


	protected void recordClicked() {
		switch (recordState) {
		case REC_IDLE:
			recordStart();
			break;
		case REC_RETRY_UPLOAD:
			uploadMessage(mUploadRetryFilename);
			break;
		}
	}
	
	
	private void setRecordState(int newState) {
		recordState = newState;
		switch (recordState) {
		case REC_IDLE:
			buttonRecord.setText(buttonRecordDefaultText);
			break;
		case REC_RECORDING:
			buttonRecord.setText("Recording...");
			break;
		case REC_UPLOADING:
			buttonRecord.setText("Upload...");
			break;
		case REC_RETRY_UPLOAD:
			buttonRecord.setText("Retry");
			break;
		}
	}


	@Override
	protected void onDestroy() {
    	// Free handler
    	uiHandler = null;

    	// Unregister for local broadcasts
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

    	super.onDestroy();
	}


	@Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	savePeristentState();
    }
    
}
