package com.example.milestone;

import java.util.Random;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.widget.TextView;

@SuppressLint("NewApi")
public class MainActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {

	TextView tv_songTitle;
	String rq_columns[] = {
			MediaStore.Audio.Media.TITLE,
			MediaStore.Audio.Media.DURATION
	};
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		tv_songTitle = (TextView) findViewById(R.id.tv_songTitle);
		

		
		//@SuppressWarnings("deprecation")
		/*
		Cursor c = managedQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, rq_columns, null, null, null);
		int name = c.getColumnIndex(MediaStore.Audio.Media.TITLE);

		c.moveToFirst();
		tv_songTitle.setText("Title: " + c.getString(name));
		c.moveToNext();
		tv_songTitle.append("\n" + c.getString(name));
		c.moveToNext();
		tv_songTitle.append("\n" + c.getString(name));
		c.moveToNext();
		tv_songTitle.append("\n" + c.getString(name));
		c.moveToNext();
		tv_songTitle.append("\n" + c.getString(name));
		*/
		getLoaderManager().initLoader(0, null, this);
		
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	
	

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		// TODO Auto-generated method stub
		return new CursorLoader(getApplication(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, rq_columns, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		// TODO Auto-generated method stub
		
		
		cursor.moveToFirst();
		String test = cursor.getString(1);
		//tv_songTitle.append("\n:\n" + cursor.getString(0));
		/*
		cursor.moveToNext();
		tv_songTitle.append("\n:\n" + cursor.getString(0));
		Random r = new Random();
		int rand = r.nextInt(cursor.getCount());
		cursor.moveToPosition(rand);
		tv_songTitle.append("\n" + cursor.getString(0));
		*/
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub
		
	}

}