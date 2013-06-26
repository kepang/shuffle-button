package com.example.milestone;

import java.util.Random;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity {

	private final int ID_INDEX = 0;
	private final int ARTIST_INDEX = 1;
	private final int TITLE_INDEX = 2;
	private final int DURATION_INDEX = 3;
	private final int ALBUM_INDEX = 4;
	
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

	
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if (mCursor == null) {
		    // query failed, handle error.
		} 
		else if (!mCursor.moveToFirst()) {
		    // no media on the device
		}
		else {
		    //int titleColumn = mCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
		    //int idColumn = mCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
		    
			mCursor.moveToPosition(new Random().nextInt(songsListSize + 1));
			tv_songTitle.setText(mCursor.getString(ARTIST_INDEX));
			tv_songTitle.append("\n" + mCursor.getString(TITLE_INDEX));
			tv_songTitle.append("\n" + mCursor.getLong(ID_INDEX));
			start();
		}	

		
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
	

	private void start() {
		
		
		
		
	}

}