package com.taha.musicplayerpro;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

import java.io.IOException;
import java.util.List;

public class PlaybackService extends Service implements MediaPlayer.OnCompletionListener {

    private final IBinder binder = new PlaybackBinder();

    private MediaPlayer player;
    private List<Song> songs;
    private int currentIndex = -1;

    public class PlaybackBinder extends Binder {
        PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnCompletionListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setSongs(List<Song> songList) {
        this.songs = songList;
    }

    public void playAt(int index) {
        if (songs == null || index < 0 || index >= songs.size()) {
            return;
        }
        currentIndex = index;
        Song song = songs.get(index);

        try {
            player.reset();
            player.setDataSource(song.getPath());
            player.prepare();
            player.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void togglePlayPause() {
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.start();
        }
    }

    public void next() {
        if (songs == null || songs.isEmpty()) {
            return;
        }
        int nextIndex = (currentIndex + 1) % songs.size();
        playAt(nextIndex);
    }

    public void previous() {
        if (songs == null || songs.isEmpty()) {
            return;
        }
        int prevIndex = (currentIndex - 1 + songs.size()) % songs.size();
        playAt(prevIndex);
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public int getCurrentPosition() {
        return player.getCurrentPosition();
    }

    public int getDuration() {
        return player.getDuration();
    }

    public void seekTo(int ms) {
        player.seekTo(ms);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        next();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
