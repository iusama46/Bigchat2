package com.big.chit.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.big.chit.R;
import com.big.chit.activities.ContactActivity;
import com.big.chit.activities.GroupIncomingActivity;
import com.big.chit.activities.IncomingCallScreenActivity;
import com.big.chit.activities.MainActivity;
import com.big.chit.models.LogCall;
import com.big.chit.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.Provider;
import java.util.HashMap;

/**
 * Created by Ussama Iftikhar on 07-Apr-2021.
 * Email iusama46@gmail.com
 * Email iusama466@gmail.com
 * Github https://github.com/iusama46
 */
public class ZegoService extends Service {

    private static final int REQUEST_PERMISSION_CALL = 751;
    private static final String CHANNEL_ID_USER_MISSCALL = "call_service";
    private static  boolean isCall = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        Intent intent1 = new Intent(this, IncomingCallScreenActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivities(this,0, new Intent[]{intent1},0);
       // return super.onStartCommand(intent, flags, startId);
        //notifyMisscall();


//       if(!isCall){
//           DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data");
//           String ph = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
//           //ph="+923104772882";
//           try {
//               reference.child("call_zego").child(ph).addValueEventListener(new ValueEventListener() {
//                   @Override
//                   public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//
////                       if (dataSnapshot.getChildrenCount() > 0) {
////                           if (dataSnapshot.getKey().equals(ph.toString())) {
////                               if( dataSnapshot.child("answered").getValue()==null || dataSnapshot.child("canceled").getValue()==null || dataSnapshot.child("video").getValue()==null
////                                       || dataSnapshot.child("isGroup").getValue()==null){
////                                   reference.child("call_zego").child(ph).removeValue();
////                                   return;
////                               }
////                               boolean value = (boolean) dataSnapshot.child("answered").getValue();
////                               boolean cancel = (boolean) dataSnapshot.child("canceled").getValue();
//////                            isVideo = (boolean) dataSnapshot.child("video").getValue();
//////                            callerId = dataSnapshot.child("uId").getValue().toString();
//////                            RoomId = dataSnapshot.child("room").getValue().toString();
//////                            name = dataSnapshot.child("name").getValue().toString();
////
////                               boolean isGroup = (boolean) dataSnapshot.child("isGroup").getValue();
////                               if (isGroup && value) {
////                                   if (value && !cancel) {
//////                                    //Toast.makeText(MainActivity.this, "true group", Toast.LENGTH_SHORT).show();
//////                                    startActivity(new Intent(this, GroupIncomingActivity.class));
////
//////                                    return;
////                                   } else if(!value &&!cancel){
//////                                    notifyMisscall();
//////                                    return;
////                                   }
////
////                               } else {
////                                   if (value && !cancel && !isGroup) {
////                                       //Toast.makeText(MainActivity.this, "true", Toast.LENGTH_SHORT).show();
////                                       //startActivity(new Intent(MainActivity.this, IncomingCallScreenActivity.class));
////                                       isCall =true;
////                                       Intent dialogIntent = new Intent(ZegoService.this, IncomingCallScreenActivity.class);
////                                       dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////                                       startActivity(dialogIntent);
////
////                                       return;
////                                   }
//////                                 else if(!value &&!cancel){
//////                                    //Toast.makeText(MainActivity.this, "missed Call", Toast.LENGTH_SHORT).show();
//////                                    HashMap<String, User> myUsers = helper.getCacheMyUsers();
//////                                    if (myUsers != null && myUsers.containsKey(callerId)) {
//////                                        user = myUsers.get(callerId);
//////                                    }
//////                                }
//////                                if (!isGroup && !value ) {
//////                                    LogCall logCall = null;
//////                                    if (user == null) {
//////                                        user = new User(MainActivity.callerId, MainActivity.callerId, getString(R.string.app_name), "");
//////                                    }
//////
//////                                    rChatDb.beginTransaction();
//////                                    logCall = new LogCall(user, System.currentTimeMillis(), 0, false, "cause.toString()", userMe.getId(), user.getId());
//////                                    rChatDb.copyToRealm(logCall);
//////                                    rChatDb.commitTransaction();
//////                                    notifyMisscall(logCall);
//////                                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego");
//////                                    reference.child(userMe.getId()).removeValue();
//////                                }
////                               }
////                           }
////                       }
//
//                   }
//
//                   @Override
//                   public void onCancelled(@NonNull DatabaseError databaseError) {
//
//                   }
//               });
//           } catch (Exception e) {
//
//           }
//           Toast.makeText(this, "us", Toast.LENGTH_SHORT).show();
//       }



        return START_STICKY;

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {//stopForeground();

        super.onDestroy();
    }

    private void notifyMisscall() {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 56, new Intent(this, ContactActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notificationBuilder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_USER_MISSCALL, "PakOne Calling", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
            notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID_USER_MISSCALL);
        } else {
            notificationBuilder = new NotificationCompat.Builder(this);
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notificationBuilder.setSmallIcon(R.drawable.ic_logo_)
                .setContentTitle("logCall.getUser().getNameToDisplay()")
                .setContentText(" Calling you")
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);
        int msgId = 0;
        try {
          //  msgId = Integer.parseInt(logCall.getUser().getId());
        } catch (NumberFormatException ex) {
            //msgId = Integer.parseInt(logCall.getUser().getId().substring(logCall.getUser().getId().length() / 2));
        }
        notificationManager.notify(msgId, notificationBuilder.build());

        //startForeground(751,notificationBuilder.);
    }
}
