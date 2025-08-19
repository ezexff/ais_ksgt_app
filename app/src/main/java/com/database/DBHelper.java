package com.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {

    // Тег класса (для логирования)
    private final static String TAG = "T_DBHelper";

    public DBHelper(Context context, String database_name) {
        // Конструктор суперкласса
        super(context, database_name, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "--- onCreate database ---");
        // Создаем таблицу с полями

        /*db.execSQL("create table DEFECTION ("
                + "id integer primary key autoincrement,"
                + "name text,"
                + "email text" + ");");*/
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}
