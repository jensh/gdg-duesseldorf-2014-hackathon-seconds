package com.jens.voicemsg;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

public class Tools {

	public static void showInfoMessage(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}
	
	public static void showErrorMessage(Activity activity, String message) {
		if (activity.isFinishing()) return;
		
		AlertDialog alert = new AlertDialog.Builder(activity).create();
		alert.setMessage(message);
		alert.setTitle("Fehler");

		alert.show();
	}
}
