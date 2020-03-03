package com.github.pwittchen.neurosky.app;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import com.github.pwittchen.neurosky.library.NeuroSky;
import com.github.pwittchen.neurosky.library.exception.BluetoothNotEnabledException;
import com.github.pwittchen.neurosky.library.listener.ExtendedDeviceMessageListener;
import com.github.pwittchen.neurosky.library.message.enums.BrainWave;
import com.github.pwittchen.neurosky.library.message.enums.Signal;
import com.github.pwittchen.neurosky.library.message.enums.State;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import android.media.MediaPlayer;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private final static String LOG_TAG = "NeuroSky";
    private NeuroSky neuroSky;
    private ImageButton forwardbtn, backwardbtn, pausebtn, playbtn;
    private MediaPlayer mPlayer;
    private TextView songName, startTime, songTime;
    private SeekBar songPrgs;
    private static int oTime = 0, sTime = 0, eTime = 0, fTime = 5000, bTime = 5000;
    private Handler hdlr = new Handler();

    @BindView(R.id.tv_state)
    TextView tvState;
    @BindView(R.id.tv_attention)
    TextView tvAttention;
    @BindView(R.id.tv_meditation)
    TextView tvMeditation;
    @BindView(R.id.tv_blink)
    TextView tvBlink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        neuroSky = createNeuroSky();
        Button playbutton = (Button) findViewById(R.id.buttonPlay);

        playbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AudioPlayer.class);
                // start the activity connect to the specified class
                startActivity(intent);
            }
        });

        backwardbtn = (ImageButton) findViewById(R.id.btnBackward);
        forwardbtn = (ImageButton) findViewById(R.id.btnForward);
        playbtn = (ImageButton) findViewById(R.id.btnPlay);
        pausebtn = (ImageButton) findViewById(R.id.btnPause);
        songName = (TextView) findViewById(R.id.txtSname);
        startTime = (TextView) findViewById(R.id.txtStartTime);
        songTime = (TextView) findViewById(R.id.txtSongTime);
        songName.setText("Song");
        mPlayer = MediaPlayer.create(this, R.raw.asdf);
        songPrgs = (SeekBar) findViewById(R.id.sBar);
        songPrgs.setClickable(false);
        pausebtn.setEnabled(false);

        playbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playMusic();
            }
        });
        pausebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseMusic();
            }
        });
        forwardbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((sTime + fTime) <= eTime) {
                    sTime = sTime + fTime;
                    mPlayer.seekTo(sTime);
                } else {
                    Toast.makeText(getApplicationContext(), "Cannot jump forward 5 seconds", Toast.LENGTH_SHORT).show();
                }
                if (!playbtn.isEnabled()) {
                    playbtn.setEnabled(true);
                }
            }
        });
        backwardbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((sTime - bTime) > 0) {
                    sTime = sTime - bTime;
                    mPlayer.seekTo(sTime);
                } else {
                    Toast.makeText(getApplicationContext(), "Cannot jump backward 5 seconds", Toast.LENGTH_SHORT).show();
                }
                if (!playbtn.isEnabled()) {
                    playbtn.setEnabled(true);
                }
            }
        });
    }

    private void pauseMusic() {
        mPlayer.pause();
        pausebtn.setEnabled(false);
        playbtn.setEnabled(true);
        Toast.makeText(getApplicationContext(), "Pausing Audio", Toast.LENGTH_SHORT).show();
    }

    private Runnable UpdateSongTime = new Runnable() {
        @Override
        public void run() {
            sTime = mPlayer.getCurrentPosition();
            startTime.setText(String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(sTime),
                    TimeUnit.MILLISECONDS.toSeconds(sTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(sTime))) );
            songPrgs.setProgress(sTime);
            hdlr.postDelayed(this, 100);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (neuroSky != null && neuroSky.isConnected()) {
            neuroSky.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (neuroSky != null && neuroSky.isConnected()) {
            neuroSky.stop();
        }
    }

    @NonNull
    private NeuroSky createNeuroSky() {
        return new NeuroSky(new ExtendedDeviceMessageListener() {
            @Override
            public void onStateChange(State state) {
                handleStateChange(state);
            }

            @Override
            public void onSignalChange(Signal signal) {
                handleSignalChange(signal);
            }

            @Override
            public void onBrainWavesChange(Set<BrainWave> brainWaves) {
                handleBrainWavesChange(brainWaves);
            }
        });
    }

    private void handleStateChange(final State state) {
        if (neuroSky != null && state.equals(State.CONNECTED)) {
            neuroSky.start();
        }

        tvState.setText(state.toString());
        Log.d(LOG_TAG, state.toString());
    }

    private void handleSignalChange(final Signal signal) {
        switch (signal) {
            case ATTENTION:
                tvAttention.setText(getFormattedMessage("attention: %d", signal));
                break;
            case MEDITATION:
                tvMeditation.setText(getFormattedMessage("meditation: %d", signal));
                break;
            case BLINK:
                Log.d(LOG_TAG, String.format("%s: %d", signal.toString(), signal.getValue()));
                hundleBlink(signal);
                tvBlink.setText(getFormattedMessage("blink: %d", signal));
                break;
        }

        //Log.d(LOG_TAG, String.format("%s: %d", signal.toString(), signal.getValue()));
    }

    private void playMusic() {
        Toast.makeText(MainActivity.this, "Playing Audio", Toast.LENGTH_SHORT).show();
        mPlayer.start();
        eTime = mPlayer.getDuration();
        sTime = mPlayer.getCurrentPosition();
        if (oTime == 0) {
            songPrgs.setMax(eTime);
            oTime = 1;
        }
        songTime.setText(String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(eTime),
                TimeUnit.MILLISECONDS.toSeconds(eTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(eTime))));
        startTime.setText(String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(sTime),
                TimeUnit.MILLISECONDS.toSeconds(sTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(sTime))));
        songPrgs.setProgress(sTime);
        hdlr.postDelayed(UpdateSongTime, 100);
        pausebtn.setEnabled(true);
        playbtn.setEnabled(false);
    }

    private int [] movingAvrgBlink = new int [4];
    private int oldestBlinkIndex = 0;
    Boolean recivedUserCommand = false;
    int lastCommnad = 0;
    private void hundleBlink(final Signal blink){
        Log.d(LOG_TAG, arrToString(movingAvrgBlink));
        movingAvrgBlink[oldestBlinkIndex % movingAvrgBlink.length] = blink.getValue();
        oldestBlinkIndex ++;

        if(recivedUserCommand){
            lastCommnad++;
            if(lastCommnad > 20)
                lastCommnad = 0;
                recivedUserCommand = false;
            return;
        }
        Log.d(LOG_TAG, String.format("%f avg", getAvrage(movingAvrgBlink)));
        if(getAvrage(movingAvrgBlink) > 70){
            recivedUserCommand = true;
            if(playbtn.isEnabled()) {
                Log.d(LOG_TAG, "Play");
                playMusic();
            }else{
                Log.d(LOG_TAG, "Pause");
                pauseMusic();

            }
        }
    }


    public String arrToString(int [] arr){
        String out = "";
        for(int i =0; i< arr.length; i++){
            out += " " + arr[i];
        }
        return out;
    }
    public double getAvrage(int[] arr){
        double sum = 0.0;
        for(int i =0; i< arr.length; i++){
            sum += arr[i];
        }
        return sum/arr.length;

    }

    private String getFormattedMessage(String messageFormat, Signal signal) {
        return String.format(Locale.getDefault(), messageFormat, signal.getValue());
    }

    private void handleBrainWavesChange(final Set<BrainWave> brainWaves) {
        for (BrainWave brainWave : brainWaves) {
           // Log.d(LOG_TAG, "Freq band start: \n");
           // Log.d(LOG_TAG, String.format("%s: %d", brainWave.toString(), brainWave.getValue()));
           // Log.d(LOG_TAG, "End: \n");
        }
    }

    @OnClick(R.id.btn_connect)
    void connect() {
        try {
            neuroSky.connect();
        } catch (BluetoothNotEnabledException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d(LOG_TAG, e.getMessage());
        }
    }

    @OnClick(R.id.btn_disconnect)
    void disconnect() {
        neuroSky.disconnect();
    }

    @OnClick(R.id.btn_start_monitoring)
    void startMonitoring() {
        neuroSky.start();
    }

    @OnClick(R.id.btn_stop_monitoring)
    void stopMonitoring() {
        neuroSky.stop();
    }


}
