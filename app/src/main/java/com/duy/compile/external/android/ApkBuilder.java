package com.duy.compile.external.android;

import android.util.Log;

import com.android.annotations.NonNull;
import com.duy.compile.external.CommandManager;
import com.duy.project.file.android.AndroidProjectFile;
import com.duy.project.file.android.KeyStore;
import com.spartacusrex.spartacuside.external.apkbuilder;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.tools.DiagnosticCollector;

import kellinwood.security.zipsigner.ZipSigner;
import kellinwood.security.zipsigner.optional.CustomKeySigner;


public class ApkBuilder {
    private static final String TAG = "BuildTask";

    private static void buildApk(AndroidProjectFile projectFile) throws Exception {
        String[] args = {
                projectFile.getApkUnsigned().getPath(),
                "-v", "-u", "-z", projectFile.getResourceFile().getPath(),
                "-f", projectFile.getDexedClassesFile().getPath()
        };
        Log.d(TAG, "buildApk args = " + Arrays.toString(args));
        apkbuilder.main(args);
    }

    public static void build(AndroidProjectFile projectFile, @NonNull OutputStream out,
                             @NonNull DiagnosticCollector diagnosticCollector,
                             @NonNull SignProgress signProgress) {
        projectFile.clean();
        PrintStream systemOut = System.out;
        PrintStream systemErr = System.err;
        try {

            System.setOut(new PrintStream(out));
            System.setErr(new PrintStream(out));

            //create R.java
            ApkBuilder.runAidl(projectFile);
            ApkBuilder.runAapt(projectFile);

            //compile java
            CommandManager.compileJava(projectFile, new PrintWriter(out), diagnosticCollector);
            System.gc();

            //classes to dex
            CommandManager.dexLibs(projectFile, true);
            CommandManager.dexBuildClasses(projectFile);
            CommandManager.dexMerge(projectFile);

            //zip apk
            ApkBuilder.buildApk(projectFile);
            ApkBuilder.zipSign(projectFile, signProgress);
            ApkBuilder.zipAlign();
            ApkBuilder.publishApk();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.setErr(systemErr);
        System.setOut(systemOut);
    }

    private static void runAidl(AndroidProjectFile projectFile) throws Exception {
        Log.d(TAG, "runAidl() called");

        // TODO make aidl.so
    }

    private static void runAapt(AndroidProjectFile projectFile) throws Exception {
        Log.d(TAG, "runAapt() called");

        Aapt aapt = new Aapt();
        String command = "aapt p -f -v" +
                " -M " + projectFile.xmlManifest.getPath() + //manifest file
                " -F " + projectFile.getResourceFile().getPath() + //
                " -I " + projectFile.classpathFile.getPath() + //include
                " -A " + projectFile.getDirAssets().getPath() + //assets dir
                " -S " + projectFile.getDirRes().getPath() + //resource dir
                " -J " + projectFile.getClassR().getParent();//out R.java dir
        File dirLibs = projectFile.getDirLibs();
        File[] files = dirLibs.listFiles();
        if (files != null) {
            for (File lib : files) {
                if (lib.isFile()) {
                    if (lib.getPath().endsWith(".jar")) {
                        command += " -I " + lib.getPath();
                    } else if (lib.getPath().endsWith(".aar")) {
                        command += " -I " + lib.getPath() + File.separator + "res";
                    }
                }
            }
        }
        Log.d(TAG, "runAapt command = " + command);
        int exitCode = aapt.fnExecute(command);
        if (exitCode != 0) {
            throw new Exception("AAPT exit(" + exitCode + ")");
        }

    }

    private static void zipSign(AndroidProjectFile projectFile, SignProgress signProgress) throws Exception {
        Log.d(TAG, "zipSign() called with: projectFile = [" + projectFile + "], signProgress = [" + signProgress + "]");

//        if (!appContext.getString(R.string.keystore).contentEquals(projectFile.jksEmbedded.getName())) {
//             TODO use user defined certificate
//        }

        // use embedded private key
        KeyStore keyStore = projectFile.getKeyStore();
        String keystorePath = keyStore.getFile().getPath();
        char[] keystorePw = keyStore.getPassword();
        String certAlias = keyStore.getCertAlias();
        char[] certPw = keyStore.getCertPassword();
        String signatureAlgorithm = "SHA1withRSA";

        ZipSigner zipsigner = new ZipSigner();
        zipsigner.addProgressListener(signProgress);
        CustomKeySigner.signZip(zipsigner, keystorePath, keystorePw, certAlias,
                certPw, signatureAlgorithm,
                projectFile.getApkUnsigned().getPath(),
                projectFile.getApkUnaligned().getPath());
    }


    private static void zipAlign() throws Exception {
//         TODO make zipalign.so
    }

    private static void publishApk() throws Exception {
//        if (projectFile.apkRedistributable.exists()) {
//            projectFile.apkRedistributable.delete();
//        }
//        Util.copy(projectFile.apkUnaligned, new FileOutputStream(projectFile.apkRedistributable));
//
//        projectFile.apkRedistributable.setReadable(true, false);
    }

    public void run() {


    }

    public static class SignProgress implements kellinwood.security.zipsigner.ProgressListener {

        public void onProgress(kellinwood.security.zipsigner.ProgressEvent event) {
        }

    }

}