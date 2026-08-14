package com.taha.musicplayerpro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class MusicDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "musicplayerpro.db";
    private static final int DB_VERSION = 1;

    // songs cache table - avoids re-querying MediaStore on every launch
    private static final String TABLE_SONGS = "songs";
    private static final String COL_ID = "_id";
    private static final String COL_TITLE = "title";
    private static final String COL_ARTIST = "artist";
    private static final String COL_ALBUM = "album";
    private static final String COL_PATH = "path";
    private static final String COL_DURATION = "duration";
    private static final String COL_ALBUM_ID = "album_id";

    private static final String TABLE_PLAYLISTS = "playlists";
    private static final String COL_PLAYLIST_ID = "playlist_id";
    private static final String COL_PLAYLIST_NAME = "name";

    private static final String TABLE_PLAYLIST_SONGS = "playlist_songs";
    private static final String COL_PS_PLAYLIST_ID = "playlist_id";
    private static final String COL_PS_SONG_ID = "song_id";

    private static final String TABLE_FAVORITES = "favorites";
    private static final String COL_FAV_SONG_ID = "song_id";

    private static MusicDatabase instance;

    public static synchronized MusicDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new MusicDatabase(context.getApplicationContext());
        }
        return instance;
    }

    private MusicDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_SONGS + " ("
                + COL_ID + " INTEGER PRIMARY KEY, "
                + COL_TITLE + " TEXT, "
                + COL_ARTIST + " TEXT, "
                + COL_ALBUM + " TEXT, "
                + COL_PATH + " TEXT, "
                + COL_DURATION + " INTEGER, "
                + COL_ALBUM_ID + " INTEGER)");

        db.execSQL("CREATE TABLE " + TABLE_PLAYLISTS + " ("
                + COL_PLAYLIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_PLAYLIST_NAME + " TEXT UNIQUE)");

        db.execSQL("CREATE TABLE " + TABLE_PLAYLIST_SONGS + " ("
                + COL_PS_PLAYLIST_ID + " INTEGER, "
                + COL_PS_SONG_ID + " INTEGER)");

        db.execSQL("CREATE TABLE " + TABLE_FAVORITES + " ("
                + COL_FAV_SONG_ID + " INTEGER PRIMARY KEY)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLISTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLIST_SONGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
        onCreate(db);
    }

    // ---- Songs cache ----

    public boolean hasCachedSongs() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SONGS, null);
        boolean hasSongs = false;
        if (cursor.moveToFirst()) {
            hasSongs = cursor.getInt(0) > 0;
        }
        cursor.close();
        return hasSongs;
    }

    public void replaceCachedSongs(List<Song> songs) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM " + TABLE_SONGS);
            for (Song song : songs) {
                ContentValues values = new ContentValues();
                values.put(COL_ID, song.getId());
                values.put(COL_TITLE, song.getTitle());
                values.put(COL_ARTIST, song.getArtist());
                values.put(COL_ALBUM, song.getAlbum());
                values.put(COL_PATH, song.getPath());
                values.put(COL_DURATION, song.getDuration());
                values.put(COL_ALBUM_ID, song.getAlbumId());
                db.insert(TABLE_SONGS, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Song> getCachedSongs() {
        List<Song> songs = new ArrayList<Song>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_SONGS, null, null, null, null, null, COL_TITLE + " ASC");

        if (cursor.moveToFirst()) {
            int idCol = cursor.getColumnIndex(COL_ID);
            int titleCol = cursor.getColumnIndex(COL_TITLE);
            int artistCol = cursor.getColumnIndex(COL_ARTIST);
            int albumCol = cursor.getColumnIndex(COL_ALBUM);
            int pathCol = cursor.getColumnIndex(COL_PATH);
            int durationCol = cursor.getColumnIndex(COL_DURATION);
            int albumIdCol = cursor.getColumnIndex(COL_ALBUM_ID);

            do {
                Song song = new Song(
                        cursor.getLong(idCol),
                        cursor.getString(titleCol),
                        cursor.getString(artistCol),
                        cursor.getString(albumCol),
                        cursor.getString(pathCol),
                        cursor.getLong(durationCol),
                        cursor.getLong(albumIdCol)
                );
                songs.add(song);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }

    // ---- Playlists ----

    public long createPlaylist(String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PLAYLIST_NAME, name);
        return db.insertWithOnConflict(TABLE_PLAYLISTS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void deletePlaylist(long playlistId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_PLAYLISTS, COL_PLAYLIST_ID + "=?", new String[]{String.valueOf(playlistId)});
        db.delete(TABLE_PLAYLIST_SONGS, COL_PS_PLAYLIST_ID + "=?", new String[]{String.valueOf(playlistId)});
    }

    public List<Playlist> getAllPlaylists() {
        List<Playlist> playlists = new ArrayList<Playlist>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_PLAYLISTS, null, null, null, null, null, COL_PLAYLIST_NAME + " ASC");

        if (cursor.moveToFirst()) {
            int idCol = cursor.getColumnIndex(COL_PLAYLIST_ID);
            int nameCol = cursor.getColumnIndex(COL_PLAYLIST_NAME);
            do {
                playlists.add(new Playlist(cursor.getLong(idCol), cursor.getString(nameCol)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return playlists;
    }

    public void addSongToPlaylist(long playlistId, long songId) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor existing = db.query(TABLE_PLAYLIST_SONGS, null,
                COL_PS_PLAYLIST_ID + "=? AND " + COL_PS_SONG_ID + "=?",
                new String[]{String.valueOf(playlistId), String.valueOf(songId)},
                null, null, null);
        boolean alreadyExists = existing.getCount() > 0;
        existing.close();

        if (!alreadyExists) {
            ContentValues values = new ContentValues();
            values.put(COL_PS_PLAYLIST_ID, playlistId);
            values.put(COL_PS_SONG_ID, songId);
            db.insert(TABLE_PLAYLIST_SONGS, null, values);
        }
    }

    public void removeSongFromPlaylist(long playlistId, long songId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_PLAYLIST_SONGS,
                COL_PS_PLAYLIST_ID + "=? AND " + COL_PS_SONG_ID + "=?",
                new String[]{String.valueOf(playlistId), String.valueOf(songId)});
    }

    public List<Long> getSongIdsInPlaylist(long playlistId) {
        List<Long> ids = new ArrayList<Long>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_PLAYLIST_SONGS, new String[]{COL_PS_SONG_ID},
                COL_PS_PLAYLIST_ID + "=?", new String[]{String.valueOf(playlistId)},
                null, null, null);

        if (cursor.moveToFirst()) {
            int songIdCol = cursor.getColumnIndex(COL_PS_SONG_ID);
            do {
                ids.add(cursor.getLong(songIdCol));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return ids;
    }

    // ---- Favorites ----

    public boolean isFavorite(long songId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_FAVORITES, null,
                COL_FAV_SONG_ID + "=?", new String[]{String.valueOf(songId)},
                null, null, null);
        boolean isFav = cursor.getCount() > 0;
        cursor.close();
        return isFav;
    }

    public void toggleFavorite(long songId) {
        SQLiteDatabase db = getWritableDatabase();
        if (isFavorite(songId)) {
            db.delete(TABLE_FAVORITES, COL_FAV_SONG_ID + "=?", new String[]{String.valueOf(songId)});
        } else {
            ContentValues values = new ContentValues();
            values.put(COL_FAV_SONG_ID, songId);
            db.insert(TABLE_FAVORITES, null, values);
        }
    }

    public List<Long> getFavoriteSongIds() {
        List<Long> ids = new ArrayList<Long>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_FAVORITES, null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            int songIdCol = cursor.getColumnIndex(COL_FAV_SONG_ID);
            do {
                ids.add(cursor.getLong(songIdCol));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return ids;
    }
}
