package com.example.routemap.app;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;

public class LogFile {

    File logFile;

    public LogFile() { CreateLogFile("LogFile.log", true); }
    public LogFile(boolean overwrite)
    {
        CreateLogFile("LogFile.log", overwrite);
    }
    public LogFile(String fileName, boolean overwrite) { CreateLogFile(fileName, overwrite); }
    public LogFile(String fileName){ CreateLogFile(fileName, true); }

    private void CreateLogFile(String fileName, boolean overwrite) {

        if (MainActivity.NOWRITE_ON_SDCARD) {
            return;
        }

        try {
            File root = Environment.getExternalStorageDirectory();

            logFile = new File(root, fileName);

            if (logFile.exists() && overwrite) {
                logFile.delete();
            }
            logFile.createNewFile();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void Log(String text) {

        if (MainActivity.NOWRITE_ON_SDCARD) {
            return;
        }

        synchronized (this) {
            try {
                FileWriter fWriter = new FileWriter(logFile, true);
                fWriter.write(text + "\r\n");
                fWriter.flush();
                fWriter.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
