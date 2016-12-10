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
import android.view.KeyEvent;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
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
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TeacherDashboard extends AppCompatActivity {

    RecyclerView recyclerView;

    String myJSON;


    private SQLiteDatabase TYT_DB;

    private static final String TAG_RESULTS="result";
    private static final String TAG_CLASSID="ClassID";
    private static final String TAG_CLASSNAME = "ClassName";
    private static final String TAG_NFCSERIALNUMBER = "NFCSerialNumber";
    private static final String TAG_SUBJECTID ="SubjectID";
    private static final String TAG_SUBJECTCODE ="SubjectCode";
    private static final String TAG_ROOMID ="RoomID";
    private static final String TAG_ROOMCODE ="RoomCode";
    private static final String TAG_DAY ="Day";
    private static final String TAG_STARTTIME ="StartTime";
    private static final String TAG_ENDTIME ="EndTime";
    private static final String TAG_CLASSTOTALSTUDENTS ="ClassTotalStudents";

    JSONArray _CLASSES = null;

    ArrayList<HashMap<String, String>> classItems;

    ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);
        InitializeDatabase();
        recyclerView = (RecyclerView) findViewById(R.id.teacherClassesRecycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        classItems = new ArrayList<HashMap<String,String>>();

        Bundle extras = getIntent().getExtras();
        getData(extras.getString("RoomID"));

        //TextView tvTeacherFullName = (TextView) findViewById(R.id.tvTeacherFullName);
        //tvTeacherFullName.setText("Hi, " + extras.getString("FirstName") + " " + extras.getString("LastName") + "!");
        //recyclerView.setAdapter(new RecyclerAdapter(roomItems));
    }

    public void getData(final String _RoomID){
        class GetDataJSON extends AsyncTask<String, Void, String> {

            @Override
            protected String doInBackground(String... params) {
                String selectquery = params[0];


                InputStream inputStream = null;
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("SelectQuery", selectquery));
                String result = null;

                try{

                    String _Server = GetFirstString("ConnectionSettings_Table","Server","ConnectionID","1");

                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(
                            "http://" + _Server + "/TakeYourTIME/get_teacher_class_list.php");
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    HttpResponse response = httpClient.execute(httpPost);

                    HttpEntity entity = response.getEntity();

                    inputStream = entity.getContent();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
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

        String query = "SELECT classes_table.ClassID, " +
                                "classes_table.ClassName, " +
                                "classes_table.NFCSerialNumber, " +
                                "subjects_table.SubjectID, " +
                                "subjects_table.SubjectCode, " +
                                "rooms_table.RoomID, " +
                                "rooms_table.RoomCode, " +
                                "classes_table.Day, " +
                                "DATE_FORMAT(classes_table.StartTime,'%h:%i %p') AS 'StartTime', " +
                                "DATE_FORMAT(classes_table.EndTime,'%h:%i %p') AS 'EndTime', " +
                                "COUNT(classstudents_table.StudentNumber) AS 'ClassTotalStudents' " +
                        "FROM classes_table " +
                        "LEFT JOIN rooms_table " +
                        "ON classes_table.RoomID = rooms_table.RoomID " +
                        "LEFT JOIN subjects_table " +
                        "ON classes_table.SubjectID = subjects_table.SubjectID " +
                        "LEFT JOIN classstudents_table " +
                        "ON classes_table.ClassID = classstudents_table.ClassID " +
                        "WHERE classes_table.RoomID = " + _RoomID + "";
        g.execute(query);
    }

    protected void showList(){
        try {
            JSONObject jsonObj = new JSONObject(myJSON);
            _CLASSES = jsonObj.getJSONArray(TAG_RESULTS);

            for(int i=0;i<_CLASSES.length();i++){
                JSONObject c = _CLASSES.getJSONObject(i);
                String _ClasID = c.getString(TAG_CLASSID);
                String _ClasName = c.getString(TAG_CLASSNAME);
                String _NFCSerialNumber = c.getString(TAG_NFCSERIALNUMBER);
                String _SubjectID = c.getString(TAG_SUBJECTID);
                String _SubjectCode = c.getString(TAG_SUBJECTCODE);
                String _RoomID = c.getString(TAG_ROOMID);
                String _RoomCode = c.getString(TAG_ROOMCODE);
                String _Day = c.getString(TAG_DAY);
                String _StartTime = c.getString(TAG_STARTTIME);
                String _EndTime = c.getString(TAG_ENDTIME);
                String _ClassTotalStudents = c.getString(TAG_CLASSTOTALSTUDENTS);

                HashMap<String,String> room = new HashMap<String,String>();

                room.put(TAG_CLASSID,_ClasID);
                room.put(TAG_CLASSNAME,_ClasName);
                room.put(TAG_NFCSERIALNUMBER,_NFCSerialNumber);
                room.put(TAG_SUBJECTID,_SubjectID);
                room.put(TAG_SUBJECTCODE,_SubjectCode);
                room.put(TAG_ROOMID,_RoomID);
                room.put(TAG_ROOMCODE,_RoomCode);
                room.put(TAG_DAY,_Day);
                room.put(TAG_STARTTIME,_StartTime);
                room.put(TAG_ENDTIME,_EndTime);
                room.put(TAG_CLASSTOTALSTUDENTS,_ClassTotalStudents);


                classItems.add(room);
            }

            //RecyclerAdapter roomItemAdapter = ;

            TeacherClassesRecyclerAdapter teacherClassesRecyclerAdapter = new TeacherClassesRecyclerAdapter(classItems);
            teacherClassesRecyclerAdapter._Server = GetFirstString("ConnectionSettings_Table","Server","ConnectionID","1");

            recyclerView.setAdapter(teacherClassesRecyclerAdapter);

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

    protected void exitByBackKey() {

        Intent intent = new Intent(TeacherDashboard.this, UserDashboard.class);
        finish();
        startActivity(intent);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exitByBackKey();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void btnBackToDashboardFromListOfClasses_Click (View view){
        Intent intent = new Intent(TeacherDashboard.this, UserDashboard.class);
        finish();
        startActivity(intent);

    }

}
