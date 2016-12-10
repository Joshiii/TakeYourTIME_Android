package com.joshii.takeyourtime;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.Arrays;
import java.util.List;

public class LoginAdministratorActivity extends AppCompatActivity {

    SQLiteDatabase TYT_DB;

    private EditText txtLoginAdminUsername;
    private EditText txtLoginAdminPassword;

    private NfcAdapter nfcAdapter;
    private MediaPlayer playNFCReadSound = new MediaPlayer();

    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String TAG = "NfcDemo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_administrator);

        txtLoginAdminUsername = (EditText) findViewById(R.id.txtUsername);
        txtLoginAdminPassword = (EditText) findViewById(R.id.txtPassword);

        InitializeDatabase();

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;

        }

        if (!nfcAdapter.isEnabled()) {
            //mTextView.setText("NFC is disabled. Please enable it at your Device Settings.");
        } else {

        }

        playNFCReadSound = MediaPlayer.create(this, R.raw.job_success);
    }

    @Override
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

    private void handleIntent(Intent intent) {
        String action = intent.getAction();


        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            String type = intent.getType();

            playNFCReadSound.start();
            LoginUsingNFC(ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)));

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

    private void LoginUsingNFC(final String nfcserialnumber) {

        class LogiUsingNFCnAsync extends AsyncTask<String, Void, String>{

            private Dialog loadingDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loadingDialog = ProgressDialog.show(LoginAdministratorActivity.this, "Please wait", "Logging in...");
            }

            @Override
            protected String doInBackground(String... params) {
                String nfcserial = params[0];

                InputStream is = null;
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("NFCSerialNumber", nfcserial));
                String result = null;

                try{

                    String _Server = GetFirstString("ConnectionSettings_Table","Server","ConnectionID","1");

                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(
                            "http://" + _Server + "/TakeYourTIME/nfc_login.php");
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
                    loadingDialog.dismiss();
                    if(s.contains("Login Success!")){
                        String [] splitted = s.split("SPLIT");

                        Intent intent = new Intent(LoginAdministratorActivity.this, UserDashboard.class);
                        intent.putExtra("FirstName",splitted[1]);
                        intent.putExtra("LastName", splitted[2]);
                        finish();
                        startActivity(intent);
                    }else {
                        Toast.makeText(getApplicationContext(), "ID Card doesn't belong to any account", Toast.LENGTH_LONG).show();
                    }
                }
                else
                {
                    loadingDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Error connecting on Server. Please check your connection or Contact System Administrator for Assistance", Toast.LENGTH_LONG).show();
                }
            }
        }

        LogiUsingNFCnAsync lnfca = new LogiUsingNFCnAsync();
        lnfca.execute(nfcserialnumber);

    }

    protected void exitByBackKey() {

        //Intent intent = new Intent(LoginAdministratorActivity.this, MainActivity.class);
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

    public void BackToMain_Click (View view)
    {
        exitByBackKey();
    }

    public void Login_Click (View view) {
        if (txtLoginAdminUsername.getText().toString().replace(" ","").equals("") &&
            txtLoginAdminPassword.getText().toString().replace(" ","").equals(""))
        {
            Toast.makeText(getApplicationContext(),"Please provide your Login Credentials",Toast.LENGTH_LONG).show();
        }
        else if (txtLoginAdminUsername.getText().toString().replace(" ","").equals(""))
        {
            Toast.makeText(getApplicationContext(),"Please provide your Username",Toast.LENGTH_LONG).show();
        }
        else if (txtLoginAdminPassword.getText().toString().replace(" ","").equals(""))
        {
            Toast.makeText(getApplicationContext(),"Please provide your Password",Toast.LENGTH_LONG).show();
        }
        else if (!isConnectedToInternet())
        {
            Toast.makeText(getApplicationContext(), "Cannot connect to the Server. Please turn on Wi-Fi and try again.", Toast.LENGTH_LONG).show();
        }
        else
        {
            LoginUsingLoginCredentials(txtLoginAdminUsername.getText().toString(),txtLoginAdminPassword.getText().toString());
        }
    }

    private void LoginUsingLoginCredentials(final String username, String password) {

        class LoginAsync extends AsyncTask<String, Void, String> {

            private Dialog loadingDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loadingDialog = ProgressDialog.show(LoginAdministratorActivity.this, "Please wait", "Logging in...");
            }

            @Override
            protected String doInBackground(String... params) {
                String uname = params[0];
                String pass = params[1];

                InputStream is = null;
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("Username", uname));
                nameValuePairs.add(new BasicNameValuePair("Password", pass));
                String result = null;

                try{

                    String _Server = GetFirstString("ConnectionSettings_Table","Server","ConnectionID","1");

                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(
                            "http://" + _Server + "/TakeYourTIME/normal_login.php");
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
                    loadingDialog.dismiss();
                    if(s.contains("Login Success!")){
                        String [] splitted = s.split("SPLIT");

                        Intent intent = new Intent(LoginAdministratorActivity.this, UserDashboard.class);
                        Bundle extras = new Bundle();
                        extras.putString("FirstName",splitted[1]);
                        extras.putString("LastName", splitted[2]);
                        intent.putExtras(extras);
                        finish();
                        startActivity(intent);
                        Toast.makeText(getApplicationContext(), "Success!", Toast.LENGTH_LONG).show();
                    }else {
                        Toast.makeText(getApplicationContext(), "Invalid User Name or Password", Toast.LENGTH_LONG).show();
                    }
                }
                else
                {
                    loadingDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Error connecting on Server. Please check your connection or Contact System Administrator for Assistance", Toast.LENGTH_LONG).show();
                }
            }
        }

        LoginAsync la = new LoginAsync();
        la.execute(username, password);

    }

    public void btnConnectionSettings_Click (View view) {

        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.connection_settings_dialog,null);

        final EditText txtServer = (EditText)alertLayout.findViewById(R.id.txtConnectionServerName);
        final EditText txtUser = (EditText)alertLayout.findViewById(R.id.txtConnectionUsername);
        final EditText txtPass = (EditText)alertLayout.findViewById(R.id.txtConnectionPassword);

        final Button buttonTestConnection = (Button)alertLayout.findViewById(R.id.btnConnectionTest);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(alertLayout);

        String _ServerName = "";
        String _UserName = "";
        String _Password = "";

        try
        {
            _ServerName = GetFirstString("ConnectionSettings_Table", "Server","ConnectionID","1");
            _UserName = GetFirstString("ConnectionSettings_Table", "UserID","ConnectionID","1");
            _Password = GetFirstString("ConnectionSettings_Table", "Password","ConnectionID","1");
        }
        catch (Exception ex){}

        final String ServerName = _ServerName;
        final String UserID = _UserName;
        final String UserPassword = _Password;

        txtServer.setText(ServerName);
        txtUser.setText(UserID);
        txtPass.setText(UserPassword);

        builder.setPositiveButton("OK/Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (txtServer.getText().toString().replace(" ","").equals("") && txtUser.getText().toString().replace(" ","").equals(""))
                {
                    Toast.makeText(getApplicationContext(), "Please provide Connection credentials", Toast.LENGTH_LONG).show();
                }
                else if (txtServer.getText().toString().replace(" ","").equals(""))
                {
                    Toast.makeText(getApplicationContext(), "Please provide Host/Server Name", Toast.LENGTH_LONG).show();
                }
                else if (txtUser.getText().toString().replace(" ","").equals(""))
                {
                    Toast.makeText(getApplicationContext(), "Please provide User ID", Toast.LENGTH_LONG).show();
                }
                else
                {
                    try
                    {
                        if (ServerName.equals("") && UserID.equals(""))
                        {
                            TYT_DB.execSQL("INSERT INTO ConnectionSettings_Table " +
                                    "VALUES (1, '" +
                                    txtServer.getText().toString() + "', '" +
                                    txtUser.getText().toString() + "', '" +
                                    txtPass.getText().toString() + "', " +
                                    "'TakeYourTIME_DB');");
                        }
                        else
                        {
                            TYT_DB.execSQL("UPDATE ConnectionSettings_Table " +
                                    "SET Server = '" + txtServer.getText().toString() + "', " +
                                    "UserID = '" + txtUser.getText().toString() + "', " +
                                    "Password = '" + txtPass.getText().toString() + "' " +
                                    "WHERE ConnectionID = 1");
                        }
                    }
                    catch (SQLException ex)
                    {
                        Toast.makeText(getApplicationContext(), "Saving Connection Error. Contact System Administrator for Assistance", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });


        buttonTestConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (txtServer.getText().toString().replace(" ","").equals("") && txtUser.getText().toString().replace(" ","").equals(""))
                {
                    Toast.makeText(getApplicationContext(), "Please provide Connection credentials", Toast.LENGTH_LONG).show();
                }
                else if (txtServer.getText().toString().replace(" ","").equals(""))
                {
                    Toast.makeText(getApplicationContext(), "Please provide Host/Server Name", Toast.LENGTH_LONG).show();
                }
                else if (txtUser.getText().toString().replace(" ","").equals(""))
                {
                    Toast.makeText(getApplicationContext(), "Please provide User ID", Toast.LENGTH_LONG).show();
                }
                else{
                    CheckConnection(txtServer.getText().toString(), txtUser.getText().toString(),txtPass.getText().toString());
                }
            }
        });


        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void CheckConnection(final String server, final String user, final String pass) {

        class CheckConnectionAsync extends AsyncTask<String, Void, String> {

            private Dialog loadingDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loadingDialog = ProgressDialog.show(LoginAdministratorActivity.this, "Please Wait", "Checking Connection...");
            }

            @Override
            protected String doInBackground(String... params) {
                String server = params[0];
                String user = params[1];
                String pass = params[2];

                InputStream is = null;
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("ServerName", "127.0.0.1"));
                nameValuePairs.add(new BasicNameValuePair("Username", user));
                nameValuePairs.add(new BasicNameValuePair("Password", pass));
                String result = null;

                try{
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(
                            "http://" + server + "/TakeYourTIME/connection.php");
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
                    loadingDialog.dismiss();
                    if(s.equalsIgnoreCase("Connection Successfully Established")){
                        Toast.makeText(getApplicationContext(), "Connection Test Successful", Toast.LENGTH_LONG).show();
                    }else {
                        Toast.makeText(getApplicationContext(), "Connection Test Failed", Toast.LENGTH_LONG).show();
                    }
                }
                else
                {
                    loadingDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Error connecting on Server. Please check your connection or Contact System Administrator for Assistance", Toast.LENGTH_LONG).show();
                }
            }
        }

        CheckConnectionAsync cca = new CheckConnectionAsync();
        cca.execute(server,user, pass);

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
}
