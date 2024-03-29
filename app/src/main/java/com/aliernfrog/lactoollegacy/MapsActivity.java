package com.aliernfrog.lactoollegacy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.aliernfrog.lactoollegacy.fragments.MapDeleteSheet;
import com.aliernfrog.lactoollegacy.fragments.MapDownloadSheet;
import com.aliernfrog.lactoollegacy.fragments.MapDuplicateSheet;
import com.aliernfrog.lactoollegacy.fragments.MapPickerSheet;
import com.aliernfrog.lactoollegacy.utils.AppUtil;
import com.aliernfrog.lactoollegacy.utils.FileUtil;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;

public class MapsActivity extends AppCompatActivity implements MapPickerSheet.MapPickerListener, MapDownloadSheet.MapDownloadListener, MapDuplicateSheet.MapDuplicateListener, MapDeleteSheet.MapDeleteListener {
    CollapsingToolbarLayout collapsingToolbarLayout;
    Toolbar toolbar;
    FloatingActionButton saveButton;
    ImageView appBarImage;
    LinearLayout mapsPickLinear;
    Button pickMap;
    LinearLayout mapNameLinear;
    EditText mapNameInput;
    LinearLayout thumbnailLinear;
    ImageView thumbnailImage;
    LinearLayout thumbnailLinearActions;
    Button thumbnailSetButton;
    Button thumbnailRemoveButton;
    LinearLayout mapActionsLinear;
    Button importMapButton;
    Button editMapButton;
    Button duplicateMapButton;
    Button backupMapButton;
    Button shareMapButton;
    Button deleteMapButton;
    LinearLayout otherLinear;
    Button mergeMapsButton;
    Button manageBackupsButton;
    TextView debugText;

    SharedPreferences prefsUpdate;
    SharedPreferences prefsConfig;

    Integer REQUEST_PICK_MAP = 1;
    Integer REQUEST_PICK_THUMBNAIL = 2;

    Boolean backupOnEdit;

    String currentPath;
    String lacPath;
    String backupPath;
    String autoBackupPath;
    Boolean isImported;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        collapsingToolbarLayout = findViewById(R.id.maps_collapsingToolbar);
        toolbar = findViewById(R.id.maps_toolbar);
        saveButton = findViewById(R.id.maps_save);
        appBarImage = findViewById(R.id.maps_appbar_image);
        mapsPickLinear = findViewById(R.id.maps_pick_linear);
        pickMap = findViewById(R.id.maps_pick_pick);
        mapNameLinear = findViewById(R.id.maps_name_linear);
        mapNameInput = findViewById(R.id.maps_name_input);
        thumbnailLinear = findViewById(R.id.maps_thumbnail_linear);
        thumbnailImage = findViewById(R.id.maps_thumbnail_thumbnail);
        thumbnailLinearActions = findViewById(R.id.maps_thumbnail_actions);
        thumbnailSetButton = findViewById(R.id.maps_thumbnail_set);
        thumbnailRemoveButton = findViewById(R.id.maps_thumbnail_remove);
        mapActionsLinear = findViewById(R.id.maps_mapActions);
        importMapButton = findViewById(R.id.maps_mapActions_import);
        editMapButton = findViewById(R.id.maps_mapActions_edit);
        duplicateMapButton = findViewById(R.id.maps_mapActions_duplicate);
        backupMapButton = findViewById(R.id.maps_mapActions_backup);
        shareMapButton = findViewById(R.id.maps_mapActions_share);
        deleteMapButton = findViewById(R.id.maps_mapActions_delete);
        otherLinear = findViewById(R.id.maps_other);
        mergeMapsButton = findViewById(R.id.maps_other_mergeMaps);
        manageBackupsButton = findViewById(R.id.maps_other_manageBackups);
        debugText = findViewById(R.id.maps_debug);

        prefsUpdate = getSharedPreferences("APP_UPDATE", MODE_PRIVATE);
        prefsConfig = getSharedPreferences("APP_CONFIG", MODE_PRIVATE);

        String appPath = prefsUpdate.getString("path-app", null);
        lacPath = prefsUpdate.getString("path-maps", null);
        backupPath = appPath+"backups";
        autoBackupPath = appPath+"auto-backups";

        backupOnEdit = prefsConfig.getBoolean("enableBackupOnEdit", true);

