package ru.shadowduel;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/** Low-latency overlapping effects plus a quieter looping battlefield bed. */
public final class AndroidAudio implements Combat.SoundListener {
    private final Combat game;
    private final SoundPool pool;
    private final AudioManager manager;
    private final int[] ids=new int[6];
    private final Set<Integer> loaded=new HashSet<>();
    private final AudioManager.OnAudioFocusChangeListener focus;
    private MediaPlayer ambience;
    private boolean active,hasFocus,released;
    private boolean requested;
    public AndroidAudio(Context context,Combat model){
        game=model;manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        pool=new SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attributes).build();
        pool.setOnLoadCompleteListener((p,id,status)->{if(status==0)loaded.add(id);});
        String[] names={"swing","punch","kick","hurt","block","land"};
        for(int i=0;i<names.length;i++)try(AssetFileDescriptor fd=context.getAssets().openFd("audio/"+names[i]+".wav")){ids[i]=pool.load(fd,1);}catch(IOException e){Log.w("Sumrak","Effect unavailable: "+names[i],e);}
        try(AssetFileDescriptor fd=context.getAssets().openFd("audio/battle_music.mp3")){
            ambience=new MediaPlayer();ambience.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            ambience.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());
            ambience.setLooping(true);ambience.setVolume(.32f,.32f);ambience.prepare();
        }catch(IOException e){Log.w("Sumrak","Ambience unavailable",e);if(ambience!=null){ambience.release();ambience=null;}}
        focus=change->{if(change!=AudioManager.AUDIOFOCUS_GAIN){game.pause();game.input.clear();suspend();}};
        game.soundListener=this;
    }
    public void sync(boolean foreground){
        if(released)return;
        boolean wanted=foreground&&game.state==Combat.PLAY&&game.soundEnabled;
        if(!wanted){suspend();return;}
        if(!requested){requested=true;hasFocus=manager.requestAudioFocus(focus,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;}
        if(hasFocus&&!active){active=true;if(ambience!=null)ambience.start();}
    }
    public void play(int cue,float x){
        if(released||!active||!game.soundEnabled||!loaded.contains(ids[cue]))return;
        float pan=Combat.clamp(x/1280f,0,1),volume=cue==Combat.HURT?.6f:cue==Combat.SWING?.65f:.88f;
        pool.play(ids[cue],volume*(1-.25f*pan),volume*(.75f+.25f*pan),1,0,.97f+(float)Math.random()*.06f);
    }
    public void suspend(){
        if(released)return;
        active=false;pool.autoPause();
        if(ambience!=null&&ambience.isPlaying())ambience.pause();
        if(hasFocus)manager.abandonAudioFocus(focus);
        hasFocus=false;requested=false;
    }
    public void release(){if(released)return;suspend();released=true;game.soundListener=null;pool.release();if(ambience!=null)ambience.release();}
}
