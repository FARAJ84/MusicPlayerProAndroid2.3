package com.taha.musicplayerpro;

public class Song {
    private long id;
    private String title;
    private String artist;
    private String album;
    private String path;
    private long duration;
    private long albumId;

    public Song(long id, String title, String artist, String album, String path, long duration, long albumId) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.path = path;
        this.duration = duration;
        this.albumId = albumId;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getPath() { return path; }
    public long getDuration() { return duration; }
    public long getAlbumId() { return albumId; }

    @Override
    public String toString() {
        return title + " - " + artist;
    }
}
