package com.example.milestone;

import java.util.Random;

import com.example.milestone.MpService;
import com.example.milestone.MpService.LocalBinder;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
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
	private final String MSG_ACTION_IMG_CHANGE = "msg img change";
	private final String MSG_ACTION = "Action";
	private final String MSG_PLAYER_READY = "msg player ready";
	private final String MSG_PLAYER_ISPLAYING = "msg player isplaying";
	
	// Gesture
	private GestureDetector gDetector;
	private static final int LARGE_MOVE=60;
	Vibrator vib;
	
	//Accelerometer Sensor
	private SensorManager sensorManager;
	private Sensor myAccelerometer;
	private long prevTime = 0;
	private long lastEventTime = 0;
	private long startShakeTime = 0;
	private long stopShakeTime = 0;
	private long shakeTime = 0;
	private final double TIME_DIFF = 250000000.0;
	private final double NEXT_SONG_SHAKE_THRESHOLD = 1000000000.0;
	private final double PAUSE_SONG_SHAKE_THRESHOLD = 1500000000.0;
	
	// Image Variables
	private long lastImageRefreshTime = 0;
	private String dataStream = "";
	private AnimationSet set = new AnimationSet(true);

	//SharedPreferences 
//	SharedPreferences myPreferenceManager;
	boolean gShaker = false;
	boolean gSwiper = false;
	boolean loadImages = false;
	
	// Service Variables
	Intent intent;
	MpService mService;
	boolean foundMusic = false;
	boolean mBound = false;
	boolean isPlaying = false;
	boolean autoPlayRequest = false;
	boolean playerReady = false;
	
	ImageButton playB, nextB, previousB;
	ImageView imgView;
	SeekBar seekBar;
	Boolean playBcheck = false;
	
	Handler mHandler;
	volatile boolean shutdown = false;
	TextView tv_songTitle, tv_songTime;
	int songsListSize;
	Bundle bundle;
	
	// Song info
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
		imgView = (ImageView) findViewById(R.id.imgView);
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
	    vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	    

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
				outState.putString("ImagePath", dataStream);
				// when activity only goes to stop then start
				
				if (bundle != null) {
					Log.i(TAG, bundle.toString());
					bundle.putLong("ID", songID);
					bundle.putInt("Position", trackPosn);
					bundle.putString("ImagePath", dataStream);
				}

				
			}
			catch (NullPointerException e) {
				e.printStackTrace();
			}
			
			
		}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		imgView.setAlpha(0);
		
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		// Check Default Shared Prefs
	    checkPrefShake();
	    checkPrefSwipe();
	    checkLoadImage();
		
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
			
			foundMusic = findMusic();
			if (foundMusic) {
				this.startService(intent);
				Log.i(TAG, "Start Service: " + intent.getAction());
			}
			else {
				tv_songTitle.setText("No songs in device");
				mBound = true;
				loadImages = false;
			}
			
			
			
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
		 
		 // Background Image Change
		if (loadImages) {
			new ImageTask().execute();
		}
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
		
		// Shutdown seekbar timer thread
		shutdown = true;
		
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
		
		
		
