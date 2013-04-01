
package com.aokp.gerrit.shell;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * An abstract implentation of AsyncTask
 *
 * since our needs are simple send a command, perform a task when we finish
 * this implentation requires you send the command as String...
 * in the .execute(String) so you can send String[] of commands if needed
 *
 * This class is not for you if...
 *     1) You do not need to perform any action after command execution
 *        you want a Thread not this.
 *     2) You need to perform more complex tasks in doInBackground
 *        than simple script/command sequence of commands
 *        you want your own AsyncTask not this.
 *
 * This class is for you if...
 *     1) You need to run a command/script/sequence of commands without
 *        blocking the UI thread and you must perform actions after the
 *        task completes.
 *     2) see #1.
 */
public abstract class AbstractAsyncSuCMDProcessor extends AsyncTask<String, Void, String> {
    private static final String TAG = AbstractAsyncSuCMDProcessor.class.getSimpleName();
    public boolean mElevatedPrivilates;
    // if /system needs to be mounted before command
    private boolean mMountSystem;
    // su terminal we execute on
    private CMDProcessor mTerm;
    // return if we recieve a null command or empty command
    public final String FAILURE = "failed_no_command";

    /**
     * Constructor that allows mounting/dismounting
     * of /system partition while in background thread
     */
    public AbstractAsyncSuCMDProcessor(boolean mountSystem) {
         this.mMountSystem = mountSystem;
    }

    /**
     * Constructor that assumes /system should not be mounted
     */
    public AbstractAsyncSuCMDProcessor() {
         this.mMountSystem = false;
    }

    /**
     * DO NOT override this method you should simply send your commands off
     * as params and expect to handle results in {@link #onPostExecute}
     *
     * if you find a need to @Override this method then you should
     * consider using a new AsyncTask implentation instead
     *
     * @param params The parameters of the task.
     *
     * @return A result, defined by the subclass of this task.
     */
    @Override
    protected String doInBackground(String... params) {
        // don't bother if we don't get a command
        if (params[0] == null || params[0].trim().equals(""))
            return FAILURE;

        mTerm = new CMDProcessor();
        String stdout = null;

        // conditionally enforce mounting
        if (mMountSystem) {
            getMount("rw");
        }
        try {
            // process all commands ***DO NOT SEND null OR ""; you have been warned***
            for (int i = 0; params.length > i; i++) {
                // always watch for null and empty strings, lazy devs :/
                if (params[i] != null && !params[i].trim().equals("")) {
                    if (mElevatedPrivilates)
                        stdout = mTerm.su.runWaitFor(params[i]).getStdout();
                    else
                        stdout = mTerm.sh.runWaitFor(params[i]).getStdout();
                } else {
                    // bail because of careless devs
                    return FAILURE;
                }
            }
        // always unmount
        } finally {
            if (mMountSystem)
                getMount("ro");
        }
        // return the stdout from the command
        return stdout;
    }

    /**
     * <p>Runs on the UI thread after {@link #doInBackground}. The
     * specified result is the value returned by {@link #doInBackground}.</p>
     *
     * <p>This method won't be invoked if the task was cancelled.</p>
     *
     * You MUST @Override this method if you don't need the result
     * then you should consider using a new Thread implentation instead
     *
     * @param result The result of the operation computed by {@link #doInBackground}.
     */
    @Override
    protected abstract void onPostExecute(String result);

    static String[] getMounts(CharSequence path) {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader("/proc/mounts"), 256);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(path)) {
                    return line.split(" ");
                }
            }
        } catch (FileNotFoundException ignored) {
            Log.d(TAG, "/proc/mounts does not exist");
        } catch (IOException ignored) {
            Log.d(TAG, "Error reading /proc/mounts");
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ignored) {
                    // ignored
                }
            }
        }
        return null;
    }

    static boolean getMount(String mount) {
        CMDProcessor cmd = new CMDProcessor();
        String[] mounts = getMounts("/system");
        if (mounts != null && mounts.length >= 3) {
            String device = mounts[0];
            String path = mounts[1];
            String point = mounts[2];
            Executable preferredMountCmd = new Executable("mount -o " + mount + ",remount -t " + point + ' ' + device + ' ' + path);
            if (cmd.su.runWaitFor(preferredMountCmd).success()) {
                return true;
            }
        }
        Executable fallbackMountCmd = new Executable("busybox mount -o remount," + mount + " /system");
        return cmd.su.runWaitFor(fallbackMountCmd).success();
    }
}

