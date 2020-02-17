package com.chops.chopsaccount;

import android.app.Notification;
import android.content.ContentValues;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ShowNotificationListenerService extends NotificationListenerService {

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("NotificationListener", "[snowdeer] onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("NotificationListener", "[snowdeer] onStartCommand()");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("NotificationListener", "[snowdeer] onDestroy()");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification notificatin = sbn.getNotification();
        Bundle extras = notificatin.extras;
        String title = extras.getString(Notification.EXTRA_TITLE);
        //int smallIconRes = extras.getInt(Notification.EXTRA_SMALL_ICON);
        //Icon largeIcon = notificatin.getLargeIcon();//((Bitmap) extras.getParcelable(Notification.EXTRA_LARGE_ICON));
        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);

        //CharSequence subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
        //Log.i("NotificationListener", "[snowdeer] Title:" + title);
        //Log.i("NotificationListener", "[snowdeer] Text:" + text);
        //Log.i("NotificationListener", "[snowdeer] Sub Text:" + subText);


        //text = "02/17 13:41 032902-**-***560 조평식 스마트폰입금 1 잔액603,245";
        String strText = (String)text;
        Toast.makeText(getApplicationContext(),text,Toast.LENGTH_LONG);

        if(true)
        {
            Date dtToday = Calendar.getInstance().getTime();
            SimpleDateFormat sdfYMD = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            try
            {
                mfnDBInsert(sdfYMD.format(dtToday), "국민", strText.split(" ")[3], strText.split(" ")[4], (strText.split(" ")[4].contains("입금") ? "" : "-" )+ strText.split(" ")[5].replace(",", ""));
            }
            catch (Exception e)
            {
                mfnDBInsert(sdfYMD.format(dtToday), "오류", "오류", title + text, "0");
            }
        }

    }

    private long mfnDBInsert(String p_strDate, String p_strKinds, String p_strClassification, String p_strContents, String p_strMoney)
    {
        DatabaseHelper dbHelper;
        SQLiteDatabase db;

        dbHelper = new DatabaseHelper(this);
        try{
            db = dbHelper.getReadableDatabase();
        }catch (SQLException e){
            db = dbHelper.getWritableDatabase();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ContentValues cv = new ContentValues();
        cv.put("Date", p_strDate);
        cv.put("Kinds", p_strKinds);
        cv.put("Classification", p_strClassification);
        cv.put("Contents", p_strContents);
        cv.put("Money", p_strMoney);
        cv.put("Valid", "Y");
        cv.put("Add_Date",sdf.format(System.currentTimeMillis()));
        cv.put("Upd_Date",sdf.format(System.currentTimeMillis()));
        long lResult = db.insert("AccountList",null,cv);
        db.close();
        dbHelper.close();
        return  lResult;
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    Log.i("NotificationListener", "[snowdeer] onNotificationRemoved() - " + sbn.toString());
}

}