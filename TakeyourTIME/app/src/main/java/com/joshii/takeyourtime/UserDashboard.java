package com.joshii.takeyourtime;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class UserDashboard extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    SQLiteDatabase TYT_DB;
    TextView tvUserFullName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        tvUserFullName = (TextView) findViewById (R.id.lblUserFullName);
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                tvUserFullName.setText("Administrator Name");
            } else {
                String FirstName = extras.getString("FirstName");
                String LastName = extras.getString("LastName");
                tvUserFullName.setText(FirstName + " " + LastName);
            }
        } else {
            tvUserFullName.setText((String) savedInstanceState.getSerializable("FirstName") + " " + (String) savedInstanceState.getSerializable("LastName"));
        }*/

        InitializeDatabase();

        CheckRoomSet(GetWIFIMacAddress(getApplicationContext()));

    }


    public void onBackPressed() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());

        builder.setMessage("Do you want to Logout?");
        builder.setTitle("Logout");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(UserDashboard.this,LoginAdministratorActivity.class);
                finish();
                startActivity(intent);
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });



        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        /*DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }*/
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.user_dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_startattendance) {
            TextView roomSetStatus = (TextView) findViewById(R.id.lblRoomSetStatus);

            if (roomSetStatus.getText().toString().equalsIgnoreCase("This device was not set in any room")){
                Toast.makeText(getApplicationContext(),"Set Device Room first before starting attendance monitoring",Toast.LENGTH_LONG).show();
            }
            else
            {
                Intent intent = new Intent(UserDashboard.this, AttendanceMonitoringActivity.class);
                finish();
                startActivity(intent);
            }

            return true;
        }
        else if (id == R.id.action_setroom) {
            TextView roomSetStatus = (TextView) findViewById(R.id.lblRoomSetStatus);

            Intent intent = new Intent(UserDashboard.this, RoomsActivity.class);
            finish();
            startActivity(intent);

            return true;
        }
        else if (id == R.id.action_logout){
            Intent intent = new Intent(UserDashboard.this,LoginAdministratorActivity.class);
            finish();
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void btnStartAttendanceMonitoring_Click (View view){
        TextView roomSetStatus = (TextView) findViewById(R.id.lblRoomSetStatus);

        if (roomSetStatus.getText().toString().equalsIgnoreCase("This device was not set in any room")){
            Toast.makeText(getApplicationContext(),"Set Device Room first before starting attendance monitoring",Toast.LENGTH_LONG).show();
        }
        else
        {
            Intent intent = new Intent(UserDashboard.this, AttendanceMonitoringActivity.class);
            finish();
            startActivity(intent);
        }
    }

    public void btnListOfClasses_Click (View view){
        Intent intent = new Intent(UserDashboard.this, TeacherDashboard.class);
        intent.putExtra("RoomID",SelectedRoomID);
        finish();
        startActivity(intent);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private String GetWIFIMacAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String macAddress = wifiManager.getConnectionInfo().getMacAddress();

        if (macAddress == null)
        {
            macAddress = "Device don't have MAC Address or Wi-Fi is Disabled";
        }
        return macAddress;
    }

    private void CheckRoomSet(String WIFI_MAC){
        class CheckRoomSetAsync extends AsyncTask<String, Void, String> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params) {
                String macadd = params[0];

                InputStream is = null;
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("WIFIMacAddress", macadd));
                String result = null;

                String _Server = GetFirstString("ConnectionSettings_Table","Server","ConnectionID","1");

                try{
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(
                            "http://" + _Server + "/TakeYourTIME/check_room.php");
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    HttpResponse response = httpClient.execute(httpPost);

                    HttpEntity entity = response.getEntity();

                    is = entity.getContent();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
                    StringBuilder sb = new StringBuilder();

                    String line = null;
                    while ((line = reader.readLine()) != null)
                    {
                        sb.append(line + "\n");
                    }
                    result = sb.toString();
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return result;
            }

            @Override
            protected void onPostExecute(String result){
                final TextView tvRoomStatus = (TextView) findViewById(R.id.lblRoomSetStatus);
                final Button btnSetDeviceRoom = (Button) findViewById(R.id.btnSetDeviceRoom);
                if (result != null)
                {
                    String s = result.trim();
                    if(s.equalsIgnoreCase("No Room Set")){
                        tvRoomStatus.setText("This device was not set in any room");
                        btnSetDeviceRoom.setText("Set Device Room");
                    }else {
                        String [] splitted = s.split("SPLIT");
                        SelectedRoomID = splitted[0];
                        tvRoomStatus.setText("This Device is set for " + splitted[2]);
                        //MenuItem setRoomItem = (MenuItem) findViewById(R.id.action_setroom);
                        btnSetDeviceRoom.setText("Change Device Room");
                    }
                }
                else
                {
                    tvRoomStatus.setText("Error fetching Room Data");
                    btnSetDeviceRoom.setVisibility(View.INVISIBLE);
                    Toast.makeText(getApplicationContext(), "Error connecting on Server. Please check your connection or Contact System Administrator for Assistance", Toast.LENGTH_LONG).show();
                }
            }
        }

        CheckRoomSetAsync cra = new CheckRoomSetAsync();
        cra.execute(WIFI_MAC);
    }

    String SelectedRoomID = "";

    private void InitializeDatabase() {

        TYT_DB = openOrCreateDatabase("TakeYourTIME_DB", Context.MODE_PRIVATE,null);
        TYT_DB.execSQL("CREATE TABLE IF NOT EXISTS " +
                "ConnectionSettings_Table (" +
                "ConnectionID INTEGER PRIMARY KEY NOT NULL, " +
                "Server VARCHAR, " +
                "UserID VARCHAR, " +
                "Password VARCHAR, " +
                "Database VARCHAR);");
    }

    protected String GetFirstString(String tableName, String columnToRead, String conditionalColumn, String conditionalValue){
        try{
            Cursor c = TYT_DB.rawQuery("SELECT " + columnToRead + " " +
                    "FROM " + tableName + " " +
                    "WHERE " + conditionalColumn + " = " + conditionalValue + ";",null);
            c.moveToFirst();

            return c.getString(0);
        }
        catch (SQLException e){
            Toast.makeText(getApplicationContext(),e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    public void btnSetDeviceRoom_Click (View view){
        Intent intent = new Intent(UserDashboard.this, RoomsActivity.class);
        //finish();
        startActivity(intent);
    }
}
