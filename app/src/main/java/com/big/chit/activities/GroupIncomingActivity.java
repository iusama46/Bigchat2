package com.big.chit.activities;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.big.chit.BaseApplication;
import com.big.chit.R;
import com.big.chit.models.Contact;
import com.big.chit.models.Group;
import com.big.chit.models.Status;
import com.big.chit.models.User;
import com.big.chit.utils.AudioPlayer;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Agora Calling SDK and other calling functionality Added by Ussama Iftikhar on 12-April-2021.
 * Email iusama46@gmail.com
 * Email iusama466@gmail.com
 * Github https://github.com/iusama46
 */
public class GroupIncomingActivity extends BaseActivity {
    private static final int REQUEST_PERMISSION_CALL = 953;
    private final String[] recordPermissions = {Manifest.permission.VIBRATE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

    boolean isVideo = false;
    String key = "";
    //    ValueEventListener valueEventListener;
//    DatabaseReference reference;
    String imageUrl = "";
    private AudioPlayer mAudioPlayer;
    private String callerName, roomToken, mRoomId;
    private final Runnable myRunnable = new Runnable() {
        @Override
        public void run() {
            String callType = isVideo ? "Video" : "Voice";
            createMissedCallNotification(callerName, "Gave You " + callType + " Group  Missed Call");
            Log.d("clima", "finish");
            finish();
        }
    };
    private String mCallerId;
    private final View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.answerButton:
                    if (recordPermissionsAvailable()) {
                        answerClicked();
                    } else {
                        ActivityCompat.requestPermissions(GroupIncomingActivity.this, recordPermissions, REQUEST_PERMISSION_CALL);
                    }
                    break;
                case R.id.declineButton:
                    declineClicked();
                    break;
            }
        }
    };
    private Handler myHandler;

    @Override
    public void onBackPressed() {

    }

    @Override
    protected void onStart() {
        super.onStart();
//        reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego").child(userMe.getId());
//        try {
//            valueEventListener = reference.child(key).addValueEventListener(new ValueEventListener() {
//                @Override
//                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//
//                    if (dataSnapshot.getChildrenCount() > 0) {
//
//                        if (dataSnapshot.child("call_status").getValue() != null) {
//
//                            int callStatus = Integer.parseInt(String.valueOf(dataSnapshot.child("call_status").getValue()));
//
//
//                            if (callStatus != 0) {
//                                mAudioPlayer.stopRingtone();
//                                finish();
//                            }
//                        }
//
//                    }
//                }
//
//
//                @Override
//                public void onCancelled(@NonNull DatabaseError databaseError) {
//
//                }
//            });
//        } catch (Exception e) {
//
//        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call_screen);

        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        mAudioPlayer = new AudioPlayer(this);
        mAudioPlayer.playRingtone();

        Intent intent = getIntent();

        mCallerId = intent.getStringExtra("caller_id");
        mRoomId = intent.getStringExtra("room_id");
        roomToken = intent.getStringExtra("room_token");
        callerName = intent.getStringExtra("caller_name");
        isVideo = intent.getBooleanExtra("is_video", false);
        key = intent.getStringExtra("key");

        findViewById(R.id.answerButton).setOnClickListener(mClickListener);
        findViewById(R.id.declineButton).setOnClickListener(mClickListener);
        onZegoConnected();
    }

    @Override
    protected void onDestroy() {
        mAudioPlayer.stopRingtone();
//        if (valueEventListener != null)
//            reference.removeEventListener(valueEventListener);
        if (myHandler != null && myRunnable != null)
            myHandler.removeCallbacks(myRunnable);
        super.onDestroy();
    }

    void onZegoConnected() {

        TextView remoteUser = findViewById(R.id.remoteUser);
        ImageView userImage1 = findViewById(R.id.userImage1);
        ImageView userImage2 = findViewById(R.id.userImage2);
        Log.d("clima remote", callerName);
        remoteUser.setText(callerName);

        try {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("data").child("groups");
            databaseReference.child(mCallerId).child("image").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        Log.d("clima", mCallerId);
                        imageUrl = dataSnapshot.getValue().toString();
                        Log.d("clima:.", imageUrl);
                        //user.setImage(imageUrl);
                        if (!imageUrl.isEmpty()) {

                            Picasso.get()
                                    .load(imageUrl)
                                    .tag(this)
                                    .placeholder(R.drawable.ic_avatar)
                                    .into(userImage2);
                        } else {
                            userImage2.setBackgroundResource(R.drawable.ic_avatar);
                        }
                    } catch (Exception e) {
                        userImage2.setBackgroundResource(R.drawable.ic_avatar);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    userImage2.setBackgroundResource(R.drawable.ic_avatar);
                }
            });
        } catch (Exception e) {
            userImage2.setBackgroundResource(R.drawable.ic_avatar);
        }
        TextView callingType = findViewById(R.id.txt_calling);
        callingType.setText(getResources().getString(R.string.app_name) + (isVideo ? " Incoming Video Calling" : " Incoming Voice Calling"));

        myHandler = new Handler();
        myHandler.postDelayed(myRunnable, 12000);


    }

    private void answerClicked() {
        mAudioPlayer.stopRingtone();
        try {
            startActivity(GroupCallActivity.newIntent(this, group, mCallerId, "IN", isVideo, mRoomId, roomToken, key, callerName, imageUrl));
            finish();
        } catch (Exception e) {
            Log.e("CHECK", e.getMessage());

        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            answerClicked();
        } else {
            Toast.makeText(this, "This application needs permission to use your microphone to function properly.", Toast.LENGTH_LONG).show();
        }
    }

    private void declineClicked() {
        mAudioPlayer.stopRingtone();
//        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego").child(userMe.getId());
//        reference.child(key).child("call_status").setValue(3);

        finish();
    }

    private boolean recordPermissionsAvailable() {
        boolean available = true;
        for (String permission : recordPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                available = false;
                break;
            }
        }
        return available;
    }

    @Override
    void myUsersResult(ArrayList<User> myUsers) {

    }


    @Override
    void myContactsResult(ArrayList<Contact> myContacts) {

    }

    @Override
    void userAdded(User valueUser) {

    }

    @Override
    void groupAdded(Group valueGroup) {

    }

    @Override
    void userUpdated(User valueUser) {

    }

    @Override
    void groupUpdated(Group valueGroup) {

    }

    @Override
    void statusAdded(Status status) {

    }

    @Override
    void statusUpdated(Status status) {

    }

    public void createMissedCallNotification(String contactName, String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 56, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManagerCompat.from(GroupIncomingActivity.this).cancel(89);
        Uri ringUri = Settings.System.DEFAULT_RINGTONE_URI;

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(GroupIncomingActivity.this, BaseApplication.CALL)
                .setContentTitle(contactName)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_baseline_phone_missed_24)
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


    }
}
