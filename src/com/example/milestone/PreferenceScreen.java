package com.example.milestone;


import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class PreferenceScreen extends PreferenceActivity implements OnPreferenceClickListener{
	Preference exit;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		
		//exit =(Preference)findPreference("Button");
		//exit.setOnPreferenceClickListener(this);
	}


	@Override
	public boolean onPreferenceClick(Preference preference) {
		Intent i = new Intent(this, MainActivity.class);
		startActivity(i);
		return true;
	}
}
