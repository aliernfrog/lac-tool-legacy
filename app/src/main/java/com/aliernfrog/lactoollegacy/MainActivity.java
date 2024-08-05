package com.aliernfrog.lactoollegacy;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Html;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aliernfrog.lactoollegacy.utils.AppUtil;

import java.io.File;

@SuppressLint({"CommitPrefEdits", "ClickableViewAccessibility"})
public class MainActivity extends AppCompatActivity {
    LinearLayout newApp;
    LinearLayout missingPerms;
    LinearLayout lacLinear;
    LinearLayout redirectMaps;
    LinearLayout redirectWallpapers;
    LinearLayout redirectScreenshots;
    LinearLayout appLinear;
    LinearLayout checkUpdates;
    LinearLayout redirectOptions;
    LinearLayout updateLinear;
    TextView updateLinearTitle;
    TextView updateLog;
    TextView log;

    Integer REQUEST_URI = 1;

    Boolean hasPerms;
    Integer version;

    SharedPreferences update;
    SharedPreferences config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        update = getSharedPreferences("APP_UPDATE", Context.MODE_PRIVATE);
        config = getSharedPreferences("APP_CONFIG", Context.MODE_PRIVATE);
        version = update.getInt("versionCode", 0);

        newApp = findViewById(R.id.main_newApp);
        missingPerms = findViewById(R.id.main_missingPerms);
        lacLinear = findViewById(R.id.main_optionsLac);
        redirectMaps = findViewById(R.id.main_maps);
        redirectWallpapers = findViewById(R.id.main_wallpapers);
        redirectScreenshots = findViewById(R.id.main_screenshots);
        appLinear = findViewById(R.id.main_optionsApp);
        checkUpdates = findViewById(R.id.main_checkUpdates);
        redirectOptions = findViewById(R.id.main_options);
        updateLinear = findViewById(R.id.main_update);
        updateLinearTitle = findViewById(R.id.main_update_title);
        updateLog = findViewById(R.id.main_update_description);
        log = findViewById(R.id.main_log);

        if (config.getBoolean("enableDebug", false)) log.setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT >= 21) newApp.setVisibility(View.VISIBLE);
        devLog("MainActivity started");

        checkUpdates(false);
        checkPerms();
        setListeners();
    }

    public void fetchUpdates() {
        devLog("attempting to fetch updates from "+config.getString("updateUrl", "default site"));
        try {
            if (AppUtil.getUpdates(getApplicationContext())) checkUpdates(true);
        } catch (Exception e) {
            e.printStackTrace();
            devLog(e.toString());
        }
    }

    public void checkUpdates(Boolean toastResult) {
        devLog("checking for updates");
        int latest = update.getInt("updateLatest", 0);
        String download = update.getString("updateDownload", null);
        String changelog = update.getString("updateChangelog", null);
        String changelogVersion = update.getString("updateChangelogVersion", null);
        String notes = update.getString("notes", null);
        boolean hasUpdate = latest > version;
        boolean linearVisible = false;
        String full = "";
        if (hasUpdate) {
            linearVisible = true;
            full = changelog+"<br /><br /><b>"+getString(R.string.optionsChangelogChangelog)+":</b> "+changelogVersion;
            updateLinearTitle.setVisibility(View.VISIBLE);
            AppUtil.handleOnPressEvent(updateLinear, () -> redirectURL(download));
            if (toastResult) Toast.makeText(getApplicationContext(), R.string.update_toastAvailable, Toast.LENGTH_SHORT).show();
        } else {
            if (notes != null && !notes.equals("")) {
                linearVisible = true;
                full = notes;
            }
            AppUtil.handleOnPressEvent(updateLinear);
            if (toastResult) Toast.makeText(getApplicationContext(), R.string.update_toastNoUpdates, Toast.LENGTH_SHORT).show();
        }
        updateLog.setText(Html.fromHtml(full));
        if (linearVisible) updateLinear.setVisibility(View.VISIBLE);
    }

    public void checkPerms() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                afterPermsDenied();
                devLog("permission denied, attempting to request permission");
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
            } else {
                devLog("permissions granted");
                afterPermsGranted();
            }
        } else {
            devLog("old SDK version detected");
            afterPermsGranted();
        }
    }

    void afterPermsGranted() {
        hasPerms = true;
        missingPerms.setVisibility(View.GONE);
        createFiles();
    }

    void afterPermsDenied() {
        hasPerms = false;
        missingPerms.setVisibility(View.VISIBLE);
    }

    public void createFiles() {
        try {
            File mapsFolder = new File(update.getString("path-maps", ""));
            File wallpapersFolder = new File(update.getString("path-wallpapers", ""));
            File screenshotsFolder = new File(update.getString("path-screenshots", ""));
            File appFolder = new File(update.getString("path-app", ""));
            File backupFolder = new File(appFolder.getPath()+"/backups/");
            File aBackupFolder = new File(appFolder.getPath()+"/auto-backups/");
            File nomedia = new File(appFolder.getPath()+"/.nomedia");
            if (!mapsFolder.exists()) mkdirs(mapsFolder);
            if (!wallpapersFolder.exists()) mkdirs(wallpapersFolder);
            if (!screenshotsFolder.exists()) mkdirs(screenshotsFolder);
            if (!appFolder.exists()) mkdirs(appFolder);
            if (!backupFolder.exists()) mkdirs(backupFolder);
            if (!aBackupFolder.exists()) mkdirs(aBackupFolder);
            if (!nomedia.exists()) nomedia.createNewFile();
        } catch (Exception e) {
            devLog(e.toString());
        }
    }

    public void mkdirs(File mk) {
        boolean state = mk.mkdirs();
        devLog(mk.getPath()+" //"+state);
    }

    public void switchActivity(Class<?> i, Boolean allowWithoutPerms) {
        if (!allowWithoutPerms && !hasPerms) {
            devLog("no required permissions, checking again");
            checkPerms();
        } else {
            Intent intent = new Intent(this.getApplicationContext(), i);
            devLog("attempting to redirect to "+i.toString());
            startActivity(intent);
        }
    }

    public void redirectURL(String url) {
        devLog("attempting to redirect to:"+url);
        Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse(url));
        startActivity(viewIntent);
    }

    void devLog(String toLog) {
        AppUtil.devLog(toLog, log);
    }

    @SuppressLint("NewApi")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        devLog(requestCode+": hasData = "+(data != null));
        if (requestCode == REQUEST_URI && data != null) {
            int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            grantUriPermission(getApplicationContext().getPackageName(), data.getData(), takeFlags);
            getApplicationContext().getContentResolver().takePersistableUriPermission(data.getData(), takeFlags);
            devLog("took uri permissions");
        }
    }

    public void setListeners() {
        AppUtil.handleOnPressEvent(newApp, () -> redirectURL("https://github.com/aliernfrog/lac-tool/releases"));
        AppUtil.handleOnPressEvent(missingPerms, this::checkPerms);
        AppUtil.handleOnPressEvent(lacLinear);
        AppUtil.handleOnPressEvent(redirectMaps, () -> switchActivity(MapsActivity.class, false));
        AppUtil.handleOnPressEvent(redirectWallpapers, () -> switchActivity(WallpaperActivity.class, false));
        AppUtil.handleOnPressEvent(redirectScreenshots, () -> switchActivity(ScreenshotsActivity.class, false));
        AppUtil.handleOnPressEvent(appLinear);
        AppUtil.handleOnPressEvent(checkUpdates, this::fetchUpdates);
        AppUtil.handleOnPressEvent(redirectOptions, () -> switchActivity(OptionsActivity.class, true));
    }
}