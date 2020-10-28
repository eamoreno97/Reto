package com.example.reto;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class TTSManager {
    private TextToSpeech mTts=null;
    private boolean isLoaded=false;

    public void init(Context context){
        try{
            mTts=new TextToSpeech(context,onInitListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TextToSpeech.OnInitListener onInitListener=new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            Locale spanish=new Locale("es","ES");
            if(status==TextToSpeech.SUCCESS){
                int result=mTts.setLanguage(spanish);
                isLoaded=true;
                if(result==TextToSpeech.LANG_MISSING_DATA || result==TextToSpeech.LANG_NOT_SUPPORTED){
                    Log.e("error","Este no está permitido");
                }
            } else {
                Log.e("error","Falló al inicializar");
            }
        }
    };

    public void shutDown(){
        mTts.shutdown();
    }

    public void addQueue(String text){
        if(isLoaded){
            mTts.speak(text,TextToSpeech.QUEUE_ADD,null);
        } else {
            Log.e("error","TTS Not Initialized");
        }
    }

    public void initQueue(String text){
        if(isLoaded){
            mTts.speak(text,TextToSpeech.QUEUE_FLUSH,null);
        } else {
            Log.e("error","TTS Not Initialized");
        }
    }
}
