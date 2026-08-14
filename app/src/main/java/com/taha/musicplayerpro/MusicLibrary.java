package com.taha.musicplayerpro;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

public class MusicLibrary {

    /**
     * Returns cached songs if available. Only touches MediaStore (a slow
     * operation on old hardware) when the cache is empty or a refresh is
     * explicitly requested.
     */
    public static List<Song> loadSongs(Context context, boolean forceRefresh) {
        MusicDatabase db = MusicDatabase.getInstance(context);

        if (!forceRefresh && db.hasCachedSongs()) {
            return db.getCachedSongs();
        }

        List<Song> scanned = scanMediaStore(context);
        db.replaceCachedSongs(scanned);
        return scanned;
    }

    private static List<Song> scanMediaStore(Context context) {
        List<Song> songs = new ArrayList<Song>();

        ContentResolver resolver = context.getContentResolver();
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        Cursor cursor = resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.TITLE + " ASC"
        );

        if (cursor != null) {
            int idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idCol);
                String title = cursor.getString(titleCol);
                String artist = cursor.getString(artistCol);
                String album = cursor.getString(albumCol);
                String path = cursor.getString(dataCol);
                long duration = cursor.getLong(durationCol);
                long albumId = cursor.getLong(albumIdCol);

                if (title == null) {
                    title = "Unknown title";
                }
                if (artist == null) {
                    artist = "Unknown artist";
                }
                if (album == null) {
                    album = "Unknown album";
                }

                songs.add(new Song(id, title, artist, album, path, duration, albumId));
            }
            cursor.close();
        }

        return songs;
    }
}
