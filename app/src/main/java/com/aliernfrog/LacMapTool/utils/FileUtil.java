package com.aliernfrog.LacMapTool.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class FileUtil {
    public static void copyFile(String source, String destination) throws Exception {
        File src = new File(source);
        InputStream in = null;
        OutputStream out = null;
        if (src.isFile()) {
            in =  new FileInputStream(new File(source));
            out = new FileOutputStream(new File(destination));
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            in.close();
            out.close();
        } else {
            File[] files = src.listFiles();
            for (int i = 0; i < files.length; i++) {
                copyFile(files[i].getPath(), destination+"/"+files[i].getName());
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static void copyFile(String source, DocumentFile destination, Context context) throws Exception {
        File src = new File(source);
        ContentResolver resolver = context.getContentResolver();
        InputStream in = null;
        OutputStream out = null;
        if (src.isFile()) {
            in = new FileInputStream(new File(source));
            out = resolver.openOutputStream(destination.getUri());
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            in.close();
            out.close();
        } else {
            File[] files = src.listFiles();
            for (int i = 0; i < files.length; i++) {
                copyFile(files[i].getPath(), destination.createFile("", files[i].getName()), context);
            }
        }
    }

    public static void copyFile(DocumentFile source, String destination, Context context) throws Exception {
        DocumentFile src = source;
        File dst = new File(destination);
        ContentResolver resolver = context.getContentResolver();
        InputStream in = null;
        OutputStream out = null;
        if (!src.isDirectory()) {
            in = resolver.openInputStream(source.getUri());
            out = new FileOutputStream(new File(destination));
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            in.close();
            out.close();
        } else {
            if (!dst.exists()) dst.mkdirs();
            DocumentFile[] files = src.listFiles();
            for (int i = 0; i < files.length; i++) {
                copyFile(files[i], destination + "/" + files[i].getName(), context);
            }
        }
    }

    public static String readFile(String source) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(new File(source).getPath()));
        String _line;
        String _full = "";
        while ((_line = reader.readLine()) != null) {
            if (_full.length() > 0) {
                _full += "\n"+_line;
            } else {
                _full += _line;
            }
        }
        reader.close();
        return _full;
    }

    static public boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }
}
