package com.example.a17280.music.entity;



/**
 * Created by 17280 on 2019/4/13.
 * 歌曲类
 */

public class Song {
    //属性
    private String songName;
    private String songPath;
    private int songId;
    private int songDuration;
    private String songArtist;


    //构造器
    public Song (){}

    //获取对应的信息
    public String getSongName() {
        return songName;
    }
    public String getSongPath() {
        return songPath;
    }
    public int getDuration() {
        return songDuration;
    }
    public int getSongId() {
        return songId;
    }
    public String getSongArtist() {
        return songArtist;
    }



    //设置对应的信息
    public void setSongDuration(int duration) {
        this.songDuration = duration;
    }
    public void setSongArtist(String songArtist) {
        this.songArtist = songArtist;
    }
    public void setSongId(int songId) {
        this.songId = songId;
    }
    public void setSongName(String songName) {
        this.songName = songName;
    }
    public void setSongPath(String songPath) {
        this.songPath = songPath;
    }
}
