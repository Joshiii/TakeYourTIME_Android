package com.joshii.takeyourtime;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
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
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class Splash extends Activity {

    SQLiteDatabase TYT_DB;

    private MediaPlayer playELESI = new MediaPlayer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        InitializeDatabase();

        if (GetFirstString("ConnectionSettings_Table","Server","ConnectionID","1") == null){
            TYT_DB.execSQL("INSERT INTO ConnectionSettings_Table VALUES (1,'192.168.43.224','root','','TakeYourTIME_DB');");
        }


        final ImageView ivSTILogo = (ImageView) findViewById(R.id.ivSTILogo);
        final Animation ivSTILogoAn = AnimationUtils.loadAnimation(getBaseContext(),R.anim.fade);

        final ImageView iv = (ImageView) findViewById(R.id.ivSplash);
        final ImageView iv2 = (ImageView) findViewById(R.id.imageView2);
        iv.setVisibility(View.INVISIBLE);
        iv2.setVisibility(View.INVISIBLE);

        final Animation an = AnimationUtils.loadAnimation(getBaseContext(),R.anim.rotate_logo);

        final TextView tv = (TextView) findViewById(R.id.tvTYTLabel);
        tv.setVisibility(View.INVISIBLE);
        final Animation tvAn = AnimationUtils.loadAnimation(getBaseContext(),R.anim.fade);

        ivSTILogo.startAnimation(ivSTILogoAn);
        ivSTILogoAn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                //playELESI.start();
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                ivSTILogo.setVisibility(View.INVISIBLE);
                iv2.setVisibility(View.VISIBLE);
                iv.setVisibility(View.VISIBLE);

                iv.startAnimation(an);
                an.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        tv.startAnimation(tvAn);
                        tv.setVisibility(View.VISIBLE);
                        tvAn.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                tv.setVisibility(View.INVISIBLE);
                                CheckRoomSet(GetWIFIMacAddress(getApplicationContext()));
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

    }

    public String GetWIFIMacAddress(Context context) {
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

            private Dialog loadingDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loadingDialog = ProgressDialog.show(Splash.this, "Checking Configuration", "Loading. Please wait..");
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
                loadingDialog.dismiss();
                if (result != null)
                {
                    String s = result.trim();
                    if(s.equalsIgnoreCase("No Room Set")){
                        finish();
                        Intent i = new Intent(Splash.this, LoginAdministratorActivity.class);
                        startActivity(i);
                    }
                    else
                    {
                        finish();
                        Intent i = new Intent(Splash.this ,AttendanceMonitoringActivity.class);
                        startActivity(i);
                    }
                }
                else{
                    finish();
                    Intent i = new Intent(Splash.this, LoginAdministratorActivity.class);
                    startActivity(i);
                }
            }
        }

        CheckRoomSetAsync cra = new CheckRoomSetAsync();
        cra.execute(WIFI_MAC);
    }

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

    boolean GetFirstStringResult = true;

    protected String GetFirstString(String tableName, String columnToRead, String conditionalColumn, String conditionalValue){
        try{
            Cursor c = TYT_DB.rawQuery("SELECT " + columnToRead + " " +
                    "FROM " + tableName + " " +
                    "WHERE " + conditionalColumn + " = " + conditionalValue + ";",null);
            c.moveToFirst();

            if (c.getCount() == 0)
            {
                return null;
            }
            else
            {
                return c.getString(0);
            }
        }
        catch (SQLException e){
            Toast.makeText(getApplicationContext(),e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }
}
