package com.taha.musicplayerpro;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SongAdapter extends BaseAdapter {

    private Context context;
    private List<Song> songs;
    private LayoutInflater inflater;
    private long currentPlayingId = -1;

    // In-memory cache of favorite song IDs. On a single-core 832MHz CPU,
    // hitting SQLite once per row on every scroll frame causes visible
    // stutter. This is refreshed only when favorites actually change.
    private Set<Long> favoriteIdsCache;

    public SongAdapter(Context context, List<Song> songs) {
        this.context = context;
        this.songs = songs;
        this.inflater = LayoutInflater.from(context);
        refreshFavoritesCache();
    }

    /** Call this after any favorite is toggled so the cache stays correct. */
    public void refreshFavoritesCache() {
        List<Long> favIds = MusicDatabase.getInstance(context).getFavoriteSongIds();
        favoriteIdsCache = new HashSet<Long>(favIds);
    }

    public void setCurrentPlayingId(long songId) {
        this.currentPlayingId = songId;
        notifyDataSetChanged();
    }

    public void updateSongs(List<Song> newSongs) {
        this.songs = newSongs;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int position) {
        return songs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return songs.get(position).getId();
    }

    static class ViewHolder {
        TextView title;
        TextView artist;
        ImageView favoriteIcon;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_song, parent, false);
            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.songTitle);
            holder.artist = (TextView) convertView.findViewById(R.id.songArtist);
            holder.favoriteIcon = (ImageView) convertView.findViewById(R.id.favoriteIcon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Song song = songs.get(position);
        holder.title.setText(song.getTitle());
        holder.artist.setText(song.getArtist());

        boolean isPlaying = song.getId() == currentPlayingId;
        holder.title.setTextColor(isPlaying ? 0xFF4CAF50 : 0xFFFFFFFF);

        // Memory lookup instead of a per-row DB query - critical on this CPU.
        boolean isFav = favoriteIdsCache.contains(song.getId());
        holder.favoriteIcon.setImageResource(isFav
                ? android.R.drawable.btn_star_big_on
                : android.R.drawable.btn_star_big_off);

        return convertView;
    }
}
