package com.ytmfdw.imageview;

import android.app.Application;
import android.graphics.Bitmap;

/**
 * Created by Administrator on 2017/5/31.
 */
public class App extends Application {
    private Bitmap bitmap;
    private static App app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
    }

    public static App getApp() {
        return app;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return this.bitmap;
    }


}
