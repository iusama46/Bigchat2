package com.big.chit.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.big.chit.BaseApplication;
import com.big.chit.R;
import com.big.chit.activities.GroupIncomingActivity;
import com.big.chit.activities.IncomingCallScreenActivity;
import com.big.chit.activities.MainActivity;
import com.big.chit.models.LogCall;
import com.big.chit.models.User;
import com.big.chit.utils.Helper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;


/**
 * Created by Ussama Iftikhar on 11-Apr-2021.
 * Email iusama46@gmail.com
 * Email iusama466@gmail.com
 * Github https://github.com/iusama46
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.e("clima", s);
    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.e("clima", "onMessageReceived");
        Log.d("clima", remoteMessage.getData().toString());
        if (new Helper(this).isLoggedIn()) {
            if (remoteMessage.getData().size() > 0) {
                if (remoteMessage.getData().get("callerId") != null) {
                    if (remoteMessage.getData().get("callStatus") != null) {
                        String callStatus = remoteMessage.getData().get("callStatus");
                        if (callStatus.equals("0"))
                            handleCallNotifications(remoteMessage, false);
                        else if (callStatus.equals("1"))
                            handleCallNotifications(remoteMessage, true);
                        return;
                    }
                }
            }
        }

//        if (new Helper(this).isLoggedIn()) {
//            Intent intent = new Intent(this, FirebaseChatService.class);
//            PendingIntent pendingIntent = PendingIntent.getService(this, 99, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
//            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 500, pendingIntent);
//            Log.e("clima", "fcm scheduled");
//        }
    }

    private void handleCallNotifications(RemoteMessage remoteMessage, boolean isMissedCall) {

        boolean isGroup = remoteMessage.getData().get("isGroupCall").equals("1");
        String callerId = remoteMessage.getData().get("callerId").toString();
        String contactName = "PakOne";

        boolean isVideo = remoteMessage.getData().get("isVideoCall").toString().equals("1");
        String callType = isVideo ? "Video" : "Voice";

        String roomId = remoteMessage.getData().get("channelName").toString();
        String roomToken = remoteMessage.getData().get("token").toString();

        String callerName = "PakOne";
        if (remoteMessage.getData().get("callerId") != null)
            callerName = remoteMessage.getData().get("callerId").toString();

        User userCaller = null;
        HashMap<String, User> myUsers = new Helper(MyFirebaseMessagingService.this).getCacheMyUsers();
        if (myUsers != null && myUsers.containsKey(callerId)) {
            userCaller = myUsers.get(callerId);
            contactName = userCaller.getNameToDisplay();

        } else {
            contactName = callerId;
            userCaller = new User(callerId, callerId, "", "");
            Log.d("clima user ", userCaller.getId());
        }

        if (isGroup && !isMissedCall) {
            Intent intent = new Intent(MyFirebaseMessagingService.this, GroupIncomingActivity.class);
            Log.d("clima id", callerId);
            intent.putExtra("is_video", isVideo);
            intent.putExtra("room_token", roomToken);
            intent.putExtra("room_id", roomId);
            intent.putExtra("caller_id", callerId);
            intent.putExtra("caller_name", callerName);
            //todo
            intent.putExtra("key", "dataSnapshot.getKey()");
            createNotificationForCall(intent, callerName, "Incoming Group " + callType + " Call");
        } else if (!isGroup && !isMissedCall) {
            Intent intent = new Intent(MyFirebaseMessagingService.this, IncomingCallScreenActivity.class);

            Log.d("clima id", callerId);

            intent.putExtra("is_video", isVideo);
            intent.putExtra("room_token", roomToken);
            intent.putExtra("room_id", roomId);
            intent.putExtra("caller_id", callerId);
            //todo
            intent.putExtra("key", "dataSnapshot.getKey()");
            intent.putExtra("user", userCaller);

            createNotificationForCall(intent, contactName, "Incoming " + callType + " Call");
        }


        if (isMissedCall) {

            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 56, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationManagerCompat.from(MyFirebaseMessagingService.this).cancel(89);
            //NotificationManagerCompat.from(MyFirebaseMessagingService.this).;
            Uri ringUri = Settings.System.DEFAULT_RINGTONE_URI;

            String missedCallName = isGroup ? callerName : contactName;
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MyFirebaseMessagingService.this, BaseApplication.CALL)
                    .setContentTitle(missedCallName)
                    .setContentText("Gave You " + callType + "  Missed Call")
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setDefaults(Notification.DEFAULT_SOUND)

                    .setAutoCancel(true)
                    .setSound(ringUri)
                    .setContentIntent(pendingIntent);
                    //.setFullScreenIntent(pendingIntent, true);
            Notification incomingCallNotification = notificationBuilder.build();
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.notify(29, incomingCallNotification);

            LogCall logCall = null;
            if (userCaller != null && !isGroup) {
//                rChatDb.beginTransaction();
//                logCall = new LogCall(userCaller, System.currentTimeMillis(), 0, false, "cause.toString()", userMe.getId(), userCaller.getId());
//                rChatDb.copyToRealm(logCall);
//                rChatDb.commitTransaction();
            }

        }
    }

    private void createNotificationForCall(Intent intent, String callerName, String message) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        stackBuilder.addNextIntentWithParentStack(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(92, PendingIntent.FLAG_UPDATE_CURRENT);
//        PendingIntent pendingIntent = PendingIntent.getActivity(MyFirebaseMessagingService.this,92,intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri ringUri = Settings.System.DEFAULT_RINGTONE_URI;
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MyFirebaseMessagingService.this, BaseApplication.CALL)
                .setContentTitle(callerName)
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                //.setSound(ringUri)
                .addAction(R.drawable.camera_icon,"Decline", pendingIntent)
                .addAction(R.drawable.camera_icon,"Answer", pendingIntent)
                .setFullScreenIntent(pendingIntent, true);
        Notification incomingCallNotification = notificationBuilder.build();
        //incomingCallNotification.notify();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(89, incomingCallNotification);
    }
}
