package com.fox2code.mmm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.core.content.FileProvider;

import com.fox2code.foxcompat.app.FoxActivity;
import com.fox2code.mmm.utils.io.Http;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import io.noties.markwon.Markwon;
import timber.log.Timber;

@SuppressWarnings("UnnecessaryReturnStatement")
public class UpdateActivity extends FoxActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);
        // Get the progress bar and make it indeterminate for now
        LinearProgressIndicator progressIndicator = findViewById(R.id.update_progress);
        progressIndicator.setIndeterminate(true);
        // get update_cancel button
        MaterialButton updateCancel = findViewById(R.id.update_cancel_button);
        // get status text view
        MaterialTextView statusTextView = findViewById(R.id.update_progress_text);
        // set status text to please wait
        statusTextView.setText(R.string.please_wait);
        // for debug builds, set update_debug_warning to visible and return
        /**if (BuildConfig.DEBUG) {
         findViewById(R.id.update_debug_warning).setVisibility(MaterialTextView.VISIBLE);
         progressIndicator.setIndeterminate(false);
         progressIndicator.setProgressCompat(0, false);
         statusTextView.setVisibility(MaterialTextView.INVISIBLE);
         return;
         }*/
        Thread updateThread = new Thread() {
            public void run() {
                // Now, parse the intent
                String extras = getIntent().getAction();
                // if extras is null, then we are in a bad state or user launched the activity manually
                if (extras == null) {
                    runOnUiThread(() -> {
                        // set status text to error
                        statusTextView.setText(R.string.error_no_extras);
                        // set progress bar to error
                        progressIndicator.setIndeterminate(false);
                        progressIndicator.setProgressCompat(0, false);
                    });
                    return;
                }

                // get action
                ACTIONS action = ACTIONS.valueOf(extras);
                // if action is null, then we are in a bad state or user launched the activity manually
                if (Objects.isNull(action)) {
                    runOnUiThread(() -> {
                        // set status text to error
                        statusTextView.setText(R.string.error_no_action);
                        // set progress bar to error
                        progressIndicator.setIndeterminate(false);
                        progressIndicator.setProgressCompat(0, false);
                    });
                    // return
                    return;
                }

                // For check action, we need to check if there is an update using the AppUpdateManager.peekShouldUpdate()
                if (action == ACTIONS.CHECK) {
                    checkForUpdate();
                } else if (action == ACTIONS.DOWNLOAD) {
                    try {
                        downloadUpdate();
                    } catch (
                            JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            // set status text to error
                            statusTextView.setText(R.string.error_download_update);
                            // set progress bar to error
                            progressIndicator.setIndeterminate(false);
                            progressIndicator.setProgressCompat(100, false);
                        });
                    }
                } else if (action == ACTIONS.INSTALL) {
                    // ensure path was passed and points to a file within our cache directory
                    String path = getIntent().getStringExtra("path");
                    if (path == null) {
                        runOnUiThread(() -> {
                            // set status text to error
                            statusTextView.setText(R.string.no_file_found);
                            // set progress bar to error
                            progressIndicator.setIndeterminate(false);
                            progressIndicator.setProgressCompat(0, false);
                        });
                        return;
                    }
                    File file = new File(path);
                    if (!file.exists()) {
                        runOnUiThread(() -> {
                            // set status text to error
                            statusTextView.setText(R.string.no_file_found);
                            // set progress bar to error
                            progressIndicator.setIndeterminate(false);
                            progressIndicator.setProgressCompat(0, false);
                        });
                        // return
                        return;
                    }
                    if (!Objects.equals(file.getParentFile(), getCacheDir())) {
                        // set status text to error
                        runOnUiThread(() -> {
                            statusTextView.setText(R.string.no_file_found);
                            // set progress bar to error
                            progressIndicator.setIndeterminate(false);
                            progressIndicator.setProgressCompat(0, false);
                        });
                        // return
                        return;
                    }
                    // set status text to installing
                    statusTextView.setText(R.string.installing_update);
                    // set progress bar to indeterminate
                    progressIndicator.setIndeterminate(true);
                    // install update
                    installUpdate(file);
                }
            }
        };
        // on click, finish the activity and anything running in it
        updateCancel.setOnClickListener(v -> {
            // end any download
            updateThread.interrupt();
            forceBackPressed();
            finish();
        });
        updateThread.start();
    }

    public void checkForUpdate() {
        LinearProgressIndicator progressIndicator = findViewById(R.id.update_progress);
        progressIndicator.setIndeterminate(true);
        // get status text view
        MaterialTextView statusTextView = findViewById(R.id.update_progress_text);
        // set status text to checking for update
        statusTextView.setText(R.string.checking_for_update);
        // set progress bar to indeterminate
        progressIndicator.setIndeterminate(true);
        // check for update
        boolean shouldUpdate = AppUpdateManager.getAppUpdateManager().peekShouldUpdate();
        // if shouldUpdate is true, then we have an update
        if (shouldUpdate) {
            // set status text to update available
            statusTextView.setText(R.string.update_available);
            // set button text to download
            MaterialButton button = findViewById(R.id.update_button);
            button.setText(R.string.download_update);
            // return
        } else {
            // set status text to no update available
            statusTextView.setText(R.string.no_update_available);
            // set progress bar to error
            // return
        }
        progressIndicator.setIndeterminate(false);
        progressIndicator.setProgressCompat(100, false);
        return;
    }

    public void downloadUpdate() throws JSONException {
        LinearProgressIndicator progressIndicator = findViewById(R.id.update_progress);
        progressIndicator.setIndeterminate(true);
        // get status text view
        MaterialTextView statusTextView = findViewById(R.id.update_progress_text);
        byte[] lastestJSON = new byte[0];
        try {
            lastestJSON = Http.doHttpGet(AppUpdateManager.RELEASES_API_URL, false);
        } catch (
                Exception e) {
            // when logging, REMOVE the json from the log
            String msg = e.getMessage();
            // remove everything from the first { to the last }
            msg = Objects.requireNonNull(msg).substring(0, msg.indexOf("{")) + msg.substring(msg.lastIndexOf("}") + 1);
            Timber.e(msg);
            progressIndicator.setIndeterminate(false);
            progressIndicator.setProgressCompat(100, false);
            statusTextView.setText(R.string.error_download_update);
        }
        // convert to JSON
        JSONObject latestJSON = new JSONObject(new String(lastestJSON));
        String changelog = latestJSON.getString("body");
        // set changelog text. changelog could be markdown, so we need to convert it to HTML
        MaterialTextView changelogTextView = findViewById(R.id.update_changelog);
        final Markwon markwon = Markwon.builder(this).build();
        runOnUiThread(() -> markwon.setMarkdown(changelogTextView, changelog));
        // we already know that there is an update, so we can get the latest version of our architecture. We're going to have to iterate through the assets to find the one we want
        JSONArray assets = latestJSON.getJSONArray("assets");
        // get the asset we want
        JSONObject asset = null;
        // iterate through assets until we find the one that contains Build.SUPPORTED_ABIS[0]
        while (Objects.isNull(asset)) {
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset1 = assets.getJSONObject(i);
                if (asset1.getString("name").contains(Build.SUPPORTED_ABIS[0])) {
                    asset = asset1;
                    break;
                }
            }
        }
        // if asset is null, then we are in a bad state
        if (Objects.isNull(asset)) {
            // set status text to error
            statusTextView.setText(R.string.error_no_asset);
            // set progress bar to error
            progressIndicator.setIndeterminate(false);
            progressIndicator.setProgressCompat(100, false);
            // return
            return;
        }
        // get the download url
        String downloadUrl = Objects.requireNonNull(asset).getString("browser_download_url");
        // get the download size
        long downloadSize = asset.getLong("size");
        runOnUiThread(() -> {
            // set status text to downloading update
            statusTextView.setText(getString(R.string.downloading_update, 0));
            // set progress bar to 0
            progressIndicator.setIndeterminate(false);
            progressIndicator.setProgressCompat(0, false);
        });
        // download the update
        byte[] update = new byte[0];
        try {
            update = Http.doHttpGet(downloadUrl, (downloaded, total, done) -> runOnUiThread(() -> {
                // update progress bar
                progressIndicator.setProgressCompat((int) (((float) downloaded / (float) total) * 100), true);
                // update status text
                statusTextView.setText(getString(R.string.downloading_update, (int) (((float) downloaded / (float) total) * 100)));
            }));
        } catch (
                Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                progressIndicator.setIndeterminate(false);
                progressIndicator.setProgressCompat(100, false);
                statusTextView.setText(R.string.error_download_update);
            });
        }
        // if update is null, then we are in a bad state
        if (Objects.isNull(update)) {
            runOnUiThread(() -> {
                // set status text to error
                statusTextView.setText(R.string.error_download_update);
                // set progress bar to error
                progressIndicator.setIndeterminate(false);
                progressIndicator.setProgressCompat(100, false);
            });
            // return
            return;
        }
        // if update is not the same size as the download size, then we are in a bad state
        if (update.length != downloadSize) {
            runOnUiThread(() -> {
                // set status text to error
                statusTextView.setText(R.string.error_download_update);
                // set progress bar to error
                progressIndicator.setIndeterminate(false);
                progressIndicator.setProgressCompat(100, false);
            });
            // return
            return;
        }
        // set status text to installing update
        runOnUiThread(() -> {
            statusTextView.setText(R.string.installing_update);
            // set progress bar to 100
            progressIndicator.setIndeterminate(true);
            progressIndicator.setProgressCompat(100, false);
        });
        // save the update to the cache
        File updateFile = null;
        try {
            updateFile = new File(getCacheDir(), "update.apk");
            FileOutputStream fileOutputStream = new FileOutputStream(updateFile);
            fileOutputStream.write(update);
            fileOutputStream.close();
        } catch (
                IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                progressIndicator.setIndeterminate(false);
                progressIndicator.setProgressCompat(100, false);
                statusTextView.setText(R.string.error_download_update);
            });
        }
        // install the update
        installUpdate(updateFile);
        // return
        return;
    }

    @SuppressLint("RestrictedApi")
    private void installUpdate(File updateFile) {
        // get status text view
        runOnUiThread(() -> {
            MaterialTextView statusTextView = findViewById(R.id.update_progress_text);
            // set status text to installing update
            statusTextView.setText(R.string.installing_update);
            // set progress bar to 100
            LinearProgressIndicator progressIndicator = findViewById(R.id.update_progress);
            progressIndicator.setIndeterminate(true);
            progressIndicator.setProgressCompat(100, false);
        });
        // request install permissions
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        Context context = getApplicationContext();
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".file-provider", updateFile);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        // return
        return;
    }

    public enum ACTIONS {
        // action can be CHECK, DOWNLOAD, INSTALL
        CHECK, DOWNLOAD, INSTALL
    }
}