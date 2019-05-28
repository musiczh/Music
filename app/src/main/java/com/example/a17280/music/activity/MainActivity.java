package com.example.a17280.music.activity;



import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.a17280.music.databases.MyDatabaseHelper;
import com.example.a17280.music.R;
import com.example.a17280.music.entity.Song;
import com.example.a17280.music.adapter.SongAdapter;
import com.example.a17280.music.service.MediaService;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends BaseActivity{
    public static List<Song> songList = new ArrayList<>();//歌曲集合
    private ListView listView;                      //ListView列表
    private SongAdapter adapter;                    //ListView的适配器
    private MediaService.MediaBinder mediaBinder;   //服务返回的实例，用以操作服务
    private TextView textView;                      //底部歌曲名字栏实例
    private ImageView playPauseButton;              //底部播放暂停图标
    public static int ifPlay = 0;                   //是否播放了
    private MyDatabaseHelper dbHelper;              //数据库帮助类变量
    private SQLiteDatabase db;                      //本地歌曲数据库
    private static Context context;                 //上下文变量
    private IntentFilter intentFilter;              //表示要接受什么广播
    private  MainBroadcastReceiver mainBroadcastReceiver; //广播接收器



    //绑定服务要用的ServiceConnection实例
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mediaBinder = (MediaService.MediaBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };



    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  //加载布局
        context = MainActivity.this;




        //设置标题栏
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        //底部歌曲显示
        textView = (TextView) findViewById(R.id.main_song_name_text);




        //绑定服务
        Intent bindIntent = new Intent(this, MediaService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);

        //开始运行搜索数据库歌曲并存在集合中
        //打开数据库,存储信息到列表
        dbHelper = new MyDatabaseHelper(this,"SongList.db",null,1);
        db = dbHelper.getReadableDatabase();
        sortSongs();
        Intent intent = new Intent("com.example.a17280.service.REFRESH");
        sendBroadcast(intent);



        //接受广播
        intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.a17280.music.MY_BROADCAST");
        mainBroadcastReceiver = new MainBroadcastReceiver();
        registerReceiver(mainBroadcastReceiver,intentFilter);



        if (songList.size() != 0) {
            //把集合装在适配器中并传入ListView中显示
            adapter = new SongAdapter(MainActivity.this, R.layout.play_list_item, songList);
            listView = (ListView) findViewById(R.id.list_view);
            listView.setAdapter(adapter);

            //给每个子项设置监听，点击播放歌曲
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    if (ifPlay == 0) {ifPlay = 1;sortSongs();}
                    //调用服务的方法播放歌曲，和改变底栏显示的歌曲名字
                    Song songSelected = songList.get(position);
                    mediaBinder.playSong(songSelected.getSongPath(), position);

                }
            });

        }else{
            Toast.makeText(MainActivity.this,"本地没有歌曲",Toast.LENGTH_LONG).show();
        }

        //给下方的布局设置监听
        //暂停和播放
        playPauseButton = (ImageView) findViewById(R.id.main_pause_play_img);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaBinder.isPlaying()) {
                    mediaBinder.pauseSong();
                    playPauseButton.setImageResource(R.drawable.play_img_big);
                } else if (ifPlay == 1){
                    mediaBinder.continueSong();
                    playPauseButton.setImageResource(R.drawable.pause_img);
                }
            }
        });
        //下一曲
        Button button_1 = (Button) findViewById(R.id.main_next_button);
        button_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaBinder.playNext();
            }
        });
        //上一曲
        Button button_2 = (Button) findViewById(R.id.main_previous_button);
        button_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               mediaBinder.playPrevious();
            }
        });
        //跳转播放界面
        ImageView imageView = (ImageView) findViewById(R.id.main_singer_image);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,PlayActivity.class);
                startActivity(intent);
            }
        });


    }   //------------onCreate方法结尾-----------------------------------------------------------

    //广播接收器,改变下栏的显示的歌曲名字以及播放图标
        class MainBroadcastReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent){
                textView.setText(intent.getStringExtra("song_name"));
                playPauseButton.setImageResource(R.drawable.pause_img);
            }
}

    //获得本活动的上下文
    public static Context getContext(){
        return context;
    }


    //给顶部标题栏按钮设置响应事件
    //先加载菜单布局文件
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }
    //再给标题栏按钮设置响应事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.back_item:
                Intent intent = new Intent(MainActivity.this,PlayActivity.class);
                startActivity(intent);
                break;
            case R.id.history_item:
                Intent intent1 = new Intent(MainActivity.this,HistoryActivity.class);
                startActivity(intent1);
                break;
            case R.id.sort_local_item:
                Toast.makeText(MainActivity.this,"重新扫描本地歌曲",Toast.LENGTH_LONG).show();
                searchSongs();
                sortSongs();
                if (songList.size()!=0) {
                    adapter.notifyDataSetChanged();
                }
                Intent intent2 = new Intent("com.example.a17280.service.REFRESH");
                sendBroadcast(intent2);

            default:
        }
        return true;
    }

    //搜索本地歌曲
    private void searchSongs(){
        db.delete("my_music",null,null);
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        //遍历媒体数据库
        if(cursor.moveToFirst()){
            while (!cursor.isAfterLast()) {
                //歌曲编号
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                //歌曲标题
                String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                //歌曲的歌手名： MediaStore.Audio.Media.ARTIST
                String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                //歌曲文件的路径 ：MediaStore.Audio.Media.DATA
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                //歌曲的总播放时长 ：MediaStore.Audio.Media.DURATION
                int duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));


                if (duration>60000){

                    //把歌曲信息存在数据库中
                    ContentValues values = new ContentValues();
                    values.put("songName", name);
                    values.put("songArtist", artist);
                    values.put("songPath", path);
                    db.insert("my_music", null, values);
                    values.clear();

                }
                cursor.moveToNext();
            }
            cursor.close();
        }

    }



    //扫面数据库中的内容
    public void sortSongs() {
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
        }else{

            searchSongs();
            sortSongs();
        }
    }

    //重写返回键方法
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    //重写onDestroy方法，关闭广播
    @Override
    public  void onDestroy(){
        super.onDestroy();
        //关闭连接服务
        unbindService(connection);
        //关闭广播
        unregisterReceiver(mainBroadcastReceiver);
    }


}





