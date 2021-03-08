package com.aliernfrog.LacMapTool;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aliernfrog.LacMapTool.utils.AppUtil;
import com.aliernfrog.LacMapTool.utils.FileUtil;
import com.hbisoft.pickit.PickiT;
import com.hbisoft.pickit.PickiTCallbacks;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;

@SuppressLint("ClickableViewAccessibility")
public class MapsActivity extends AppCompatActivity implements PickiTCallbacks {
    ImageView goback;
    ImageView backupManage;
    TextView android11warning;
    LinearLayout pickLinear;
    Spinner mapsSpinner;
    Button selectImported;
    Button filePick;
    LinearLayout nameLinear;
    EditText mapname;
    Button fileRename;
    TextView mapInfo;
    Button fileImport;
    Button editMapSettings;
    LinearLayout map_linear;
    Button fileBackup;
    Button fileShare;
    Button fileDownload;
    Button fileDelete;
    TextView devlog;

    SharedPreferences update;
    SharedPreferences config;
    SharedPreferences.Editor configEdit;

    Boolean devMode;
    String lacPath;
    String dataPath;
    String backupPath;
    String aBackupPath;
    String rawPath;
    String savePath;
    String tempPath;
    String logs = "";

    String mapName;
    Boolean isImported;
    Boolean isDeleting = false;

    Uri lacTreeUri;
    DocumentFile lacTreeFile;

    int TREE_REQUEST_CODE = 4;
    int FILE_PICK_CODE = 2;

    PickiT pickiT;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        //StrictMode.setThreadPolicy(policy);
        //StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        //StrictMode.setVmPolicy(builder.build());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        update = getSharedPreferences("APP_UPDATE", Context.MODE_PRIVATE);
        config = getSharedPreferences("APP_CONFIG", Context.MODE_PRIVATE);
        configEdit = config.edit();

        devMode = config.getBoolean("enableDebug", false);
        lacPath = update.getString("path-lac", null);
        dataPath = update.getString("path-app", null);
        backupPath = dataPath+"backups/";
        aBackupPath = dataPath+"auto-backups/";
        tempPath = dataPath+"temp/";
        if (config.getBoolean("enableLacd",false)) lacPath = update.getString("path-lacd", null);
        if (config.getBoolean("enableLegacyPath", false)) lacPath = update.getString("path-legacy", null);
        pickiT = new PickiT(this, this, this);

        goback = findViewById(R.id.maps_goback);
        backupManage = findViewById(R.id.maps_backupmanage);
        android11warning = findViewById(R.id.maps_dialog_android11);
        pickLinear = findViewById(R.id.maps_pick_linear);
        mapsSpinner = findViewById(R.id.maps_mapsSpinner);
        selectImported = findViewById(R.id.maps_select);
        filePick = findViewById(R.id.maps_filePick);
        nameLinear = findViewById(R.id.maps_name_linear);
        fileRename = findViewById(R.id.maps_rename);
        mapInfo = findViewById(R.id.maps_mapInfo);
        fileImport = findViewById(R.id.maps_fileImport);
        editMapSettings = findViewById(R.id.maps_editmapsettings);
        map_linear = findViewById(R.id.maps_map_linear);
        fileBackup = findViewById(R.id.maps_backuprestore);
        fileShare = findViewById(R.id.maps_shareMap);
        mapname = findViewById(R.id.maps_editname);
        fileDownload = findViewById(R.id.maps_downloadmap);
        fileDelete = findViewById(R.id.maps_deleteMap);
        devlog = findViewById(R.id.maps_log);
        if (!devMode) devlog.setVisibility(View.GONE);
        devLog("==== DEBUG LOGS ====", false);
        devLog("lacPath: "+lacPath, false);
        devLog("dataPath: "+dataPath, false);
        devLog("", false);

