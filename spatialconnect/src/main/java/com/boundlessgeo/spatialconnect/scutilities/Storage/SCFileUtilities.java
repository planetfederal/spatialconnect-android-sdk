/**
 * Copyright 2015-2016 Boundless, http://boundlessgeo.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the
 * License
 */
package com.boundlessgeo.spatialconnect.scutilities.Storage;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Utility class for reading and writing to the filesystem.
 */
public class SCFileUtilities {
  private final static String LOG_TAG = SCFileUtilities.class.getSimpleName();
  private static final File[] NO_FILES = {};

  public SCFileUtilities() {
  }

  public static boolean isStorageAvailable(Context context, SCStoreConfig scStoreConfig) {
    boolean storageStatus = false;
    if (!scStoreConfig.isMainBundle()) {
      // since the file is not packaged with the app, we check if external storage is available
      storageStatus = isExternalStorageAvailable();
      if (storageStatus) {
        File f = new File(scStoreConfig.getUri());
        if (!f.exists()) storageStatus = false;
      }
    } else {
      if (context != null) {
        File f = new File(context.getFilesDir(), scStoreConfig.getUri());
        if (!f.exists()) storageStatus = false;
      }
    }
    return storageStatus;
  }

  public static File getInternalFileObject(String uri, Context context) {
    if (!uri.contains(File.pathSeparator)) {
      return new File(context.getFilesDir(), uri);
    }
    return null;
  }

  public static File getExternalFileObject(String uri) {
    return new File(uri);
  }

  public static String readTextFile(String fname, AndroidStorageType driveType, Context context) {
    String text = "";
    if (driveType == AndroidStorageType.INTERNAL) {
      if (!fname.contains(File.pathSeparator)) {
        text = readTextFromInternalStorage(fname, context);
      }
    } else if (driveType == AndroidStorageType.EXTERNAL) {
      text = readTextFromExternalStorage(fname);
    } else if (driveType == AndroidStorageType.NON_ANDROID) {
      text = readTextFromFileSystem(fname);
    }
    return text;
  }

  public static String readTextFromFileSystem(String fname) {
    File file = new File(fname);
    StringBuilder sb = new StringBuilder();
    try {
      BufferedReader inputReader =
          new BufferedReader(new InputStreamReader(new FileInputStream(file)));
      String inputString;

      while ((inputString = inputReader.readLine()) != null) {
        sb.append(inputString);
        sb.append("\n");
      }
      inputReader.close();
    } catch (IOException ex) {
      Log.e(LOG_TAG, "Error in readTextFromFileSystem()", ex);
    }
    return sb.toString();
  }

  public static String readTextFromInternalStorage(String fname, Context context) {
    StringBuilder sb = new StringBuilder();
    try {
      BufferedReader inputReader =
          new BufferedReader(new InputStreamReader(context.openFileInput(fname)));
      String inputString;

      while ((inputString = inputReader.readLine()) != null) {
        sb.append(inputString);
        sb.append("\n");
      }
      inputReader.close();
    } catch (IOException ex) {
      Log.e(LOG_TAG, "Error in readTextFromInternalStorage()", ex);
    }
    return sb.toString();
  }

  public static String readTextFromExternalStorage(String uri) {
    StringBuilder sb = new StringBuilder();
    if (isExternalStorageAvailable()) {
      File file = new File(uri);

      if (!file.exists()) return null;

      try {
        BufferedReader inputReader =
            new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String inputString;

        while ((inputString = inputReader.readLine()) != null) {
          sb.append(inputString);
          sb.append("\n");
        }
        inputReader.close();
      } catch (IOException ex) {
        Log.e(LOG_TAG, "Error in readTextFromExternalStorage()", ex);
      }
    }
    return sb.toString();
  }

  public static boolean isExternalStorageReadOnly() {
    try {
      String extStorageState = Environment.getExternalStorageState();
      if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
        return true;
      }
    } catch (Exception ex) {
      Log.e(LOG_TAG, "Error in isExternalStorageReadOnly", ex);
    }
    return false;
  }

  public static boolean isExternalStorageAvailable() {
    try {
      String extStorageState = Environment.getExternalStorageState();
      if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
        return true;
      }
    } catch (Exception ex) {
      Log.e(LOG_TAG, "Error in isExternalStorageAvailable", ex);
    }
    return false;
  }

  public static File[] findFilesByExtension(File dir, final String ext) {
    try {
      if (!dir.isDirectory()) {
        return NO_FILES;
      }
      File[] matchingFiles = dir.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String filename) {
          return filename.endsWith(ext);
        }
      });
      if (matchingFiles.length > 0) {
        return matchingFiles;
      }
    } catch (Exception ex) {
      Log.e(LOG_TAG, "Error in findFilesByExtension", ex);
    }
    return NO_FILES;
  }

  public void writeToInternalStorage(String fname, String contents, Context context) {
    try {
      FileOutputStream fos = context.openFileOutput(fname, Context.MODE_PRIVATE);
      fos.write(contents.getBytes());
      fos.close();
    } catch (Exception ex) {
      Log.e(LOG_TAG, "Error in writeToInternalStorage()", ex);
    }
  }

  public void writeToExternalStorage(String fname, String contents) {
    if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) return;
    try {
      FileOutputStream fos = new FileOutputStream(fname);
      fos.write(contents.getBytes());
      fos.close();
    } catch (IOException ex) {
      Log.e(LOG_TAG, "Error in writeToExternalStorage()", ex);
    }
  }
}
