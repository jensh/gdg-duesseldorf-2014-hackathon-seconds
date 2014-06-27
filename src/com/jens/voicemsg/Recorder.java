/* 
 * (c)2014 Jens Hauke <jens@4k2.de>
 */
package com.jens.voicemsg;

import java.io.IOException;

import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

public class Recorder implements MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener {

	public interface OnDoneListener {
		void onDone(Recorder recorder, Boolean err);
	}

	private static final String LOG_TAG = Configuration.LOG_TAG + ":Recorder";
	private static final int MSG_LENGTH_MS = 6 * 1000 /* ms */;
	private MediaRecorder mRecorder;
	private String mFileName;
	private OnDoneListener onDoneListener = null;
	private boolean mRecording = false;

	public Recorder() {
		mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
		mFileName += "/voicemsg.mp4";
	}
	
	public void start() {
		// TODO Auto-generated method stub
		mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setMaxDuration(MSG_LENGTH_MS);
        mRecorder.setOnInfoListener(this);
        mRecorder.setOnErrorListener(this);
        
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
            recordDone(true);
        }
        mRecording = true;
        mRecorder.start();
	}

	private void recordDone(Boolean err) {
		if (!mRecording) return;
		mRecorder.stop();
		mRecorder.release();
		mRecorder = null;
		mRecording = false;
		if (onDoneListener != null) {
			onDoneListener.onDone(this, err);
		}
	}

	@Override
	public void onInfo(MediaRecorder mr, int what, int extra) {
		switch (what) {
		case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
			recordDone(false);
			break;
		}
	}

	@Override
	public void onError(MediaRecorder arg0, int arg1, int arg2) {
		recordDone(true); // Any error
	}

	public void setOnDoneListener(OnDoneListener onDoneListener) {
		this.onDoneListener = onDoneListener;
	}

	public String getFilename() {
		return mFileName;
	}

}
