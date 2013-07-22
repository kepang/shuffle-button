package com.example.milestone;

public class Song {

	private int ID;
	private long systemID;
	private String artist;
	private String title;
	private long duration;
	private int upvotes;
	private int downvotes;
	
	public Song (int ID, long sysid, String artist, String title, long duration, int upvotes, int downvotes) {
		this.ID = ID;
		this.systemID = sysid;
		this.artist = artist;
		this.title = title;
		this.duration = duration;
		this.upvotes = upvotes;
		this.downvotes = downvotes;
	}
	public int getID() {
		return ID;
	}
	
	public long getSystemID() {
		return systemID;
	}
	
	public String getArtist() {
		return artist;
	}
	
	public String getTitle() {
		return title;
	}
	
	public long getDuration() {
		return duration;
	}
	
	public int getUpVotes() {
		return upvotes;
	}
	
	public void addUpVote() {
		upvotes++;
	}
	
	public int getDownVotes() {
		return downvotes;
	}
	
	public void addDownVote() {
		downvotes++;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