        if (prefsConfig.getBoolean("enableDebug", false)) debugText.setVisibility(View.VISIBLE);

        devLog("MapsActivity started");
        setListeners();
        devLog("lacPath: "+lacPath);
        autoBackup();
    }

    public void getMap(String path) {
        devLog("attempting to read map: "+path);
        File map = new File(path);
        currentPath = path;
        isImported = map.getPath().startsWith(lacPath);
        String mapName = FileUtil.removeExtension(map.getName());
        collapsingToolbarLayout.setTitle(mapName);
        mapNameInput.setText(mapName);
        devLog("isImported: "+isImported);
        devLog("got map");
        getMapThumbnail(map.getPath());
        resetVisibilities();
    }

    public void getMapThumbnail(String path) {
        String thumbnailPath = FileUtil.removeExtension(path)+".jpg";
        File thumbnailFile = new File(thumbnailPath);
        boolean hasThumbnail = isImported && thumbnailFile.exists();
        if (hasThumbnail) {
            Bitmap bitmap = BitmapFactory.decodeFile(thumbnailFile.getPath());
            appBarImage.setImageBitmap(bitmap);
            thumbnailImage.setVisibility(View.VISIBLE);
            thumbnailImage.setImageBitmap(bitmap);
            thumbnailRemoveButton.setVisibility(View.VISIBLE);
            devLog("set thumbnail bitmap");
        } else {
            appBarImage.setImageBitmap(null);
            thumbnailImage.setVisibility(View.GONE);
            thumbnailImage.setImageBitmap(null);
            thumbnailLinearActions.setVisibility(View.VISIBLE);
            thumbnailRemoveButton.setVisibility(View.GONE);
            devLog("no thumbnail");
        }
    }

    public void resetVisibilities() {
        mapNameLinear.setVisibility(View.VISIBLE);
        mapActionsLinear.setVisibility(View.VISIBLE);
        if (isImported) {
            importMapButton.setVisibility(View.GONE);
            thumbnailLinear.setVisibility(View.VISIBLE);
            duplicateMapButton.setVisibility(View.VISIBLE);
            deleteMapButton.setVisibility(View.VISIBLE);
        } else {
            importMapButton.setVisibility(View.VISIBLE);
            thumbnailLinear.setVisibility(View.GONE);
            duplicateMapButton.setVisibility(View.GONE);
            deleteMapButton.setVisibility(View.GONE);
        }
    }

    public void renameMap(String newName) {
        devLog("attempting to rename the map to: "+newName);
        File currentFile = new File(currentPath);
        String oldName = FileUtil.removeExtension(currentFile.getName());
        String parentPath = currentFile.getParent();
        String newPath = parentPath+"/"+newName+".txt";
        devLog("newPath: "+newPath);
        File checkFile = new File(parentPath+"/"+newName+".txt");
        File thumbnail = new File(parentPath+"/"+oldName+".jpg");
        File data = new File(parentPath+"/"+oldName);
        if (checkFile.exists()) {
            Toast.makeText(getApplicationContext(), R.string.denied_alreadyExists, Toast.LENGTH_SHORT).show();
            devLog(checkFile.getPath()+" already exists");
        } else {
            if (isImported && thumbnail.exists()) thumbnail.renameTo(new File(lacPath+"/"+newName+".jpg"));
            if (isImported && data.exists()) data.renameTo(new File(lacPath+"/"+newName));
            currentFile.renameTo(new File(parentPath+"/"+newName+".txt"));
            devLog("renamed the map to: "+newName);
            getMap(newPath);
            Toast.makeText(getApplicationContext(), R.string.info_done, Toast.LENGTH_SHORT).show();
        }
    }

    public void setThumbnail(String path) {
        devLog("attempting to set thumbnail to :"+path);
        File mapFile = new File(currentPath);
        String mapName = FileUtil.removeExtension(mapFile.getName());
        String thumbnailPath = lacPath+"/"+mapName+".jpg";
        copyFile(path, thumbnailPath, false, true);
        getMapThumbnail(currentPath);
    }

    public void removeThumbnail() {
        devLog("attempting to delete thumbnail file");
        File mapFile = new File(currentPath);
        String mapName = FileUtil.removeExtension(mapFile.getName());
        String thumbnailPath = lacPath+"/"+mapName+".jpg";
        File thumbnail = new File(thumbnailPath);
        thumbnail.delete();
        devLog("deleted thumbnail file");
        Toast.makeText(getApplicationContext(), R.string.info_done, Toast.LENGTH_SHORT).show();
        getMapThumbnail(currentPath);
    }

    public void importMap() {
        devLog("attempting to import the map");
        File file = new File(currentPath);
        String name = FileUtil.removeExtension(file.getName());
        String dest = lacPath+"/"+name+".txt";
        File check = new File(dest);
        if (check.exists()) {
            Toast.makeText(getApplicationContext(), R.string.denied_alreadyExists, Toast.LENGTH_SHORT).show();
            devLog(check.getPath()+" already exists");
        } else {
            copyFile(currentPath, dest, true, true);
            devLog("imported the map");
        }
    }

    public void editMap() {
        devLog("attempting to edit the map");
        if (backupOnEdit) backupMap(false);
        File currentFile = new File(currentPath);
        Intent intent = new Intent(this, MapsOptionsActivity.class);
        intent.putExtra("path", currentPath);
        intent.putExtra("name", FileUtil.removeExtension(currentFile.getName()));
        startActivity(intent);
    }

    public void openDuplicateMapView() {
        File currentFile = new File(currentPath);
        Bundle bundle = new Bundle();
        bundle.putString("mapName", FileUtil.removeExtension(currentFile.getName()));
        MapDuplicateSheet mapDuplicateSheet = new MapDuplicateSheet();
        mapDuplicateSheet.setArguments(bundle);
        mapDuplicateSheet.show(getSupportFragmentManager(), "map_duplicate");
    }

    public void duplicateMap(String name) {
        devLog("attempting to duplicate the map with name: "+name);
        String fullPath = lacPath+"/"+name+".txt";
        File checkFile = new File(fullPath);
        if (checkFile.exists()) Toast.makeText(getApplicationContext(), R.string.denied_alreadyExists, Toast.LENGTH_SHORT).show();
        if (!checkFile.exists()) copyFile(currentPath, fullPath, true, true);
    }

    public void backupMap(Boolean createToastOnEnd) {
        devLog("attempting to backup the map");
        File currentFile = new File(currentPath);
        String mapName = FileUtil.removeExtension(currentFile.getName());
        String backupFileName = mapName+"-"+AppUtil.timeString("yyMMddhhmmss")+".txt";
        String fullPath = backupPath+"/"+backupFileName;
        copyFile(currentPath, fullPath, false, createToastOnEnd);
    }

    public void shareMap() {
        devLog("attempting to share the map");
        File file = new File(currentPath);
        Intent share = FileUtil.shareFile(file.getPath(), "text/*", getApplicationContext());
        startActivity(Intent.createChooser(share, "Share Map"));
    }

    public void openDeleteMapView() {
        MapDeleteSheet mapDeleteSheet = new MapDeleteSheet();
        mapDeleteSheet.show(getSupportFragmentManager(), "map_delete_confirm");
    }

    public void deleteMap() {
        devLog("attempting to delete the map");
        File map = new File(currentPath);
        String name = FileUtil.removeExtension(map.getName());
        File thumbnail = new File(lacPath+"/"+name+".jpg");
        File data = new File(lacPath+"/"+name);
        if (thumbnail.exists()) thumbnail.delete();
        if (data.exists()) FileUtil.deleteDirectory(data);
        map.delete();
        mapNameLinear.setVisibility(View.GONE);
        thumbnailLinear.setVisibility(View.GONE);
        mapActionsLinear.setVisibility(View.GONE);
        appBarImage.setImageBitmap(null);
        collapsingToolbarLayout.setTitle(getString(R.string.manageMaps));
        devLog("deleted the map");
        Toast.makeText(getApplicationContext(), R.string.info_done, Toast.LENGTH_SHORT).show();
    }

    public void mergeMaps() {
        devLog("attempting to open merge maps screen");
        Intent intent = new Intent(this, MapsMergeActivity.class);
        intent.putExtra("mapsPath", lacPath);
        startActivity(intent);
    }

    public void manageBackups() {
        devLog("attempting to open manage backups screen");
        Intent intent = new Intent(this, RestoreActivity.class);
        intent.putExtra("backupPath", backupPath);
        intent.putExtra("mapsPath", lacPath);
        startActivity(intent);
    }

    @SuppressWarnings("deprecation")
    public void pickFile(String fileType, String[] allowedTypes, Integer requestCode) {
        devLog("attempting to pick a file with request code: "+requestCode);
        Intent intent = new Intent(this, FilePickerActivity.class);
        intent.putExtra("FILE_TYPE_SAF", fileType);
        intent.putExtra("FILE_TYPE_INAPP", allowedTypes);
        startActivityForResult(intent, requestCode);
    }

    public void pickMapFile() {
        pickFile("text/*", new String[]{"txt"}, REQUEST_PICK_MAP);
    }

    public void pickMap() {
        Bundle bundle = new Bundle();
        bundle.putString("mapsPath", lacPath);
        MapPickerSheet mapPickerSheet = new MapPickerSheet();
        mapPickerSheet.setArguments(bundle);
        mapPickerSheet.show(getSupportFragmentManager(), "map_picker");
    }

    public void autoBackup() {
        if (prefsConfig.getBoolean("enableAutoBackups", false)) {
            devLog("attempting to backup all");
            String parent = autoBackupPath+"/"+AppUtil.timeString("yyMMddhhmmss");
            File parentFile = new File(parent);
            if (!parentFile.exists()) parentFile.mkdirs();
            File[] files = new File(lacPath).listFiles();
            if (files == null) {
                devLog("file is null");
            } else {
                for (File file : files) {
                    if (!file.isDirectory()) copyFile(file.getPath(), parent+"/"+file.getName(), false, false);
                }
            }
        }
    }

    public void copyFile(String source, String destination, Boolean getMapWhenDone, Boolean toastResult) {
        devLog("attempting to copy "+source+" to "+destination);
        try {
            FileUtil.copyFile(source, destination);
            devLog("copied successfully");
            if (getMapWhenDone) getMap(destination);
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
        if (requestCode == REQUEST_PICK_MAP) {
            String path = data.getStringExtra("path");
            devLog("received path: "+path);
            getMap(path);
        } else if (requestCode == REQUEST_PICK_THUMBNAIL) {
            String path = data.getStringExtra("path");
            devLog("received path: "+path);
            setThumbnail(path);
        }
    }

    void setListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());
        AppUtil.handleOnPressEvent(saveButton, this::finish);
        AppUtil.handleOnPressEvent(mapsPickLinear);
        AppUtil.handleOnPressEvent(pickMap, this::pickMap);
        AppUtil.handleOnPressEvent(mapNameLinear);
        mapNameInput.setOnEditorActionListener((textView, i, keyEvent) -> {
            renameMap(mapNameInput.getText().toString());
            return true;
        });
        AppUtil.handleOnPressEvent(thumbnailLinear, () -> AppUtil.toggleView(thumbnailLinearActions));
        AppUtil.handleOnPressEvent(thumbnailSetButton, () -> pickFile("image/*", new String[]{"jpg","jpeg","png"}, REQUEST_PICK_THUMBNAIL));
        AppUtil.handleOnPressEvent(thumbnailRemoveButton, this::removeThumbnail);
        AppUtil.handleOnPressEvent(mapActionsLinear);
        AppUtil.handleOnPressEvent(importMapButton, this::importMap);
        AppUtil.handleOnPressEvent(editMapButton, this::editMap);
        AppUtil.handleOnPressEvent(duplicateMapButton, this::openDuplicateMapView);
        AppUtil.handleOnPressEvent(backupMapButton, () -> backupMap(true));
        AppUtil.handleOnPressEvent(shareMapButton, this::shareMap);
        AppUtil.handleOnPressEvent(deleteMapButton, this::openDeleteMapView);
        AppUtil.handleOnPressEvent(otherLinear);
        AppUtil.handleOnPressEvent(mergeMapsButton, this::mergeMaps);
        AppUtil.handleOnPressEvent(manageBackupsButton, this::manageBackups);
    }

    @Override
    public void onMapPicked(String path) {
        devLog("received path: "+path);
        getMap(path);
    }

    @Override
    public void onFilePickRequested() {
        devLog("map file pick requested");
        pickMapFile();
    }

    @Override
    public void onMapDownloaded(String path) {
        devLog("received path: "+path);
        getMap(path);
    }

    @Override
    public void onMapDuplicateConfirm(String name) {
        duplicateMap(name);
    }

    @Override
    public void onDeleteConfirm() {
        deleteMap();
    }
}