package com.joshii.takeyourtime;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    ArrayList<HashMap<String, String>> roomItems;

    SQLiteDatabase TYT_DB;
    String _Server;
    TextView tvSetRoom;

    public RecyclerAdapter(ArrayList<HashMap<String, String>> arrayList){
        this.roomItems = arrayList;
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.room_recycler_child,parent,false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.tvRoomCode.setText(roomItems.get(position).get("RoomCode"));
        holder.tvRoomName.setText(roomItems.get(position).get("RoomName"));
        holder.tvRoomDescription.setText(roomItems.get(position).get("RoomDescription"));
        holder.tvRoomDevice.setText(roomItems.get(position).get("RoomDeviceMAC"));
        holder.tvRoomID.setText(roomItems.get(position).get("RoomID"));

    }

    @Override
    public int getItemCount() {
        return roomItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView tvRoomName;
        TextView tvRoomCode;
        TextView tvRoomDescription;
        TextView tvRoomDevice;
        TextView tvRoomID;

        public ViewHolder(View itemView) {
            super(itemView);
            tvRoomName = (TextView) itemView.findViewById(R.id.roomName);
            tvRoomCode = (TextView) itemView.findViewById(R.id.roomCode);
            tvRoomDescription = (TextView) itemView.findViewById(R.id.roomDescription);
            tvRoomDevice = (TextView) itemView.findViewById(R.id.roomDevice);
            tvRoomID = (TextView) itemView.findViewById(R.id.roomID);

            itemView.setOnClickListener(this);

        }

        @Override
        public void onClick(final View view) {


            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());

            if (tvRoomDevice.getText().toString().equalsIgnoreCase("No Device Set"))
            {
                builder.setMessage("Set '" + tvRoomName.getText() + "' for this Device?");
                builder.setTitle("SET ROOM");
                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        builder2 = new AlertDialog.Builder(view.getContext());
                        SetRoom(new Splash().GetWIFIMacAddress(view.getContext()),tvRoomID.getText().toString(),view.getContext());
                        tvRoomDevice.setText(new Splash().GetWIFIMacAddress(view.getContext()));
                    }
                });

                builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
            }
            else if (new Splash().GetWIFIMacAddress(view.getContext()).equalsIgnoreCase(tvRoomDevice.getText().toString()))
            {
                builder.setMessage("'" + tvRoomName.getText() + "' is already set to this device.");
                builder.setTitle("ROOM ALREADY SET");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
            }
            else
            {
                builder.setMessage("'" + tvRoomName.getText() + "' is already set to other device.");
                builder.setTitle("ROOM ALREADY SET");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
            }



            AlertDialog alertDialog = builder.create();
            alertDialog.show();
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
