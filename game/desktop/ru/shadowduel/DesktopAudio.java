package ru.shadowduel;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public final class DesktopAudio implements Combat.SoundListener {
    final Combat game;final Clip[][] clips=new Clip[6][2];final int[] next=new int[6];
    Clip ambience;boolean active;
    DesktopAudio(Combat model){
        game=model;
        try{
            String[] names={"swing","punch","kick","hurt","block","land"};
            for(int i=0;i<6;i++)for(int j=0;j<2;j++)clips[i][j]=load(names[i],i==Combat.HURT?.6f:.85f);
            ambience=load("battle_music",.32f);
        }catch(Exception e){System.err.println("Audio output unavailable: "+e.getMessage());close();}
        game.soundListener=this;
    }
    private Clip load(String name,float gain)throws Exception{
        InputStream raw=getClass().getResourceAsStream("/audio/"+name+".wav");
        if(raw==null)throw new java.io.IOException("Missing sound "+name);
        try(AudioInputStream input=AudioSystem.getAudioInputStream(new BufferedInputStream(raw))){
            Clip clip=AudioSystem.getClip();clip.open(input);
            if(clip.isControlSupported(FloatControl.Type.MASTER_GAIN))((FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN)).setValue((float)(20*Math.log10(gain)));
            return clip;
        }
    }
    void sync(boolean foreground){
        boolean wanted=foreground&&game.state==Combat.PLAY&&game.soundEnabled;
        if(wanted==active)return;active=wanted;
        if(active){if(ambience!=null)ambience.loop(Clip.LOOP_CONTINUOUSLY);}
        else {if(ambience!=null)ambience.stop();for(Clip[] row:clips)for(Clip clip:row)if(clip!=null)clip.stop();}
    }
    public void play(int cue,float x){if(!active||!game.soundEnabled)return;Clip clip=clips[cue][next[cue]++%2];if(clip!=null){clip.stop();clip.setFramePosition(0);clip.start();}}
    void close(){if(ambience!=null){ambience.close();ambience=null;}for(int i=0;i<6;i++)for(int j=0;j<2;j++)if(clips[i][j]!=null){clips[i][j].close();clips[i][j]=null;}}
}
