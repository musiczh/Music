package com.example.a17280.music.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.a17280.music.R;
import com.example.a17280.music.entity.Song;

import java.util.ArrayList;
import java.util.List;

import static android.R.id.list;

/**
 * Created by 17280 on 2019/4/21.
 */

public class SongAdapter extends ArrayAdapter<Song> {
    private int defaultPosition = -1;
    private int resourceId;
    private int pressedColor;
    private int normalColor;
    private Resources resources;
    private List<Song> mList = new ArrayList<>();

    public SongAdapter(Context context, int textResourceId, List<Song> objects){
        super(context,textResourceId,objects);
        resourceId = textResourceId;
        resources = context.getResources();
        pressedColor = resources.getColor(R.color.pressed);
        normalColor = resources.getColor(R.color.normal);
        mList = objects;
    }

    @Override
    public View getView(int position, View converView, ViewGroup parent){
        Song song = getItem(position);
        View view;
        if (converView == null) {
            view = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
        }else{
            view = converView;

        }
        TextView songName = (TextView)view.findViewById(R.id.song_name_text);
        TextView songArtist = (TextView)view.findViewById(R.id.song_artist_text);
        songName.setText(song.getSongName());
        songArtist.setText(song.getSongArtist());

        return view;
    }



}
