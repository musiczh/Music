package com.example.a17280.music.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.a17280.music.R;
import com.example.a17280.music.databases.MyDatabaseHelper;
import com.example.a17280.music.service.MediaService;
import com.example.a17280.music.util.ActivityCollector;

import static android.os.Process.killProcess;
import static android.os.Process.myPid;


public class PlayActivity extends BaseActivity  implements View.OnClickListener{
    private MediaService.MediaBinder mediaBinder;   //服务返回的实例，用以操作服务
    public static SeekBar audioSeekBar;             //进度条实例
    private ImageView play_imgView;                 //播放暂停按钮
    private TextView textView;                      //播放界面显示歌曲名字
    private IntentFilter intentFilter;              //表示要接受什么广播
    private PlayActivity.PlayBroadcastReceiver playBroadcastReceiver; //广播接收器
    private ImageView model_imgView;                //改变播放模式按钮
    private int model_num = 1;                          //1是顺序播放，2是单曲循环
    private int ifPlay = 0 ;                        //判断是否已经播放
    private MyDatabaseHelper dbHelper;              //数据库帮助类变量
    private SQLiteDatabase db;                      //本地歌曲数据库



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
        setContentView(R.layout.activity_play);
        textView = (TextView)findViewById(R.id.play_text);


        //设置标题栏
        Toolbar toolbar = (Toolbar) findViewById(R.id.play_toolbar);
        setSupportActionBar(toolbar);




        //申请权限
        if(Build.VERSION.SDK_INT >= 16) {
            if (ContextCompat.checkSelfPermission(PlayActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(PlayActivity.this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
        //打开数据库,存储信息到列表
        dbHelper = new MyDatabaseHelper(this,"SongList.db",null,1);
        db = dbHelper.getReadableDatabase();


        //绑定服务
        Intent bindIntent = new Intent(this, MediaService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);

        //接受广播,播放歌曲时改变下方的播放按钮
        intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.a17280.music.MY_BROADCAST");
        playBroadcastReceiver = new PlayActivity.PlayBroadcastReceiver();
        registerReceiver(playBroadcastReceiver,intentFilter);


        play_imgView = (ImageView)findViewById(R.id.play_play_pause_img);
        ImageView next_imgView = (ImageView)findViewById(R.id.play_next_img);
        //ImageView sound_imgView = (ImageView)findViewById(R.id.play_sound_img);
        model_imgView = (ImageView)findViewById(R.id.play_model_img);
        ImageView previous_imgView = (ImageView)findViewById(R.id.play_previous_img);
        ImageView song_imgView = (ImageView)findViewById(R.id.play_song_list_img);

        play_imgView.setOnClickListener(this);
        previous_imgView.setOnClickListener(this);
        next_imgView.setOnClickListener(this);
        song_imgView.setOnClickListener(this);
        model_imgView.setOnClickListener(this);



        //播放进度监听
        audioSeekBar = (SeekBar) findViewById(R.id.play_seek_bar);
        audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //从拖动的地方开始播放
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                //假设改变源于用户拖动且播放器里面已经有歌了
                if (fromUser && ifPlay == 1) {
                    mediaBinder.seekToPlay(progress);// 当进度条的值改变时，音乐播放器从新的位置开始播放
                }
            }

            // 开始拖动进度条时，音乐暂停播放
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (mediaBinder.isPlaying()) {
                    mediaBinder.pauseSong();
                    play_imgView.setImageResource(R.drawable.play_img_big);
                }
            }

            // 停止拖动进度条时，音乐开始播放
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (ifPlay == 1) {
                    mediaBinder.continueSong();
                    play_imgView.setImageResource(R.drawable.pause_img);
                }else {
                    seekBar.setProgress(0);
                    Toast.makeText(PlayActivity.this,"请先播放一首歌曲",Toast.LENGTH_SHORT).show();
                }
            }
        });




    }//-------------------------------------------------------------------------------------------




    //给顶部标题栏按钮设置响应事件
    //先加载菜单布局文件
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.play_toolbar, menu);
        return true;
    }
    //再给标题栏按钮设置响应事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.exit_item:
                ActivityCollector.finishAll();
                killProcess(myPid());
                break;
            default:
        }
        return true;
    }


    //广播接收器,改变下栏的显示的歌曲名字以及播放图标
    class PlayBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent){
            textView.setText(intent.getStringExtra("song_name"));
            play_imgView.setImageResource(R.drawable.pause_img);
            ifPlay = 1;

        }
    }


    //布局控件的点击事件
    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.play_play_pause_img :
                if (mediaBinder.isPlaying()) {
                    mediaBinder.pauseSong();
                    play_imgView.setImageResource(R.drawable.play_img_big);
                } else if (ifPlay == 1){
                    mediaBinder.continueSong();
                    play_imgView.setImageResource(R.drawable.pause_img);}
                break;
            case R.id.play_next_img :
                if (ifPlay == 1) {
                    mediaBinder.playNext();
                    play_imgView.setImageResource(R.drawable.pause_img);
                }else{
                    Toast.makeText(PlayActivity.this,"请先播放一首歌曲",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.play_previous_img :
                if (ifPlay == 1) {
                    mediaBinder.playPrevious();
                    play_imgView.setImageResource(R.drawable.pause_img);
                }else {
                    Toast.makeText(PlayActivity.this,"请先播放一首歌曲",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.play_song_list_img:
                Intent intent = new Intent(PlayActivity.this,MainActivity.class);
                startActivity(intent);
                break;
            case R.id.play_model_img:
                mediaBinder.changeModel();
                if (model_num == 1) {
                    model_imgView.setImageResource(R.drawable.repeat_one_img);
                    model_num = 2;
                    }
                else {
                    model_imgView.setImageResource(R.drawable.order_model_img);
                    model_num = 1;
                }

        }
    }

    //在访问sd卡权限中判断用户选择了什么结果
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(PlayActivity.this,"获取权限成功",Toast.LENGTH_LONG).show();
                    sortSongs();

                } else {
                    Toast.makeText(PlayActivity.this,"不用试啦，没有权限还怎么玩呐",Toast.LENGTH_LONG).show();
                    killProcess(myPid());
                }
                break;
            default:
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

    //搜索本地歌曲
    private void sortSongs(){

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


    //重写onDestroy方法，关闭广播
    @Override
    public  void onDestroy(){
        super.onDestroy();
        //关闭广播
        unregisterReceiver(playBroadcastReceiver);
        unbindService(connection);
    }


}
