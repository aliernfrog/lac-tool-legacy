package com.aliernfrog.lactoollegacy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.aliernfrog.lactoollegacy.utils.AppUtil;
import com.aliernfrog.lactoollegacy.utils.FileUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;

public class WallpaperActivity extends AppCompatActivity {
    Toolbar toolbar;
    FloatingActionButton saveButton;
    TextView helpText;
    LinearLayout actionsLinear;
    Button pickWallpaperButton;
    LinearLayout pickedWallpaperLinear;
    ImageView pickedWallpaperImage;
    Button importWallpaperButton;
    LinearLayout rootLinear;
    TextView debugText;

    SharedPreferences prefsUpdate;
    SharedPreferences prefsConfig;

    Integer REQUEST_PICK_WALLPAPER = 1;

    String currentPath;
    String lacPath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper);

        toolbar = findViewById(R.id.wallpaper_toolbar);
        saveButton = findViewById(R.id.wallpaper_save);
        helpText = findViewById(R.id.wallpaper_helpText);
        actionsLinear = findViewById(R.id.wallpaper_actionsLinear);
        pickWallpaperButton = findViewById(R.id.wallpaper_pickFile);
        pickedWallpaperLinear = findViewById(R.id.wallpaper_picked_linear);
        pickedWallpaperImage = findViewById(R.id.wallpaper_picked_image);
        importWallpaperButton = findViewById(R.id.wallpaper_picked_import);
        rootLinear = findViewById(R.id.wallpaper_rootLinear);
        debugText = findViewById(R.id.wallpaper_debug);

        prefsUpdate = getSharedPreferences("APP_UPDATE", MODE_PRIVATE);
        prefsConfig = getSharedPreferences("APP_CONFIG", MODE_PRIVATE);

        lacPath = prefsUpdate.getString("path-wallpapers", "");

        if (prefsConfig.getBoolean("enableDebug", false)) debugText.setVisibility(View.VISIBLE);

        devLog("WallpaperActivity started");
        setListeners();
        devLog("lacPath: "+lacPath);
        getImportedWallpapers();
    }

    public void getWp(String path) {
        devLog("attempting to read: "+path);
        currentPath = path;
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        pickedWallpaperImage.setImageBitmap(bitmap);
        pickedWallpaperLinear.setVisibility(View.VISIBLE);
        devLog("done reading");
    }

    @SuppressWarnings("deprecation")
    public void pickWallpaperFile() {
        devLog("attempting to pick a file with request code: "+REQUEST_PICK_WALLPAPER);
        Intent intent = new Intent(this, FilePickerActivity.class);
        intent.putExtra("FILE_TYPE_SAF", "image/*");
        intent.putExtra("FILE_TYPE_INAPP", new String[]{"jpg","jpeg","png"});
        startActivityForResult(intent, REQUEST_PICK_WALLPAPER);
    }

    public void importWallpaper() {
        devLog("attempting to import chosen wallpaper");
        File file = new File(currentPath);
        String name = FileUtil.removeExtension(file.getName());
        String dest = lacPath+"/"+name+".jpg";
        copyFile(currentPath, dest, true);
        devLog("imported the wallpaper");
        pickedWallpaperLinear.setVisibility(View.GONE);
        getImportedWallpapers();
    }

    public void getImportedWallpapers() {
        devLog("attempting to get imported wallpapers");
        rootLinear.removeAllViews();
        File lacFolder = new File(lacPath);
        File[] files = lacFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                ViewGroup viewGroup = (ViewGroup) getLayoutInflater().inflate(R.layout.inflate_wallpaper, rootLinear, false);
                LinearLayout bg = viewGroup.findViewById(R.id.wp_bg);
                TextView name = viewGroup.findViewById(R.id.wp_name);
                ImageView image = viewGroup.findViewById(R.id.wp_image);
                Button copyUrl = viewGroup.findViewById(R.id.wp_copyUrl);
                Button delete = viewGroup.findViewById(R.id.wp_delete);
                AppUtil.handleOnPressEvent(bg);
                name.setText(file.getName());
                image.setImageBitmap(BitmapFactory.decodeFile(file.getPath()));
                AppUtil.handleOnPressEvent(copyUrl, () -> {
                    AppUtil.copyToClipboard("file://"+lacPath+"/"+file.getName(), getApplicationContext());
                    Toast.makeText(getApplicationContext(), R.string.info_done, Toast.LENGTH_SHORT).show();
                });
                AppUtil.handleOnPressEvent(delete, () -> {
                    devLog("deleting: "+file.getPath());
                    file.delete();
                    rootLinear.removeView(viewGroup);
                    Toast.makeText(getApplicationContext(), R.string.info_done, Toast.LENGTH_SHORT).show();
                });
                rootLinear.addView(viewGroup);
            }
        }
    }

    public void copyFile(String source, String destination, Boolean toastResult) {
        devLog("attempting to copy "+source+" to "+destination);
        try {
            FileUtil.copyFile(source, destination);
            devLog("copied successfully");
            if (toastResult) Toast.makeText(getApplicationContext(), R.string.info_done, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            devLog(e.toString());
            if (toastResult) Toast.makeText(getApplicationContext(), R.string.info_error, Toast.LENGTH_SHORT).show();
        }
    }

    void devLog(String toLog) {
        AppUtil.devLog(toLog, debugText);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        boolean hasData = data != null;
        devLog(requestCode+": hasData = "+hasData);
        if (!hasData) return;
        if (requestCode == REQUEST_PICK_WALLPAPER) {
            getWp(data.getStringExtra("path"));
        } else {
            devLog("result is not handled");
        }
    }

    void setListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());
        AppUtil.handleOnPressEvent(saveButton, this::finish);
        AppUtil.handleOnPressEvent(helpText, () -> helpText.setVisibility(View.GONE));
        AppUtil.handleOnPressEvent(actionsLinear);
        AppUtil.handleOnPressEvent(pickWallpaperButton, this::pickWallpaperFile);
        AppUtil.handleOnPressEvent(pickedWallpaperLinear);
        AppUtil.handleOnPressEvent(importWallpaperButton, this::importWallpaper);
    }
}
