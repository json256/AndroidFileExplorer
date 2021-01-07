package com.illusory.fileexplorer.app;

import android.os.StrictMode;

import androidx.multidex.MultiDexApplication;

import com.illusory.fileexplorer.BuildConfig;
import com.illusory.fileexplorer.utils.CrashUtils;

import java.lang.Thread.UncaughtExceptionHandler;

public class FileExplorer extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();

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

    public class CustomExceptionHandler implements UncaughtExceptionHandler {
        private final UncaughtExceptionHandler defaultHandler;

        public CustomExceptionHandler() {
            this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        }

        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            CrashUtils.report(throwable);

            defaultHandler.uncaughtException(thread, throwable);
        }
    }
}