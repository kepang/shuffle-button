package com.example.milestone;

import com.example.milestone.MpService;
import com.example.milestone.MpService.LocalBinder;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;



public class MainActivity extends Activity implements OnGestureListener, SensorEventListener {

	
	private final int ID_INDEX = 0;
	private final int ARTIST_INDEX = 1;
	private final int TITLE_INDEX = 2;
	private final int DURATION_INDEX = 3;
	private final int ALBUM_INDEX = 4;
	private final String TAG = "Debug Activity";
	private final int TIMER = 100;
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
	
	// Gesture
	private GestureDetector gDetector;
	private static final int LARGE_MOVE=60;
	
	//Accelerometer Sensor
	private SensorManager sensorManager;
	private Sensor myAccelerometer;
	
	// Service Variables
	Intent intent;
	MpService mService;
	boolean mBound = false;
	boolean isPlaying = false;
	boolean autoPlayRequest = false;
	boolean playerReady = false;
	
	ImageButton playB, nextB, previousB;
	SeekBar seekBar;
	Boolean playBcheck = false;
	
	Handler mHandler;
	TextView tv_songTitle, tv_songTime;
	int songsListSize;
	Bundle bundle;
	
	String artist = "";
	String title = "";
	String duration = "";
	String id = "";
	
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
		
		// init UI
		tv_songTitle = (TextView) findViewById(R.id.tv_songTitle);
		tv_songTime = (TextView) findViewById(R.id.tv_songTime);
		mHandler = new Handler();
	    addMusicControlListenerOnButton();

	    seekBar.setProgress(0);
	    seekBar.setMax(100);
	    
	    // Save song info and progress for orientation change
	    if (savedInstanceState != null) {
	    	bundle = new Bundle(savedInstanceState);
	    	Log.i(TAG, "savedInstance trackposn:" + savedInstanceState.getInt("Position"));
	    }
	    else {
	    	bundle = null;
	    }
	    
