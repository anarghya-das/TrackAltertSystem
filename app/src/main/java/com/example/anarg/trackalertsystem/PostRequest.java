package com.example.anarg.trackalertsystem;

import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.cunoraz.gifview.library.GifView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class PostRequest extends AsyncTask<String, Void, String> {
    private BackEnd backEnd;
    private String value;
    private MediaPlayer mediaPlayer;
    private GifView gifView;
    private TextView textView;
    private boolean pause;
    private boolean danger;
    AsyncResponse response;
    private ThreadControl threadControl;

    PostRequest(String s, MediaPlayer mp, GifView gifView, boolean pause, TextView textView, ThreadControl threadControl,AsyncResponse response) {
        backEnd = new BackEnd();
        value = s;
        mediaPlayer = mp;
        this.gifView = gifView;
        this.pause = pause;
        danger = false;
        this.textView = textView;
        this.threadControl = threadControl;
        this.response=response;
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            boolean t = true;
            while (t) {
                threadControl.waitIfPaused();
                //Stop work if control is cancelled.
                if (threadControl.isCancelled()) {
                    break;
                }
                String res = post(strings[0], "asd");
                ArrayList<Train> trains = backEnd.jsonGov(res);
                if (trains==null){
                    throw new Exception();
                }else if (trains.size() != 0) {
                    ArrayList<String> trackNames = backEnd.trackNames(trains);
                    Log.d("sound", trackNames.toString());
                    boolean result = checkTrack(value, trackNames);
                    if (result) {
                        danger = true;
                        if (!pause) {
                            if (!mediaPlayer.isPlaying()) {
                                mediaPlayer.start();
                            }
                        }
                        if (mediaPlayer.isPlaying() && pause) {
                            mediaPlayer.pause();
                        }
                    } else {
                        if (mediaPlayer.isPlaying() && !pause) {
                            danger = false;
                            mediaPlayer.stop();
                        }
                    }
                }
                t = false;
            }
        } catch (Exception e) {
            return null;
        }
        return "normal";
    }

    @Override
    protected void onPostExecute(String result) {
        if (result==null){
            response.processFinish("null");
        }else {
            response.processFinish("good");
            if (danger) {
                gifView.setVisibility(View.VISIBLE);
                gifView.play();
                textView.setText("TRAIN INCOMING!");
            } else {
                gifView.setVisibility(View.INVISIBLE);
                gifView.pause();
                textView.setText("No Train enroute on this Track");
            }
        }
    }

    private boolean checkTrack(String s, ArrayList<String> trains) {
        for (String t : trains) {
            if (t.equals(s)) {
                return true;
            }
        }
        return false;
    }

    private String post(String u, String json) throws IOException {
        String response;
        // This is getting the url from the string we passed in
        URL url = new URL(u);

        // Create the urlConnection
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();


        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);

        urlConnection.setRequestProperty("Content-Type", "application/json");

        urlConnection.setRequestMethod("POST");


        // OPTIONAL - Sets an authorization header
        urlConnection.setRequestProperty("Authorization", "someAuthString");

        // Send the post body
        if (json != null && !json.isEmpty()) {
            OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
            writer.write(json);
            writer.flush();
        }

        int statusCode = urlConnection.getResponseCode();


        if (statusCode == 200) {

            InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());

            response = convertInputStreamToString(inputStream);
            if (response == null || response.isEmpty()) {
                throw new IOException();
            }

        }
        // From here you can convert the string to JSON with whatever JSON parser you like to use
        // After converting the string to JSON, I call my custom callback. You can follow this process too, or you can implement the onPostExecute(Result) method
        else {
            // Status code is not 200
            // Do something to handle the error
            throw new IOException();
        }

        return response;
    }

    private String convertInputStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}