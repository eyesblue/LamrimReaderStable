package eyes.blue;

import android.media.MediaPlayer;

public interface MediaPlayerControllerListener {
	/*
	 * Show subtitle in subtitle bar.
	 * */
	public void onSubtitleChanged(int index, SubtitleElement subtitle);
	public void onPlayerError();
	/*
	 * Called while user seek.
	 * */
	public void onSeek(int index, SubtitleElement subtitle);
	
	/*
	 * Call on media prepared.
	 * */
	public void onMediaPrepared();
	public void onStartPlay();
	public void onPause();
	public void onComplatePlay();
	public void getAudioFocusFail();
}
