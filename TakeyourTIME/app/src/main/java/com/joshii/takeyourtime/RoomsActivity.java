package com.joshii.takeyourtime;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class RoomsActivity extends AppCompatActivity {

    RecyclerView recyclerView;

    String myJSON;

    private SQLiteDatabase TYT_DB;

    private static final String TAG_RESULTS="result";
    private static final String TAG_ROOMID="RoomID";
    private static final String TAG_ROOMCODE = "RoomCode";
    private static final String TAG_ROOMNAME = "RoomName";
    private static final String TAG_ROOMDESCRIPTION ="RoomDescription";
    private static final String TAG_ROOMDEVICEMAC ="RoomDeviceMAC";

    JSONArray _ROOMS = null;

    ArrayList<HashMap<String, String>> roomItems;

    ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rooms);
        InitializeDatabase();
        recyclerView = (RecyclerView) findViewById(R.id.roomRecycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        roomItems = new ArrayList<HashMap<String,String>>();
        getData();
        //recyclerView.setAdapter(new RecyclerAdapter(roomItems));
    }

    public void getData(){
        class GetDataJSON extends AsyncTask<String, Void, String> {

            @Override
            protected String doInBackground(String... params) {

                String _Server = GetFirstString("ConnectionSettings_Table","Server","ConnectionID","1");

                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(
                        "http://" + _Server + "/TakeYourTIME/get_room_list.php");

                // Depends on your web service
                httpPost.setHeader("Content-type", "application/json");

                InputStream inputStream = null;
                String result = null;
                try {
                    HttpResponse response = httpClient.execute(httpPost);
                    HttpEntity entity = response.getEntity();

                    inputStream = entity.getContent();
                    // json is UTF-8 by default
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                    StringBuilder sb = new StringBuilder();

                    String line = null;
                    while ((line = reader.readLine()) != null)
                    {
                        sb.append(line + "\n");
                    }
                    result = sb.toString();
                } catch (Exception e) {
                    // Oops
                }
                finally {
                    try{if(inputStream != null)inputStream.close();}catch(Exception squish){}
                }
                return result;
            }

            @Override
            protected void onPostExecute(String result){
                myJSON=result;
                showList();
            }
        }
        GetDataJSON g = new GetDataJSON();
        g.execute();
    }

    protected void showList(){
        try {
            JSONObject jsonObj = new JSONObject(myJSON);
            _ROOMS = jsonObj.getJSONArray(TAG_RESULTS);

            for(int i=0;i<_ROOMS.length();i++){
                JSONObject c = _ROOMS.getJSONObject(i);
                String _RoomID = c.getString(TAG_ROOMID);
                String _RoomCode = c.getString(TAG_ROOMCODE);
                String _RoomName = c.getString(TAG_ROOMNAME);
                String _RoomDescription = c.getString(TAG_ROOMDESCRIPTION);
                String _RoomDeviceMAC = c.getString(TAG_ROOMDEVICEMAC);

                HashMap<String,String> room = new HashMap<String,String>();

                room.put(TAG_ROOMID,_RoomID);
                room.put(TAG_ROOMCODE,_RoomCode);
                room.put(TAG_ROOMNAME,_RoomName);
                room.put(TAG_ROOMDESCRIPTION,_RoomDescription);
                room.put(TAG_ROOMDEVICEMAC,_RoomDeviceMAC);

                roomItems.add(room);
            }

            //RecyclerAdapter roomItemAdapter = ;

            RecyclerAdapter recyclerAdapter = new RecyclerAdapter(roomItems);
            recyclerAdapter._Server = GetFirstString("ConnectionSettings_Table","Server","ConnectionID","1");

            recyclerView.setAdapter(recyclerAdapter);

        } catch (JSONException e) {
            e.printStackTrace();
        }

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

    public void btnBackToDashboard_Click (View view){
        Intent intent = new Intent(RoomsActivity.this,UserDashboard.class);
        finish();
        startActivity(intent);
    }

}
