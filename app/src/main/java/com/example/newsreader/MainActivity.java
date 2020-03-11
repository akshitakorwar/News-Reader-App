package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    SQLiteDatabase sqLiteDatabase;
    ArrayList<String> titleList = new ArrayList<String>();
    ArrayList<String> urlList = new ArrayList<String>();
    ArrayAdapter<String> arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sqLiteDatabase = this.openOrCreateDatabase("NewsReader", MODE_PRIVATE, null);
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS NewsReader (headline varchar, details varchar)");

        DownloadContent downloadContent = new DownloadContent();
        String result = null;
        try{
            result = downloadContent.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i("Result", result);

        String id = null;
        final int topHeadlines = 10;

        try {
            JSONArray jsonArray = new JSONArray(result);
            Log.i("Json array length", String.valueOf(jsonArray.length()));
            for(int i=0; i<topHeadlines; i++){
                id = jsonArray.getString(i);
                Log.i("ID", id);
                getTitleURL(id);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ListView listView = findViewById(R.id.listView);

        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, titleList);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), WebActivity.class);
                Log.i("INFO","Clicked on option");
                intent.putExtra("news", urlList.get(position));
                startActivity(intent);
            }
        });
    }

    public void getTitleURL(String id){
        DownloadContent downloadContent = new DownloadContent();
        String data = null;
        String title =null;
        String url = null;

        try {
            data = downloadContent.execute("https://hacker-news.firebaseio.com/v0/item/"+id+".json?print=pretty").get();
            JSONObject jsonObject = new JSONObject(data);
            title = jsonObject.getString("title");
            Log.i("Title", title);
            titleList.add(title);
            url = jsonObject.getString("url");
            Log.i("url", url);
            urlList.add(url);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //sqLiteDatabase.execSQL("INSERT INTO NewsReader (headline, details) VALUES (title, url)");
        String sql = "INSERT INTO NewsReader (headline, details) VALUES (?,?)";
        SQLiteStatement statement = sqLiteDatabase.compileStatement(sql);
        statement.bindString(1, title);
        statement.bindString(2, url);
        statement.execute();

        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM NewsReader", null);
        int headlineIndex = cursor.getColumnIndex("headline");
        int detailsIndex = cursor.getColumnIndex("details");

        cursor.moveToFirst();

        while(!cursor.isAfterLast()){
           Log.i("Headline", cursor.getString(headlineIndex));
           Log.i("Details", cursor.getString(detailsIndex));

          //  titleList.add(cursor.getString(headlineIndex));
          //  urlList.add(cursor.getString(detailsIndex));

            cursor.moveToNext();
        }
    }
    public class DownloadContent extends AsyncTask<String, String, String>{

        @Override
        protected String doInBackground(String... strings) {

            String result = "";
            URL url;
            HttpURLConnection httpURLConnection = null;

            try
            {
                url =new URL(strings[0]);
                httpURLConnection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = httpURLConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

                int data = inputStreamReader.read();

                while (data != -1){
                    char current = (char) data;
                    result += current;

                    data = inputStreamReader.read();
                }
                Log.i("Result down here", result);
                return result;
            }
            catch (MalformedURLException e) {
                return "Failed";
            }
            catch (IOException e){
                e.printStackTrace();
                return "IOException";
            }
        }
    }
}