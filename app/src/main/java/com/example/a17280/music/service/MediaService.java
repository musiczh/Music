package com.example.a17280.music.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.example.a17280.music.activity.MainActivity;
import com.example.a17280.music.databases.MyDatabaseHelper;
import com.example.a17280.music.activity.PlayActivity;
import com.example.a17280.music.entity.Song;

/**
 * 播放器服务，后台播放，处理播放逻辑
 */

public class MediaService extends Service implements Runnable{


    private static MediaPlayer mediaPlayer = new MediaPlayer(); //创建一个播放器；
    private MediaBinder mBinder = new MediaBinder();            //IBinder对象可以作为参数传出去给活动
    public static List<Song> songList = new ArrayList<>();      //歌曲集合
    private Song songSelected;                                  //正在播放哪首歌
    int songPosition = 0;                                       //正在播放的歌的位置是那一首
    private MyDatabaseHelper dbHelper;                          //数据库帮助类变量
    private SQLiteDatabase db;                                  //本地歌曲信息数据库
    private ContentValues values = new ContentValues();         //在表中增加数据的载体
    private int playModel = 1;                                  //播放逻辑，1是顺序播放，2是单曲循环
    private RefreshBroadcastReceiver broadcastReceiver; //广播接收器



    @Override
    public void onCreate(){

        //接受广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.a17280.service.REFRESH");
        broadcastReceiver = new RefreshBroadcastReceiver();
        registerReceiver(broadcastReceiver,intentFilter);
    }


    //创建一个内部类，可以把操作服务器的方法放在里面，供给外部使用
    public class MediaBinder extends Binder{

        //上下曲
        public void playNext(){
            int check = songPosition;
            nextSong();
            if(check == songPosition-1) {
                playMusic(songSelected.getSongPath(), songPosition);
            }else{
                nextSong();
                playMusic(songSelected.getSongPath(), songPosition);
            }
        }
        public void playPrevious(){
            previousSong();
            playMusic(songSelected.getSongPath(),songPosition);
        }

        public void changeModel(){
            if (playModel == 1) playModel = 2;
            else playModel = 1;
        }


        //播放歌曲方法
        public void playSong(String path, int id ){
            playMusic(path, id);
        }

        //暂停播放
        public void pauseSong(){
            mediaPlayer.pause();
        }

        //继续播放
        public void continueSong(){
            mediaPlayer.start();
        }

        //判断是否在播放
        public boolean isPlaying(){
            return mediaPlayer.isPlaying();
        }

        //从指定位置播放
        public void seekToPlay(int position){
            mediaPlayer.seekTo(position);
        }




    }

    //播放歌曲方法，包括重置数据，发送广播，开启线程
    public void playMusic(String path, int id ){
        //修改播放相关数据
        songPosition = id;
        songSelected = songList.get(id);
        //存储播放记录
        check(songSelected.getSongName());
        values.put("songName", songSelected.getSongName());
        values.put("songArtist", songSelected.getSongArtist());
        values.put("songPath", path);
        values.put("songId",id);
        db.insert("history_music", null, values);
        values.clear();
        //发送广播，改变显示 的歌曲
        Intent intent = new Intent("com.example.a17280.music.MY_BROADCAST");
        intent.putExtra("song_name",songSelected.getSongName());
        sendBroadcast(intent);

        //如果当前有歌曲在播放就先停止
        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            mediaPlayer.reset();
        }
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
            //开启新线程同步进度条
            new Thread(MediaService.this).start();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.getContext(),"歌曲不存在",Toast.LENGTH_SHORT).show();
            nextSong();
            playMusic(songSelected.getSongPath(),songPosition);
        }
    }

    //子线程run方法，实现后台动态刷新进度条
    @Override
    public void run() {
        int total = mediaPlayer.getDuration();// 总时长
        int currentPosition = 0;
        // 设置进度条初始化
            PlayActivity.audioSeekBar.setMax(total);
            PlayActivity.audioSeekBar.setProgress(0);
            while (mediaPlayer != null && currentPosition < total) {
                try {
                    Thread.sleep(1000);
                    if (mediaPlayer != null) {
                        currentPosition = mediaPlayer.getCurrentPosition();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                PlayActivity.audioSeekBar.setProgress(currentPosition);
            }
    }

    //重写结束方法，在服务结束的时候关闭线程并关闭media
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        //关闭线程
        Thread.currentThread().interrupt();
        stopForeground(true);
    }

    //查询数据库中的数据并存在集合中
    public void searchSong() {
        songList.clear();
        Cursor cursor = db.query("my_music", null, null, null, null, null, null);
        //遍历媒体数据库
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                //歌曲名字
                String name = cursor.getString(cursor.getColumnIndex("songName"));
                //歌曲的歌手名： MediaStore.Audio.Media.ARTIST
                String artist = cursor.getString(cursor.getColumnIndex("songArtist"));
                //歌曲文件的路径 ：MediaStore.Audio.Media.DATA
                String path = cursor.getString(cursor.getColumnIndex("songPath"));

                Song eSong = new Song();
                eSong.setSongArtist(artist);
                eSong.setSongName(name);
                eSong.setSongPath(path);

                songList.add(eSong);

                cursor.moveToNext();
            }
            cursor.close();
        }
    }

    //上下曲的方法
    public void nextSong(){
            if (songPosition==songList.size()-1) {
                songSelected = songList.get(0);
                songPosition = 0;
            }
            else {
                songSelected = songList.get(++songPosition);
                }

    }
    public void previousSong(){
            if (songPosition==0) {
                songSelected = songList.get(songList.size() - 1);
                songPosition = songList.size() - 1;
            }
            else songSelected = songList.get(--songPosition);
    }

    //自动播放下一首
    public void nextOne(){
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (playModel == 1) {
                    nextSong();
                }
                playMusic(songSelected.getSongPath(), songPosition);
            }
        });
    }

    //查询是否有重复
    private void check(String songName){
            Cursor cursor = db.query("history_music", null, null, null, null, null, null);
            //遍历媒体数据库
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {

                    //歌曲名字
                    String name = cursor.getString(cursor.getColumnIndex("songName"));
                    if(songName.equals(name)){
                        db.delete("history_music","songName=?",new String[]{name});
                        break;
                    }

                    cursor.moveToNext();
                }
                cursor.close();
            }

    }

    //广播接收器,改变下栏的显示的歌曲名字以及播放图标
    class RefreshBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            searchSong();
        }
    }




    //返回给活动的实例
    @Override
    public IBinder onBind(Intent intent){
        //搜索歌曲中所有歌曲的信息
        dbHelper = new MyDatabaseHelper(this,"SongList.db",null,1);
        db = dbHelper.getReadableDatabase();
        searchSong();
        //自动播放下一曲
        nextOne();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        unregisterReceiver(broadcastReceiver);
        return super.onUnbind(intent);
    }
}
