package com.taha.musicplayerpro;

public class Playlist {
    private long id;
    private String name;

    public Playlist(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return name;
    }
}
