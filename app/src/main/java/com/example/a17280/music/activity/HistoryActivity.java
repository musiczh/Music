package com.example.a17280.music.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.a17280.music.databases.MyDatabaseHelper;
import com.example.a17280.music.R;
import com.example.a17280.music.entity.Song;
import com.example.a17280.music.adapter.SongAdapter;
import com.example.a17280.music.service.MediaService;

import java.util.ArrayList;
import java.util.List;

import static com.example.a17280.music.activity.MainActivity.ifPlay;

public class    HistoryActivity extends BaseActivity {
    public  ListView listView;                      //ListView列表
    private SongAdapter adapter;                    //ListView的适配器
    public static List<Song> historySongList = new ArrayList<>();
    private MyDatabaseHelper dbHelper;              //数据库帮助类变量
    private SQLiteDatabase db;                      //数据库变量
    private MediaService.MediaBinder mediaBinder;   //服务返回的实例，用以操作服务





    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mediaBinder = (MediaService.MediaBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        //设置标题栏
        Toolbar toolbar = (Toolbar) findViewById(R.id.history_toolbar);
        setSupportActionBar(toolbar);

        //获取或者建立数据库,并获得数据库里面的内容
        dbHelper = new MyDatabaseHelper(this,"SongList.db",null,1);
        db = dbHelper.getReadableDatabase();
        searchSong();

        //绑定服务
        Intent bindIntent = new Intent(this, MediaService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);



        if (historySongList.size() != 0) {
            //把集合装在适配器中并传入ListView中显示
            adapter = new SongAdapter(HistoryActivity.this, R.layout.play_list_item, historySongList);
            listView = (ListView) findViewById(R.id.history_list_view);
            listView.setAdapter(adapter);
            //给每个子项设置监听，点击播放歌曲
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    //调用服务的方法播放歌曲，和改变底栏显示的歌曲名字
                    Song songSelected = historySongList.get(position);
                    mediaBinder.playSong(songSelected.getSongPath(),songSelected.getSongId());
                    if (ifPlay == 0) ifPlay =1;
                }
            });
        }else{
            Toast.makeText(HistoryActivity.this,"赶紧去播放一首歌曲吧",Toast.LENGTH_LONG).show();
        }




    }


    //给顶部标题栏按钮设置响应事件
    //先加载菜单布局文件
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history_menu, menu);
        return true;
    }
    //再给标题栏按钮设置响应事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.h_back_item:
                Intent intent = new Intent(HistoryActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
                break;
            default:
        }
        return true;
    }


    //查询数据库的内容
    public void searchSong() {
        historySongList.clear();
        Cursor cursor = db.query("history_music", null, null, null, null, null, " id desc");
        //遍历媒体数据库
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                int id = cursor.getInt(cursor.getColumnIndex("songId"));
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
                eSong.setSongId(id);
                historySongList.add(eSong);
                cursor.moveToNext();
            }
            cursor.close();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }
}
