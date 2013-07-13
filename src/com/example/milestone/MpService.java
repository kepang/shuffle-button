package com.example.milestone;


import java.io.IOException;
import java.util.Random;

import com.example.milestone.MainActivity;
import com.example.milestone.R;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class MpService extends Service implements OnPreparedListener, OnErrorListener, OnCompletionListener{

	private final IBinder mBinder = new LocalBinder();
    
	private final int ID_INDEX = 0;
	private final int ARTIST_INDEX = 1;
	private final int TITLE_INDEX = 2;
	private final int DURATION_INDEX = 3;
	private final int ALBUM_INDEX = 4;
	private final String TAG = "Debug Svc";
	private final int TIMER = 1000;
	private final String ACTION_PLAY = "com.example.milestone.PLAY";
	private final String ACTION_RESUME = "com.example.milestone.RESUME";
	private final String BROADCAST_STR = "MP Actions";
	private final String MSG_SONGTITLE = "msg song title";
	private final String MSG_SONGARTIST = "msg song artist";
	private final String MSG_SONGDURATION = "msg song duration";
	private final String MSG_SONGINFO = "msg song info";
	private final String MSG_SONGID = "msg song id";
	private final String MSG_ACTION_PLAY = "msg action play";
	private final String MSG_ACTION = "Action";
	private final String MSG_PLAYER_READY = "msg player ready";
	private final String MSG_PLAYER_ISPLAYING = "msg player isplaying";
	
	
    MediaPlayer mp = null;
	Cursor mCursor = null;
	int songsListSize;
	int seekBarProgress;
	
	PendingIntent pi;
	Notification notification;
	
	String songTitle = "";
	String songArtist = "";
	String songDuration = "";
	String songID = "";
	String rq_columns[] = {
			
			MediaStore.Audio.Media._ID,
			MediaStore.Audio.Media.ARTIST,
			MediaStore.Audio.Media.TITLE,
			MediaStore.Audio.Media.DURATION,
			MediaStore.Audio.Media.ALBUM
			
			
	};
	
	
	
	
	public MpService() {
		Log.i(TAG, "mpSvc constructor");

	}

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        MpService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MpService.this;
        }
    }

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		Log.i(TAG, "Service Created");
		// assign the song name to songName
		pi = PendingIntent.getActivity(getApplicationContext(), 0,
		                new Intent(getApplicationContext(), MainActivity.class),
		                PendingIntent.FLAG_UPDATE_CURRENT);
		
		notification = new Notification();
		notification.tickerText = "Woooooooooooo!";
		notification.icon = R.drawable.ic_launcher;
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.setLatestEventInfo(getApplicationContext(), "Music Player", "Running", pi);
		
		//startForeground(NOTIFICATION_ID, notification);
		//startForeground(4711, notification);

	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		
		// In case system calls service with null intent
		if (intent == null) {
    		Log.i(TAG, "Start Service with NULL Intent");
    		return super.onStartCommand(intent, flags, startId);
		}
		
		else {
		
			Log.i(TAG, "Svc Start from Service:" + intent.getAction());
		   
			// init mp
	     	mp = new MediaPlayer();
	     	mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
			mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
		    mp.setOnCompletionListener(this);
	        mp.setOnPreparedListener(this);
	        
	        // Init Cursor from MediaStore Content Provider
	        findMusic();
	        
	        // Fresh load of app
	        if (intent.getAction() == ACTION_PLAY) {
	        	selectSong(moveCursorToNextSong());
	        }
	        
	        // Orientation change while music is not playing
	        // Restore track selection and song position
	        if (intent.getAction() == ACTION_RESUME) {
	        	long id = intent.getLongExtra("ID", moveCursorToNextSong());
	        	int cPosn = 0;
	        	
	        	// Sync mCursor with the right song from saved bundle
	        	mCursor.moveToFirst();
	        	while (!mCursor.isAfterLast()) {
	        		if (mCursor.getLong(ID_INDEX) == id) {
	        			cPosn = mCursor.getPosition();
	        		}
	        		mCursor.moveToNext();
	        	}
	        	mCursor.moveToPosition(cPosn);
	        	

	        	selectSong(id);
	        	mp.seekTo(intent.getIntExtra("Position", 0));
	        	
	        	Log.i(TAG, "selectSong ID:" + id);
	        	Log.i(TAG, "cursor posn:" + cPosn + "id:" + mCursor.getLong(ID_INDEX));
	        }
	        
	        
	        return Service.START_NOT_STICKY;
		}
		
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		//throw new UnsupportedOperationException("Not yet implemented");
		Log.i(TAG, "Service onBind() from Service");
		sendMessage(MSG_SONGINFO, new String[] {songTitle, songArtist, songDuration, songID});
		return mBinder;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		stopService();
		Log.i(TAG, "Svc destroyed");

	}
	

    /*******
     * Media Player Listeners
     */
	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		selectSong(moveCursorToNextSong());
		startMusic();
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
		
		// Player is prepared
		sendMessage(MSG_PLAYER_READY, new String[] {"1"});
		
		
	}
	
	/**********
	 * Public Methods
	 */
	public Cursor findMusic() {
		ContentResolver contentResolver = getContentResolver();
		Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

		mCursor = contentResolver.query(uri, rq_columns, null, null, null);
	    songsListSize = mCursor.getCount();
	    return mCursor;
	}
	
	public long moveCursorToNextSong() {
		// Get next song
		mCursor.moveToPosition(new Random().nextInt(songsListSize));
		// ID to send to music player which song to play
		long id = mCursor.getLong(ID_INDEX);
		
		return id;
	}
	
	// Play Song. INPUT: system song id
	public void selectSong(long id) {

		
		Uri myUri = ContentUris.withAppendedId(
				android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
		
		try {
			mp.reset();
			mp.setDataSource(getApplicationContext(), myUri);
			mp.prepare();
			//sendMessage(MSG_PLAYER_READY, new String[] {"0"});
			//mp.prepareAsync();

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
		
		// Send Local BCast msg to UI Thread
		songTitle = mCursor.getString(ARTIST_INDEX);
		songArtist = mCursor.getString(TITLE_INDEX);
		songDuration = mCursor.getString(DURATION_INDEX);
		songID = mCursor.getString(ID_INDEX);
		
		String[] msg = {songArtist, songTitle, songDuration, songID};
		
		sendMessage(MSG_SONGINFO, msg);
		
	}
	
	// Cleanup is here
	public void stopService() {
		
		if (mCursor != null) {
			mCursor.close();
		}
		if (mp != null) {
			mp.stop();
			mp.release();
			mp = null;	
		}
		
		stopForeground(true);
		stopSelf();
		Log.i(TAG, "Svc Stopped");
	}

	
	public void startMusic() {
		startForeground(4711, notification);
		mp.start();
	}
	
	public void pauseMusic() {
		mp.pause();
		stopForeground(true);
	}
	
	public void playNext() {
		selectSong(moveCursorToNextSong());
	}
	
	public void playPrev() {
		mp.seekTo(0);
	}
	
	public void requestSongInfoMsg() {
		sendMessage(MSG_SONGINFO, new String[] {songArtist, songTitle, songDuration, songID});
	}

	/*******
	 * Private Methods
	 */
	
	private void sendMessage(String type, String[] msg) {
		Intent intent = new Intent(BROADCAST_STR);

		if (type == MSG_SONGINFO) {
			intent.putExtra(MSG_ACTION, MSG_SONGINFO);
			intent.putExtra(MSG_SONGARTIST, msg[0]);
			intent.putExtra(MSG_SONGTITLE, msg[1]);
			intent.putExtra(MSG_SONGDURATION, msg[2]);
			intent.putExtra(MSG_SONGID, msg[3]);
			intent.putExtra(MSG_PLAYER_ISPLAYING, mp.isPlaying());
		}
		
		if (type == MSG_PLAYER_READY) {
			intent.putExtra(MSG_ACTION, MSG_PLAYER_READY);
			intent.putExtra(MSG_PLAYER_READY, msg[0]);
		}
	
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		Log.i(TAG, "Sent msg info from service");
	}
	
	private void sendAction(String action) {
		Intent intent = new Intent(BROADCAST_STR);
		intent.putExtra(MSG_ACTION, MSG_ACTION_PLAY);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		Log.i(TAG, "Sent action info from service");

	}
	
	
	
}
