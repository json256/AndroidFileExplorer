package com.illusory.fileexplorer.fragments;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.illusory.fileexplorer.R;
import com.illusory.fileexplorer.adapters.FolderAdapter;
import com.illusory.fileexplorer.app.MainActivity;
import com.illusory.fileexplorer.models.Clipboard;
import com.illusory.fileexplorer.models.FileInfo;
import com.illusory.fileexplorer.utils.CrashUtils;
import com.illusory.fileexplorer.utils.Dialogs;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class FolderFragment extends Fragment {
    private static final String PARAMETER_FOLDER_PATH = "folder.path";

    private MainActivity mainActivity;
    private SwipeRefreshLayout swipeContainer;
    private ListView listView;
    private TextView labelNoItems;
    private FolderAdapter adapter;

    public static FolderFragment newInstance(String folderPath) {
        FolderFragment fragment = new FolderFragment();
        Bundle parameters = new Bundle();
        parameters.putSerializable(PARAMETER_FOLDER_PATH, folderPath);
        fragment.setArguments(parameters);

        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        mainActivity = (MainActivity) context;
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.screen_folder, container, false);

        swipeContainer = view.findViewById(R.id.swipeContainer);
        listView = view.findViewById(R.id.list);
        labelNoItems = view.findViewById(R.id.label_noItems);

        return view;
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public final void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        swipeContainer.setColorSchemeResources(R.color.blue1);
        swipeContainer.setOnRefreshListener(() -> {
            refreshFolder();
            swipeContainer.setRefreshing(false);
        });

        adapter = new FolderAdapter(mainActivity);

        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            FileInfo fileInfo = (FileInfo) parent.getItemAtPosition(position);

            if (adapter.isSelectionMode()) {
                adapter.updateSelection(fileInfo.toggleSelection());
                updateButtonBar();
            } else {
                if (fileInfo.isDirectory()) {
                    openFolder(fileInfo);
                } else {
                    openFile(fileInfo);
                }
            }
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            FileInfo fileInfo = (FileInfo) parent.getItemAtPosition(position);
            adapter.updateSelection(fileInfo.toggleSelection());
            updateButtonBar();

            return true;
        });

        listView.setOnTouchListener((v, event) -> {
            if ((event.getAction() == MotionEvent.ACTION_DOWN) &&
                    listView.pointToPosition((int) (event.getX() * event.getXPrecision()), (int) (event.getY() * event.getYPrecision())) == -1) {
                onBackPressed();

                return true;
            }

            return false;
        });

        refreshFolder();
    }

    public synchronized boolean onBackPressed() {
        if ((adapter != null) && adapter.isSelectionMode()) {
            unselectAll();

            return false;
        } else {
            return true;
        }
    }

    private void unselectAll() {
        adapter.unselectAll();
        updateButtonBar();
    }

    private void updateButtonBar() {
        Clipboard clipboard = mainActivity.clipboard();

        mainActivity.buttonBar().displayButtons(adapter.itemsSelected(), !adapter.allItemsSelected(), !clipboard.isEmpty() && clipboard.someExist() && !clipboard.hasParent(folder()), adapter.hasFiles(), true);
    }

    public String folderName() {
        return folder().getAbsolutePath();
    }

    private File folder() {
        String folderPath = parameter(PARAMETER_FOLDER_PATH, "/");

        return new File(folderPath);
    }

    private List<FileInfo> fileList() {
        File root = folder();
        File[] fileArray = root.listFiles();

        if (fileArray != null) {
            List<File> files = Arrays.asList(fileArray);

            Collections.sort(files, (lhs, rhs) -> {
                if (lhs.isDirectory() && !rhs.isDirectory()) {
                    return -1;
                } else if (!lhs.isDirectory() && rhs.isDirectory()) {
                    return 1;
                } else {
                    return lhs.getName().toLowerCase().compareTo(rhs.getName().toLowerCase());
                }
            });

            List<FileInfo> result = new ArrayList<>();

            for (File file : files) {
                if (file != null) {
                    result.add(new FileInfo(file));
                }
            }

            return result;
        } else {
            return new ArrayList<>();
        }
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    private <Type> Type parameter(String key, Type defaultValue) {
        Bundle extras = getArguments();

        if ((extras != null) && extras.containsKey(key)) {
            return (Type) extras.get(key);
        } else {
            return defaultValue;
        }
    }

    private void openFolder(FileInfo fileInfo) {
        FolderFragment folderFragment = FolderFragment.newInstance(fileInfo.path());

        mainActivity.addFragment(folderFragment, true);
    }

    private void openFile(FileInfo fileInfo) {
        try {
            String type = fileInfo.mimeType();
            Intent intent = openFileIntent(fileInfo.uri(context()), type);

            if (isResolvable(intent)) {
                startActivity(intent, R.string.open_unable);
            } else {
                defaultOpenFile(fileInfo);
            }
        } catch (Exception e) {
            defaultOpenFile(fileInfo);
        }
    }

    private Intent openFileIntent(Uri uri, String type) {
        // content://com.illusory.fileexplorer.provider/external_files/3D_MEDIA/Edited.mp4
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, type);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        return intent;
    }

    private void defaultOpenFile(FileInfo fileInfo) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, fileInfo.uri(context()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception e) {
            CrashUtils.report(e);

            showMessage(R.string.open_unable);
        }
    }

    public void onCut() {
        List<FileInfo> items = adapter.selectedItems(false);
        mainActivity.clipboard().cut(items);
        unselectAll();
    }

    public void onCopy() {
        List<FileInfo> items = adapter.selectedItems(false);
        mainActivity.clipboard().copy(items);
        unselectAll();
    }

    @SuppressLint("StaticFieldLeak")
    public void onPaste() {
        Clipboard clipboard = mainActivity.clipboard();

        String message = "";

        if (clipboard.isCut()) {
            message = getString(R.string.clipboard_cut);
        } else if (clipboard.isCopy()) {
            message = getString(R.string.clipboard_copy);
        }

        ProgressDialog dialog = Dialogs.progress(context(), message);

        ListeningExecutorService service = MoreExecutors
                .listeningDecorator(Executors.newFixedThreadPool(1));
        ListenableFuture<Void> future = service.submit(() -> {
            clipboard.paste(new FileInfo(folder()));
            return null;
        });
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                getActivity().runOnUiThread(() -> {
                    dialog.dismiss();
                    refreshFolder();
                });
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                throw new RuntimeException(t);
            }
        }, service);

