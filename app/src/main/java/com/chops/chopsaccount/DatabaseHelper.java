package com.chops.chopsaccount;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper
{
    private static final String DATABASE_NAME = "accounts.db";
    private static final int DATABASE_VERSION=5;

    public DatabaseHelper(Context context)
    {
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase)
    {
        sqLiteDatabase.execSQL("create table AccountList ( Seq INTEGER PRIMARY KEY AUTOINCREMENT" +
                                                        ", Date TEXT NOT NULL" +
                                                        ", Kinds TEXT NOT NULL" +
                                                        ", Classification TEXT NOT NULL" +
                                                        ", Contents TEXT NOT NULL" +
                                                        ", Money INT NOT NULL" +
                                                        ", Valid TEXT NOT NULL" +
                                                        ", Add_Date DATE" +
                                                        ", Upd_Date DATE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1)
    {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS AccountList");
        onCreate(sqLiteDatabase);
    }
}
