package com.example.a17280.music.util;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 17280 on 2019/5/18.
 *
 */

public class ActivityCollector {
    private static List<Activity> mListActivity = new ArrayList<>();

    public static void addActivity(Activity activity){
        mListActivity.add(activity);
    }

    public static void removeActivity(Activity activity){
        mListActivity.remove(activity);
    }

    public static void finishAll(){
        for (Activity activity : mListActivity){
            if (!activity.isFinishing())
            activity.finish();
        }
        mListActivity.clear();
    }

}