//        new AsyncTask<Void, Void, Void>() {
//            @Override
//            protected Void doInBackground(Void... params) {
//                clipboard.paste(new FileInfo(folder()));
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(Void result) {
//                dialog.dismiss();
//                refreshFolder();
//            }
//        }.execute();
    }

    public void onSelectAll() {
        adapter.selectAll();
        updateButtonBar();
    }

    public void onRename() {
        List<FileInfo> items = adapter.selectedItems(false);

        if (items.size() == 1) {
            Dialogs.rename(context(), items.get(0), this::renameItem);
        }
    }

    public void onShare() {
        List<FileInfo> selectedItems = adapter.selectedItems(true);

        if (selectedItems.size() == 1) {
            shareSingle(selectedItems.get(0));
        } else if (!selectedItems.isEmpty()) {
            shareMultiple(selectedItems);
        }
    }

    private void shareSingle(FileInfo fileInfo) {
        try {
            String type = fileInfo.mimeType();
            Intent intent = shareSingleIntent(fileInfo.uri(context()), type);

            if (isResolvable(intent)) {
                startActivity(intent, R.string.shareFile_unable);
            } else {
                showMessage(R.string.shareFile_unable);
            }
        } catch (Exception e) {
            CrashUtils.report(e);

            showMessage(R.string.shareFile_unable);
        }
    }

    private Intent shareSingleIntent(Uri uri, String type) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(type);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        return intent;
    }

    private void shareMultiple(List<FileInfo> list) {
        try {
            Intent intent = shareMultipleIntent(list);

            if (isResolvable(intent)) {
                startActivity(intent, R.string.shareFiles_unable);
            } else {
                showMessage(R.string.shareFiles_unable);
            }
        } catch (Exception e) {
            CrashUtils.report(e);

            showMessage(R.string.shareFiles_unable);
        }
    }

    private Intent shareMultipleIntent(List<FileInfo> list) {
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        ArrayList<Uri> files = new ArrayList<>();

        for (FileInfo fileInfo : list) {
            files.add(fileInfo.uri(context()));
        }

        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);

        return Intent.createChooser(intent, getString(R.string.shareFile_title));
    }

    public void onDelete() {
        Dialogs.delete(context(), adapter, this::deleteSelected);
    }

    public void onCreate() {
        Dialogs.create(context(), this::createFolder);
    }

    private void createFolder(String name) {
        File parent = folder();
        File newFolder = new File(parent, name);

        if (newFolder.mkdir()) {
            refreshFolder();
        } else {
            showMessage(R.string.create_error);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void deleteSelected(List<FileInfo> selectedItems) {
        ProgressDialog dialog = Dialogs.progress(context(), getString(R.string.delete_deleting));

        ListeningExecutorService service = MoreExecutors
                .listeningDecorator(Executors.newFixedThreadPool(1));
        ListenableFuture<Boolean> future = service.submit(() -> {
            boolean allDeleted = true;

            for (FileInfo fileInfo : selectedItems) {
                if (!fileInfo.delete()) {
                    allDeleted = false;
                }
            }

            return allDeleted;
        });
        Futures.addCallback(future, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(@Nullable Boolean result) {
                getActivity().runOnUiThread(() -> {
                    dialog.dismiss();
                    refreshFolder();

                    if (!result) {
                        showMessage(R.string.delete_error);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                throw new RuntimeException(t);
            }
        }, service);

//        new AsyncTask<Void, Void, Boolean>() {
//            @Override
//            protected Boolean doInBackground(Void... params) {
//                boolean allDeleted = true;
//
//                for (FileInfo fileInfo : selectedItems) {
//                    if (!fileInfo.delete()) {
//                        allDeleted = false;
//                    }
//                }
//
//                return allDeleted;
//            }
//
//            @Override
//            protected void onPostExecute(Boolean result) {
//                dialog.dismiss();
//                refreshFolder();
//
//                if (!result) {
//                    showMessage(R.string.delete_error);
//                }
//            }
//        }.execute();
    }

    private void renameItem(FileInfo fileInfo, String newName) {
        if (fileInfo.rename(newName)) {
            refreshFolder();
        } else {
            showMessage(R.string.rename_error);
        }
    }

    private void showMessage(@StringRes int text) {
        Toast.makeText(context(), text, Toast.LENGTH_SHORT).show();
    }

    public void refreshFolder() {
        List<FileInfo> files = fileList();
        adapter.setData(files);
        updateButtonBar();

        if (files.isEmpty()) {
            listView.setVisibility(View.GONE);
            labelNoItems.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            labelNoItems.setVisibility(View.GONE);
        }
    }

    private void startActivity(Intent intent, @StringRes int resId) {
        try {
            startActivity(intent);
        } catch (Exception e) {
            CrashUtils.report(e);

            showMessage(resId);
        }
    }

    private boolean isResolvable(Intent intent) {
        PackageManager manager = mainActivity.getPackageManager();
        @SuppressLint("QueryPermissionsNeeded") List<ResolveInfo> resolveInfo = manager.queryIntentActivities(intent, 0);

        return !resolveInfo.isEmpty();
    }

    private Context context() {
        Context context = getContext();

        if (context != null) {
            return context;
        } else {
            Context fragmentActivity = getActivity();

            if (fragmentActivity != null) {
                return fragmentActivity;
            } else {
                return mainActivity;
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // no call for super(). Bug on API Level > 11.
    }
}