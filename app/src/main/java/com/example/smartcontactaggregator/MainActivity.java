package com.example.smartcontactaggregator;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.provider.CallLog.Calls.CACHED_NAME;
import static android.provider.CallLog.Calls.DATE;
import static android.provider.CallLog.Calls.DURATION;
import static android.provider.CallLog.Calls.INCOMING_TYPE;
import static android.provider.CallLog.Calls.MISSED_TYPE;
import static android.provider.CallLog.Calls.NUMBER;
import static android.provider.CallLog.Calls.OUTGOING_TYPE;
import static android.provider.CallLog.Calls.TYPE;

import org.hashids.Hashids;

public class MainActivity extends AppCompatActivity {

    Button startButton;
    EditText nameTextBox;
    CheckBox consentCheckbox;
    String[] SMSRecords;
    String[] callRecords;
    Hashids hashids;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialise elements
        startButton = findViewById(R.id.startButton);
        nameTextBox = findViewById(R.id.nameTextBox);
        consentCheckbox = findViewById(R.id.consentCheckbox);

        // initialise the hashid
        hashids = new Hashids("Samsung Worklet");


        // fetch records
        if(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_CALL_LOG)!= PackageManager.PERMISSION_GRANTED&&ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_SMS)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_CALL_LOG,Manifest.permission.READ_SMS},1);
        }
        else
        {
            callRecords=getCallDetails();
            SMSRecords=getSMSData();
        }

        // on click listener for start button
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // check if name and checkbox are filled
                String name = nameTextBox.getText().toString().trim();
                if (name.length() > 0 && consentCheckbox.isChecked()) {

                    // move to the aggregator page after passing the records
                    Intent aggregatorOpener = new Intent(MainActivity.this, Aggregator.class);
                    aggregatorOpener.putExtra("callRecords", callRecords);
                    aggregatorOpener.putExtra("SMSRecords", SMSRecords);
                    startActivity(aggregatorOpener);
                }else {
                    Toast.makeText(getApplicationContext(), "Please fill all the details.", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String[] getCallDetails() {
        String[] callRecordArray = new String[10];
        int k = 0;

        String[] projection = new String[]{
                CACHED_NAME,
                NUMBER,
                TYPE,
                DATE,
                DURATION
        };

        try (@SuppressLint("MissingPermission") Cursor managedCursor = getApplicationContext().getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, null, null, null)) {
            if (managedCursor != null) {
                while (managedCursor.moveToNext() && k < 10) {
                    String name = managedCursor.getString(0);
                    String number = managedCursor.getString(1);
                    String type = managedCursor.getString(2);
                    String date = managedCursor.getString(3);
                    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.ENGLISH);
                    String dateString = formatter.format(new Date(Long.parseLong(date)));
                    String duration = managedCursor.getString(4);
                    String dir = null;
                    int dircode = Integer.parseInt(type);
                    switch (dircode) {
                        case OUTGOING_TYPE:
                            dir = "OUTGOING";
                            break;

                        case INCOMING_TYPE:
                            dir = "INCOMING";
                            break;

                        case MISSED_TYPE:
                            dir = "MISSED";
                            break;
                    }
                    // append to array if element does not exist already.
                    if (name.length() > 0) {
                        //hash phone number
                        if (number.length() > 10) {
                            number = number.substring(3);
                        }
                        long numberInt = Long.parseLong(number);
                        String hashedNumber = hashids.encode(numberInt);

                        callRecordArray[k++] = "\n" + name + "," + hashedNumber + "," + dir + "," + dateString + "," + duration;
                    }
                }
            }
            System.out.println("\n----------------CALL RECORD ARRAY-------------");
            System.out.println("Length of Call Record Array : " + callRecordArray.length);
            for (String callRecord : callRecordArray) {
                System.out.println(callRecord);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return callRecordArray;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String[] getSMSData(){
        String[] SMSRecordArray = new String[10];
        int k = 0;
        String[] projection = new String[] { "_id", "address", "person", "body", "date", "type" };
        String INBOX = "content://sms/inbox";
        // String SENT = "content://sms/sent";
        // String DRAFT = "content://sms/draft";

        try (Cursor managedCursor = getContentResolver().query(Uri.parse(INBOX), projection, null, null, null)) {
            if (managedCursor != null) {
                while (managedCursor.moveToNext() && k < 10) {
                    String id = managedCursor.getString(0);
                    String number = managedCursor.getString(1);
                    String person = managedCursor.getString(2);
                    String body = managedCursor.getString(3);
                    String date = managedCursor.getString(4);
                    String type = managedCursor.getString(5);
                    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.ENGLISH);
                    String dateString = formatter.format(new Date(Long.parseLong(date)));
                    //removing the commas from the SMS body so that it does not interfere with the csv file format
                    body = body.replace(',','.');
                    body = body.replace('\n',' ');
                    if (person != null) {
                        //hash phone number
                        if (number.length() > 10) {
                            number = number.substring(3);
                        }
                        long numberInt = Long.parseLong(number);
                        String hashedNumber = hashids.encode(numberInt);
                        SMSRecordArray[k++] = "\n" +  id + "," + hashedNumber + "," + person + "," + body + "," + dateString + "," + type;
                    }
                }//gives number of records
            }
            //add to the view
            System.out.println("----------------SMS RECORD ARRAY-------------");
            System.out.println("Length of Call SMS Array : " + SMSRecordArray.length);
            for (String SMSRecord : SMSRecordArray) {
                System.out.println(SMSRecord);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SMSRecordArray;
    }
}