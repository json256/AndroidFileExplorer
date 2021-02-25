package com.illusory.fileexplorer.app;

import android.content.Intent;
import android.net.Uri;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.multidex.MultiDexApplication;

import com.illusory.fileexplorer.BuildConfig;
import com.illusory.fileexplorer.utils.CrashUtils;

import java.lang.Thread.UncaughtExceptionHandler;

public class FileExplorer extends MultiDexApplication {
    public static FileExplorer application;

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;

        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler());

        if (BuildConfig.DEBUG) {
            StrictMode.ThreadPolicy.Builder threadBuilder = new StrictMode.ThreadPolicy.Builder();
            threadBuilder.detectAll();
            threadBuilder.penaltyLog();
            StrictMode.setThreadPolicy(threadBuilder.build());

            StrictMode.VmPolicy.Builder vmBuilder = new StrictMode.VmPolicy.Builder();
            vmBuilder.detectAll();
            vmBuilder.penaltyLog();
            StrictMode.setVmPolicy(vmBuilder.build());
        }
    }

    public static class CustomExceptionHandler implements UncaughtExceptionHandler {
        private final UncaughtExceptionHandler defaultHandler;

        public CustomExceptionHandler() {
            this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        }

        @Override
        public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
            CrashUtils.report(throwable);

            defaultHandler.uncaughtException(thread, throwable);
        }
    }

    public void updateGallery(Uri uri) {
        Log.i("updateGallery", uri.toString());
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
    }
}