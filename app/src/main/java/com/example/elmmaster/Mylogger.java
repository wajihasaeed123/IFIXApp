package com.example.elmmaster;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Mylogger {
    private static final String TAG = "MyApp";
    private static final String LOG_FILE_NAME = "app_log.txt";

    public static void logToFile(String message) {
        try {
//            File logFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "app_log.txt");
//            if (!logFile.exists()) {
//                logFile.createNewFile();
//            }
            File logFile = new File("/sdcard/" + LOG_FILE_NAME);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            Log.d("msg","my name is wajiha");
            FileOutputStream outputStream = new FileOutputStream(logFile, true);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String formattedDate = sdf.format(new Date());
            String logMessage = formattedDate + ": " + message + "\n";
            outputStream.write(logMessage.getBytes());
            Log.d("msg","my name is fatima");
            outputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error writing to log file: " + e.getMessage());
        }
    }
    public static void logAnalysePIDS(String message) {
        logToFile("analysPIDS: " + message);
    }
}
