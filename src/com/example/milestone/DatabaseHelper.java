package com.example.milestone;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper{

    private static final String DATABASE_NAME = "PlaylistMgr";
    private static final int DATABASE_VER = 1;
 
    // Contacts table name
    private static final String TABLE_SONGS = "songs";
 
    // Contacts Table Columns names
    private static final String ID = "ID";
    private static final String systemID = "systemID";
    private static final String artist = "artist";
    private static final String title = "title";
    private static final String duration = "duration";
    private static final String upvotes = "upvotes";
    private static final String downvotes = "downvotes";
    
	private final String TAG = "Debug DBHelper";

	
	public DatabaseHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
		// TODO Auto-generated constructor stub
	}
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VER);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		
		String CREATE_SONGS_TABLE = "CREATE TABLE " + TABLE_SONGS + "("
				+ ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " 
				+ systemID + " INTEGER, " 
				+ artist + " TEXT, "
				+ title + " TEXT, " 
				+ duration + " INTEGER, " 
				+ upvotes + " INTEGER, "
				+ downvotes + " INTEGER"
				+ ")";
		
		db.execSQL(CREATE_SONGS_TABLE);
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONGS);
 
        // Create tables again
        onCreate(db);		
	}
	
	
	 // Adding new song
    public long addSong(Song song) {
        long rowID;
    	
    	SQLiteDatabase db = this.getWritableDatabase();
 
        ContentValues values = new ContentValues();
        values.put(systemID, song.getSystemID());
        values.put(artist, song.getArtist());
        values.put(title, song.getTitle());
        values.put(duration, song.getDuration());
        values.put(upvotes, song.getUpVotes());
        values.put(downvotes, song.getDownVotes());
 
        // Inserting Row
        rowID = db.insert(TABLE_SONGS, null, values);
        db.close(); // Closing database connection
        
        return rowID;
    }
 
    // Getting single song
    public Song getSong(long sysid) {
        SQLiteDatabase db = this.getReadableDatabase();
        Song song = null;
        
        Cursor cursor = db.query(TABLE_SONGS, new String[] { ID,
                systemID, artist, title, duration, upvotes, downvotes }, systemID + "=?",
                new String[] { String.valueOf(sysid) }, null, null, null, null);
        Log.i(TAG, "sid:" + sysid);
        
        if (cursor != null) {
            Log.i(TAG, "cursor not null with count: " + cursor.getCount());
            int count = cursor.getCount();
            
            if (count > 0) {
	            cursor.moveToFirst();
	            
	            song = new Song(cursor.getInt(0), cursor.getLong(1), cursor.getString(2),
	                    cursor.getString(3), cursor.getLong(4), cursor.getInt(5), 
	                    cursor.getInt(6));
            }
        }
        
        // returns null if no record is returned
        return song;
    }
    
    // Updating single song
    public int updateSong(Song song) {
        SQLiteDatabase db = this.getWritableDatabase();
 
        ContentValues values = new ContentValues();
        values.put(upvotes, song.getUpVotes());
        values.put(downvotes, song.getDownVotes());
 
        // updating row
        return db.update(TABLE_SONGS, values, systemID + " = ?",
                new String[] { String.valueOf(song.getSystemID()) });
    }
    
    public boolean addUpVote(long sysid) {
    	Song s = getSong(sysid);
    	int n;
    	
    	if (s == null) {
    		return false;
    	}
    	
    	s.addUpVote();
    
    	n = updateSong(s);
    	
    	Log.i(TAG, "add UPVOTE to db:" + n);
    	
    	return true;
    }
    
    public boolean addDownVote(long sysid) {
    	Song s = getSong(sysid);
    	int n;
    	if (s == null) {
    		return false;
    	}
    	
    	s.addDownVote();
    	n = updateSong(s);
    	
    	Log.i(TAG, "add DOWNVOTE to db:" + n);
    	
    	return true;
    }
    
 // Getting songs count
    public int getSongsCount() {
        String countQuery = "SELECT  * FROM " + TABLE_SONGS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();
 
        // return count
        return cursor.getCount();
    }

}
