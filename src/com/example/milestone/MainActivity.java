package com.example.milestone;

import java.io.IOException;
import java.util.Random;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnCompletionListener {

	private final int ID_INDEX = 0;
	private final int ARTIST_INDEX = 1;
	private final int TITLE_INDEX = 2;
	private final int DURATION_INDEX = 3;
	private final int ALBUM_INDEX = 4;
	private final String TAG = "DEBUG";
	private final int TIMER = 1000;
	
	ImageButton playB, nextB, previousB;
	SeekBar seekBar;
	Boolean playBcheck = false;
	
	
	MediaPlayer mPlayer;
	Cursor mCursor;
	Handler mHandler;
	TextView tv_songTitle;
	int songsListSize;
	
	String rq_columns[] = {
			
			MediaStore.Audio.Media._ID,
			MediaStore.Audio.Media.ARTIST,
			MediaStore.Audio.Media.TITLE,
			MediaStore.Audio.Media.DURATION,
			MediaStore.Audio.Media.ALBUM
			
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		tv_songTitle = (TextView) findViewById(R.id.tv_songTitle);

		mHandler = new Handler();
		ContentResolver contentResolver = getContentResolver();
		Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		mCursor = contentResolver.query(uri, rq_columns, null, null, null);
	    songsListSize = mCursor.getCount();
	    
	    addMusicControlListenerOnButton();
	
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if (mCursor == null) {
		    // query failed, handle error.
			tv_songTitle.setText("Cursor query failed");
		} 
		else if (!mCursor.moveToFirst()) {
		    // no media on the device
			tv_songTitle.setText("No songs in the device");
		}
		else {
			// system song ID to send to music player which song to play
			long id = moveCursorToNextSong();

			mPlayer = new MediaPlayer();
			mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		    mPlayer.setOnCompletionListener(this);

		    selectSong(id);
		    
		    // init seekbar
		    seekBar.setProgress(0);
		    seekBar.setMax(100);
		    updateSeekBar();
		    autoPlay();
		}			
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		if (mPlayer != null) {
			mPlayer.stop();
			mPlayer.release();
			mPlayer = null;	
		}
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub

		super.onStop();

	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (mCursor != null) {
			mCursor.close();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void updateSeekBar() {
		mHandler.postDelayed(timerThread, TIMER);
	}
	
	/*********** PRIVATE METHODS ************/
	// SeekBar and song timer update thread
	private Runnable timerThread = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			long duration;
			long position;

			// Make sure main thread has not released the mPlayer
			if (mPlayer != null) {
				duration = mPlayer.getDuration();
				position = mPlayer.getCurrentPosition();	
				int progress = (int) (100*position/duration);	
				// update seekbar position

				seekBar.setProgress(progress);
			}
				


			//Log.d(TAG, progress + "=(100)" + position + "/" + duration);
			mHandler.postDelayed(timerThread, TIMER);
		}
		
	};
	
	
	// Play Song. INPUT: system song id
	private void selectSong(long id) {

		
		Uri myUri = ContentUris.withAppendedId(
				android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
		
		try {
			mPlayer.reset();
			mPlayer.setDataSource(getApplicationContext(), myUri);
			mPlayer.prepare();

		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (IllegalStateException e) {
			e.printStackTrace();
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		catch (SecurityException e) {
			e.printStackTrace();			
		}
		catch (RuntimeException e) {
			e.printStackTrace();
		}
		
		// Update text
		tv_songTitle.setText(mCursor.getString(ARTIST_INDEX));
		tv_songTitle.append("\n" + mCursor.getString(TITLE_INDEX));
		tv_songTitle.append("\n" + mCursor.getLong(ID_INDEX));
		
	}
	
	private long moveCursorToNextSong() {
		// Get next song
		mCursor.moveToPosition(new Random().nextInt(songsListSize));
		// ID to send to music player which song to play
		long id = mCursor.getLong(ID_INDEX);
		
		return id;
	}
	
	private void autoPlay() {
		playB.setImageResource(R.drawable.pausebtn);
		playBcheck = true;
		mPlayer.start();
		
	}
	

	/************ LISTENERS ******************/
	// onCompletion
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onCompletion");
		selectSong(moveCursorToNextSong());
		mPlayer.start();
	}
	
	
	/************* button listeners *************/
	public void addMusicControlListenerOnButton(){
		
		// SeekBar
		seekBar = (SeekBar) findViewById(R.id.seekBar);
		
		//Music Player Buttons
		previousB =(ImageButton) findViewById(R.id.previousBtn);
		playB = (ImageButton) findViewById(R.id.playBtn);
		nextB =(ImageButton) findViewById(R.id.nextBtn);
		
		// SeekBar Callback
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				mHandler.removeCallbacks(timerThread);
				
				int progress = seekBar.getProgress();
				long duration = mPlayer.getDuration();
				int time = (int) (progress * duration) / 100;
				//Log.i(TAG, progress + ":" + duration + ":" + time);

				mPlayer.seekTo(time); // seekto in msec 
				updateSeekBar();
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				mHandler.removeCallbacks(timerThread);				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub

			}
		});
		
		//Calls the previous song on the playlist
		previousB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Log.i(TAG, "Previous Button Clicked");	
				mPlayer.seekTo(0);
				updateSeekBar();
			}		
		});
		
		//Play or pause song
		playB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if(!playBcheck){
					playB.setImageResource(R.drawable.pausebtn);
					playBcheck = true;
					mPlayer.start();
				}else{
					playB.setImageResource(R.drawable.playbtn);
					playBcheck = false;
					mPlayer.pause();
				}	
			}
			
		});
		
		//Next button event listener
		
		nextB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				mPlayer.stop();
				long id = moveCursorToNextSong();
				selectSong(id);
				if (playBcheck) {
					mPlayer.start();
				}
			}
		});
	}
}