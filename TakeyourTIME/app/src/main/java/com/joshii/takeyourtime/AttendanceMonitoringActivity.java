package com.joshii.takeyourtime;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Date;
import java.util.Locale;

public class AttendanceMonitoringActivity extends AppCompatActivity {


    private static final boolean AUTO_HIDE = true;

    private static final int AUTO_HIDE_DELAY_MILLIS = 2000;

    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    public int SelectedRoomID = 1;

    private SQLiteDatabase TYT_DB;

    private TextView tvResult;

    private String ROOM_NAME = "";
    private String ROOM_ID = "";

    private FrameLayout frameLayout;

    private NfcAdapter nfcAdapter;
    private MediaPlayer playNFCReadSound = new MediaPlayer();

    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String TAG = "NfcDemo";

    String FetchedServerDateTime = "2016-12-10 08:49:09";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_monitoring);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        frameLayout = (FrameLayout) findViewById(R.id.attendance_frameLayout);


        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        InitializeDatabase();

        tvResult = (TextView) findViewById(R.id.RoomSetText);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;

        }

        if (!nfcAdapter.isEnabled()) {

        } else {

        }

        GetServerDateTime();
        CheckRoomSet(GetWIFIMacAddress(getApplicationContext()));

        RunClock();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    protected void onResume() {
        super.onResume();

        setupForegroundDispatch(this, nfcAdapter);
    }

    @Override
    protected void onPause() {
        stopForegroundDispatch(this, nfcAdapter);

        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};


        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    public String Read_NFCSerialNumber = "";

    private void handleIntent(Intent intent) {
        String action = intent.getAction();


        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            String type = intent.getType();

            final Calendar c = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            try {
                c.setTime(sdf.parse(FetchedServerDateTime));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            final Date d = c.getTime();
            final String _day = String.format("%tA",d);
            final String _time = String.format("%tT",d);
            final String _date = String.format("%tF",d);


            //playNFCReadSound.start();
            Read_NFCSerialNumber = ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
            TakeAttendance(ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)), _day,_time,_date,SelectedRoomID+"");

            if (MIME_TEXT_PLAIN.equals(type)) {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);

            } else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();

            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NdefReaderTask().execute(tag);
                    break;
                }
            }
        }
    }

    public String ByteArrayToHexString(byte [] inarray) {
        int i, j, in;
        String [] hex = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        String out= "";

        for(j = 0 ; j < inarray.length ; ++j)
        {
            in = (int) inarray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
    }

    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {

            byte[] payload = record.getPayload();

            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

            int languageCodeLength = payload[0] & 0063;

            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }

        @Override
        protected void onPostExecute(String result) {
            /*if (result != null) {
                mTextView.setText("NFC Card Successfully Recognized!");
                editTextUserName.setText(result);
                editTextPassword.setText(result);
                buttonLogin = (Button)findViewById(R.id.btnLogin);
                buttonLogin.performClick();
            }*/
        }
    }

    public boolean isConnectedToInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Network[] networks = connectivityManager.getAllNetworks();
            NetworkInfo networkInfo;
            for (Network mNetwork : networks) {
                networkInfo = connectivityManager.getNetworkInfo(mNetwork);
                if (networkInfo.getState().equals(NetworkInfo.State.CONNECTED)) {
                    return true;
                }
            }
        }else {
            if (connectivityManager != null) {
                //noinspection deprecation
                NetworkInfo[] info = connectivityManager.getAllNetworkInfo();
                if (info != null) {
                    for (NetworkInfo anInfo : info) {
                        if (anInfo.getState() == NetworkInfo.State.CONNECTED) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    int ColorID = 0;
    private void TakeAttendance(final String _NFCSerialNumber, final String _Day, final String _Time,final String _Date, final String _RoomID) {

        class TakeAttendanceAsync extends AsyncTask<String, Void, String>{

            private Dialog loadingDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                //loadingDialog = ProgressDialog.show(AttendanceMonitoringActivity.this, "Please wait", "Checking..");
            }

            @Override
            protected String doInBackground(String... params) {
                String nfcserial = params[0];
                String day = params[1];
                String time = params[2];
                String date = params[3];
                String room = params[4];

                InputStream is = null;
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("NFCSerialNumber", nfcserial));
                nameValuePairs.add(new BasicNameValuePair("Day", day));
                nameValuePairs.add(new BasicNameValuePair("Time", time));
                nameValuePairs.add(new BasicNameValuePair("Date", date));
                nameValuePairs.add(new BasicNameValuePair("RoomID", room));
                String result = null;

                try{

                    String _Server = GetFirstString("ConnectionSettings_Table","Server","ConnectionID","1");

                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(
                            "http://" + _Server + "/TakeYourTIME/attendance_monitoring.php");
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
                if (result != null)
                {

                    String s = result.trim();
                    //loadingDialog.dismiss();
                    if(s.equalsIgnoreCase(""))
                    {
                        ShowErrorMessage("...");
                        playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.card_reject);
                        ColorID = R.color.attendance_error;
                    }
                    else if(s.equalsIgnoreCase("Card not Recognized!"))
                    {
                        ColorID = R.color.attendance_error;
                        playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.card_reject);
                        ShowErrorMessage("Your card was not Recognized or not Registered from the System.");
                        playNFCReadSound.start();
                    }
                    else if (s.contains("Administrator"))
                    {
                        String [] splitted = s.split("SPLIT");

                        Intent intent = new Intent(AttendanceMonitoringActivity.this, UserDashboard.class);
                        Bundle extras = new Bundle();
                        extras.putString("FirstName",splitted[3]);
                        extras.putString("LastName", splitted[4]);
                        intent.putExtras(extras);
                        finish();
                        startActivity(intent);
                    }
                    else if (s.contains("Teacher"))
                    {
                        String [] teacherInfo = s.split("SPLIT");
                        ShowTeacherOptions(Read_NFCSerialNumber,teacherInfo[2],teacherInfo[3]);
                    }
                    else
                    {
                        if(s.equalsIgnoreCase("You have no Class in this Room"))
                        {
                            ColorID = R.color.attendance_error;
                            playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.card_reject);
                            ShowErrorMessage("Attendance not Accepted. You have no Class in this Room.");
                        }
                        else if(s.equalsIgnoreCase("There are no class in this room"))
                        {
                            ColorID = R.color.attendance_error;
                            playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.card_reject);
                            ShowErrorMessage("Attendance not Accepted. There are no class in this room.");
                        }
                        else if(s.equalsIgnoreCase("You are not allowed to time out"))
                        {
                            ColorID = R.color.attendance_error;
                            playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.card_reject);
                            ShowErrorMessage("Attendance not Accepted. You are not allowed to time out yet.");
                        }
                        else if(s.equalsIgnoreCase("You already timed out"))
                        {
                            ColorID = R.color.attendance_error;
                            playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.card_reject);
                            ShowErrorMessage("Attendance not Accepted. You already timed out.");
                        }
                        else if(s.contains("was Canceled."))
                        {
                            ColorID = R.color.attendance_error;
                            playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.card_reject);
                            ShowErrorMessage(s);
                        }
                        else if(s.contains("was already Dismissed."))
                        {
                            ColorID = R.color.attendance_error;
                            playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.card_reject);
                            ShowErrorMessage(s);
                        }
                        else if(s.equalsIgnoreCase("Error"))
                        {
                            ColorID = R.color.attendance_error;
                            playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.card_reject);
                            ShowErrorMessage("Error Taking Attendance. Contact System Administrator for Assistance.");
                        }
                        else if(s.equalsIgnoreCase("It's Sunday!"))
                        {
                            ColorID = R.color.attendance_in;
                            playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.job_success);
                            ShowErrorMessage("It's Sunday");
                        }
                        else
                        {
                            ColorID = R.color.attendance_in;
                            playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.job_success);
                            String [] splitted = s.split("SPLIT");

                            ShowAttendeeInfo(splitted);
                        }

                        playNFCReadSound.start();
                    }
                }
                else
                {
                    loadingDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Error connecting on Server. Please check your connection or Contact System Administrator for Assistance", Toast.LENGTH_LONG).show();
                }
            }
        }

        TakeAttendanceAsync takeAttendanceAs = new TakeAttendanceAsync();
        takeAttendanceAs.execute(_NFCSerialNumber,_Day,_Time,_Date,_RoomID);

    }

    private void TakeTeacherAttendance(final String _NFCSerialNumber, final String _Day, final String _Time,final String _Date, final String _RoomID) {

        class TakeTeacherAttendanceAsync extends AsyncTask<String, Void, String>{


            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params) {
                String nfcserial = params[0];
                String day = params[1];
                String time = params[2];
                String date = params[3];
                String room = params[4];

                InputStream is = null;
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("NFCSerialNumber", nfcserial));
                nameValuePairs.add(new BasicNameValuePair("Day", day));
                nameValuePairs.add(new BasicNameValuePair("Time", time));
                nameValuePairs.add(new BasicNameValuePair("Date", date));
                nameValuePairs.add(new BasicNameValuePair("RoomID", room));
                String result = null;

                try{

                    String _Server = GetFirstString("ConnectionSettings_Table","Server","ConnectionID","1");

                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(
                            "http://" + _Server + "/TakeYourTIME/teacher_attendance_monitoring.php");
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
                if (result != null)
                {

                    String s = result.trim();
                    if(s.equalsIgnoreCase("You have no Class in this Room"))
                    {
                        ColorID = R.color.attendance_error;
                        playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.card_reject);
                        ShowErrorMessage("Attendance not Accepted. You have no Class in this Room.");
                    }
                    else if(s.equalsIgnoreCase("There are no class in this room"))
                    {
                        ColorID = R.color.attendance_error;
                        playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.card_reject);
                        ShowErrorMessage("Attendance not Accepted. There are no class in this room.");
                    }
                    else if(s.equalsIgnoreCase("You are not allowed to time out"))
                    {
                        ColorID = R.color.attendance_error;
                        playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.card_reject);
                        ShowErrorMessage("Attendance not Accepted. You are not allowed to time out yet.");
                    }
                    else if(s.equalsIgnoreCase("You already timed out"))
                    {
                        ColorID = R.color.attendance_error;
                        playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.card_reject);
                        ShowErrorMessage("Attendance not Accepted. You already timed out.");
                    }
                    else if(s.contains("was Canceled."))
                    {
                        ColorID = R.color.attendance_error;
                        playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.card_reject);
                        ShowErrorMessage(s);
                    }
                    else if(s.contains("was already Dismissed."))
                    {
                        ColorID = R.color.attendance_error;
                        playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.card_reject);
                        ShowErrorMessage(s);
                    }
                    else if(s.equalsIgnoreCase("Error"))
                    {
                        ColorID = R.color.attendance_error;
                        playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.card_reject);
                        ShowErrorMessage("Error Taking Attendance. Contact System Administrator for Assistance.");
                    }
                    else if(s.equalsIgnoreCase("It's Sunday!"))
                    {
                        ColorID = R.color.attendance_in;
                        playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.job_success);
                        ShowErrorMessage("It's Sunday");
                    }
                    else
                    {
                        ColorID = R.color.attendance_in;
                        playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.job_success);
                        String [] splitted = s.split("SPLIT");

                        ShowAttendeeInfo(splitted);
                    }
                    teacherOptionDialog.dismiss();
                    playNFCReadSound.start();
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Error connecting on Server. Please check your connection or Contact System Administrator for Assistance", Toast.LENGTH_LONG).show();
                }
            }
        }

        TakeTeacherAttendanceAsync takeTeacherAttendanceAs = new TakeTeacherAttendanceAsync();
        takeTeacherAttendanceAs.execute(_NFCSerialNumber,_Day,_Time,_Date,_RoomID);

    }


    AlertDialog teacherOptionDialog;

    private void ShowTeacherOptions(final String _NFCSerialNumber, final String FirstName, final String LastName){
        //teacherOptionDialog.dismiss();
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.teacher_options_dialog,null);

        final Button btnTimeInTimeOut = (Button) alertLayout.findViewById(R.id.btnTimeInTimeOut);
        final Button btnDismissClass = (Button)alertLayout.findViewById(R.id.btnTeacherLogin);
        final TextView tvTeacherName = (TextView) alertLayout.findViewById(R.id.tvTeacherName);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(alertLayout);

        tvTeacherName.setText("Hi, " + FirstName + " " + LastName);


        btnTimeInTimeOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar c = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
                try {
                    c.setTime(sdf.parse(FetchedServerDateTime));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                final Date d = c.getTime();
                final String _day = String.format("%tA",d);
                final String _time = String.format("%tT",d);
                final String _date = String.format("%tF",d);
                TakeTeacherAttendance(_NFCSerialNumber, _day,_time,_date,SelectedRoomID+"");
            }
        });

        btnDismissClass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar c = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
                try {
                    c.setTime(sdf.parse(FetchedServerDateTime));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                final Date d = c.getTime();
                final String _date = String.format("%tF",d);
                DismissClass(SelectedClassID,_date);
            }
        });


        teacherOptionDialog = builder.create();
        teacherOptionDialog.show();
    }

    private void ShowErrorMessage(String message){
        final GIFView gifView = (GIFView) findViewById(R.id.gvNfcanimation);
        final TextView tvAttendanceMessage = (TextView) findViewById(R.id.AttendanceMessage);

        tvAttendanceMessage.setText(message);

        frameLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),ColorID));
        Animation tvAn = AnimationUtils.loadAnimation(getBaseContext(),R.anim.fade);

        tvAttendanceMessage.startAnimation(tvAn);

        tvAn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                gifView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                tvAttendanceMessage.setText("");
                frameLayout.setBackgroundColor(getResources().getColor(R.color.white));
                gifView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void ShowAttendeeInfo(String [] AttendeeInfo){
        final GIFView gifView = (GIFView) findViewById(R.id.gvNfcanimation);
        final TextView tvAttendanceMessage = (TextView) findViewById(R.id.AttendanceMessage);
        final TextView tvAttendanceType = (TextView) findViewById(R.id.tvAttendanceType);

        if (AttendeeInfo[0].equalsIgnoreCase("FirstIn"))
        {
            ColorID = R.color.attendance_in;
            tvAttendanceType.setText(AttendeeInfo[2] + " Timed In at " + AttendeeInfo[6]);
            tvAttendanceMessage.setText("Good Day, " + AttendeeInfo[3] + "!");
        }
        else if (AttendeeInfo[0].equalsIgnoreCase("In"))
        {
            ColorID = R.color.attendance_in;
            tvAttendanceType.setText(AttendeeInfo[2] + " Timed In at " + AttendeeInfo[6]);
            tvAttendanceMessage.setText("Welcome back, " + AttendeeInfo[3] + "!");
        }
        else if (AttendeeInfo[0].equalsIgnoreCase("Class Dismissed!"))
        {
            playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.job_success);
            ColorID = R.color.attendance_in;
            tvAttendanceType.setText("");
            tvAttendanceMessage.setText("Class Dismissed. Have a nice day!");
            playNFCReadSound.start();
        }
        else
        {
            ColorID = R.color.attendance_out;
            tvAttendanceType.setText(AttendeeInfo[2] + " Timed Out at " + AttendeeInfo[6]);
            tvAttendanceMessage.setText("Have a Nice Day, " + AttendeeInfo[3] + "!");
        }

        frameLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),ColorID));
        Animation tvAn = AnimationUtils.loadAnimation(getBaseContext(),R.anim.fade);

        tvAttendanceMessage.startAnimation(tvAn);
        tvAn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                gifView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                tvAttendanceMessage.setText("");
                tvAttendanceType.setText("");
                frameLayout.setBackgroundColor(getResources().getColor(R.color.white));
                gifView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    protected void exitByBackKey() {

        //Intent intent = new Intent(AttendanceMonitoringActivity.this, MainActivity.class);
        //finish();
        //startActivity(intent);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exitByBackKey();
            return true;
        }
        return super.onKeyDown(keyCode, event);
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

    private void RunClock(){
        GetServerDateTime();
        final TextView tvRunningClock = (TextView) findViewById(R.id.tvRunningTime);
        final TextView tvRunningDate = (TextView) findViewById(R.id.tvRunningDate);

        final Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        try {
            c.setTime(sdf.parse(FetchedServerDateTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        final Date d = c.getTime();
        final String _day = String.format("%tA",d);
        final String _time = String.format("%tT",d);

        class TimeAsync extends AsyncTask<String, Void, String>{
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params) {
                String _time = String.format("%tr", d);

                return _time;
            }

            @Override
            protected void onPostExecute(String result){
                CheckRoomSet(GetWIFIMacAddress(getApplicationContext()));
                if (ROOM_NAME != null)
                {
                    GetClassInfo(ROOM_ID,_day,_time);
                    tvRunningClock.setText(result);
                    tvRunningDate.setText(String.format("%tA", d) + ", " +
                                          String.format("%tB", d) + " " +
                                          String.format("%td", d) + ", " +
                                          String.format("%tY", d));
                    RunClock();
                }
                else
                {
                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(AttendanceMonitoringActivity.this);
                    alertDialogBuilder.setTitle("Room Settings Changed");
                    alertDialogBuilder.setMessage("The device detected changes in Room Settings. Attendance Monitoring will Exit.");
                    alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    alertDialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            finish();
                            Intent i = new Intent(AttendanceMonitoringActivity.this,LoginAdministratorActivity.class);
                            startActivity(i);
                        }
                    });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            }
        }
        TimeAsync TimeAs = new TimeAsync();
        TimeAs.execute();
    }

    private void GetServerDateTime() {

        class DateTimeAsync extends AsyncTask<String, Void, String>{
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params) {

                InputStream is = null;
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                String result = null;

                try{

                    String _Server = GetFirstString("ConnectionSettings_Table","Server","ConnectionID","1");

                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(
                            "http://" + _Server + "/TakeYourTIME/server_date_time.php");
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
                if (result != null)
                {
                    FetchedServerDateTime = result.trim();
                }
            }
        }
        DateTimeAsync DateTimeAs = new DateTimeAsync();
        DateTimeAs.execute();
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
                final TextView tvRoomName = (TextView) findViewById(R.id.RoomSetText);
                if (result != null)
                {
                    String s = result.trim();
                    if(s.equalsIgnoreCase("No Room Set")){
                        ROOM_NAME = null;
                        ROOM_ID = null;
                    }else {
                        String [] splitted = s.split("SPLIT");
                        ROOM_ID = splitted[0];
                        ROOM_NAME = splitted[2];

                        tvResult.setText(ROOM_NAME);
                        SelectedRoomID = Integer.parseInt(ROOM_ID);
                    }
                }
            }
        }

        CheckRoomSetAsync cra = new CheckRoomSetAsync();
        cra.execute(WIFI_MAC);
    }

    private void GetClassInfo(String RoomID, String _Day, String _Time){
        class GetClassInfoAsync extends AsyncTask<String, Void, String> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params) {
                String roomid = params[0];
                String day = params[1];
                String time = params[2];

                InputStream is = null;
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("RoomID", roomid));
                nameValuePairs.add(new BasicNameValuePair("Day", day));
                nameValuePairs.add(new BasicNameValuePair("Time", time));
                String result = null;

                String _Server = GetFirstString("ConnectionSettings_Table","Server","ConnectionID","1");

                try{
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(
                            "http://" + _Server + "/TakeYourTIME/check_ongoing_class.php");
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
                final TextView tvClassTeacher = (TextView) findViewById(R.id.tvClassTeacher);
                final TextView tvClassName = (TextView) findViewById(R.id.tvClassName);

                if (result != null)
                {
                    String s = result.trim();
                    if(s.equalsIgnoreCase("No Ongoing Class")){
                        tvClassTeacher.setText("N/A");
                        tvClassName.setText("N/A");
                    }
                    else if (s.equalsIgnoreCase("It's Sunday"))
                    {
                        tvClassTeacher.setText("N/A");
                        tvClassName.setText("N/A");
                    }
                    else{
                        String [] splitted = s.split("SPLIT");
                        tvClassTeacher.setText(splitted[0]);
                        tvClassName.setText(splitted[1]);
                        SelectedClassID = splitted[2];
                    }
                }
            }
        }

        GetClassInfoAsync gcia = new GetClassInfoAsync();
        gcia.execute(RoomID, _Day, _Time);
    }

    String SelectedClassID = "";

    private void DismissClass(String ClassID, String Date){
        class DismissClassAsync extends AsyncTask<String, Void, String> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params) {
                String classid = params[0];
                String date = params[1];

                InputStream is = null;
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("ClassID", classid));
                nameValuePairs.add(new BasicNameValuePair("Date", date));
                String result = null;

                String _Server = GetFirstString("ConnectionSettings_Table","Server","ConnectionID","1");

                try{
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(
                            "http://" + _Server + "/TakeYourTIME/dismiss_class.php");
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

                if (result != null)
                {
                    String s = result.trim();
                    if(s.equalsIgnoreCase("Class Dismissed!")){
                        String [] class_dismiss = {"Class Dismissed!","","","","","",""};
                        ShowAttendeeInfo(class_dismiss);
                    }
                    else if (s.equalsIgnoreCase("There is no Class to dismiss!"))
                    {
                        ColorID = R.color.attendance_error;
                        playNFCReadSound = MediaPlayer.create(AttendanceMonitoringActivity.this, R.raw.card_reject);
                        ShowErrorMessage("There's no class to dismiss");
                        playNFCReadSound.start();
                    }
                    else{
                        ColorID = R.color.attendance_error;
                        ShowErrorMessage(s);
                    }
                    teacherOptionDialog.dismiss();
                }
            }
        }

        DismissClassAsync dcas = new DismissClassAsync();
        dcas.execute(ClassID, Date);
    }
}
