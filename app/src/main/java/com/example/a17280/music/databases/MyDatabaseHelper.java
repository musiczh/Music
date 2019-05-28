package com.example.a17280.music.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by 17280 on 2019/4/23.
 *
 */

public class MyDatabaseHelper extends SQLiteOpenHelper {

    public static final String CREATE_BOOK = "create table my_music(" +
            "songId integer primary key autoincrement," +
            "songName text," +
            "songArtist text," +
            "songPath text)";

    public static final String CREATE_HISTORY_BOOK = "create table history_music(" +
            "id integer primary key autoincrement," +
            "songName text," +
            "songArtist text," +
            "songPath text," +
            "songId int)";

    private Context mContext;

    public MyDatabaseHelper(Context context, String name,
                            SQLiteDatabase.CursorFactory factory,int version){
        super(context,name,factory,version);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL(CREATE_BOOK);
        db.execSQL(CREATE_HISTORY_BOOK);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
