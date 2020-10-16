package com.example.smartcontactaggregator;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Objects;

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
    HashMap<String, String> nameHash = new HashMap<String, String>();
    HashMap<String, Integer> idHash = new HashMap<String, Integer>();

    TextView loadingView;
    ProgressBar loader;
    Button viewResultsButton;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aggregator);

        // initialise elements
        loadingView = findViewById(R.id.loadingView);
        loader = findViewById(R.id.loader);
        viewResultsButton = findViewById(R.id.viewResultsButton);

        // needed for creating the hashes
        final Intent inte = getIntent();

        // get the records from the previous page
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            callRecords = extras.getStringArray("callRecords");
            SMSRecords = extras.getStringArray("SMSRecords");
            System.out.println("Length of Call Record Array : " + callRecords.length);
            System.out.println("Length of SMS record Array : " + SMSRecords.length);
            nameHash = (HashMap<String, String>)inte.getSerializableExtra("nameHash");
            idHash = (HashMap<String, Integer>)inte.getSerializableExtra("idHash");
        }else{
            System.out.println("No records found!");
        }

        // build the string to convert to a csv
        callRecordsCommaSeparated = new StringBuilder();
        callRecordsCommaSeparated.append("Id, Name, CallType, Date_Time, Duration\n");
        for (String record: callRecords){
            if(record != null || record != "null")
                callRecordsCommaSeparated.append(record);
        }

        SMSRecordsCommaSeparated = new StringBuilder();
        SMSRecordsCommaSeparated.append("ID, Name, Message, Date_Time\n");
        for (String record: SMSRecords){
            if(record != null || record != "null")
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

            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("calls_file", callRecordsFile.getName(),
                            RequestBody.create(MediaType.parse("text/csv"), callRecordsFile))
                    .addFormDataPart("SMS_file", SMSRecordsFile.getName(),
                            RequestBody.create(MediaType.parse("text/csv"), SMSRecordsFile))
                    .build();

            Request request = new Request.Builder()
                    .url(endpoint)
                    .post(requestBody)
                    .build();

            OkHttpClient client = new OkHttpClient();
            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(final Call call, final IOException e) {

                    // display error on failure
                    System.out.println("ERROR OCCURRED." + e);
                }

                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public void onResponse(@NotNull final Call call, @NotNull final Response response) throws IOException {
                    if (response.isSuccessful()) {
                        final String myResponse = Objects.requireNonNull(response.body()).string();
                        Aggregator.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try{
                                    System.out.println("DEBUG RESPONSE: " + myResponse);

                                    // hide the loading entities
                                    loadingView.setVisibility(View.GONE);
                                    loader.setVisibility(View.GONE);

                                    // display the button to the result page
                                    viewResultsButton.setVisibility(View.VISIBLE);
                                    viewResultsButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent resultsDisplayOpener = new Intent(Aggregator.this, ResultDisplay.class);
                                            resultsDisplayOpener.putExtra("RESPONSE", myResponse);
                                            startActivity(resultsDisplayOpener);
                                        }
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } else {
                        // display incorrect file type errors to the user
                        Aggregator.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loader.setVisibility(View.GONE);
                                loadingView.setText("An unexpected error occurred.");
                            }
                        });
                        System.out.println("DEBUG RESPONSE: " + response.body().string());
                    }
                }
            });

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
}