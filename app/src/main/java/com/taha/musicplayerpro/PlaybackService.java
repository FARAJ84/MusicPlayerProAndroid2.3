package com.taha.musicplayerpro;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.widget.RemoteViews;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PlaybackService extends Service implements MediaPlayer.OnCompletionListener {

    public static final String ACTION_PLAY_PAUSE = "com.taha.musicplayerpro.PLAY_PAUSE";
    public static final String ACTION_NEXT = "com.taha.musicplayerpro.NEXT";
    public static final String ACTION_PREV = "com.taha.musicplayerpro.PREV";
    public static final String ACTION_STOP = "com.taha.musicplayerpro.STOP";

    private static final String PREFS_NAME = "playback_state_prefs";
    private static final String KEY_LAST_SONG_ID = "last_song_id";
    private static final String KEY_LAST_POSITION = "last_position";

    private static final int NOTIFICATION_ID = 1;

    public static final int REPEAT_OFF = 0;
    public static final int REPEAT_ALL = 1;
    public static final int REPEAT_ONE = 2;

    private final IBinder binder = new PlaybackBinder();

    private MediaPlayer player;
    // PARTIAL_WAKE_LOCK keeps the CPU running (screen can stay off) so playback
    // continues past a lock-screen without the process being killed.
    private PowerManager.WakeLock wakeLock;

    private List<Song> songs;
    private List<Integer> shuffleOrder;
    private int currentIndex = -1;
    // Tracks position directly within shuffleOrder so next()/previous() never
    // need an O(n) indexOf() scan - important for large libraries on a
    // single-core 832MHz CPU.
    private int currentShufflePosition = -1;

    private boolean shuffleEnabled = false;
    private int repeatMode = REPEAT_OFF;

    private PlaybackListener listener;

    public interface PlaybackListener {
        void onTrackChanged(int index);
        void onPlaybackStateChanged(boolean isPlaying);
    }

    public class PlaybackBinder extends Binder {
        PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    private final BroadcastReceiver controlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_PLAY_PAUSE.equals(action)) {
                togglePlayPause();
            } else if (ACTION_NEXT.equals(action)) {
                next();
            } else if (ACTION_PREV.equals(action)) {
                previous();
            } else if (ACTION_STOP.equals(action)) {
                stopSelf();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnCompletionListener(this);
        // MediaPlayer already manages its own wake lock internally via
        // setWakeMode, which is the lighter-weight, API-10-safe approach.
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PLAY_PAUSE);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PREV);
        filter.addAction(ACTION_STOP);
        registerReceiver(controlReceiver, filter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setListener(PlaybackListener listener) {
        this.listener = listener;
    }

    public void setSongs(List<Song> songList) {
        this.songs = songList;
        rebuildShuffleOrder();
    }

    private void rebuildShuffleOrder() {
        shuffleOrder = new ArrayList<Integer>();
        if (songs == null) {
            return;
        }
        for (int i = 0; i < songs.size(); i++) {
            shuffleOrder.add(i);
        }
        Collections.shuffle(shuffleOrder, new Random());
        syncShufflePosition();
    }

    public void setShuffleEnabled(boolean enabled) {
        this.shuffleEnabled = enabled;
        if (enabled) {
            rebuildShuffleOrder();
        }
    }

    public boolean isShuffleEnabled() {
        return shuffleEnabled;
    }

    public void cycleRepeatMode() {
        repeatMode = (repeatMode + 1) % 3;
    }

    public int getRepeatMode() {
        return repeatMode;
    }

    public void playAt(int index) {
        if (songs == null || index < 0 || index >= songs.size()) {
            return;
        }
        currentIndex = index;
        syncShufflePosition();
        Song song = songs.get(index);

        try {
            player.reset();
            player.setDataSource(song.getPath());
            player.prepare();
            player.start();
            savePlaybackState();
            showNotification();
            if (listener != null) {
                listener.onTrackChanged(index);
                listener.onPlaybackStateChanged(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Resume the last played song at its saved position without auto-starting playback. */
    public void restoreLastSession(long lastSongId, int lastPosition) {
        if (songs == null) {
            return;
        }
        for (int i = 0; i < songs.size(); i++) {
            if (songs.get(i).getId() == lastSongId) {
                currentIndex = i;
                syncShufflePosition();
                try {
                    player.reset();
                    player.setDataSource(songs.get(i).getPath());
                    player.prepare();
                    player.seekTo(lastPosition);
                    if (listener != null) {
                        listener.onTrackChanged(i);
                        listener.onPlaybackStateChanged(false);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
    }

    public void togglePlayPause() {
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.start();
        }
        savePlaybackState();
        showNotification();
        if (listener != null) {
            listener.onPlaybackStateChanged(player.isPlaying());
        }
    }

    public void next() {
        if (songs == null || songs.isEmpty()) {
            return;
        }
        int nextIndex = computeNextIndex();
        playAt(nextIndex);
    }

    public void previous() {
        if (songs == null || songs.isEmpty()) {
            return;
        }
        int prevIndex = computePreviousIndex();
        playAt(prevIndex);
    }

    private int computeNextIndex() {
        if (shuffleEnabled && shuffleOrder != null && !shuffleOrder.isEmpty()) {
            currentShufflePosition = (currentShufflePosition + 1) % shuffleOrder.size();
            return shuffleOrder.get(currentShufflePosition);
        }
        return (currentIndex + 1) % songs.size();
    }

    private int computePreviousIndex() {
        if (shuffleEnabled && shuffleOrder != null && !shuffleOrder.isEmpty()) {
            currentShufflePosition = (currentShufflePosition - 1 + shuffleOrder.size()) % shuffleOrder.size();
            return shuffleOrder.get(currentShufflePosition);
        }
        return (currentIndex - 1 + songs.size()) % songs.size();
    }

    /**
     * Called whenever the track changes via a direct jump (row tap, restore
     * session) rather than next()/previous() - the one place a lookup is
     * unavoidable, but it only happens on explicit user selection, not on
     * every navigation step.
     */
    private void syncShufflePosition() {
        if (shuffleOrder != null) {
            currentShufflePosition = shuffleOrder.indexOf(currentIndex);
        }
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

    public Song getCurrentSong() {
        if (songs == null || currentIndex < 0 || currentIndex >= songs.size()) {
            return null;
        }
        return songs.get(currentIndex);
    }

    private void savePlaybackState() {
        Song current = getCurrentSong();
        if (current == null) {
            return;
        }
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_LAST_SONG_ID, current.getId());
        editor.putInt(KEY_LAST_POSITION, player.getCurrentPosition());
        editor.commit();
    }

    public static long getLastSongId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_LAST_SONG_ID, -1);
    }

    public static int getLastPosition(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_LAST_POSITION, 0);
    }

    private void showNotification() {
        Song current = getCurrentSong();
        if (current == null) {
            return;
        }

        RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification_playback);
        views.setTextViewText(R.id.notifTitle, current.getTitle());
        views.setTextViewText(R.id.notifArtist, current.getArtist());
        views.setImageViewResource(R.id.notifPlayPause,
                player.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);

        views.setOnClickPendingIntent(R.id.notifPlayPause, buildPendingIntent(ACTION_PLAY_PAUSE));
        views.setOnClickPendingIntent(R.id.notifNext, buildPendingIntent(ACTION_NEXT));
        views.setOnClickPendingIntent(R.id.notifPrev, buildPendingIntent(ACTION_PREV));

        Notification notification = new Notification();
        notification.icon = android.R.drawable.ic_media_play;
        notification.contentView = views;
        notification.flags = Notification.FLAG_ONGOING_EVENT;

        Intent openAppIntent = new Intent(this, MainActivity.class);
        notification.contentIntent = PendingIntent.getActivity(
                this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        startForeground(NOTIFICATION_ID, notification);
    }

    private PendingIntent buildPendingIntent(String action) {
        Intent intent = new Intent(action);
        return PendingIntent.getBroadcast(this, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (repeatMode == REPEAT_ONE) {
            playAt(currentIndex);
        } else if (repeatMode == REPEAT_OFF && !shuffleEnabled
                && currentIndex == songs.size() - 1) {
            if (listener != null) {
                listener.onPlaybackStateChanged(false);
            }
        } else {
            next();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        savePlaybackState();
        try {
            unregisterReceiver(controlReceiver);
        } catch (IllegalArgumentException e) {
            // receiver already unregistered
        }
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
