/* 
 * (c)2014 Jens Hauke <jens@4k2.de>
 */
package com.jens.voicemsg;

import java.io.IOException;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;

public class Player implements OnPreparedListener, OnErrorListener, OnCompletionListener {

	public interface OnDoneListener {
		void onDone(Recorder recorder, Boolean err);
	}

	private String mUrlAudio;
	private MediaPlayer mediaPlayer = null;

	public void setUrl(String url_audio) {
		mUrlAudio = url_audio;
	}

	public void start() {
		assert(mediaPlayer == null);
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		try {
			mediaPlayer.setDataSource(mUrlAudio);
			mediaPlayer.setOnPreparedListener(this);
			mediaPlayer.setOnErrorListener(this);
			mediaPlayer.setOnCompletionListener(this);
			mediaPlayer.prepareAsync();
		} catch (IllegalArgumentException e) {
			playDone(true);
		} catch (IllegalStateException e) {
			playDone(true);
		} catch (IOException e) {
			playDone(true);
		}
	}

	@Override
	public void onPrepared(MediaPlayer mediaPlayer) {
		mediaPlayer.start();
	}

	@Override
	public boolean onError(MediaPlayer mediaPlayer, int arg1, int arg2) {
		playDone(true);
		return true; // return false would trigger the onComletion listener
	}

	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
		playDone(false);
	}

	private void playDone(boolean err) {
		if (mediaPlayer == null) return;
		mediaPlayer.release();
		mediaPlayer = null;
	}

}