//		Intent prefsActivity = new Intent(this, PreferenceScreen.class);
//		startActivity(prefsActivity);
//		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		
		
		switch(item.getItemId()) {
			case R.id.action_settings:
				startActivity(new Intent(this, PreferenceScreen.class));
				return true;
			default:
				return super.onOptionsItemSelected(item);
				
		}
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
					//Log.i(TAG, "msg player ready 1");

				}
				else {
					playerReady = false;
					//Log.i(TAG, "msg player ready 0");

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
			
			if (action == MSG_ACTION_IMG_CHANGE) {
				if (loadImages) {
					new ImageTask().execute();
				}
			}
			
		}
	};
	
	// Handle music player buttons, seekbar, song info, etc here
	private void updateUI() {
		int timeInSeconds;
		
		// Update Song Info
		Log.i(TAG, "artist:" + artist + " title:" + title +" duration" + duration);
		tv_songTitle.setText(artist);
		tv_songTitle.append("\n" + title);
		
		// Time info
		String fillzero = "";
		Log.i(TAG, "debug parseLong duration val:" + duration);
		if (duration == "" || duration == "null" || duration == null) {
			duration = "0";
			Log.i(TAG, "parseLong(duration) == '' or 'null' or null");
		}
		try {
			Log.i(TAG, "parseLong(duration):" + Long.parseLong(duration));
			timeInSeconds = (int) (Long.parseLong(duration) / 1000); // duration is a string
		}
		catch (NumberFormatException e) {
			e.printStackTrace();
			timeInSeconds = 0;
			Log.i(TAG, "ParseLong Number Exception Called");
		}
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
	
	
	private class ImageTask extends AsyncTask<Void, Void, Bitmap> {

		@SuppressLint("InlinedApi")
		@Override
		protected Bitmap doInBackground(Void... params) {
			// TODO Auto-generated method stub
			
			String img_rq_columns[] = {
					
					MediaStore.Images.Media._ID,
					MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
					MediaStore.Images.Media.WIDTH,
					MediaStore.Images.Media.HEIGHT,
					MediaStore.Images.Media.ORIENTATION,
					MediaStore.Images.Media.DATA
			};
			
			int imgListSize;
			int w, h, scaled_w, scaled_h;
			
			long id;
			
			Display display = getWindowManager().getDefaultDisplay();
			int displayWidth = display.getWidth();
			int displayHeight = display.getHeight();
			
			ContentResolver contentResolver = getContentResolver();
			Cursor imgCursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
					img_rq_columns, null, null, null);
			
			
			if (imgCursor.moveToFirst()) {
				imgListSize = imgCursor.getCount();
				imgCursor.moveToPosition(new Random().nextInt(imgListSize));
				dataStream = imgCursor.getString(5);
				id = imgCursor.getLong(0);
				Log.i(TAG, "image id:" + id);
			}
			
			BitmapFactory.Options opt = new BitmapFactory.Options();
			opt.inSampleSize = 4;
			
			w = imgCursor.getInt(2);
			h = imgCursor.getInt(3);
			
			if (w == 0) {
				w = 1;
			}
			if (h == 0) { 
				h = 1;
			}
			
			Log.i(TAG, "ow:" + w + "oh:" + h);

			// landscape
			if (displayWidth > displayHeight) {
				scaled_w = displayWidth;
				scaled_h = (displayWidth * h) / w; 
			}
			// portrait
			else {
				scaled_h = displayHeight;
				scaled_w = (displayHeight * w) / h;
			}
			
			Log.i(TAG, " dw:" + displayWidth + " dh:" + displayHeight + " sw:" + scaled_w + " sh:" + scaled_h);
			
			// investigate later:
			// image id: 14668 caused crash of createScaledBitmap
			// 1920x2560, scaled to600x800, in portrait mode
			Bitmap bm = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(dataStream, opt), scaled_w, scaled_h, false);
			
			if (bm != null) {
				Log.i(TAG, "BM IS not NULL");
				//imgView.startAnimation(set);
				//imgView.setImageBitmap(bm);
				
				
				//AnimationSet as = new AnimationSet(true);
		
				ScaleAnimation scaleAnim = new ScaleAnimation(1.5f, 1.0f, 1.5f, 1.0f, 300.0f, 300.0f);
				//scaleAnim.setDuration(20000);
				scaleAnim.setInterpolator(new DecelerateInterpolator());
				//scaleAnim.start();
				
				TranslateAnimation tranAnim = new TranslateAnimation(0, -50, 0, 50);
				//tranAnim.setDuration(20000);
				tranAnim.setInterpolator(new DecelerateInterpolator());
				//tranAnim.setFillAfter(true);
				//tranAnim.start();
				
				
				
				
				
				
				//set.setInterpolator(new LinearInterpolator());
				set.setFillAfter(true);
				
				set.addAnimation(scaleAnim);
				//set.addAnimation(tranAnim);
				

				
				
				set.setDuration(25000);

				
				
		
		
			}
			else {
				Log.i(TAG, "BM IS NULL");
			}
			
			imgCursor.close();
			
			return bm;
		}
		
		@Override
		protected void onPostExecute(Bitmap bm) {
			// TODO Auto-generated method stub
			super.onPostExecute(bm);
			

			imgView.setImageBitmap(bm);
			imgView.setScaleType(ImageView.ScaleType.CENTER);
			imgView.setAlpha(100);
			imgView.startAnimation(set);
			
			
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
			mHandler.removeCallbacks(timerThread);
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
		
		// Cycle Through Background Images
//		if (Math.abs(msec - lastImageRefreshTime) > 2100) {
//			if (seconds == 30 || (seconds == 0 && minutes > 0)  || (seconds == 5 && minutes == 0)){
//				new ImageTask().execute();
//
//				lastImageRefreshTime = msec;
//				
//			}
//		}
//		
		//Log.i(TAG, "msec:" + msec + " lastTime=" + lastImageRefreshTime);

		
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
		if(gSwiper){
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
					//Toast.makeText(MainActivity.this, "Back", Toast.LENGTH_SHORT).show();
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
						//Toast.makeText(MainActivity.this, "Next Song", Toast.LENGTH_SHORT).show();
						return true;
					}
		}
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
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
		
		vib.vibrate(50);

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
		if (event.timestamp - prevTime > TIME_DIFF) {
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)  {
				getAccelerometer(event);
				prevTime = event.timestamp;
			}
			
		}
	}
	
	private void getAccelerometer(SensorEvent event) {
		if(gShaker){
		float[] values = event.values;
		
	    // Movement
	    float x = values[0];
	    float y = values[1];
	    float z = values[2];

	    // Get rid of negative values
	    float accellerationSquareRoot = (x * x + y * y + z * z)
	        / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);

	    
	    // Shake states: stop, undetermined, start
	    // 0---(stop shake range)--->1.5--(undetermined range)-->2.0---(start shake range)--->
	    
	    
	    // Listen for start shake
	    if (accellerationSquareRoot >= 2) {
	    	// Only record the start shake time if "stop shake listener" was triggered to reset timers
	    	if (startShakeTime == 0) {
	    		startShakeTime = event.timestamp;	
	    	}   	
	    }
    	
    	//Log.d(TAG, "accel: " + accellerationSquareRoot);

	    
	  	    
	    // Listen for stop shake
	    if (accellerationSquareRoot < 1.5) {
	    	stopShakeTime = event.timestamp;
	    	// Only record shakeTime if "start shake listener" was triggered to record timer
	    	if (startShakeTime > 0) {
	    		shakeTime = stopShakeTime - startShakeTime;
	    		Log.d(TAG, "shakeTime " + shakeTime);
	    	}

	    	
	    	
	    	// START/PAUSE
	    	if (shakeTime > PAUSE_SONG_SHAKE_THRESHOLD) {
	    		Log.d(TAG, "long shake");
				if (mService != null) {
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
	    	}
	    	// NEXT SONG
	    	else if (shakeTime > NEXT_SONG_SHAKE_THRESHOLD) {
	    		Log.d(TAG, "short shake");
		    	if (mService != null) {
					boolean isPlaying = mService.mp.isPlaying();
					mService.playNext();
					if (isPlaying) {
						mService.startMusic();
					}
				}
	    	}
	    	
	    	// Reset timers
	    	startShakeTime = 0;
	    	shakeTime = 0;
	    }
		}
	}
	
	private void checkPrefSwipe() {
		SharedPreferences myPreferenceManager = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		boolean gSwipe = myPreferenceManager.getBoolean("gesture_update", true);
			if(gSwipe){
				gSwiper = true;
				//Toast.makeText(MainActivity.this, "Swiper ON " + gSwipe, Toast.LENGTH_SHORT).show();
			}else{
				gSwiper = false;
				//Toast.makeText(MainActivity.this, "Swiper OFF " + gSwipe, Toast.LENGTH_SHORT).show();
			}
		
	}
	
	private void checkPrefShake(){
		SharedPreferences myPreferenceManager = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		boolean gShake = myPreferenceManager.getBoolean("shaker_update", true);
		if(gShake){
			gShaker = true;
			//Toast.makeText(MainActivity.this, "Shaker ON " + gShake, Toast.LENGTH_SHORT).show();
		}else{
			gShaker = false;
			//Toast.makeText(MainActivity.this, "Shake OFF " + gShake, Toast.LENGTH_SHORT).show();
		}
	}
	
	private void checkLoadImage(){
		SharedPreferences myPreferenceManager = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		boolean update = myPreferenceManager.getBoolean("backgroundimages_update", true);
		if(update){
			loadImages = true;
			
		}else{
			loadImages = false;
			
		}
	}
	
	private boolean findMusic() {
		ContentResolver contentResolver = getContentResolver();
		Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

		Cursor mCursor = contentResolver.query(uri, rq_columns, null, null, null);
	    if (mCursor != null) {
			songsListSize = mCursor.getCount();
			Log.i(TAG, "songsListSize:" + songsListSize);
			if (songsListSize == 0) {
				return false;
			}
			else {
				return true;
			}
	    }
	    else {
		    Log.i(TAG, "Could not find music on device");
	    	return false;
	    }
	}
}