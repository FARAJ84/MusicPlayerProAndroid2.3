package com.taha.musicplayerpro;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements PlaybackService.PlaybackListener {

    private ListView listView;
    private TextView nowPlayingText;
    private SeekBar seekBar;
    private Button playPauseButton;
    private ImageView shuffleButton;
    private ImageView repeatButton;
    private EditText searchBox;
    private ProgressBar loadingSpinner;

    private List<Song> allSongs;
    private List<Song> displayedSongs;
    private SongAdapter adapter;

    private PlaybackService playbackService;
    private boolean isBound = false;
    private boolean pendingBindAfterLoad = false;

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
            playbackService.setListener(MainActivity.this);
            isBound = true;

            if (allSongs != null) {
                finishBindingSetup();
            } else {
                // songs still loading in background - wire up once scan finishes
                pendingBindAfterLoad = true;
            }
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
        shuffleButton = (ImageView) findViewById(R.id.shuffleButton);
        repeatButton = (ImageView) findViewById(R.id.repeatButton);
        searchBox = (EditText) findViewById(R.id.searchBox);
        loadingSpinner = (ProgressBar) findViewById(R.id.loadingSpinner);
        Button nextButton = (Button) findViewById(R.id.nextButton);
        Button prevButton = (Button) findViewById(R.id.prevButton);

        setupListeners(nextButton, prevButton);

        // Scanning MediaStore on a single-core 832MHz CPU can take a
        // noticeable moment on first run (before the SQLite cache exists).
        // Doing this on the UI thread would risk an ANR dialog, so it
        // always runs in the background even though it's usually instant
        // once cached.
        new LoadSongsTask().execute();

        Intent serviceIntent = new Intent(this, PlaybackService.class);
        startService(serviceIntent);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    private class LoadSongsTask extends AsyncTask<Void, Void, List<Song>> {
        @Override
        protected void onPreExecute() {
            loadingSpinner.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        }

        @Override
        protected List<Song> doInBackground(Void... params) {
            return MusicLibrary.loadSongs(MainActivity.this, false);
        }

        @Override
        protected void onPostExecute(List<Song> result) {
            allSongs = result;
            displayedSongs = new ArrayList<Song>(allSongs);
            adapter = new SongAdapter(MainActivity.this, displayedSongs);
            listView.setAdapter(adapter);

            loadingSpinner.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);

            if (allSongs.isEmpty()) {
                Toast.makeText(MainActivity.this, "No music found on device", Toast.LENGTH_LONG).show();
            }

            if (pendingBindAfterLoad && isBound) {
                finishBindingSetup();
            }
        }
    }

    private void finishBindingSetup() {
        playbackService.setSongs(allSongs);

        long lastSongId = PlaybackService.getLastSongId(MainActivity.this);
        if (lastSongId != -1) {
            int lastPosition = PlaybackService.getLastPosition(MainActivity.this);
            playbackService.restoreLastSession(lastSongId, lastPosition);
        }
        pendingBindAfterLoad = false;
    }

    private void setupListeners(Button nextButton, Button prevButton) {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Song clicked = displayedSongs.get(position);
                int realIndex = allSongs.indexOf(clicked);
                if (isBound && realIndex >= 0) {
                    playbackService.playAt(realIndex);
                }
            }
        });

        // tap-and-hold toggles favorite without triggering row playback
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Song song = displayedSongs.get(position);
                MusicDatabase.getInstance(MainActivity.this).toggleFavorite(song.getId());
                adapter.refreshFavoritesCache();
                adapter.notifyDataSetChanged();
                return true;
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
                }
            }
        });

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound) {
                    playbackService.previous();
                }
            }
        });

        shuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound) {
                    boolean newState = !playbackService.isShuffleEnabled();
                    playbackService.setShuffleEnabled(newState);
                    updateShuffleIcon(newState);
                }
            }
        });

        repeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound) {
                    playbackService.cycleRepeatMode();
                    updateRepeatIcon(playbackService.getRepeatMode());
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

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSongs(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterSongs(String query) {
        if (allSongs == null || displayedSongs == null || adapter == null) {
            return;
        }
        displayedSongs.clear();
        if (query == null || query.length() == 0) {
            displayedSongs.addAll(allSongs);
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            for (Song song : allSongs) {
                if (song.getTitle().toLowerCase(Locale.getDefault()).contains(lowerQuery)
                        || song.getArtist().toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                    displayedSongs.add(song);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void updateShuffleIcon(boolean enabled) {
        shuffleButton.setAlpha(enabled ? 255 : 120);
    }

    private void updateRepeatIcon(int mode) {
        if (mode == PlaybackService.REPEAT_OFF) {
            repeatButton.setAlpha(120);
        } else {
            repeatButton.setAlpha(255);
        }
    }

    @Override
    public void onTrackChanged(int index) {
        if (allSongs == null || index < 0 || index >= allSongs.size()) {
            return;
        }
        Song song = allSongs.get(index);
        nowPlayingText.setText(song.getTitle() + " - " + song.getArtist());
        if (adapter != null) {
            adapter.setCurrentPlayingId(song.getId());
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        playPauseButton.setText(isPlaying ? "Pause" : "Play");
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
