package com.taha.musicplayerpro;

public class Song {
    private long id;
    private String title;
    private String artist;
    private String path;
    private long duration;

    public Song(long id, String title, String artist, String path, long duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.path = path;
        this.duration = duration;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getPath() { return path; }
    public long getDuration() { return duration; }

    @Override
    public String toString() {
        return title + " - " + artist;
    }
}
