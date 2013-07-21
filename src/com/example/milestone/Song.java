package com.example.milestone;

public class Song {

	private int id;
	private long system_id;
	private String artist;
	private String title;
	private long duration;
	private int upvotes;
	private int downvotes;
	
	public Song (long system_id, String artist, String title, long duration) {
		this.system_id = system_id;
		this.artist = artist;
		this.title = title;
		this.duration = duration;
	}
}
