package com.example.smartcontactaggregator;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.Iterator;

public class ResultDisplay extends AppCompatActivity {

    String response;

    TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_display);

        // initialise the elements
        resultTextView = findViewById(R.id.resultTextView);

        // get the results from the previous page
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            response = extras.getString("RESPONSE");
        }

        // parse json and display the result
        resultTextView.setText(response);

    }
}