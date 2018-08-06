package com.example.sameer.group10;

import android.content.Intent;
import android.view.Menu;
import android.widget.EditText;
import android.widget.RadioButton;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.Button;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    String table;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn = (Button) findViewById(R.id.start);
        btn.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View view) {
                                           EditText nameBox = (EditText) findViewById(R.id.name);
                                           EditText ageBox = (EditText) findViewById(R.id.age);
                                           EditText idBox = (EditText) findViewById(R.id.patid);
                                           // gender = ((RadioButton)findViewById(R.id.male)).getCheckedRadioButtonId()

                                           final String name = nameBox.getText().toString();
                                           final String age = ageBox.getText().toString();
                                           final String id = idBox.getText().toString();
                                           if (name.equals("") || id.equals("") || age.equals("")) {
                                               Toast.makeText(MainActivity.this, "Please enter all fields", Toast.LENGTH_SHORT).show();
                                               return;
                                           }
                                           String gen = "";
                                           RadioButton str = (RadioButton) findViewById(R.id.male);
                                           Boolean value = str.isChecked();
                                           if (value == true) {
                                               gen = "M";
                                           } else {
                                               gen = "F";
                                           }

                                           table = name + "_" + id + "_" + age + "_" + gen;
                                           Intent myIntent = new Intent(MainActivity.this, AppActivity.class);
                                           myIntent.putExtra("name", name);
                                           myIntent.putExtra("age", age);
                                           myIntent.putExtra("id", id);
                                           myIntent.putExtra("sex", gen);
                                           myIntent.putExtra("TableName", table);
                                           startActivity(myIntent);

                                       }
                                   }
        );

    }
}


