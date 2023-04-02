package com.aliernfrog.lactoollegacy;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aliernfrog.lactoollegacy.utils.AppUtil;
import com.aliernfrog.lactoollegacy.utils.FileUtil;

import java.io.File;

@SuppressLint("ClickableViewAccessibility")
public class ScreenshotsActivity extends AppCompatActivity {
    Toolbar toolbar;
    TextView noScreenshots;
    LinearLayout rootLinear;
    TextView log;

    String lacPath;

    SharedPreferences config;
    SharedPreferences update;

    @SuppressLint({"CommitPrefEdits", "InlinedApi"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screenshots);

        config = getSharedPreferences("APP_CONFIG", Context.MODE_PRIVATE);
        update = getSharedPreferences("APP_UPDATE", Context.MODE_PRIVATE);

        lacPath = update.getString("path-screenshots", null);

        toolbar = findViewById(R.id.screenshots_toolbar);
        noScreenshots = findViewById(R.id.screenshots_noScreenshots);
        rootLinear = findViewById(R.id.screenshots_linear_screenshots);
        log = findViewById(R.id.screenshots_log);

        if (config.getBoolean("enableDebug", false)) log.setVisibility(View.VISIBLE);

        devLog("ScreenshotsActivity started");

        setListeners();
        devLog("lacPath: "+lacPath);
        getScreenshots();
    }

    public void getScreenshots() {
        devLog("attempting to get screenshots");
        File file = new File(lacPath);
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files != null) {
                if (files.length < 1) noScreenshots.setVisibility(View.VISIBLE);
                for (File value : files) {
                    if (value.getName().endsWith(".jpg")) {
                        devLog("found: " + value.getName());
                        ViewGroup layout = (ViewGroup) getLayoutInflater().inflate(R.layout.inflate_screenshot, rootLinear, false);
                        setScreenshotView(layout, value);
                    }
                }
            }
        } else {
            devLog("screenshots file is null");
            noScreenshots.setVisibility(View.VISIBLE);
        }
    }

    public void setScreenshotView(ViewGroup layout, File file) {
        LinearLayout background = layout.findViewById(R.id.ss_bg);
        ImageView image = layout.findViewById(R.id.ss_image);
        Button share = layout.findViewById(R.id.ss_share);
        Button delete = layout.findViewById(R.id.ss_delete);
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
        image.setImageBitmap(bitmap);
        AppUtil.handleOnPressEvent(background);
        AppUtil.handleOnPressEvent(share, () -> shareFile(file.getPath()));
        AppUtil.handleOnPressEvent(delete, () -> deleteScreenshot(layout, file));
        rootLinear.addView(layout);
    }

    public void deleteScreenshot(ViewGroup layout, File file) {
        devLog("attempting to delete: "+file.getPath());
        file.delete();
        rootLinear.removeView(layout);
        Toast.makeText(getApplicationContext(), R.string.info_done, Toast.LENGTH_SHORT).show();
    }

    public void shareFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            devLog("attempting to share: "+path);
            Intent share = FileUtil.shareFile(path, "image/*", getApplicationContext());
            startActivity(Intent.createChooser(share, "Share Screenshot"));
        } else {
            Toast.makeText(getApplicationContext(), R.string.denied_doesntExist, Toast.LENGTH_SHORT).show();
            devLog("file does not exist");
        }
    }

    void devLog(String toLog) {
        AppUtil.devLog(toLog, log);
    }

    void setListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());
        AppUtil.handleOnPressEvent(noScreenshots);
    }
}