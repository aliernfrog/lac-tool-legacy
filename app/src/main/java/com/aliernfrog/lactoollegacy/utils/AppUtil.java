package com.aliernfrog.lactoollegacy.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@SuppressLint({"CommitPrefEdits", "ApplySharedPref", "ClickableViewAccessibility"})
public class AppUtil {

    public static String getVersName(Context context) throws Exception {
        PackageManager pm = context.getPackageManager();
        PackageInfo pInfo = pm.getPackageInfo(context.getPackageName(), 0);
        return pInfo.versionName;
    }

    public static Integer getVersCode(Context context) throws Exception {
        PackageManager pm = context.getPackageManager();
        PackageInfo pInfo = pm.getPackageInfo(context.getPackageName(), 0);
        return pInfo.versionCode;
    }

    public static void copyToClipboard(String string, Context context) {
        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("LAC Tool", string);
        manager.setPrimaryClip(clip);
    }

    public static Boolean getUpdates(Context context) throws Exception {
        SharedPreferences update = context.getSharedPreferences("APP_UPDATE", Context.MODE_PRIVATE);
        SharedPreferences config = context.getSharedPreferences("APP_CONFIG", Context.MODE_PRIVATE);
        SharedPreferences.Editor updateEdit = update.edit();
        String updateUrl = config.getString("updateUrl", "https://aliernfrog.github.io/lactool/update.json");
        String rawUpdate = WebUtil.getContentFromURL(updateUrl);
        JSONObject object = new JSONObject(rawUpdate);
        updateEdit.putInt("updateLatest", object.getInt("latest"));
        updateEdit.putString("updateDownload", object.getString("download"));
        updateEdit.putString("updateChangelog", object.getString("changelog"));
        updateEdit.putString("updateChangelogVersion", object.getString("changelogVersion"));
        updateEdit.putString("notes", object.getString("notes"));
        updateEdit.commit();
        return true;
    }

    @SuppressLint("SimpleDateFormat")
    public static String timeString(String format) {
        SimpleDateFormat frm = new SimpleDateFormat(format);
        Date now = Calendar.getInstance().getTime();
        return frm.format(now);
    }

    public static void clearTempData(String path) {
        File tempDir = new File(path);
        File[] files = tempDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) FileUtil.deleteDirectoryContent(file);
                if (file.isFile()) file.delete();
            }
        }
    }

    public static void devLog(String toLog, TextView logView) {
        if (logView.getVisibility() == View.VISIBLE) {
            String tag = Thread.currentThread().getStackTrace()[3].getMethodName();
            if (tag.equals("devLog")) tag = Thread.currentThread().getStackTrace()[4].getMethodName();
            if (toLog.contains("Exception")) tag = "ERR-"+tag;
            String log = logView.getText().toString();
            String full = log+"["+tag+"] "+toLog+"\n\n";
            logView.setText(full);
        }
    }

    public static void toggleView(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
        }
    }

    public static void afterTextChanged(EditText view, Runnable runnable) {
        view.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                runnable.run();
            }
        });
    }

    public static void handleOnPressEvent(View view, MotionEvent event, @Nullable Runnable onClick) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 0.9f);
                ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.9f);
                scaleDownX.setDuration(100);
                scaleDownY.setDuration(100);
                AnimatorSet scaleDown = new AnimatorSet();
                scaleDown.play(scaleDownX).with(scaleDownY);
                scaleDown.start();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (onClick != null && event.getAction() == MotionEvent.ACTION_UP) onClick.run();
                ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1f);
                ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f);
                scaleUpX.setDuration(100);
                scaleUpY.setDuration(100);
                AnimatorSet scaleUp = new AnimatorSet();
                scaleUp.play(scaleUpX).with(scaleUpY);
                scaleUp.start();
                break;
        }
    }

    public static void handleOnPressEvent(View view, @Nullable Runnable onClick) {
        view.setOnTouchListener((v, event) -> {
            handleOnPressEvent(v, event, onClick);
            return true;
        });
    }

    public static void handleOnPressEvent(View view) {
        handleOnPressEvent(view, null);
    }
}
