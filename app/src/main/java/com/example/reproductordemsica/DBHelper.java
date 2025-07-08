package com.example.reproductordemsica;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME="musica.db";
    private static final int DB_VERSION=1;

    public DBHelper(Context c){ super(c,DB_NAME,null,DB_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db){
        String sql="CREATE TABLE songs("+
                "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                "title TEXT,"+
                "path TEXT UNIQUE)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db,int o,int n){
        db.execSQL("DROP TABLE IF EXISTS songs");
        onCreate(db);
    }
}