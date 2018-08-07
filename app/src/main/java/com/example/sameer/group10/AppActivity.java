package com.example.sameer.group10;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.DataOutputStream;
import java.net.URL;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;

import java.net.HttpURLConnection;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;



/**
 * Created by sameer kulkarni on 8/2/18.
 */
/**
 * Android app to display accelerometer data in a graph with upload/download functionality
 * to/from the impact lab server.
 */

public class AppActivity extends Activity {

    private SQLiteDatabase conn;
    private SensorManager snsrmgr;
    private Sensor Accelerometer;
    public final static String serverURL = "http://impact.asu.edu/CSE535Spring18Folder/";
    public final static String uploadURIinServer = serverURL + "UploadToServer.php";
    File root = android.os.Environment.getExternalStorageDirectory();
    File dir = new File(root.getAbsolutePath() + "/Android/Data/CSE535_ASSIGNMENT2/");

    public final static String uploadFilePath = "/data/data/com.example.sameer.group10/databases/";
    public final static String uploadFileName = "group10.db";

    public final static String downloadFilePath = "/storage/emulated/0/Android/Data/CSE535_ASSIGNMENT2/";
    //File file = new File(dir);
    public final static String downloadURL = serverURL + uploadFileName;
    public final static String redCC = "#ff0000";
    public final static String blueCC = "#00ff00";
    public final static String greenCC = "#0000ff";
    public final static String postRequest = "POST";
    public final static String connectionString = "Connection";
    public final static String keepAliveString = "Keep-Alive";
    public final static String encTypeString = "ENCTYPE";
    public final static String multipartData = "multipart/form-data";
    public final static String multipartType = "multipart/form-data;boundary=*****";
    public final static String uploadedFileString = "uploaded_file";

    private LineGraphSeries<DataPoint> values;
    private LineGraphSeries<DataPoint> secondValues;
    private LineGraphSeries<DataPoint> thirdValues;
    private int counter = 0;
    private int counter2 = 0;
    private int counter3 = 0;

    private Button runButton;
    private Button stopButton;
    private boolean stopIt = false;
    private Thread keyThread;
    private GraphView gv;
    private Viewport vp;
    ProgressDialog progressDialogObject = null;
    SSLContext context = null;
    private boolean firstStart = true;
    String tableName;
    boolean flag = true;

    long previousTime = 0;

