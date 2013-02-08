package com.example.googledrivedemo;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class DemoActivity extends Activity {

    String TAG = getClass().getSimpleName();
    String filename = "demo.txt";
    String CONTENT_TYPE = "text/plain";
    final static int REQUEST_ACCOUNT_PICKER = 1;
    final static int REQUEST_AUTHORIZATION = 2;

    private static Drive service;
    private GoogleAccountCredential credential;
    private boolean useTask = false;
    java.io.File demoFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Log.i(TAG, String.format("Package name:%s", getApplication().getPackageName()));

        extractDemoFile();
        credential = GoogleAccountCredential.usingOAuth2(this, DriveScopes.DRIVE);
        Button taskButton = (Button) findViewById(R.id.button_task);
        taskButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View arg0) {
                useTask = true;
                startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
            }});
        Button threadButton = (Button) findViewById(R.id.button_thread);
        threadButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                useTask = false;
                startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
            }});
    }

    private void extractDemoFile() {

        demoFile = new java.io.File(getApplicationContext().getFilesDir(), filename);
        if(demoFile.exists()){
            return;
        }

        try {
            InputStream input = getAssets().open(filename);
            FileOutputStream output = new FileOutputStream(demoFile);
            int length;
            byte[] buffer = new byte[4096];
            while((length = input.read(buffer)) > 0){
                output.write(buffer, 0, length);
            }
            input.close();
            output.close();
        } catch (IOException e) {
            Log.i(TAG, e.toString());
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        credential.setSelectedAccountName(accountName);
                        service = getDriveService(credential);
                        if(useTask){
                            saveFileToDriveTask();
                        } else {
                            saveFileToDriveThread();
                        }
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == Activity.RESULT_OK) {
                    if(useTask){
                        saveFileToDriveTask();
                    } else {
                        saveFileToDriveThread();
                    }
                } else {
                    startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
                }
                break;
        }
    }

    private void saveFileToDriveTask(){

        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>(){
            protected Boolean doInBackground(Void... params) {
                boolean success = false;
                try {
                    FileContent content = new FileContent(CONTENT_TYPE, demoFile);
                    File body = new File();
                    body.setTitle(demoFile.getName());
                    body.setMimeType(CONTENT_TYPE);
                    Drive.Files files = service.files();
                    Drive.Files.Insert insert = files.insert(body, content);
                    File file = insert.execute();
                    success = file != null;
                } catch (UserRecoverableAuthIOException e) {
                    startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return success;
            }

            protected void onPostExecute(Boolean success){
                showToast(String.format("Upload status:%s", success));
            }
        };
        task.execute();
    }

    private void saveFileToDriveThread() {

        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    FileContent content = new FileContent(CONTENT_TYPE, demoFile);
                    File body = new File();
                    body.setTitle(demoFile.getName());
                    body.setMimeType(CONTENT_TYPE);
                    Drive.Files files = service.files();
                    Drive.Files.Insert insert = files.insert(body, content);
                    File file = insert.execute();
                    showToast(String.format("Upload status:%s", file != null));
                } catch (UserRecoverableAuthIOException e) {
                    startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    private Drive getDriveService(GoogleAccountCredential credential) {
        return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                .build();
    }

    public void showToast(final String toast) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

