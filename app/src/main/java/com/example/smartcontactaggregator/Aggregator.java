package com.example.smartcontactaggregator;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Aggregator extends AppCompatActivity {

    StringBuilder callRecordsCommaSeparated;
    StringBuilder SMSRecordsCommaSeparated;
    String[] callRecords, SMSRecords;
    TextView results;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aggregator);

        // get the records from the previous page
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            callRecords = extras.getStringArray("callRecords");
            SMSRecords = extras.getStringArray("SMSRecords");
            System.out.println("Length of Call Record Array : " + callRecords.length);
            System.out.println("Length of SMS record Array : " + SMSRecords.length);
        }else{
            System.out.println("No records found!");
        }

        // build the string to convert to a csv
        callRecordsCommaSeparated = new StringBuilder();
        for (String record: callRecords){
            callRecordsCommaSeparated.append(record);
        }

        SMSRecordsCommaSeparated = new StringBuilder();
        for (String record: SMSRecords){
            SMSRecordsCommaSeparated.append(record);
        }

        // write strings to files
        writeToFile(callRecordsCommaSeparated.toString(), "Call_Records.csv", getApplicationContext());
        writeToFile(SMSRecordsCommaSeparated.toString(), "SMS_Records.csv", getApplicationContext());

        // get the files
        File path = getApplicationContext().getFilesDir();
        File callRecordsFile = new File(path, "Call_Records.csv");
        File SMSRecordsFile = new File(path, "SMS_Records.csv");

        // make the HTTP request
        String endpoint = "https://samsung-worklet.herokuapp.com/file-upload-api";
        uploadRecords(endpoint, callRecordsFile, SMSRecordsFile);

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void writeToFile(String data, String fileName, Context context) {
        File path = context.getFilesDir();
        File file = new File(path, fileName);
        try (FileOutputStream stream = new FileOutputStream(file))  {
            stream.write(data.getBytes());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Boolean uploadRecords(String serverURL, File file1, File file2) {
        try {

            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("calls_file", file1.getName(),
                            RequestBody.create(MediaType.parse("text/csv"), file1))
                    .addFormDataPart("SMS_file", file2.getName(),
                            RequestBody.create(MediaType.parse("text/csv"), file2))
                    .build();

            Request request = new Request.Builder()
                    .url(serverURL)
                    .post(requestBody)
                    .build();

            OkHttpClient client = new OkHttpClient();
            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(final Call call, final IOException e) {
                    System.out.println("ERROR OCCURRED." + e);
                }

                @Override
                public void onResponse(final Call call, final Response response) throws IOException {
                    String serverResponse;
                    if (!response.isSuccessful()) {
                        serverResponse = response.body().string();
                        System.out.println("SERVER ERROR OCCURRED: " + serverResponse);
                        results = findViewById(R.id.resultsView);
                        results.setText(serverResponse);
                    } else {
                        serverResponse = response.body().string();
                        System.out.println("UPLOAD SUCCESSFUL.");
                        System.out.println(serverResponse);

                        // set the response in the text view
                        results = findViewById(R.id.resultsView);
                        results.setText(serverResponse);
                    }
                }
            });

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}