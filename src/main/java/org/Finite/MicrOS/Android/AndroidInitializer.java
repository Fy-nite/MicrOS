package org.Finite.MicrOS.Android;

import android.app.Application;
import android.content.Context;

public class AndroidInitializer extends Application {
    private static Context context;

    public void onCreate() {
        super.onCreate();
        AndroidInitializer.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return AndroidInitializer.context;
    }
}
