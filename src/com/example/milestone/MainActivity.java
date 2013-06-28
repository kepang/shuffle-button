package com.example.milestone;

import java.io.IOException;
import java.util.Random;
import java.util.Stack;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnCompletionListener {

	private final int ID_INDEX = 0;
	private final int ARTIST_INDEX = 1;
	private final int TITLE_INDEX = 2;
	private final int DURATION_INDEX = 3;
	private final int ALBUM_INDEX = 4;
	private final String TAG = "DEBUG: ";
	
	ImageButton playB, nextB, previousB;
	Boolean playBcheck = false;
	
	
	MediaPlayer mPlayer;
	Cursor mCursor;
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

			playSong(id);
		}			
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		if (mPlayer != null) {
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
	
	/*********** PRIVATE METHODS ************/
	
	// Play Song. INPUT: system song id
	private void playSong(long id) {

		
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
	

	/************ LISTENERS ******************/
	// onCompletion
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onCompletion");
		playSong(moveCursorToNextSong());
	}
	
	
	/************* button listeners *************/
	public void addMusicControlListenerOnButton(){
		
		//Music Player Buttons
		previousB =(ImageButton) findViewById(R.id.previousBtn);
		playB = (ImageButton) findViewById(R.id.playBtn);
		nextB =(ImageButton) findViewById(R.id.nextBtn);
		
		//Calls the previous song on the playlist
		previousB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Toast.makeText(MainActivity.this, "Previous Button Clicked", Toast.LENGTH_SHORT).show();		
			}		
		});
		
		//Play or pause song
		playB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				//Toast.makeText(MainActivity.this, "Play Button Clicked", Toast.LENGTH_SHORT).show();
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
				playSong(id);
				if (playBcheck) {
					mPlayer.start();
				}
			}
		});
	}
}