	    gDetector = new GestureDetector(this);
	    sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);	
	    myAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	            
	}
	
	@Override
		protected void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
			try {
				
				long songID = mService.mCursor.getLong(ID_INDEX);
				int trackPosn = mService.mp.getCurrentPosition();
				
				Log.i(TAG, "outstate songID: " + songID);
				Log.i(TAG, "mCursor.getPosition(): " + mService.mCursor.getPosition());
				Log.i(TAG, "save trackPosn: " + trackPosn);
				
				outState.putLong("ID", songID);
				outState.putInt("Position", trackPosn);
				// when activity only goes to stop then start
				Log.i(TAG, bundle.toString());
				bundle.putLong("ID", songID);
				bundle.putInt("Position", trackPosn);

				
			}
			catch (NullPointerException e) {
				e.printStackTrace();
			}
			
			
		}
	
	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		// Register Broadcast Receiver
		IntentFilter iff = new IntentFilter(BROADCAST_STR);
		LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, iff);

		
		// Start Music Player Service if it's not running
		if (!isMpServiceRunning()) {	
			intent = null;
			
			// Fresh start of the app
			if (bundle == null) {
				intent = new Intent(ACTION_PLAY, null, this, MpService.class);
				bundle = new Bundle();
				autoPlayRequest = true;
			}
			// Orientation Change
			else {
				intent = new Intent(ACTION_RESUME, null, this, MpService.class);
				intent.putExtra("ID", bundle.getLong("ID"));
				intent.putExtra("Position", bundle.getInt("Position"));
				intent.putExtra("Cursor", bundle.getInt("Cursor"));
				autoPlayRequest = false;
				Log.i(TAG, "bundle: trackPosn: " + bundle.getInt("Position"));
			}
			this.startService(intent);
			
			Log.i(TAG, "Start Service: " + intent.getAction());
		}
		else {
			Log.i(TAG, "Service is already running");
		}
		
		// Bind activity to Music Player Service
		if (!mBound) {
			intent = null;
			intent = new Intent(this, MpService.class);
			Log.i(TAG, "Binding Service");
			
			// Takes time to bind to service.
			// Go to mConnection > onServiceConnection
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		}
		 //register a listener for accelerometer sensors
		 sensorManager.registerListener(this, myAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		// Unbind from music player service
		if (mBound) {
			Log.i(TAG, "mBound = true // from Activity-onPause");
			unbindService(mConnection);
			mBound = false;
			Log.i(TAG, "Service Unbound (from Activity-onPause)");
		}
		
		// Unbind Broadcast Receiver
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
		
		//unregister sensorManager
		sensorManager.unregisterListener(this);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		// Stop Music Player Service if song is not playing
		
		if (!mService.mp.isPlaying()) {
			Log.i(TAG, "Call stopService()");

			mService.stopService();
		}
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy");
		bundle = null;
		
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
	
	// Broadcast Receiver Callback
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "broadcast msg received from activity");
			String action = intent.getStringExtra(MSG_ACTION);
			
			// Song info message
			if (action == MSG_SONGINFO) {
				artist = intent.getStringExtra(MSG_SONGARTIST);
				title = intent.getStringExtra(MSG_SONGTITLE);
				duration = intent.getStringExtra(MSG_SONGDURATION);
				id = intent.getStringExtra(MSG_SONGID);
				
				if (intent.getBooleanExtra(MSG_PLAYER_ISPLAYING, false)) {
					isPlaying = true;
				}
				else {
					isPlaying = false;
				}
				
				updateUI();
				Log.i(TAG, "msg song info");

			}
			
			// Player ready message (not used)
			if (action == MSG_PLAYER_READY) {
				if (intent.getStringExtra(MSG_PLAYER_READY) == "1") {
					playerReady = true;
					Log.i(TAG, "msg player ready 1");

				}
				else {
					playerReady = false;
					Log.i(TAG, "msg player ready 0");

				}
			}
			
			// Play request from Service
			if (action == MSG_ACTION_PLAY) {
				// Have to wait until service binding is finished
				//autoPlayRequest = true;
				//autoPlay();
				mService.startMusic();
				Log.i(TAG, "msg action play");
			}
			
		}
	};
	
	// Handle music player buttons, seekbar, song info, etc here
	private void updateUI() {
		// Update Song Info
		Log.i(TAG, "artist:" + artist + " title:" + title +" duration" + duration);
		tv_songTitle.setText(artist);
		tv_songTitle.append("\n" + title);
		
		// Time info
		String fillzero = "";
		Log.i(TAG, "debug parseLong duration val:" + duration);
		if (duration == "") {
			duration = "0";
		}
		Log.i(TAG, "parseLong(duration):" + Long.parseLong(duration));
		int timeInSeconds = (int) (Long.parseLong(duration) / 1000); // duration is a string
		int minutes = timeInSeconds / 60;
		int seconds = timeInSeconds % 60;
		if (seconds < 10) {
			fillzero = "0";
		}
		Log.i(TAG, "time:" + minutes + seconds);
		tv_songTitle.append("\n" + minutes + ":" + fillzero + seconds);
		
		// Update Play button
		if (mService != null) {
			if (mService.mp != null) {
				isPlaying = mService.mp.isPlaying();
				Log.i(TAG, "mp:" + mService.mp.toString());

			}
		}

		if (isPlaying){
			playB.setImageResource(R.drawable.pausebtn);
			
		}
		else {
			playB.setImageResource(R.drawable.playbtn);
		}
		
	}
	
	
	// Check if MpService is running
	private boolean isMpServiceRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (MpService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	// Service connection callback
	private ServiceConnection mConnection = new ServiceConnection() {
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mBound = true;
			Log.i(TAG, "Service Binding Connected");
			
			updateSeekBar();
			mService.requestSongInfoMsg();
			if (autoPlayRequest) {
				Log.i(TAG, "Autoplay Requested");
				autoPlay();
			}
			
						
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			Log.i(TAG, "Service Binding Disconnected");
			mBound = false;
		}

	};
	
	// SeekBar and song timer update thread
	public Runnable timerThread = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			long duration;
			long position;

			// Make sure main thread has not released the mPlayer
			if (mService.mp != null) {
				duration = mService.mp.getDuration();
				position = mService.mp.getCurrentPosition();
				int progress = (int) (100*position/duration);	
				
				// update seekbar position
				seekBar.setProgress(progress);
				// update timer
				tv_songTime.setText(msecToTime((int)position));
			}
				
			mHandler.postDelayed(timerThread, TIMER);
		}
		
	};
	
	private void autoPlay() {
		playB.setImageResource(R.drawable.pausebtn);
		mService.startMusic();
		isPlaying = true;
		autoPlayRequest = false;
		Log.i(TAG, "autoplay");
	    //updateSeekBar();
	}
	
	private String msecToTime(int msec) {
		String fillzero = "";
		
		int timeInSeconds = msec/ 1000;
		int minutes = timeInSeconds / 60;
		int seconds = timeInSeconds % 60;
		if (seconds < 10) {
			fillzero = "0";
		}
		
		return minutes + ":" + fillzero + seconds;
	}

	/************ LISTENERS ******************/	
	/************* BUTTONS *************/
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
				long duration = mService.mp.getDuration();
				int time = (int) (progress * duration) / 100;

				mService.mp.seekTo(time); // seekto in msec 
				
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
				mService.playPrev();
				updateSeekBar();
			}		
		});
		
		//Play or pause song
		playB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if(!isPlaying){
					playB.setImageResource(R.drawable.pausebtn);
					playBcheck = true;
					isPlaying = true;
					mService.startMusic();
				}else{
					playB.setImageResource(R.drawable.playbtn);
					playBcheck = false;
					isPlaying = false;
					mService.pauseMusic();
				}	
			}
			
		});
		
		//Next button event listener
		
		nextB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (mService != null) {
					boolean isPlaying = mService.mp.isPlaying();
					mService.playNext();
					if (isPlaying) {
						mService.startMusic();
					}
				}
								
			}
		});
		
	}

	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velX, float velY) {
		// TODO Auto-generated method stub
//		if((e1.getY()-e2.getY()) > LARGE_MOVE){
//			//up
//			//Toast.makeText(MainActivity.this, "UP", Toast.LENGTH_SHORT).show();
//			return true;
//		}
//		else
//			if((e2.getY()-e1.getY()) > LARGE_MOVE){
//				//down
//				//Toast.makeText(MainActivity.this, "Down", Toast.LENGTH_SHORT).show();
//				return true;
//			}
//			else
				if((e1.getX()-e2.getX()) > LARGE_MOVE){
					//left
					mService.playPrev();
					updateSeekBar();
					Toast.makeText(MainActivity.this, "Previous", Toast.LENGTH_SHORT).show();
					return true;	
				}
				else
					if((e2.getX()-e1.getX()) > LARGE_MOVE){
						//right
						if (mService != null) {
							boolean isPlaying = mService.mp.isPlaying();
							mService.playNext();
							if (isPlaying) {
								mService.startMusic();
							}
						}
						Toast.makeText(MainActivity.this, "Next Song", Toast.LENGTH_SHORT).show();
						return true;
					}
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
	return gDetector.onTouchEvent(event);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
		      getAccelerometer(event);
		    }
		
	}
	
	private void getAccelerometer(SensorEvent event) {
	    float timer = event.timestamp;
		float[] values = event.values;
	    // Movement
	    float x = values[0];
	    float y = values[1];
	    float z = values[2];

	    float accelationSquareRoot = (x * x + y * y + z * z)
	        / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
	    if (accelationSquareRoot >= 2) {
	    	if (mService != null) {
				boolean isPlaying = mService.mp.isPlaying();
				mService.playNext();
				if (isPlaying) {
					mService.startMusic();
				}
			}	
	    }
	}
}