package com.example.daniel.smarthumidor;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.*;
import com.android.volley.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    static final String API_KEY = "282JUYUBCHO368XH"; //these are provided by ThingSpeak
    static final String CHANNEL = "107907";

    TextView TempText;
    TextView RHText;

    Button update;
    RequestQueue queue;

    NotificationManager mNotificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TempText = (TextView) findViewById(R.id.tempdisp);//our simple app has just 2 updatable text fields...
        RHText = (TextView) findViewById(R.id.rhdisp);

        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Log.d("MainActivity", "OnCreate");
        update = (Button) findViewById(R.id.update_button);//...as well as a button to manually update
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateManual();
            }
        });
        queue = Volley.newRequestQueue(this); //volley is a library that does json object requests via http in its own thread
        ScheduledExecutorService scheduleTaskExecutor = Executors.newScheduledThreadPool(5); //the scheduled task executor will run a background service that updates the fields

        // This schedules a runnable task every minute (for demo purposes)
        scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                updateManual();
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    public void updateManual() {
        Log.d("MainActivity", "updateManual");

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, "http://api.thingspeak.com/channels/"+CHANNEL+"/feed.json?key="+API_KEY , null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        // mTxtDisplay.setText("Response: " + response.toString());
                        double rh = 0, temp = 0;

                        try { //json object parsing
                            temp = response.getJSONArray("feeds").getJSONObject(response.getJSONArray("feeds").length() - 1).getDouble("field2");
                            rh = response.getJSONArray("feeds").getJSONObject(response.getJSONArray("feeds").length() - 1).getDouble("field1");

                        } catch (JSONException e) {
                            Log.d("MainActivity", "Didn't work");
                        }
                        if(rh < 63 || rh > 74 || temp < 63 || temp > 74) { 
                            setNotification(temp, rh);//if the humidity/temp is outside normal boundaries, alert the user
                        }
                        else mNotificationManager.cancelAll();//otherwise cancel any pending notifications

                        TempText.setText(Double.toString(temp) + "°F");
                        RHText.setText(Double.toString(rh) + "%");
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.d("MainActivity", "ERRorlistenerdeal");

                    }
                });
        queue.add(jsObjRequest);

    }

    void setNotification(double t, double rh) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.cigar) //a nice cigar icon from the Noun Project
                        .setContentTitle("Your humidor is in need of attention")
                        .setContentText("RH = "+Double.toString(rh)+"%   T = "+Double.toString(t));
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        // mId allows you to update the notification later on.
        mNotificationManager.notify(0, mBuilder.build());
    }

    double cToF(double c) { //convert celsius to F
        return c * 9 / 5 + 32.0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("MainActivity", "OnCreateOptionsMenu");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("MainActivity", "onOptionsItemSelected");
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
