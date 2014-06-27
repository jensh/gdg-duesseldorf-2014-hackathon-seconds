/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jens.voicemsg;
/* 
 * 2014 jens@4k2.de
 * Modifications made for the App VoiceMsg
 */
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.jens.voicemsg.R;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }
	private static final String LOG_TAG = Configuration.LOG_TAG;

    @Override
    protected void onHandleIntent(Intent intent) {
        // sendNotification("A Notification");

        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " + extras.toString());
            // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            	// Post notification of received message.
                // sendNotification("Received: " + extras.toString());
                Log.i(LOG_TAG, "Received: " + extras.toString());
                onMessage(extras);
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

	private void onMessage(Bundle extras) {
    	String cmd = extras.getString("cmd");
    	if (cmd == null) return;
    	Log.i(LOG_TAG, "Received: cmd " + cmd);
    	
    	if (cmd.equals("updatechat")) {
    		String user = extras.getString("user");
    		if (user == null) user = "unknown";
    		Log.i(LOG_TAG, "Received:   user:" + user);
        	
    		String url_audio = extras.getString("url_audio");
    		if (url_audio == null) url_audio = "";
    		
    		String msg = extras.getString("msg");
    		if (msg == null) msg = "";

    		Log.i(LOG_TAG, "Received:   msg:" + msg + " url:" + url_audio + " user:" + user);
        	/* Not allowed here to play.? */
    		if (url_audio.length() > 4) {
    			Player player = new Player();
    			player.setUrl(url_audio);
    			player.start();
    		}
    		Intent i = new Intent(this, JeMainActivity.class);
    		i.putExtra("cmd", cmd);
    		i.putExtra("user", user);
    		i.putExtra("msg", msg);
    		i.putExtra("url_audio", url_audio);
    		sendNotification(user.length() > 0 ? user : "New message",
    				msg.length() == 0 ? "has something to say" : msg, i);
    		
    		// Send local broadcast to a already running activity. If there is one.
    		i = new Intent("updatechat");
    		i.putExtra("cmd", cmd);
    		i.putExtra("user", user);
    		i.putExtra("msg", msg);
    		i.putExtra("url_audio", url_audio);
    		LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    		Log.d(LOG_TAG, "Sent local broadcast message.");
    	}
	}

	// Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String title, String msg, Intent intent) {
    	mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
    	
    	PendingIntent contentIntent = PendingIntent.getActivity(this, (int)System.currentTimeMillis(),
    			intent, PendingIntent.FLAG_UPDATE_CURRENT);
    	
    	PendingIntent contentIntent2 = PendingIntent.getActivity(this, 1,
    			intent, PendingIntent.FLAG_UPDATE_CURRENT);
    	
    	Intent intentReply = new Intent(this, JeMainActivity.class);
    	intentReply.putExtra("cmd", "record");
    	
    	PendingIntent contentIntent3 = PendingIntent.getActivity(this, 0,
    			intentReply, PendingIntent.FLAG_UPDATE_CURRENT);
        
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.icon)
        .setContentTitle(title)
        .setStyle(new NotificationCompat.BigTextStyle()
        .bigText(msg))
	// .setNumber(unique++)
	// .setSound(Uri.parse("http://4k2.de/up/ping.mp4"))
        .setContentText(msg);
        //startActivity(intentJeMainActivity);
        mBuilder.setContentIntent(contentIntent);
        mBuilder.addAction(R.drawable.play, "Play", contentIntent2);
        mBuilder.addAction(R.drawable.icon, "Send 7 seconds", contentIntent3);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void sendNotification(String string) {
    	sendNotification("Message", string, new Intent(this, JeMainActivity.class));
	}


}