    //Gets time and sensor x,y,z values each time the phone is moved at the rate of 1 times per second
    private SensorEventListener acclListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent acclEvent) {
            Sensor AcclSensor = acclEvent.sensor;
            if (AcclSensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float x = acclEvent.values[0];
                float y = acclEvent.values[1];
                float z = acclEvent.values[2];
                long currentTime = System.currentTimeMillis();
                String msg = Long.toString(currentTime) + "," + Float.toString(x) + "," + Float.toString(y) + "," + Float.toString(z);
                if ((currentTime - previousTime) > 1000) {
                    try {
                        conn.execSQL("INSERT INTO " + tableName + " VALUES (" + msg + ");");
                    } catch (Exception e) {
                        Log.d(e.getMessage(), " Insert part");
                    }
                    previousTime = currentTime;
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d("Sensor Accuracy changed", "Sensor Accuracy Changed");
        }
    };

    // Register the accelerometer of the device to start getting data from it.
    private void registerAcclListener() {
        snsrmgr = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Accelerometer = snsrmgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        snsrmgr.registerListener(acclListener, Accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d("Registered Listener", "Registered Listener");
    }


    //instantiate database and create table with name time_stamp_x_y_z values
    private void createTable(String t_name, SQLiteDatabase connection) {
        Log.d(t_name, t_name);
        try {
            connection.execSQL("CREATE TABLE " + t_name + " (Time_Stamp REAL, X_Value REAL, Y_Value REAL, Z_Value REAL);");
            Log.d("Table Created ", t_name);
        } catch (Exception e) {
            Log.d("Table Already exists: ", t_name);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    //takes the entered values and stores it under name,age etc variables for later use
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String appName = uploadFileName;
        setContentView(R.layout.graph_display_page);
        TextView txtview = (TextView) findViewById(R.id.name);
        TextView txtview1 = (TextView) findViewById(R.id.age);
        TextView txtview2 = (TextView) findViewById(R.id.patid);
        TextView txtview3 = (TextView) findViewById(R.id.gen);
        Intent intent = getIntent();
        Bundle bd = intent.getExtras();
        if (bd != null) {
            String getName = (String) bd.get("name");
            String getAge = (String) bd.get("age");
            String getId = (String) bd.get("id");
            String gen = (String) bd.get("sex");
            tableName = (String) bd.get("TableName");
            txtview.setText(getName);
            txtview1.setText(getAge);
            txtview2.setText(getId);
            txtview3.setText(gen);

        }

        //defines the graph bounds and makes the graph scrollable.

        conn = openOrCreateDatabase(appName, MODE_PRIVATE, null);
        registerAcclListener();
        createTable(tableName, conn);

        gv = (GraphView) findViewById(R.id.graph);
        values = new LineGraphSeries<DataPoint>();
        values.setColor(Color.parseColor(redCC));
        secondValues = new LineGraphSeries<DataPoint>();
        secondValues.setColor(Color.parseColor(greenCC));
        thirdValues = new LineGraphSeries<DataPoint>();
        thirdValues.setColor(Color.parseColor(blueCC));
        vp = gv.getViewport();
        //vp.setYAxisBoundsManual(true);
        vp.setMinX(0);
        vp.setMaxX(10);
        vp.setMinY(-30);
        vp.setMaxY(30);
        vp.setXAxisBoundsManual(true);
        vp.setScrollable(true);
        vp.scrollToEnd();
        this.runListener();
        this.stopListener();
        this.uploadListener();
        this.downloadListener();
        keyThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; !stopIt; i++) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            appendValues();
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.d("Interrupted", e.toString());
                    }
                }
            }
        });

    }


    //handles what run button does. It gets data from the sensor every second and appends it to the values
    // to be plotted on the graph.

    public void runListener() {
        runButton = (Button) findViewById(R.id.run_button);
        runButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopIt = false;
                registerAcclListener();
                if (gv.getSeries().isEmpty()) {
                    gv.addSeries(values);
                    gv.addSeries(secondValues);
                    gv.addSeries(thirdValues);
                    keyThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; !stopIt; i++) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        appendValues();
                                    }
                                });
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    Log.d("Interrupted", e.toString());
                                }
                            }
                        }
                    });
                    keyThread.start();
                } else {
                    return;
                }
            }
        });
    }

    //handles what stop button does. It clears the graph after stop button us clicked
    public void stopListener() {
        stopButton = (Button) findViewById(R.id.stop_button);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopIt = true;
                flag = true;
                gv.removeAllSeries();
            }
        });
    }

    private void appendValues() {
        Log.d("Table's name", tableName);
        float fl;
        float x, y, z;
        String selectQuery = "SELECT * FROM " + tableName + " ORDER BY Time_Stamp DESC LIMIT 1;";
        if (flag) {
            selectQuery = "SELECT * FROM " + tableName + " ORDER BY Time_Stamp DESC LIMIT 10;";
            flag = false;
        }
        try {
            Cursor sel = conn.rawQuery(selectQuery, null);
            int c = 0;
            sel.moveToFirst();
            do {
                int timeStamp = sel.getInt(0);
                x = sel.getFloat(1);
                y = sel.getFloat(2);
                z = sel.getFloat(3);
                c++;
                values.appendData(new DataPoint(counter++, x), true, 12);
                secondValues.appendData(new DataPoint(counter2++, y), true, 12);
                thirdValues.appendData(new DataPoint(counter3++, z), true, 12);
            } while (sel.moveToNext());
            Log.d("C: ", Integer.toString(c));

        }//comment
        catch (Exception e) {
            Log.d("Append Value", " DB object closed");
        }
    }

    //unregisters the sensor on exiting from the activity.

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        conn.close();
        Log.d("Unregistering sensor", "unregistering sensor");
        try {
            snsrmgr.unregisterListener(acclListener);
        } catch (Exception e) {
            Log.d(e.getMessage(), "Sensor not registered");
        }
    }


    //Handles what the upload button does. This method calls the Upload method.

    public void uploadListener() {
        Button uploadButton = (Button) findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialogObject = ProgressDialog.show(AppActivity.this, "", "Uploading Database", true);
                new Upload(AppActivity.this).execute();
                progressDialogObject.dismiss();
            }
        });
    }



    //Handles the uploading task.


    class Upload extends AsyncTask {
        private Context context1;

        public Upload(Context context) {
            this.context1 = context;
        }

        @Override
        protected Object doInBackground(Object[] params) {
            HttpURLConnection conn = null;
            int serverResponseCode = 0;
            DataOutputStream dataOutputStreamObject = null;
            String lineEnd = "\r\n";
            String hyphens = "--";
            String boundaryMarker = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;
            URL url = null;
            FileInputStream fileInputStream = null;
            File sourceFile = new File(uploadFilePath + "" + uploadFileName);

            // Since Impact lab ssl certificates are not recognized authorizing to accept any ssl certificates
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }
            }};
            try {
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                //checks if source db file is present in /data/ folder
                if (!sourceFile.isFile()) {
                    Log.e("uploadFile", "Source File not exist :"
                            + uploadFilePath + "" + uploadFileName);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Source File not exist :" + uploadFilePath + "" + uploadFileName, Toast.LENGTH_LONG).show();
                        }
                    });
                    return "Source File not exist";
                } else {

                    // open a URL connection to the Servlet
                    fileInputStream = new FileInputStream(sourceFile);
                    url = new URL(uploadURIinServer);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true); // Allow Inputs
                    conn.setDoOutput(true); // Allow Outputs
                    conn.setUseCaches(false); // Don't use a Cached Copy
                    conn.setRequestMethod(postRequest);
                    conn.setRequestProperty(connectionString, keepAliveString);
                    conn.setRequestProperty(encTypeString, multipartData);
                    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundaryMarker);
                    conn.setRequestProperty(uploadedFileString, uploadFilePath + "" + uploadFileName);
                    dataOutputStreamObject = new DataOutputStream(conn.getOutputStream());

                    dataOutputStreamObject.writeBytes(hyphens + boundaryMarker + lineEnd);
                    dataOutputStreamObject.writeBytes("Content-Disposition: form-data; name=" + "uploaded_file;filename="
                            + uploadFilePath + "" + uploadFileName + "" + lineEnd);

                    dataOutputStreamObject.writeBytes(lineEnd);

                    // create a buffer of  maximum size
                    bytesAvailable = fileInputStream.available();

                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];

                    // read file and write it into form...
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    while (bytesRead > 0) {
                        dataOutputStreamObject.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }
                    dataOutputStreamObject.writeBytes(lineEnd);
                    dataOutputStreamObject.writeBytes(hyphens + boundaryMarker + hyphens + lineEnd);
                    serverResponseCode = conn.getResponseCode();
                    String serverResponseMessage = conn.getResponseMessage();
                    conn.connect();
                    serverResponseCode = conn.getResponseCode();
                    if (conn.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                        return "Server returned HTTP " + conn.getResponseCode()
                                + " " + conn.getResponseMessage();
                    }
                    Log.i("uploadFile", "HTTP Response is : "
                            + serverResponseMessage + ": " + serverResponseCode);
                    if (serverResponseCode == HttpsURLConnection.HTTP_OK) {
                        runOnUiThread(new Runnable() {
                                          public void run() {
                                              Toast.makeText(getApplicationContext(),
                                                      "File Upload Completed."
                                                              + uploadFileName, Toast.LENGTH_LONG).show();
                                          }
                                      }
                        );
                    }

                    //close the streams //
                    fileInputStream.close();
                    dataOutputStreamObject.flush();
                    dataOutputStreamObject.close();
                    conn.disconnect();
                    return ("String response code:" + serverResponseCode);

                }

            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (MalformedURLException ex) {
                //dialog.dismiss();
                ex.printStackTrace();
                String temp = "value displayed";
                Log.i("Response Code:" + serverResponseCode, temp);
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "MalformedURLException Exception : check script url.", Toast.LENGTH_LONG).show();
                    }
                });
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {
                //dialog.dismiss();
                e.printStackTrace();
                String temp = "value displayed";
                Log.i("Response Code:" + serverResponseCode, temp);
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Got Exception : see logcat", Toast.LENGTH_LONG).show();
                    }
                });
                Log.e("Upload file to server", "Exception : "
                        + e.getMessage(), e);
            }
            return "success";
        }
    }


    //handles download task


    class Download extends AsyncTask {
        private Context context1;

        public Download(Context context) {
            this.context1 = context;
        }

        @Override
        protected Object doInBackground(Object[] params) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection conn = null;
            URL url = null;
            FileInputStream fileInputStream = null;
            byte[] downData = null;
            int serverResponseCode = 0;

            // Since Impact lab ssl certificates are not recognized authorizing to accept any ssl certificates
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }
            }};

            try {
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                DataOutputStream dataOutputStreamObject = null;
                // open a URL connection to the Servlet
                url = new URL(downloadURL);
                conn = (HttpURLConnection) url.openConnection();
                input = conn.getInputStream();

                //creates CSE535_ASSIGNMENT2_DOWN folder in /Android/Data/
                String rootPath = android.os.Environment.getExternalStorageDirectory()
                        .getAbsolutePath() + "/Android/Data/CSE535_ASSIGNMENT2_DOWN/";
                File root = new File(rootPath);
                if (!root.exists()) {
                    root.mkdirs();
                }

                //writes the input data stream to the created db file
                File f = new File(rootPath + "group10.db");
                output = new FileOutputStream(f);
                downData = new byte[4096];
                int count;
                conn.connect();
                while ((count = input.read(downData)) != -1) {
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    output.write(downData, 0, count);
                }
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (MalformedURLException ex) {
                //dialog.dismiss();
                ex.printStackTrace();
                String temp = "value displayed";
                Log.i("Response Code:" + serverResponseCode, temp);
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "MalformedURLException Exception : check script url.", Toast.LENGTH_LONG).show();
                    }
                });
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {
                //dialog.dismiss();
                e.printStackTrace();
                String temp = "value displayed";
                Log.i("Response Code:" + serverResponseCode, temp);
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Got Exception : see logcat", Toast.LENGTH_LONG).show();
                    }
                });
                Log.e("Upload file to server", "Exception : "
                        + e.getMessage(), e);
                return ("String response code:" + serverResponseCode);

            }
            return "success";
        }
    }


    //handles what download button does. This method calls the download method above.

    public void downloadListener() {
        Button uploadButton = (Button) findViewById(R.id.download_button);

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialogObject = ProgressDialog.show(AppActivity.this, "", "Downloading Database", true);
                new Download(AppActivity.this).execute();
                progressDialogObject.dismiss();
                stopIt = true;
                flag = true;
                gv.removeAllSeries();
                Log.d("Unregistering sensor", "unregistering sensor");
                try {
                    snsrmgr.unregisterListener(acclListener);
                } catch (Exception e) {
                    Log.d(e.getMessage(), "Sensor not registered");
                }
                runOnUiThread(new Runnable() {
                                  public void run() {
                                      Toast.makeText(getApplicationContext(),
                                              "File Download Completed. Please hit run to view recent 10 seconds data"
                                                      + uploadFileName, Toast.LENGTH_LONG).show();
                                  }
                              }
                );
            }
        });


    }
}


