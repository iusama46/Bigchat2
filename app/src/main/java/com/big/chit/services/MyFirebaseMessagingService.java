package com.big.chit.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import com.big.chit.activities.IncomingCallScreenActivity;
import com.big.chit.utils.Helper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.e("clima", s);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.e("clima", "onMessageReceived");

        if (remoteMessage.getData().size() > 0) {
            Log.e("clima", "onMessageReceived");

            Log.e("clima", "onMessageReceivedsize " + remoteMessage.getData().size());
            Log.e("clima", "onMessageReceived" + remoteMessage.getData().toString());
            for (String key : remoteMessage.getData().keySet()) {
                Log.d("clima " + key, " " + remoteMessage.getData().get(key));
            }
//TODO
//            if (remoteMessage.getData().get("is_call") != null && Boolean.parseBoolean(remoteMessage.getData().get("is_call"))) {
//                if (remoteMessage.getData().get("missed_call") != null && !Boolean.parseBoolean(remoteMessage.getData().get("missed_call"))) {
//                    Intent intent = new Intent(this, IncomingCallScreenActivity.class);
//                    intent.putExtra("is_video",Boolean.parseBoolean(remoteMessage.getData().get("is_video")));
//                    intent.putExtra("stream_id",remoteMessage.getData().get("call_type").toString());
//                    intent.putExtra("room_id",remoteMessage.getData().get("call_type").toString());
//                    intent.putExtra("caller_id",remoteMessage.getData().get("caller_id").toString());
//                    return;
//                }
//            }
        }

        if (new Helper(this).isLoggedIn()) {
            Intent intent = new Intent(this, FirebaseChatService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 99, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 500, pendingIntent);
            Log.e("FCM", "scheduled");
        }
    }
}
