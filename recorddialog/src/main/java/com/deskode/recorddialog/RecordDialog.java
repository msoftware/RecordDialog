package com.deskode.recorddialog;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import omrecorder.AudioChunk;
import omrecorder.AudioRecordConfig;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.PullableSource;
import omrecorder.Recorder;

public class RecordDialog extends DialogFragment {
    private String _strTitle;
    private String _strPositiveButtonText;
    private FloatingActionButton _recordButton;
    private String STATE_BUTTON = "INIT";
    private String _AudioSavePathInDevice = null;
    private ClickListener _clickListener;
    Recorder recorder;
    MediaPlayer mediaPlayer;

    public RecordDialog() {

    }

    public static RecordDialog newInstance(String title) {
        RecordDialog frag = new RecordDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams
                .SOFT_INPUT_STATE_HIDDEN);
        setupRecorder();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Getting the layout inflater to inflate the view in an alert dialog.
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View rootView = inflater.inflate(R.layout.record_dialog, null);
        _recordButton = rootView.findViewById(R.id.btnRecord);
        _recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scaleAnimation();
                switch (STATE_BUTTON) {
                    case "INIT":
                        _recordButton.setImageResource(R.drawable.ic_stop);
                        STATE_BUTTON = "RECORD";
                        try {
                            recorder.startRecording();
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "RECORD":
                        try {
                            recorder.stopRecording();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        _recordButton.setImageResource(R.drawable.ic_play);
                        STATE_BUTTON = "STOP";
                        break;
                    case "STOP":
                        startMediaPlayer();
                        break;
                    case "PLAY":
                        pauseMediaPlayer();
                        break;
                    case "PAUSE":
                        resumeMediaPlayer();
                        break;
                }
            }
        });

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setView(rootView);

        String strPositiveButton = _strPositiveButtonText == null ? "CLOSE" : _strPositiveButtonText;
        alertDialog.setPositiveButton(strPositiveButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    recorder.stopRecording();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                _clickListener.OnClickListener(_AudioSavePathInDevice);
            }
        });

        String strTitle = _strTitle == null ? "Grabar audio" : _strTitle;
        alertDialog.setTitle(strTitle);

        final AlertDialog dialog = alertDialog.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams
                .SOFT_INPUT_STATE_HIDDEN);

        return dialog;
    }

    // Change End

    public void setTitle(String strTitle) {
        _strTitle = strTitle;
    }

    public void setPositiveButton(String strPositiveButtonText, ClickListener onClickListener) {
        _strPositiveButtonText = strPositiveButtonText;
        _clickListener = onClickListener;
    }

    private void setupRecorder() {
        recorder = OmRecorder.wav(
                new PullTransport.Default(mic(), new PullTransport.OnAudioChunkPulledListener() {
                    @Override
                    public void onAudioChunkPulled(AudioChunk audioChunk) {
                    }
                }), file());
    }

    private PullableSource mic() {
        return new PullableSource.Default(
                new AudioRecordConfig.Default(
                        MediaRecorder.AudioSource.MIC, AudioFormat.ENCODING_PCM_16BIT,
                        AudioFormat.CHANNEL_IN_MONO, 44100
                )
        );
    }

    @NonNull
    private File file() {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), timeStamp + ".wav");
        _AudioSavePathInDevice = file.getPath();
        return file;
    }

    public String getAudioPath() {
        return _AudioSavePathInDevice;
    }

    private void startMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(_AudioSavePathInDevice);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopMediaPlayer();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        _recordButton.setImageResource(R.drawable.ic_pause);
        STATE_BUTTON = "PLAY";
        mediaPlayer.start();
    }

    private void resumeMediaPlayer() {
        _recordButton.setImageResource(R.drawable.ic_pause);
        STATE_BUTTON = "PLAY";
        mediaPlayer.start();
    }

    private void pauseMediaPlayer() {
        _recordButton.setImageResource(R.drawable.ic_play);
        STATE_BUTTON = "PAUSE";
        mediaPlayer.pause();
    }

    private void stopMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
            _recordButton.setImageResource(R.drawable.ic_play);
            STATE_BUTTON = "STOP";
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void scaleAnimation() {
        final Interpolator interpolador = AnimationUtils.loadInterpolator(getContext(),
                android.R.interpolator.fast_out_slow_in);
        _recordButton.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setInterpolator(interpolador)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        _recordButton.animate().scaleX(1f).scaleY(1f).start();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
    }

    @Override
    public void onPause() {
        super.onPause();
        dismiss();
    }

    public interface ClickListener
    {
        void OnClickListener(String path);
    }
}
