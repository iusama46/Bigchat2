package com.big.chit.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.big.chit.BaseApplication;
import com.big.chit.R;
import com.big.chit.activities.CallListActivity;
import com.big.chit.activities.IncomingCallScreenActivity;
import com.big.chit.utils.Helper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    /**
     * Created by Ussama Iftikhar on 11-Apr-2021.
     * Email iusama46@gmail.com
     * Email iusama466@gmail.com
     * Github https://github.com/iusama46
     */
    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.e("clima", s);
    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
        Log.d("clima", "deleted");
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.e("clima", "onMessageReceived");
        if (remoteMessage.getNotification() != null) {
            Log.d("clima", "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
        Log.d("clima", "From: " + remoteMessage.getFrom());


//        Notification notification = new NotificationCompat.Builder(this, BaseApplication.CALL)
//                .setContentTitle("test").
//                        setSmallIcon(R.drawable.camera_icon).
//                        setContentText("call").
//                        build();
//        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
//        manager.notify(171, notification);

        if (remoteMessage.getData().size() > 0) {
            Log.e("clima", "onMessageReceived");
            Log.e("clima", "onMessageReceivedsize " + remoteMessage.getData().size());
            Log.e("clima", "onMessageReceived" + remoteMessage.getData().toString());
            for (String key : remoteMessage.getData().keySet()) {
                Log.d("clima " + key, " " + remoteMessage.getData().get(key));
            }
            if (new Helper(this).isLoggedIn()) {
                if (remoteMessage.getData().get("is_call") != null && Boolean.parseBoolean(remoteMessage.getData().get("is_call"))) {
                    if (remoteMessage.getData().get("missed_call") != null && !Boolean.parseBoolean(remoteMessage.getData().get("missed_call"))) {
                        Intent intent = new Intent(this, IncomingCallScreenActivity.class);
                        intent.putExtra("is_video", Boolean.parseBoolean(remoteMessage.getData().get("is_video")));
                        intent.putExtra("stream_id", remoteMessage.getData().get("room_id").toString());
                        intent.putExtra("room_id", remoteMessage.getData().get("room_id").toString());
                        intent.putExtra("caller_id", remoteMessage.getData().get("caller_id").toString());
                        //intent.putExtra("caller_id", "+923104772882")
                        //intent.putExtra("caller_id", getIntent().getStringExtra("caller_id").toString());;
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        Log.d("clima", "dd");
                        return;
                    }
                }
            }
        }

        if (new Helper(this).isLoggedIn()) {
            Intent intent = new Intent(this, FirebaseChatService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 99, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 500, pendingIntent);
            Log.e("clima", "fcm scheduled");
        }
    }
}
