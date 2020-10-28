package com.example.reto;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_CODE_SPEECH_INPUT=100;
    private Button btnHablar;
    private MediaRecorder grabacion;
    private String audio=null;
    private CountDownTimer countDownTimer;
    private long timeLeftMilliseconds=10000;
    private boolean timerRunning;
    TTSManager ttsManager=null;
    String mCurrentPhotoPath;
    static final int REQUEST_TAKE_PHOTO=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat .requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA}, 1000);
        }

        btnHablar=findViewById(R.id.btnRec);
        btnHablar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRec();
            }
        });
        ttsManager=new TTSManager();
        ttsManager.init(this);
    }

    private void startRec() {
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Hola diga una acción a realizar");
        try{
            startActivityForResult(intent,REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQ_CODE_SPEECH_INPUT:{
                if(resultCode==RESULT_OK && null!=data){
                    ArrayList<String> intencion=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    switch (intencion.get(0)){
                        case "grabar":
                            startStop();
                            break;
                        case "reproducir":
                            Play();
                            break;
                        case "cámara":
                            takePhoto();
                            break;
                        case "instrucciones":
                            Toast.makeText(getApplicationContext(), "Dictando instrucciones",Toast.LENGTH_SHORT).show();
                            ttsManager.initQueue("Esta aplicación opera bajo los mandatos de comandos de voz establecidos, " +
                                    "ahora se procederá a mencionar las instrucciones de los comandos de voz: " +
                                    "el primer comando, " +
                                    "Grabar, permite que la aplicación comience un proceso de grabación de audio de 10 segundos y creación de un archivo de audio, " +
                                    "el segundo comando de voz, " +
                                    "Reproducir, este comando procede a reproducir un archivo de audio creado por esta aplicación en cuyo caso de no encontrarlo se le informará al usuario que este no ha sido creado, " +
                                    "el tercer comando de voz, " +
                                    "Cámara, este comando hace que la aplicación inicie la cámara del dispositivo permitiendo al usuario tomar fotografías o vídeos");
                            break;
                        default:
                            Toast.makeText(getApplicationContext(), "Comando de voz inválido",Toast.LENGTH_SHORT).show();
                            ttsManager.initQueue("Porfavor ingresar un comando de voz válido, como: Grabar, Reproducir o Cámara");
                            break;
                    }
                }
                break;
            }
        }
    }

    private void takePhoto() {
        Intent takePictureIntent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePictureIntent.resolveActivity(getPackageManager())!=null){
            File photoFile=null;
            try{
                photoFile=createImageFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(photoFile!=null){
                Uri photoURI=FileProvider.getUriForFile(this,"com.example.android.fileprovider",photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoURI);
                startActivityForResult(takePictureIntent,REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void Play() {
        File audioFile=new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Grabacion.mp3");
        if(audioFile.exists()){
            MediaPlayer mediaPlayer=new MediaPlayer();
            try {
                mediaPlayer.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Grabacion.mp3");
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                mediaPlayer.prepare();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer.start();
            grabacion=null;
            Toast.makeText(getApplicationContext(), "Reproduciendo audio",Toast.LENGTH_SHORT).show();
        } else {
            ttsManager.initQueue("No se ha encontrado un archivo de audio");
        }
    }

    private void startStop() {
        if(timerRunning){
            stopTimer();
        } else {
            grabacion=null;
            audio=Environment.getExternalStorageDirectory().getAbsolutePath() + "/Grabacion.mp3";
            grabacion=new MediaRecorder();
            grabacion.setAudioSource(MediaRecorder.AudioSource.MIC);
            grabacion.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            grabacion.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
            grabacion.setOutputFile(audio);
            startTimer();
        }
    }

    private void stopTimer() {
        countDownTimer.cancel();
        timerRunning=false;
    }

    private void startTimer() {
        countDownTimer=new CountDownTimer(timeLeftMilliseconds,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Recorder();
            }
            @Override
            public void onFinish() {
                Stop();
            }
        }.start();
        timerRunning=true;
    }

    private void Stop() {
        grabacion.stop();
        grabacion.release();
        Toast.makeText(getApplicationContext(), "Grabación finalizada", Toast.LENGTH_SHORT).show();
    }

    private void Recorder() {
        try {
            grabacion.prepare();
            grabacion.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(getApplicationContext(), "Grabando...", Toast.LENGTH_SHORT).show();
    }

    private File createImageFile() throws IOException {
        String timeStamp=new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName="Backup_"+timeStamp+"_";
        File storageDir=getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image=File.createTempFile(imageFileName,".jpg",storageDir);

        mCurrentPhotoPath=image.getAbsolutePath();
        return image;
    }
}