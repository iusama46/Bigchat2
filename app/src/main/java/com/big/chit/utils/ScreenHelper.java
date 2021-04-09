package com.big.chit.utils;

/**
 * Created by Ussama Iftikhar on 05-Apr-2021.
 * Email iusama46@gmail.com
 * Email iusama466@gmail.com
 * Github https://github.com/iusama46
 */
import android.app.Application;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class ScreenHelper {
    private volatile static ScreenHelper singleton;
    WindowManager windowManager;
    DisplayMetrics outMetrics;
    private ScreenHelper (Application application){
        windowManager = (WindowManager) application.getSystemService(application.WINDOW_SERVICE);
        outMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(outMetrics);
    }
    public static ScreenHelper getSingleton(Application application) {
        if (singleton == null) {
            synchronized (ScreenHelper.class) {
                if (singleton == null) {
                    singleton = new ScreenHelper(application);
                }
            }
        }
        return singleton;
    }
    public int  getScreenWidthPixels(){
        return outMetrics.widthPixels;
    }
    public int getScreenHeightPixels(){
        return  outMetrics.heightPixels;
    }
}

