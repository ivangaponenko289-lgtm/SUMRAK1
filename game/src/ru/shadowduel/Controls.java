package ru.shadowduel;

import java.util.HashMap;
import java.util.Map;

/** Pointer IDs are tracked independently so movement and attacks can overlap. */
public final class Controls {
    public static final float STICK_X=130,STICK_Y=620,STICK_RADIUS=80,STICK_TRAVEL=48;
    private final Combat game;
    private final Map<Integer,Integer> roles=new HashMap<>();
    private int stick=-1;
    private boolean jumpLatched,rollLatched;
    public Controls(Combat game){this.game=game;}
    public void down(int id,float x,float y){
        if(x>=520&&x<=575&&y>=40&&y<=100){game.soundEnabled=!game.soundEnabled;return;}
        if(game.state!=Combat.PLAY){
            if(x>=466&&x<=814&&y>=454&&y<=510){clear();if(game.state==Combat.MENU)game.startTraining();else game.menu();return;}
            if(x>=466&&x<=814&&y>=374&&y<=444){clear();if(game.state==Combat.PAUSE)game.resume();else game.start();}
            return;
        }
        if(x>=700&&x<=760&&y>=40&&y<=100){clear();game.pause();return;}
        if(near(x,y,STICK_X,STICK_Y,STICK_RADIUS)&&stick==-1){stick=id;jumpLatched=rollLatched=false;roles.put(id,1);move(id,x,y);}
        else if(near(x,y,944,620,56)){roles.put(id,3);game.input.block=true;}
        else if(near(x,y,1066,625,62)){roles.put(id,5);game.input.kick=true;}
        else if(near(x,y,1182,562,66)){roles.put(id,4);game.input.punch=true;}
    }
    public void move(int id,float x,float y){if(id==stick){
        float horizontal=(x-STICK_X)/STICK_TRAVEL,vertical=(y-STICK_Y)/STICK_TRAVEL;
        float length=(float)Math.sqrt(horizontal*horizontal+vertical*vertical);
        float radius=Math.max(1,length);
        horizontal/=radius;vertical/=radius;
        game.input.move=length<.18f||Math.abs(horizontal)<.18f?0:horizontal;
        game.input.stickY=length<.18f?0:vertical;
        // Hysteresis: one jump per upward gesture, rearmed on return toward center.
        if(vertical<=-.65f&&!jumpLatched){game.input.jump=true;game.input.jumpDirection=game.input.move;jumpLatched=true;}
        else if(vertical>-.3f)jumpLatched=false;
        if(vertical>=.55f&&Math.abs(horizontal)>=.35f&&!rollLatched){
            game.input.roll=horizontal<0?-1:1;rollLatched=true;
        }else if(vertical<.3f)rollLatched=false;
    }}
    public void up(int id){roles.remove(id);if(id==stick){stick=-1;jumpLatched=rollLatched=false;game.input.move=game.input.stickY=0;}game.input.block=roles.containsValue(3);}
    public void clear(){roles.clear();stick=-1;jumpLatched=rollLatched=false;game.input.clear();}
    private boolean near(float x,float y,float cx,float cy,float r){return (x-cx)*(x-cx)+(y-cy)*(y-cy)<=r*r;}
}
