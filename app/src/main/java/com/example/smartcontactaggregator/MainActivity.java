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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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

    HashMap<String, String> nameHash = new HashMap<String, String>();
    HashMap<String, Integer> idHash = new HashMap<String, Integer>();
    int maxId = 1;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialise elements
        startButton = findViewById(R.id.startButton);
        nameTextBox = findViewById(R.id.nameTextBox);
        consentCheckbox = findViewById(R.id.consentCheckbox);

        // initialise the hash id
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
                    aggregatorOpener.putExtra("nameHash", nameHash);
                    aggregatorOpener.putExtra("idHash", idHash);

                    startActivity(aggregatorOpener);
                }else {
                    Toast.makeText(getApplicationContext(), "Please fill all the details.", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode){
            case 1:{
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED&&grantResults[1]==PackageManager.PERMISSION_GRANTED){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        callRecords=getCallDetails();
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        SMSRecords=getSMSData();
                    }
                }
            }
            default:super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String[] getCallDetails() {
        //String[] callRecordArray = new String[10];

        DynamicArray callRecordArray = new DynamicArray();
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
                    String dateStr = formatter.format(new Date(Long.parseLong(date)));
                    String  dateString = unixTime(dateStr).toString();
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
                    if (name != null || name != "null"|| name.length() > 0 ) {
                        //hash phone number
                        if (number.length() > 10) {
                            number = number.substring(3);
                        }
                        long numberInt = Long.parseLong(number);
                        String hashedNumber = hashids.encode(numberInt);

                        if(hashedNumber != null){
                            if(nameHash.containsKey(hashedNumber) == false){
                                nameHash.put(hashedNumber, name);
                                //System.out.println("Inside :   " + name + "   " + number);
                            }
                            if(idHash.containsKey(hashedNumber) == false){
                                idHash.put(hashedNumber, maxId);
                                maxId += 1;
                            }
                        }

                        callRecordArray.add(idHash.get(hashedNumber) + "," + name + "," + dir + "," + dateString + "," + duration+"\n");
                    }
                }
            }
            callRecordArray.shrinkSize();
            System.out.println("\n----------------CALL RECORD ARRAY-------------");
            System.out.println("Length of Call Record Array : " + callRecordArray.array.length);
            for (String callRecord : callRecordArray.array) {
                System.out.println(callRecord);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return callRecordArray.array;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String[] getSMSData(){
        //String[] SMSRecordArray = new String[10];
        DynamicArray SMSRecordArray = new DynamicArray();
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
                    String dateStr = formatter.format(new Date(Long.parseLong(date)));
                    String  dateString = unixTime(dateStr).toString();
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
                        if(hashedNumber != null){
                            if(idHash.containsKey(hashedNumber) == false){
                                idHash.put(hashedNumber, maxId);
                                maxId += 1;
                            }
                            if(nameHash.containsKey(hashedNumber)){
                                SMSRecordArray.add(idHash.get(hashedNumber) + "," +  nameHash.get(hashedNumber)+ "," + body + "," + dateString +"\n");
                            }
                        }
                        //SMSRecordArray[k++] = "\n" +  id + "," + hashedNumber + "," + person + "," + body + "," + dateString + "," + type;
                    }
                }//gives number of records
            }
            SMSRecordArray.shrinkSize();
            //add to the view
            System.out.println("----------------SMS RECORD ARRAY-------------");
            System.out.println("Length of Call SMS Array : " + SMSRecordArray.array.length);
            for (String SMSRecord : SMSRecordArray.array) {
                System.out.println(SMSRecord);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SMSRecordArray.array;
    }
    public static Long unixTime(String timestamp){
        if(timestamp == null) return null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.ENGLISH);
            Date dt = sdf.parse(timestamp);
            long epoch = dt.getTime();
            //return (int)(epoch/1000);
            return epoch;
        } catch(ParseException e) {
            return null;
        }
    }
    public static class DynamicArray {

        public String array[];
        public int count;
        public int size;

        public DynamicArray() {
            array = new String[1];
            count = 0;
            size = 1;
        }

        public void add(String data) {
            if (count == size) {
                growSize();
            }
            array[count] = data;
            count++;
        }

        public void growSize() {

            String temp[] = null;
            if (count == size) {
                temp = new String[size * 2];
                {
                    for (int i = 0; i < size; i++) {
                        temp[i] = array[i];
                    }
                }
            }
            array = temp;
            size = size * 2;
        }
        public void shrinkSize() {
            String temp[] = null;
            if (count > 0) {
                temp = new String[count];
                for (int i = 0; i < count; i++) {
                    temp[i] = array[i];
                }
                size = count;
                array = temp;
            }
        }
    }
}