package com.illusory.fileexplorer.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.illusory.fileexplorer.R;
import com.illusory.fileexplorer.fragments.FolderFragment;
import com.illusory.fileexplorer.fragments.StorageFragment;
import com.illusory.fileexplorer.models.ButtonBar;
import com.illusory.fileexplorer.models.Clipboard;
import com.illusory.fileexplorer.models.ToolBar;
import com.illusory.fileexplorer.utils.CrashUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {
    private ToolBar toolBar;
    private ButtonBar buttonBar;
    private StorageFragment storageFragment = null;
    private final Stack<FolderFragment> fragments = new Stack<>();
    private final Clipboard clipboard = new Clipboard();

    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.toolBar = new ToolBar(findViewById(R.id.folderName));
        this.buttonBar = new ButtonBar(findViewById(R.id.buttonBar), fragments);

        String[] storages = storages();
        if (storages.length > 1) {
            storageFragment = StorageFragment.newInstance(storages);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(R.id.fragmentContainer, storageFragment);
            transaction.addToBackStack(null);
            transaction.commitAllowingStateLoss();

            toolBar.update(getString(R.string.app_name));
        } else {
            String root = Environment.getExternalStorageDirectory().getAbsolutePath();
            FolderFragment folderFragment = FolderFragment.newInstance(root);
            addFragment(folderFragment, false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_WRITE_EXTERNAL_STORAGE: {
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    if (storageFragment != null) {
                        storageFragment.reload();
                    } else {
                        FolderFragment folderFragment = fragments.peek();
                        folderFragment.refreshFolder();
                    }
                } else {
                    finish();
                }
            }
        }
    }

    private String[] storages() {
        List<String> storages = new ArrayList<>();
        try {
            File[] externalStorageFiles = ContextCompat.getExternalFilesDirs(this, null);
            String base = String.format("/Android/data/%s/files", getPackageName());

            for (File file : externalStorageFiles) {
                try {
                    if (file != null) {
                        String path = file.getAbsolutePath();
                        if (path.contains(base)) {
                            String finalPath = path.replace(base, "");
                            if (validPath(finalPath)) {
                                storages.add(finalPath);
                            }
                        }
                    }
                } catch (Exception e) {
                    CrashUtils.report(e);
                }
            }
        } catch (Exception e) {
            CrashUtils.report(e);
        }

        String[] result = new String[storages.size()];
        storages.toArray(result);

        return result;
    }

    private boolean validPath(String path) {
        try {
            StatFs stat = new StatFs(path);
            stat.getBlockCount();

            return true;
        } catch (Exception e) {
            CrashUtils.report(e);

            return false;
        }
    }

    public void addFragment(FolderFragment fragment, boolean addToBackStack) {
        fragments.push(fragment);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (addToBackStack) {
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_right);
        }
        transaction.add(R.id.fragmentContainer, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commitAllowingStateLoss();

        toolBar.update(fragment);
    }

    private void removeFragment(FolderFragment fragment) {
        fragments.pop();

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_right);
        transaction.remove(fragment);
        transaction.commitAllowingStateLoss();

        if (!fragments.isEmpty()) {
            FolderFragment topFragment = fragments.peek();
            topFragment.refreshFolder();

            toolBar.update(topFragment);
        }
    }

    public Clipboard clipboard() {
        return clipboard;
    }

    public ButtonBar buttonBar() {
        return buttonBar;
    }

    @Override
    public void onBackPressed() {
        if (fragments.size() > 0) {
            FolderFragment fragment = fragments.peek();
            if (fragment.onBackPressed()) {
                if (storageFragment == null) {
                    if (fragments.size() > 1) {
                        removeFragment(fragment);
                    } else {
                        finish();
                    }
                } else {
                    removeFragment(fragment);
                    if (fragments.isEmpty()) {
                        toolBar.update(getString(R.string.app_name));
                        buttonBar.displayButtons(0, false, false, false, false);
                    }
                }
            }
        } else {
            finish();
        }
    }

    @Override
    @SuppressLint("MissingSuperCall")
    protected void onSaveInstanceState(Bundle outState) {
        // no call for super(). Bug on API Level > 11.
    }
}