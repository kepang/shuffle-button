package com.example.milestone;

import java.io.IOException;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
	
	ImageButton playB;
	ImageButton forwardB;
	ImageButton previousB;
	
	
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
	    
	    addPlayerControlListenerOnButton();
	
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if (mCursor == null) {
		    // query failed, handle error.
			tv_songTitle.setText("query failed, handle error");
		} 
		else if (!mCursor.moveToFirst()) {
		    // no media on the device
			tv_songTitle.setText("no media in the device");
		}
		else {
		    //int titleColumn = mCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
		    //int idColumn = mCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
		    
			mCursor.moveToPosition(new Random().nextInt(songsListSize));
			tv_songTitle.setText(mCursor.getString(ARTIST_INDEX));
			tv_songTitle.append("\n" + mCursor.getString(TITLE_INDEX));
			tv_songTitle.append("\n" + mCursor.getLong(ID_INDEX));
			
			// ID to send to music player which song to play
			long id = mCursor.getLong(ID_INDEX);

			mPlayer = new MediaPlayer();
			mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		    mPlayer.setOnCompletionListener(this);

			start(id);
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
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void start(long id) {
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
		
		
		mPlayer.start();
		
		
		
	}

	/************ LISTENERS ******************/
	// onCompletion
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		Toast.makeText(this, "on completion", Toast.LENGTH_SHORT).show();
		mCursor.moveToPosition(new Random().nextInt(songsListSize));
		tv_songTitle.setText(mCursor.getString(ARTIST_INDEX));
		tv_songTitle.append("\n" + mCursor.getString(TITLE_INDEX));
		tv_songTitle.append("\n" + mCursor.getLong(ID_INDEX));
		
		// ID to send to music player which song to play
		long id = mCursor.getLong(ID_INDEX);
		start(id);
	}
	
	
	/************* button listeners *************/
	public void addPlayerControlListenerOnButton(){
		
		previousB =(ImageButton) findViewById(R.id.previousBtn);
		previousB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Toast.makeText(MainActivity.this, "Previous Button Clicked", Toast.LENGTH_SHORT).show();		
			}		
		});
		
		playB = (ImageButton) findViewById(R.id.playBtn);
		playB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg1) {
				Toast.makeText(MainActivity.this, "Play Button Clicked", Toast.LENGTH_SHORT).show();
				playB.setImageResource(R.drawable.pausebtn);
			}
			
		});
		
		forwardB =(ImageButton) findViewById(R.id.forwardBtn);
		forwardB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg2) {
				// TODO Auto-generated method stub
				Toast.makeText(MainActivity.this, "Forward Button Clicked", Toast.LENGTH_SHORT).show();	
			}
		});
	}
}