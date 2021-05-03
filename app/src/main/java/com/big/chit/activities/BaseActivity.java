package com.big.chit.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import com.big.chit.BaseApplication;
import com.big.chit.models.Contact;
import com.big.chit.models.Group;
import com.big.chit.models.Status;
import com.big.chit.models.User;
import com.big.chit.services.FirebaseChatService;
import com.big.chit.services.SinchService;
import com.big.chit.utils.Helper;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import io.realm.Realm;


public abstract class BaseActivity extends AppCompatActivity implements ServiceConnection {
    //Group updates receiver(new or updated)
    private final BroadcastReceiver groupReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(Helper.BROADCAST_GROUP)) {
                Group group = intent.getParcelableExtra("data");
                String what = intent.getStringExtra("what");
                switch (what) {
                    case "added":
                        groupAdded(group);
                        break;
                    case "changed":
                        groupUpdated(group);
                        break;
                }
            }
        }
    };
    //User updates receiver(new or updated)
    private final BroadcastReceiver userReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(Helper.BROADCAST_USER)) {
                User user = intent.getParcelableExtra("data");
                String what = intent.getStringExtra("what");
                switch (what) {
                    case "added":
                        userAdded(user);
                        break;
                    case "changed":
                        userUpdated(user);
                        Intent local = new Intent("custom-event-name");
                        local.putExtra("status", user.getStatus());
                        LocalBroadcastManager.getInstance(BaseActivity.this).sendBroadcast(local);
                        break;
                }
            }
        }
    };
    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(Helper.BROADCAST_STATUS)) {
                Status status = intent.getParcelableExtra("data");
                String what = intent.getStringExtra("what");
                switch (what) {
                    case "added":
                        statusAdded(status);
                        break;
                    case "changed":
                        statusUpdated(status);
                        break;
                }
            }
        }
    };
    private final BroadcastReceiver myUsersReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<User> myUsers = intent.getParcelableArrayListExtra("data");
            if (myUsers != null) {
                myUsersResult(myUsers);
            }
        }
    };
    private final BroadcastReceiver myContactsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<Contact> myContacts = intent.getParcelableArrayListExtra("data");
            if (myContacts != null) {
                myContactsResult(myContacts);
            }
        }
    };
    protected String[] permissionsRecord = {Manifest.permission.VIBRATE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    protected String[] permissionsContact = {Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    protected String[] permissionsStorage = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    protected String[] permissionsCamera = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    protected String[] permissionsSinch = {Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS, Manifest.permission.READ_PHONE_STATE};
    protected User userMe, user;
    protected Group group;
//    private SinchService.SinchServiceInterface mSinchServiceInterface;
    protected Helper helper;
    protected Realm rChatDb;
    protected DatabaseReference usersRef, groupRef, chatRef, statusRef, callRef;
    private FirebaseApp secondApp;
    private FirebaseDatabase secondDatabase;

    abstract void myUsersResult(ArrayList<User> myUsers);

    abstract void myContactsResult(ArrayList<Contact> myContacts);

    abstract void userAdded(User valueUser);

    abstract void groupAdded(Group valueGroup);

    abstract void userUpdated(User valueUser);

    abstract void groupUpdated(Group valueGroup);

    abstract void statusAdded(Status status);

    abstract void statusUpdated(Status status);

    //abstract void onSinchConnected();

    //abstract void onSinchDisconnected();



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        helper = new Helper(this);
        userMe = helper.getLoggedInUser();
        Realm.init(this);
        rChatDb = Helper.getRealmInstance();
        usersRef = BaseApplication.getUserRef();
        groupRef = BaseApplication.getGroupRef();
        chatRef = BaseApplication.getChatRef();
        statusRef = BaseApplication.getStatusRef();
        callRef = BaseApplication.getCallRef();

        Intent intent = new Intent(this, FirebaseChatService.class);


        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        startService(new Intent(this, FirebaseChatService.class));
           getApplicationContext().bindService(new Intent(this, SinchService.class), this, BIND_AUTO_CREATE);
    }
    private SinchService.SinchServiceInterface mSinchServiceInterface;
    protected SinchService.SinchServiceInterface getSinchServiceInterface() {
        return mSinchServiceInterface;
    }
    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(userReceiver, new IntentFilter(Helper.BROADCAST_USER));
        localBroadcastManager.registerReceiver(groupReceiver, new IntentFilter(Helper.BROADCAST_GROUP));
        localBroadcastManager.registerReceiver(myContactsReceiver, new IntentFilter(Helper.BROADCAST_MY_CONTACTS));
        localBroadcastManager.registerReceiver(myUsersReceiver, new IntentFilter(Helper.BROADCAST_MY_USERS));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.unregisterReceiver(userReceiver);
        localBroadcastManager.unregisterReceiver(groupReceiver);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mSinchServiceInterface = (SinchService.SinchServiceInterface) iBinder;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mSinchServiceInterface = null;
    }

    protected boolean permissionsAvailable(String[] permissions) {
        boolean granted = true;
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                granted = false;
                break;
            }
        }
        return granted;
    }
}
