package com.example.anarg.trackalertsystem;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.cunoraz.gifview.library.GifView;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class controls the main activity of the app.
 * @author Anarghya Das
 */
public class MainActivity extends AppCompatActivity implements AsyncResponse{
    //URL for the TMS server from which the data is fetched
    private static final String tmsURL = "http://tms.affineit.com:4445/SignalAhead/Json/SignalAhead";
    //Stores the reference of the siren sound which plays in the background
    private MediaPlayer mediaPlayer;
    //Stores the reference of the Post Request Async Task
    private PostRequest asyncPost;
    //Stores the track name value entered by the user
    private String value;
    private ArrayList<String> trackValues;
    //Boolean variables which keeps track of media pause/play and connection error
    private boolean pause,error;
    //Stores the time for which error is shown in milliseconds
    private int errorFrequency;
    //Stores the gif image of siren gif
    private GifView gifView;
    //Shows the text displayed on the screen
    private TextView textView;
    //Stores the audio button for pause/play
    private FloatingActionButton audioButton;
    //Stores the alert dialog for errors
    private AlertDialog dialog;
    //Stores the reference for thread control
    private ThreadControl threadControl;
    //Timeout duration of the app after it encounters an error
    private static final int TIMEOUT_ERROR_TIME=60000;//in milliseconds ~ 60 seconds
    /**
     * The first function which runs after the activity has started
     * Initializes the the instance variables declared above
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("loadingTime", "loadingStarted ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0);
        pause=false;
        error=false;
        errorFrequency=0;
        audioButton= findViewById(R.id.soundButton);
        textView=findViewById(R.id.textView);
        gifView= findViewById(R.id.gif1);
        gifView.setGifResource(R.drawable.siren);
        Intent i= getIntent();
        value=i.getStringExtra("value").trim();
        String a[]=value.split(",");
        trackValues=new ArrayList<>(Arrays.asList(a));
        Log.d("TrackValue", trackValues.toString());
        mediaPlayer=MediaPlayer.create(this,R.raw.music);
        mediaPlayer.setLooping(true);
        threadControl=new ThreadControl();
        asyncPost=new PostRequest(trackValues,mediaPlayer,gifView,pause,textView,threadControl,this);
        asyncPost.execute(tmsURL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.post(timerTask);
    }
    /**
     * This method runs after the async task is complete and executes proper functions based on the
     * result received.
     * @param output Stores the result of the async task after completion.
     */
    @Override
    public void processFinish(String output) {
        if (!isRunning){
            mHandler.post(timerTask);
        }
        if (output.equals("null")) {
            if (dialog == null) {
                if (mediaPlayer.isPlaying()){
                    mediaPlayer.stop();
                }
                error=true;
                gifView.setVisibility(View.INVISIBLE);
                exceptionRaised("Connection Error", "Please wait while we try to reconnect." +
                        "\nIn the mean while check if your internet connection is working.", false);
            } else if (!dialog.isShowing()) {
                if (mediaPlayer.isPlaying()){
                    mediaPlayer.stop();
                }
                error=true;
                gifView.setVisibility(View.INVISIBLE);
                exceptionRaised("Connection Error", "Please wait while we try to reconnect." +
                        "\nIn the mean while check if your internet connection is working.", false);
            }else if (errorFrequency>=TIMEOUT_ERROR_TIME){
                dialog.dismiss();
                exceptionRaised("Connection Error", "Could not reconnect." +
                        "\nThere might be some problem, please try again later!", true);
                errorFrequency=0;
            }
        }else if (dialog!=null&&dialog.isShowing()&&output.equals("good")){
            error=false;
            errorFrequency=0;
            dialog.dismiss();
            if (audioButton.getTag().equals("noaudio")) {
                pause = true;
            }else if (audioButton.getTag().equals("audio")){
                pause =false;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("sound", "onDestroy: ");
        mHandler.removeCallbacks(timerTask);
        mediaPlayer.stop();
        asyncPost.cancel(true);
        threadControl.cancel();
    }
    /**
     * Handler which creates a new async Task every second to fetch the data from the server and do
     * the relevant job after receiving the data.
     */
    private Handler mHandler = new Handler();
    private boolean isRunning=false;
    private Runnable timerTask = new Runnable() {
        @Override
        public void run() {
            if (asyncPost.getStatus()== AsyncTask.Status.FINISHED) {
                asyncPost= new PostRequest(trackValues,mediaPlayer,gifView,pause,textView,threadControl,MainActivity.this);
                asyncPost.execute(tmsURL);
                isRunning=true;
            }
            if (error){
                errorFrequency++;
            }
            mHandler.postDelayed(timerTask, 1);
        }};
    /**
     * onClick handler of the mute which stops or starts the media based on user input
     */
    public void soundChange(View view) {
        if (audioButton.getTag().equals("audio")){
            pause=true;
            audioButton.setTag("noaudio");
            audioButton.setImageResource(R.drawable.noaudio);
        }else if (audioButton.getTag().equals("noaudio")){
            pause=false;
            audioButton.setTag("audio");
            audioButton.setImageResource(R.drawable.audio);
        }
    }
    /**
     * onClick button handler which stops the activity and exits to main menu
     */
    public void stop(View view) {
        finish();
    }
    /**
     * Creates a custom dialog box.
     * @param title The title of the dialog box
     * @param body Body text of the dialog box
     * @param buttons If true buttons will appear else not
     */
    public void exceptionRaised(String title,String body,boolean buttons) {
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setMessage(body)
                .setTitle(title);
        if (buttons) {
            builder.setNegativeButton("Restart", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                    Intent i = getIntent();
                    startActivity(i);
                }
            });
            builder.setPositiveButton("Main Menu", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
        }
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }

}
