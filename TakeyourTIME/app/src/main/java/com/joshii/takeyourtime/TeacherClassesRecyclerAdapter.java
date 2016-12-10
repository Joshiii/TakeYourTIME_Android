package com.joshii.takeyourtime;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by Daniel on 10/3/2016.
 */
public class TeacherClassesRecyclerAdapter extends RecyclerView.Adapter<TeacherClassesRecyclerAdapter.ViewHolder> {

    ArrayList<HashMap<String, String>> classItems;

    SQLiteDatabase TYT_DB;
    String _Server;
    TextView tvSetRoom;

    public TeacherClassesRecyclerAdapter(ArrayList<HashMap<String, String>> arrayList){
        this.classItems = arrayList;
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.teacher_classes_recycler_child,parent,false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        holder.tvClassName.setText(classItems.get(position).get("ClassName"));
        holder.tvClassTotalStudents.setText("Students in this Class : " + classItems.get(position).get("ClassTotalStudents") + " student(s)");
        holder.tvSubjectName.setText(classItems.get(position).get("SubjectCode"));
        holder.tvClassDay.setText(classItems.get(position).get("Day"));
        holder.tvClassRoom.setText(classItems.get(position).get("RoomCode"));
        holder.tvClassTime.setText(classItems.get(position).get("StartTime") + " - " + classItems.get(position).get("EndTime"));
        holder.tvClassID.setText(classItems.get(position).get("ClassID"));

    }

    @Override
    public int getItemCount() {
        return classItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView tvClassName;
        TextView tvClassTotalStudents;
        TextView tvSubjectName;
        TextView tvClassDay;
        TextView tvClassRoom;
        TextView tvClassTime;
        TextView tvClassID;

        public ViewHolder(View itemView) {
            super(itemView);
            tvClassName = (TextView) itemView.findViewById(R.id.className);
            tvClassTotalStudents = (TextView) itemView.findViewById(R.id.classNoOfStudents);
            tvSubjectName = (TextView) itemView.findViewById(R.id.classSubjectName);
            tvClassDay = (TextView) itemView.findViewById(R.id.classDay);
            tvClassRoom = (TextView) itemView.findViewById(R.id.classRoom);
            tvClassTime = (TextView) itemView.findViewById(R.id.classTime);
            tvClassID = (TextView) itemView.findViewById(R.id.classID);


            itemView.setOnClickListener(this);

        }

        @Override
        public void onClick(final View view) {


            /*AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());

            builder.setTitle(tvClassName.getText().toString());



            AlertDialog alertDialog = builder.create();
            alertDialog.show();*/
        }
    }
    AlertDialog.Builder builder2;

    private void SetRoom(final String _WIFIMAC, final String _RoomID, final Context context) {
        class SetRoomAsync extends AsyncTask<String, Void, String> {

            private Dialog loadingDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loadingDialog = ProgressDialog.show(context, "Please wait", "Setting Room..");
            }

            @Override
            protected String doInBackground(String... params) {
                String wifimac = params[0];
                String roomid = params[1];

                InputStream is = null;
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("RoomID", roomid));
                nameValuePairs.add(new BasicNameValuePair("WIFIMacAddress", wifimac));
                String result = null;

                try{

                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(
                            "http://" + _Server + "/TakeYourTIME/set_room.php");
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
                String s = result.trim();
                if (result != null)
                {
                    if(s.equalsIgnoreCase("Success"))
                    {
                    }
                    else
                    {
                        builder2.setMessage("Error connecting on Server. Please check your connection or Contact System Administrator for Assistance");
                        builder2.setTitle("SET ROOM FAILED");
                        builder2.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });
                        AlertDialog alertDialog = builder2.create();
                        alertDialog.show();
                    }

                }
                else
                {
                    builder2.setMessage("Error connecting on Server. Please check your connection or Contact System Administrator for Assistance");
                    builder2.setTitle("SET ROOM FAILED");
                    builder2.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
                    AlertDialog alertDialog = builder2.create();
                    alertDialog.show();
                }
            }
        }

        SetRoomAsync SetRoomAs = new SetRoomAsync();
        SetRoomAs.execute(_WIFIMAC,_RoomID);

    }
}
