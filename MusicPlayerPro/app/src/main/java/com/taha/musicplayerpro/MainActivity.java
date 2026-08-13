package com.taha.musicplayerpro;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {

    private ListView listView;
    private TextView nowPlayingText;
    private SeekBar seekBar;
    private Button playPauseButton;

    private List<Song> songs;
    private ArrayAdapter<Song> adapter;

    private PlaybackService playbackService;
    private boolean isBound = false;

    private final Handler progressHandler = new Handler();

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (isBound && playbackService != null && playbackService.isPlaying()) {
                seekBar.setMax(playbackService.getDuration());
                seekBar.setProgress(playbackService.getCurrentPosition());
            }
            progressHandler.postDelayed(this, 500);
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackService.PlaybackBinder binder = (PlaybackService.PlaybackBinder) service;
            playbackService = binder.getService();
            playbackService.setSongs(songs);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            playbackService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.songListView);
        nowPlayingText = (TextView) findViewById(R.id.nowPlayingText);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        playPauseButton = (Button) findViewById(R.id.playPauseButton);
        Button nextButton = (Button) findViewById(R.id.nextButton);
        Button prevButton = (Button) findViewById(R.id.prevButton);

        songs = MusicLibrary.loadSongs(this);
        adapter = new ArrayAdapter<Song>(this, android.R.layout.simple_list_item_1, songs);
        listView.setAdapter(adapter);

        if (songs.isEmpty()) {
            Toast.makeText(this, "No music found on device", Toast.LENGTH_LONG).show();
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (isBound) {
                    playbackService.playAt(position);
                    updateNowPlaying(position);
                }
            }
        });

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound) {
                    playbackService.togglePlayPause();
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound) {
                    playbackService.next();
                    updateNowPlaying(playbackService.getCurrentIndex());
                }
            }
        });

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound) {
                    playbackService.previous();
                    updateNowPlaying(playbackService.getCurrentIndex());
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isBound) {
                    playbackService.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        Intent serviceIntent = new Intent(this, PlaybackService.class);
        startService(serviceIntent);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    private void updateNowPlaying(int index) {
        if (index >= 0 && index < songs.size()) {
            Song song = songs.get(index);
            nowPlayingText.setText(song.getTitle() + " - " + song.getArtist());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressHandler.post(progressRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        progressHandler.removeCallbacks(progressRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }
}
