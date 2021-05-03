package com.big.chit.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.big.chit.models.User;
import com.big.chit.utils.Helper;


public class SinchService extends Service {

    private SinchServiceInterface mSinchServiceInterface = new SinchServiceInterface();


    private Helper helper;
    private User userMe;


    @Override
    public void onCreate() {
        super.onCreate();
        helper = new Helper(this);
        userMe = helper.getLoggedInUser();
        if (User.validate(userMe)) {
            start(userMe.getId());
        }
    }

    @Override
    public void onDestroy() {
//        if (mSinchClient != null && mSinchClient.isStarted()) {
//            mSinchClient.terminate();
//        }
        super.onDestroy();
    }

    private void start(String userName) {

    }

    private void stop() {

    }


    @Override
    public IBinder onBind(Intent intent) {
        return mSinchServiceInterface;
    }

    public boolean isStarted() {
        return SinchService.this.isStarted();
    }

    public class SinchServiceInterface extends Binder {


    }


}




