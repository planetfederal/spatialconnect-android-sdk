package com.boundlessgeo.spatialconnect.jsbridge;

import android.content.Context;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class WebBundleUtil {

    private static final String BUNDLE_DIRECTORY_NAME = "bundles";
    private File rootBundlesDir;

    public WebBundleUtil(Context context) {
        this.rootBundlesDir = context.getExternalFilesDir(BUNDLE_DIRECTORY_NAME);
    }

    /**
     * Helper method to unzip any zip files in the bundle directory and return a List File objects for the
     * unzipped web bundle directories.
     */
    public ArrayList<File> getWebBundleFiles() {

        ArrayList<File> bundleList = new ArrayList<>();

        if (!rootBundlesDir.exists()) {
            rootBundlesDir.mkdir();
        }

        // if the zip file isn't already unzipped in the bundles directory, then unzip it.
        // this is to support bundles that are packaged with the application as well as zip files (bundles) that have
        // been downloaded since the last time the WebBundleAdapter has been notified of an update
        for (File f : rootBundlesDir.listFiles()) {
            unzipFile(f);
        }

        // add all unzipped bundles
        for (File f : rootBundlesDir.listFiles()) {
            if (f.isDirectory() && !f.getName().equals("__MACOSX")) {
                bundleList.add(f);
            }
        }

        return bundleList;
    }

    /**
     * Unzips a file into the root bundles directory.
     *
     * @param f
     */
    public void unzipFile(File f) {
        if (f.isFile() && !bundleIsUnzipped(f)) {
            ZipFile zipFile = null;
            File bundleDir = new File(rootBundlesDir, f.getName().replace(".zip", ""));
            try {
                zipFile = new ZipFile(f);
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    File entryDestination = new File(bundleDir, entry.getName());
                    if (entry.isDirectory()) {
                        entryDestination.mkdirs();
                    }
                    else {
                        entryDestination.getParentFile().mkdirs();
                        InputStream in = zipFile.getInputStream(entry);
                        OutputStream out = new FileOutputStream(entryDestination);
                        IOUtils.copy(in, out);
                        IOUtils.closeQuietly(in);
                        out.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Helper method to determine if a web bundle has been unzipped in the bundles directory.
     *
     * @param bundle - a file that may or may not have been unziped
     * @return true if a directory with the bundle name exists in the bundles directory
     */
    private boolean bundleIsUnzipped(File bundle) {
        String bundleName = bundle.getName().replace(".zip", "");
        return new File(rootBundlesDir, bundleName).exists();
    }

    public File getRootBundlesDir() {
        return rootBundlesDir;
    }
}