        if (Build.VERSION.SDK_INT >= 27) {
            String lacTreeId = lacPath.replace(Environment.getExternalStorageDirectory()+"/", "primary:");
            Uri lacUri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", lacTreeId);
            lacTreeUri = DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents", lacTreeId);
            int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
            if (getApplicationContext().checkUriPermission(lacTreeUri, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                devLog("no permissions to lac data, attempting to request", false);
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                        .putExtra(DocumentsContract.EXTRA_INITIAL_URI, lacUri)
                        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        .putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                        .addFlags(takeFlags);
                startActivityForResult(intent, TREE_REQUEST_CODE);
            } else {
                useTempPath();
            }
        }

        setListener();
        getImportedMaps();
    }

    public void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        startActivityForResult(intent, FILE_PICK_CODE);
        devLog("attempting to pick a file with request code 2", false);
    }

    public void renameMap() {
        savePath = lacPath + mapName;
        String[] arr = rawPath.replace(".txt", "").split("/");
        String oldName = arr[arr.length - 1];
        File check = new File(savePath);
        File thisFile = new File(rawPath);
        File _thumbnail = new File(lacPath+oldName+".jpg");
        File _data = new File(lacPath+oldName);
        if (check.exists()) {
            Toast.makeText(getApplicationContext(), R.string.denied_alreadyExists, Toast.LENGTH_SHORT).show();
            devLog(check.getPath()+" already exists", false);
        } else {
            devLog("oldName = "+oldName, false);
            if (_thumbnail.exists() && _thumbnail.isFile()) _thumbnail.renameTo(new File(savePath+".jpg"));
            if (_data.exists() && _data.isDirectory()) _data.renameTo(new File(savePath));
            thisFile.renameTo(new File(savePath+".txt"));
            devLog("Renamed: "+rawPath+" to: "+savePath, false);
            getMap(savePath);
            Toast.makeText(getApplicationContext(), R.string.info_done, Toast.LENGTH_SHORT).show();
        }
    }

    public void importMap() {
        savePath = lacPath + mapName + ".txt";
        if (new File(savePath).exists()) {
            Toast.makeText(getApplicationContext(), R.string.denied_alreadyExists, Toast.LENGTH_SHORT).show();
            devLog("map already exists", false);
        } else {
            copyFile(rawPath, savePath, true);
            Toast.makeText(getApplicationContext(), R.string.info_done, Toast.LENGTH_SHORT).show();
            devLog("savePath: "+savePath, false);
        }
    }

    public void backupMap(Boolean toast) {
        devLog("attempting to backup with toast "+toast.toString(), false);
        try {
            savePath = backupPath + mapName + "-" + timeString("yyMMddhhmmss") + ".txt";
            copyFile(rawPath, savePath, false);
            if (toast) Toast.makeText(getApplicationContext(), R.string.info_done, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), R.string.info_error, Toast.LENGTH_SHORT).show();
            devLog(e.toString(), true);
            refreshVisibility();
        }
    }

    public void deleteMap() {
        if (!isDeleting) {
            fileDelete.setText(R.string.mapDelete_confirm);
            isDeleting = true;
            Toast.makeText(getApplicationContext(), R.string.mapDelete_clickAgain, Toast.LENGTH_SHORT).show();
        } else {
            devLog("attempting to delete", false);
            File mapFile = new File(rawPath);
            File thumbnailFile = new File(rawPath.replace(".txt", ".jpg"));
            File dataFile = new File(rawPath.replace(".txt", ""));
            fileDelete.setText(R.string.mapDelete);
            isDeleting = false;
            if (mapFile.delete()) Toast.makeText(getApplicationContext(), R.string.info_done, Toast.LENGTH_SHORT).show();
            if (thumbnailFile.exists()) thumbnailFile.delete();
            if (dataFile.exists()) dataFile.delete();
            switchActivity(MapsActivity.class);
            finish();
        }
    }

    public void getMap(String path) {
        File mapFile = new File(path);
        map_linear.setVisibility(View.VISIBLE);
        rawPath = path;
        mapName = mapFile.getName().replace(".txt", "");
        if (rawPath.startsWith(lacPath)) { isImported = true; } else { isImported = false; }
        mapname.setText(mapName);
        getImportedMaps();
        if (isImported) getMapThumb();
        refreshVisibility();
        mapInfo.setText(Html.fromHtml("<b>"+getString(R.string.mapInfo_size)+":</b> "+mapFile.length()/1024+" kB"));
        configEdit.putString("lastPath", rawPath);
        configEdit.commit();
        devLog("rawPath: "+ rawPath, false);
        devLog("mapName: "+ mapName, false);
        devLog("isImported: "+ isImported.toString(), false);
    }

    public void getMapThumb() {
        String thumbnailDir = lacPath+mapName+".jpg";
        File thumb = new File(thumbnailDir);
        ImageView thumbView = findViewById(R.id.maps_thumbnail);
        if (thumb.exists()) {
            Bitmap thumbBitmap = BitmapFactory.decodeFile(thumb.getAbsolutePath());
            thumbView.setImageBitmap(thumbBitmap);
            devLog("attempting to set thumbnail", false);
        } else {
            thumbView.setImageBitmap(null);
            devLog("thumbnail doesn't exist", false);
        }
    }

    public void getImportedMaps() {
        String _mapname;
        File directory = new File(lacPath);
        File[] files = directory.listFiles();
        if (files == null) {
            devLog("directory "+directory+" is null", false);
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner);
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile() && files[i].getName().endsWith(".txt")) {
                    _mapname = files[i].getName().replace(".txt", "");
                    adapter.add(_mapname);
                    devLog("found map: "+files[i].getName(), false);
                }
            }
            mapsSpinner.setAdapter(adapter);
            devLog("done getting files from "+directory, false);
        }
    }

    public void refreshVisibility() {
        if (isImported) {
            fileRename.setVisibility(View.VISIBLE);
            fileImport.setVisibility(View.GONE);
            fileBackup.setVisibility(View.VISIBLE);
            fileShare.setVisibility(View.VISIBLE);
            fileDelete.setVisibility(View.VISIBLE);
        } else {
            fileRename.setVisibility(View.GONE);
            fileImport.setVisibility(View.VISIBLE);
            fileBackup.setVisibility(View.GONE);
            fileShare.setVisibility(View.GONE);
            fileDelete.setVisibility(View.GONE);
        }
        mapInfo.setVisibility(View.VISIBLE);
        fileDelete.setText(R.string.mapDelete);
        isDeleting = false;
    }

    public void copyFile(String src, String dst, Boolean GetMap) {
        devLog("attempting to copy "+src+" to "+dst, false);
        try {
            FileUtil.copyFile(src, dst);
            devLog("copied successfully", false);
            if (GetMap) getMap(dst);
        } catch (Exception e) {
           e.printStackTrace();
           devLog(e.toString(), true);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void copyFile(String src, DocumentFile dst) {
        devLog("attempting to copy "+src+" to "+dst, false);
        try {
            FileUtil.copyFile(src, dst, getApplicationContext());
            devLog("copied successfully", false);
        } catch (Exception e) {
            e.printStackTrace();
            devLog(e.toString(), true);
        }
    }

    public void copyFile(DocumentFile src, String dst, Boolean GetMap) {
        devLog("attempting to copy "+src+" to "+dst, false);
        try {
            FileUtil.copyFile(src, dst, getApplicationContext());
            devLog("copied successfully", false);
            if (GetMap) getMap(dst);
        } catch (Exception e) {
            e.printStackTrace();
            devLog(e.toString(), true);
        }
    }

    public void shareFile(String path) {
        Intent share = new Intent(Intent.ACTION_SEND);
        File file = new File(path);
        if (file.exists()) {
            devLog("attempting to share: "+path, false);
            share.setType("application/txt");
            share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+file.getPath()));
            share.putExtra(Intent.EXTRA_SUBJECT, R.string.mapSharing);
            share.putExtra(Intent.EXTRA_TEXT, R.string.mapSharing);
            startActivity(Intent.createChooser(share, "Share Map"));
        } else {
            Toast.makeText(getApplicationContext(), R.string.denied_doesntExist, Toast.LENGTH_SHORT).show();
            devLog("file does not exist", false);
        }
    }

    public void switchActivity(Class i) {
        devLog("attempting to redirect to class: "+i.toString(), false);
        Intent intent = new Intent(this.getApplicationContext(), i);
        startActivity(intent);
    }

    public String timeString(String frmString) {
        SimpleDateFormat frm = new SimpleDateFormat(frmString);
        Date now = Calendar.getInstance().getTime();
        return frm.format(now);
    }

    public void downloadMap(String link) {
        try {
            String _downloaded = URLUtil.guessFileName(link, null, null);
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(link);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, _downloaded)
                    .setTitle(_downloaded) .setDescription("Downloaded via LAC Map Tool")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            downloadManager.enqueue(request);
            Toast.makeText(getApplicationContext(), R.string.info_done, Toast.LENGTH_SHORT).show();
            getMap(Environment.getExternalStorageDirectory().toString()+"/Download/"+_downloaded);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), R.string.info_error, Toast.LENGTH_SHORT).show();
            devLog(e.toString(), true);
            e.printStackTrace();
        }
    }

    public void redirectURL(String url) {
        devLog("redirecting to: "+url, false);
        Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse(url));
        startActivity(viewIntent);
    }

    public void saveChangesAndFinish() {
        if (Build.VERSION.SDK_INT >= 27) {
            devLog("attempting to save changes", false);
            File file = new File(tempPath);
            File[] files = file.listFiles();
            try {
                for (int i = 0; i < files.length; i++) {
                    DocumentFile fileInLac = lacTreeFile.findFile(files[i].getName());
                    if (fileInLac == null) fileInLac = lacTreeFile.createFile("", files[i].getName());
                    copyFile(files[i].getPath(), fileInLac);
                }
            } finally {
                for (int i = 0; i < files.length; i++) {
                    files[i].delete();
                }
                finish();
            }
        } else {
            finish();
        }
    }

    public void useTempPath() {
        lacTreeFile = DocumentFile.fromTreeUri(getApplicationContext(), lacTreeUri);
        File tempFile = new File(tempPath);
        if (!tempFile.exists()) tempFile.mkdirs();
        if (lacTreeFile != null) {
            DocumentFile[] files = lacTreeFile.listFiles();
            for (int i = 0; i < files.length; i++) {
                copyFile(files[i], tempPath+files[i].getName(), false);
            }
        }
        lacPath = tempPath;
    }

    void handleCmd(String cmdText) {
        String cmd = cmdText.replace("cmd://", "").replace("_", "");
        if (cmd.startsWith("hidden-enable")) {
            configEdit.putBoolean("hidden-enable", true);
            configEdit.commit();
        } else if (cmd.startsWith("hidden-disable")) {
            configEdit.putBoolean("hidden-enable", false);
            configEdit.commit();
        }
    }

    void devLog(String toLog, Boolean error) {
        if (devMode) {
            String tag = Thread.currentThread().getStackTrace()[3].getMethodName();
            if (error) toLog = "<font color=red>"+toLog+"</font>";
            logs = logs+"<br /><font color=#00FFFF>["+tag+"]</font> "+toLog;
            devlog.setText(Html.fromHtml(logs));
        }
    }

    void devLog(String toLog) {
        if (devMode) {
            String tag = Thread.currentThread().getStackTrace()[3].getMethodName();
            logs = logs+"<br /><font color=#00FFFF>["+tag+"]</font> "+toLog;
            devlog.setText(Html.fromHtml(logs));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        devLog("received result for: "+requestCode, false);
        if (requestCode == FILE_PICK_CODE) {
            if (data == null) {
                devLog(requestCode+": no data", false);
            } else {
                pickiT.getPath(data.getData(), Build.VERSION.SDK_INT);
            }
        } else if (requestCode == TREE_REQUEST_CODE) {
            if (data == null) {
                devLog(requestCode+": no data", false);
            } else {
                if (Build.VERSION.SDK_INT >= 30) {
                    int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                    grantUriPermission(getApplicationContext().getPackageName(), data.getData(), takeFlags);
                    getApplicationContext().getContentResolver().takePersistableUriPermission(data.getData(), takeFlags);
                    devLog(requestCode+": granted permissions for: "+data.getData());
                    useTempPath();
                }
            }
        }
    }

    void setListener() {
        goback.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    saveChangesAndFinish();
                }
                AppUtil.handleOnPressEvent(v, event);
                return true;
            }
        });

        backupManage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    switchActivity(RestoreActivity.class);
                }
                AppUtil.handleOnPressEvent(v, event);
                return true;
            }
        });

        android11warning.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    android11warning.setVisibility(View.GONE);
                }
                AppUtil.handleOnPressEvent(v, event);
                return true;
            }
        });

        pickLinear.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {

                }
                AppUtil.handleOnPressEvent(v, event);
                return true;
            }
        });

        selectImported.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (mapsSpinner.getSelectedItem() != null) {
                        String mapname = mapsSpinner.getSelectedItem().toString();
                        if (mapname != null && mapname != "") {
                            getMap(lacPath+mapname+".txt");
                        }
                    }
                }
                AppUtil.handleOnPressEvent(v, event);
                return true;
            }
        });

        fileRename.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    renameMap();
                }
                AppUtil.handleOnPressEvent(v, event);
                return true;
            }
        });

        filePick.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    pickFile();
                }
                AppUtil.handleOnPressEvent(v, event);
                return true;
            }
        });

        nameLinear.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {

                }
                AppUtil.handleOnPressEvent(v, event);
                return true;
            }
        });

        map_linear.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {

                }
                AppUtil.handleOnPressEvent(v, event);
                return true;
            }
        });

        fileImport.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    importMap();
                }
                AppUtil.handleOnPressEvent(v, event);
                return true;
            }
        });

        fileBackup.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    backupMap(true);
                }
                AppUtil.handleOnPressEvent(v, event);
                return true;
            }
        });

        fileShare.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    shareFile(rawPath);
                }
                AppUtil.handleOnPressEvent(v, event);
                return true;
            }
        });

        fileDelete.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    deleteMap();
                }
                AppUtil.handleOnPressEvent(v, event);
                return true;
            }
        });

        mapname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mapName = mapname.getText().toString();
                String text = mapname.getText().toString();
                if ((text.startsWith("http://") || text.startsWith("https://") || text.startsWith("cmd://")) && (text.endsWith(".txt") || text.endsWith("_"))) {
                    fileDownload.setVisibility(View.VISIBLE);
                } else {
                    fileDownload.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        fileDownload.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (mapname.getText().toString().startsWith("cmd://")) {
                        handleCmd(mapname.getText().toString());
                    } else {
                        downloadMap(mapname.getText().toString());
                    }
                }
                AppUtil.handleOnPressEvent(v, event);
                return true;
            }
        });

        editMapSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    backupMap(false);
                    switchActivity(MapsOptionsActivity.class);
                } catch (Exception e) {
                    e.printStackTrace();
                    devLog(e.toString(), true);
                }
            }
        });

        editMapSettings.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    backupMap(false);
                    switchActivity(MapsOptionsActivity.class);
                }
                AppUtil.handleOnPressEvent(v, event);
                return true;
            }
        });
    }

    ProgressDialog pickitProgress;

    @Override
    public void PickiTonUriReturned() {
        pickitProgress = new ProgressDialog(this);
        pickitProgress.setMessage(getString(R.string.info_wait));
        pickitProgress.setCancelable(false);
        pickitProgress.show();
    }

    @Override
    public void PickiTonStartListener() {
    }

    @Override
    public void PickiTonProgressUpdate(int progress) {
    }

    @Override
    public void PickiTonCompleteListener(String path, boolean wasDriveFile, boolean wasUnknownProvider, boolean wasSuccessful, String Reason) {
        if (pickitProgress != null && pickitProgress.isShowing()) pickitProgress.cancel();
        if (wasSuccessful) {
            devLog("got path: "+path, false);
            getMap(path);
        } else {
            devLog(Reason, true);
        }
    }

    @Override
    public void onBackPressed() {
        pickiT.deleteTemporaryFile(this);
        saveChangesAndFinish();
        super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            pickiT.deleteTemporaryFile(this);
            saveChangesAndFinish();
        }
    }
}