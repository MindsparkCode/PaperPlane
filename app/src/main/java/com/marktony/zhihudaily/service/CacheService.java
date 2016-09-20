package com.marktony.zhihudaily.service;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.marktony.zhihudaily.app.VolleySingleton;
import com.marktony.zhihudaily.bean.ZhihuDailyStory;
import com.marktony.zhihudaily.db.DatabaseHelper;
import com.marktony.zhihudaily.util.Api;

import java.util.ArrayList;

/**
 * Created by Lizhaotailang on 2016/9/18.
 */

public class CacheService extends Service {

    private ArrayList<Integer> zhihuIds = new ArrayList<Integer>();
    private ArrayList<Integer> guokrIds = new ArrayList<Integer>();
    private ArrayList<Integer> doubanIds = new ArrayList<Integer>();
    private Gson gson = new Gson();

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    private static final String TAG = CacheService.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new DatabaseHelper(this,"History.db",null,4);
        db = dbHelper.getWritableDatabase();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public void setZhihuIds(ArrayList<Integer> zhihuIds) {
        this.zhihuIds = zhihuIds;
    }

    // TODO: 2016/9/21 需要改进，不能每次都请求数据，理想的状态是已经存在的直接跳过，只请求不存在的部分
    // TODO: 2016/9/21 service也应该在适当的时候自动停止
    public void startZhihuCache() {
        for (int i = 0; i < zhihuIds.size(); i++) {
            final int finali = i;
            StringRequest request = new StringRequest(Request.Method.GET, Api.ZHIHU_NEWS + zhihuIds.get(i), new Response.Listener<String>() {
                @Override
                public void onResponse(String s) {
                    ZhihuDailyStory story = gson.fromJson(s, ZhihuDailyStory.class);
                    Cursor cursor = db.query("Zhihu", null, null, null, null, null, null);
                    if (cursor.moveToFirst()) {
                        do {
                            if (cursor.getInt(cursor.getColumnIndex("zhihu_id")) == (zhihuIds.get(finali))) {
                                db.beginTransaction();
                                try {
                                    ContentValues values = new ContentValues();
                                    values.put("zhihu_content", s);
                                    db.update("Zhihu", values, "zhihu_id = ?", new String[] {String.valueOf(story.getId())});
                                    values.clear();
                                    db.setTransactionSuccessful();
                                } catch (Exception e){
                                    e.printStackTrace();
                                } finally {
                                    db.endTransaction();
                                }
                                break;
                            }
                        } while (cursor.moveToNext());
                    }
                    cursor.close();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {

                }
            });
            VolleySingleton.getVolleySingleton(this).getRequestQueue().add(request);
        }
    }

    public void setGuokrIds(ArrayList<Integer> guokrIds) {
        this.guokrIds = guokrIds;
    }

    public void startGuokrCache() {
        for (int i = 0; i < guokrIds.size(); i++) {
            final int finalI = i;
            StringRequest request = new StringRequest(Api.GUOKR_ARTICLE_LINK_V2 + guokrIds.get(i), new Response.Listener<String>() {
                @Override
                public void onResponse(String s) {
                    Cursor cursor = db.query("Guokr", null, null, null, null, null, null);
                    if (cursor.moveToFirst()) {
                        do {
                            if (cursor.getInt(cursor.getColumnIndex("guokr_id")) == (guokrIds.get(finalI))) {
                                db.beginTransaction();
                                try {
                                    ContentValues values = new ContentValues();
                                    values.put("guokr_content", s);
                                    db.update("Guokr", values, "guokr_id = ?", new String[] {String.valueOf(guokrIds.get(finalI))});
                                    values.clear();
                                    db.setTransactionSuccessful();
                                } catch (Exception e){
                                    e.printStackTrace();
                                } finally {
                                    db.endTransaction();
                                }
                                break;
                            }
                        } while (cursor.moveToNext());
                    }
                    cursor.close();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {

                }
            });
            VolleySingleton.getVolleySingleton(this).getRequestQueue().add(request);
        }
    }

    public class MyBinder extends Binder {
        public CacheService getService() {
            return CacheService.this;
        }
    }

}