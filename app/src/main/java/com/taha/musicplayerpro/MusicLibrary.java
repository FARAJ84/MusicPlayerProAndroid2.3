package com.taha.musicplayerpro;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

public class MusicLibrary {

    public static List<Song> loadSongs(Context context) {
        List<Song> songs = new ArrayList<Song>();

        ContentResolver resolver = context.getContentResolver();
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
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
            int dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idCol);
                String title = cursor.getString(titleCol);
                String artist = cursor.getString(artistCol);
                String path = cursor.getString(dataCol);
                long duration = cursor.getLong(durationCol);

                if (title == null) {
                    title = "Unknown title";
                }
                if (artist == null) {
                    artist = "Unknown artist";
                }

                songs.add(new Song(id, title, artist, path, duration));
            }
            cursor.close();
        }

        return songs;
    }
}
