package ru.shadowduel;

import java.util.Random;

/** Fixed-step combat model, independent of Android and rendering. */
public final class Combat {
    public static final int MENU=0, PLAY=1, PAUSE=2, END=3;
    public static final float FLOOR=490;
    public static final float INTRO_DURATION=3;
    public static final float ROLL_DURATION=1.30f;
    public static final float ROLL_DISTANCE=240;
    public final Fighter hero=new Fighter(), enemy=new Fighter();
    public final Input input=new Input();
    private final Pose transitionPose=new Pose();
    public int state=MENU, hits=0;
    public float time=75, elapsed=0, countdown=INTRO_DURATION, freeze=0, flash=0, hitX, hitY;
    public boolean blockedHit;
    public String result="";
    private final Random random=new Random(7);
    private float think=0, aiMove=0;
    private boolean aiBlock=false;
    public boolean botEnabled=true;
    public boolean training;
    public float bagAngle,bagVelocity;
    public int trainingDamage;
    public static final float BAG_X=860, BAG_PIVOT_Y=110, BAG_LENGTH=245;
    public void startTraining(){start();training=true;countdown=0;}
    public void menu(){state=MENU;training=false;input.clear();}
    public boolean soundEnabled=true;
    /** Optional authored forward roll. Null keeps the original procedural animation. */
    public AnimationClip forwardRollAnimation;
    public interface SoundListener { void play(int cue, float x); }
    public SoundListener soundListener;
    public static final int SWING=0, PUNCH=1, KICK=2, HURT=3, BLOCK=4, LAND=5;
    public void sound(int cue,float x){if(soundEnabled&&soundListener!=null)soundListener.play(cue,x);}

    public static final class Input {
        private int queuedAttack;
        private float attackBuffer;
        public float move,stickY,jumpDirection;
        public int roll;
        public boolean block, jump, punch, kick;
        public void clear(){move=stickY=jumpDirection=attackBuffer=0;queuedAttack=roll=0;block=jump=punch=kick=false;}
        void buffer(float dt){
            attackBuffer=Math.max(0,attackBuffer-dt);
            if(attackBuffer==0)queuedAttack=0;
            if(punch||kick){queuedAttack=punch?1:2;attackBuffer=.18f;}
            punch=kick=false;
        }
    }
    public static final class Fighter {
        public float walkSigned,recoveryTime;
        public AnimationClip.PoseData recoveryFrom;
        public float x,y,vy,hp=100,attackTime,stun,walk,motion,hurt,landing,vx,jumpPrep,airTime,takeoffVx;
        public float frontFoot,backFoot,frontLift,backLift,stepTime,stepStart,stepTarget,stepDuration=.18f,stepLift,stepPulse;
        public float rollTime,rollCooldown,rollOriginX;
        public final float[] rollStart=new float[22];
        public int rollDirection;
        public boolean rolling;
        public AnimationClip rollAnimation;
        public final AnimationClip[] animations=new AnimationClip[3];
        public int pendingFoot=-1,gaitDirection;
        public float weightDrop,weightSpeed,weightShift,shiftSpeed,bodyTurn,turnSpeed;
        public int stepping=-1,nextFoot,feetFacing=1;
        public int facing=1,attack=0;
        public boolean hit,block;
        void reset(float at,int direction){x=at;y=vy=attackTime=stun=walk=motion=hurt=landing=vx=jumpPrep=airTime=takeoffVx=0;hp=100;walkSigned=recoveryTime=0;recoveryFrom=null;rollTime=rollCooldown=0;rolling=false;rollAnimation=null;rollDirection=0;facing=direction;attack=0;hit=block=false;plantFeet();}
        public void plantFeet(){frontFoot=x+facing*40;backFoot=x-facing*42;frontLift=backLift=stepTime=stepPulse=0;weightDrop=weightSpeed=weightShift=shiftSpeed=bodyTurn=turnSpeed=0;stepping=pendingFoot=-1;nextFoot=0;gaitDirection=0;feetFacing=facing;}
    }
    public Combat(){hero.reset(420,1);enemy.reset(860,-1);}
    public void start(){training=false;bagAngle=bagVelocity=0;trainingDamage=0;hero.reset(420,1);enemy.reset(860,-1);time=75;countdown=INTRO_DURATION;elapsed=freeze=flash=think=0;aiMove=0;aiBlock=false;hits=0;result="";input.clear();state=PLAY;}
    public void pause(){if(state==PLAY){state=PAUSE;input.clear();}}
    public void resume(){if(state==PAUSE){state=PLAY;input.clear();}}
    public void attack(Fighter f,int kind){
        if(state!=PLAY||countdown>0||f.attack!=0||f.stun>0||f.hp<=0||f.block||f.rolling||f.jumpPrep>0||f.landing>.12f)return;
        f.attack=kind;f.attackTime=0;f.hit=false;f.recoveryTime=0;
        sound(SWING,f.x);
    }
    public void tick(float dt){
        if(state!=PLAY)return;
        elapsed+=dt;flash=Math.max(0,flash-dt);
        if(countdown>0){countdown=Math.max(0,countdown-dt);if(countdown<.00001f)countdown=0;input.jump=input.punch=input.kick=false;input.roll=0;input.jumpDirection=0;return;}
        input.buffer(dt);
        if(freeze>0){freeze=Math.max(0,freeze-dt);return;}
        if(!training)time=Math.max(0,time-dt);
        if(!hero.rolling)hero.facing=hero.x<=enemy.x?1:-1;
        if(!enemy.rolling)enemy.facing=enemy.x>=hero.x?-1:1;
        hero.block=input.block&&hero.attack==0&&hero.y==0&&hero.stun<=0&&hero.jumpPrep==0&&!hero.rolling;
        if(input.roll!=0)roll(hero,input.roll);
        else if(input.jump)jump(hero,input.jumpDirection!=0?input.jumpDirection:input.move);
        if(input.queuedAttack!=0&&hero.attack==0){
            attack(hero,input.queuedAttack);
            if(hero.attack!=0){input.queuedAttack=0;input.attackBuffer=0;}
        }
        input.jump=input.punch=input.kick=false;input.roll=0;input.jumpDirection=0;
        if(botEnabled&&!training){
            think-=dt;
            if(think<=0){
                think=.22f+random.nextFloat()*.28f;
                float distance=Math.abs(hero.x-enemy.x);
                aiMove=distance>125?enemy.facing:(distance<75?-enemy.facing*.5f:0);
                aiBlock=distance<175&&random.nextFloat()<.24f;
                enemy.block=aiBlock&&enemy.attack==0&&enemy.stun<=0;
                if(distance<160&&!aiBlock&&random.nextFloat()<.78f)attack(enemy,distance>112?2:(random.nextBoolean()?1:2));
                if(distance>170&&random.nextFloat()<.06f)jump(enemy,aiMove);
            }
            enemy.block=aiBlock&&enemy.attack==0&&enemy.y==0&&enemy.stun<=0&&enemy.jumpPrep==0;
        }
        move(hero,input.move,dt);
        if(training){
            bagVelocity+=(-9*bagAngle-2*bagVelocity)*dt;
            bagAngle=clamp(bagAngle+bagVelocity*dt,-.35f,.35f);
            enemy.x=BAG_X+(float)Math.sin(bagAngle)*BAG_LENGTH;
            // The suspended target is immovable by body contact and has no health pool.
            float distance=hero.x-enemy.x;
            if(Math.abs(distance)<66){hero.x=clamp(enemy.x+(distance<=0?-66:66),65,1215);hero.vx=0;}
            int hit=contact(hero,enemy);
            if(hit!=0){
                int kind=Math.abs(hit);trainingDamage+=kind==1?8:14;hits++;
                bagVelocity=clamp(bagVelocity+hero.facing*(kind==1?.65f:1.1f),-1.8f,1.8f);
                hitX=enemy.x-hero.facing*30;hitY=FLOOR-hero.y-132;
                blockedHit=false;flash=.2f;freeze=.04f;sound(kind==1?PUNCH:KICK,enemy.x);
            }
            return;
        }
        move(enemy,botEnabled?aiMove:0,dt);
        separate();
        // Snapshot both contacts before damage can interrupt either attack.
        int heroContact=contact(hero,enemy),enemyContact=contact(enemy,hero);
        resolve(hero,enemy,heroContact);resolve(enemy,hero,enemyContact);
        separate();
        if(hero.hp<=0||enemy.hp<=0||time<=0){
            result=hero.hp==enemy.hp?"НИЧЬЯ":hero.hp>enemy.hp?"ПОБЕДА":"ПОРАЖЕНИЕ";
            state=END;input.clear();
        }
    }
    private void separate(){
        if(Math.abs(hero.y-enemy.y)>=100)return;
        Fighter left=hero.x<=enemy.x?hero:enemy,right=left==hero?enemy:hero;
        float overlap=66-(right.x-left.x);
        if(overlap<=0)return;
        float leftShift=Math.min(overlap/2,left.x-65);
        float rightShift=Math.min(overlap-leftShift,1215-right.x);
        leftShift=Math.min(overlap-rightShift,left.x-65);
        left.x-=leftShift;right.x+=rightShift;
    }
    private void jump(Fighter f,float direction){
        if(f.y==0&&f.jumpPrep==0&&f.landing<=.05f&&f.stun<=0&&f.attack==0&&!f.block&&!f.rolling){
            f.jumpPrep=.12f;f.airTime=0;
            f.takeoffVx=Math.abs(direction)>.22f?Math.signum(direction)*245:f.vx;
            f.frontLift=f.backLift=0;f.stepping=-1;
        }
    }
    private void roll(Fighter f,int direction){
        if(f.y>0||f.jumpPrep>0||f.landing>.05f||f.stun>0||f.attack!=0||f.block||f.rolling||f.rollCooldown>0)return;
        transitionPose.sample(f,elapsed);System.arraycopy(transitionPose.joints,0,f.rollStart,0,22);
        f.rolling=true;f.rollOriginX=f.x;f.rollTime=0;f.rollDirection=direction<0?-1:1;f.vx=0;
        f.rollAnimation=f.rollDirection*f.facing>0?forwardRollAnimation:null;
        f.plantFeet();
    }
    public static float smooth(float t){t=clamp(t,0,1);return t*t*(3-2*t);}
    public static float rollDuration(Fighter f){return f.rollAnimation==null?ROLL_DURATION:f.rollAnimation.duration/60f;}
    public static boolean rollEvades(Fighter f){
        if(!f.rolling)return false;
        if(f.rollAnimation!=null)return f.rollTime*60>=f.rollAnimation.startupEnd&&f.rollTime*60<f.rollAnimation.activeEnd;
        return f.rollTime>=ROLL_DURATION*.20f&&f.rollTime<=ROLL_DURATION*.74f;
    }
    private void move(Fighter f,float direction,float dt){
        f.rollCooldown=Math.max(0,f.rollCooldown-dt);
        if(f.rolling){
            float before=RollAnimation.travel(f,f.rollTime);
            float duration=rollDuration(f);f.rollTime=Math.min(duration,f.rollTime+dt*(f.rollAnimation==null?1:(float)f.rollAnimation.playbackSpeed));
            float shift=(RollAnimation.travel(f,f.rollTime)-before)*f.rollDirection;
            f.x=clamp(f.x+shift,65,1215);f.vx=0;f.motion=0;
            if(f.rollTime>=duration-.00001f){f.rolling=false;f.rollCooldown=.28f;f.plantFeet();sound(LAND,f.x);}
            return;
        }
        f.stun=Math.max(0,f.stun-dt);
        f.recoveryTime=Math.max(0,f.recoveryTime-dt);f.hurt=Math.max(0,f.hurt-dt);f.landing=Math.max(0,f.landing-dt);
        boolean air=f.y>0||f.vy>0;
        float target=direction*walkSpeed(f)*(direction*f.facing<0?104f/138:1)*(f.block?.4f:1)*animationSpeed(f,0);
        if(f.stun>0||f.attack!=0||f.jumpPrep>0)target=0;
        if(f.landing>.12f)target*=.3f;
        // Ground acceleration and modest aerial steering preserve momentum.
        if(!air)f.vx=approach(f.vx,target,dt*(target==0?1400:1100));
        else if(direction!=0&&f.stun<=0)f.vx=approach(f.vx,direction*245,dt*110);
        float previous=f.x;f.x=clamp(f.x+f.vx*dt,65,1215);
        if(f.x==65||f.x==1215)f.vx=0;
        f.motion+=(f.vx/(150*animationSpeed(f,0))-f.motion)*Math.min(1,dt*14);
        f.walk+=Math.abs(f.x-previous)*.09f;f.walkSigned+=(f.x-previous)*f.facing;
        if(f.jumpPrep>0){f.jumpPrep=Math.max(0,f.jumpPrep-dt);if(f.jumpPrep==0){f.vy=530;f.vx=f.takeoffVx;f.airTime=0;}}
        if(f.y>0||f.vy>0){f.airTime+=dt;f.y+=f.vy*dt-.5f*1450*dt*dt;f.vy-=1450*dt;if(f.y<=0){f.y=f.vy=0;f.landing=.24f;f.plantFeet();sound(LAND,f.x);}}
        if(f.y==0&&f.jumpPrep==0&&f.attack==0&&f.stun==0)Gait.update(f,dt);
        if(f.attack!=0){f.attackTime+=dt*animationSpeed(f,f.attack);if(f.attackTime>(f.attack==1?.4f:.68f)){AnimationClip end=f.animations[f.attack];if(end!=null&&end.version==3){f.recoveryFrom=AnimationRuntime.sampleAnimation(end,end.duration);f.recoveryTime=.18f;}f.attack=0;f.attackTime=0;}}
    }
    public static float walkSpeed(Fighter f){AnimationClip c=f.animations[0];return c!=null&&c.walkDistance>0?(float)(c.walkDistance*60/c.duration):138;}
    public static float animationSpeed(Fighter f,int slot){return f.animations[slot]==null?1:(float)f.animations[slot].playbackSpeed;}
    private int contact(Fighter a,Fighter b){
        if(a.attack==0||a.hit||a.hp<=0)return 0;
        float active=a.attack==1?.12f:.24f;
        if(a.attackTime<active)return 0;
        a.hit=true;
        float distance=(b.x-a.x)*a.facing;
        if(rollEvades(b))return 0;
        if(distance<0||distance>(a.attack==1?119:165)||Math.abs(a.y-b.y)>78)return 0;
        return b.block&&b.facing==-a.facing?-a.attack:a.attack;
    }
    private void resolve(Fighter a,Fighter b,int contact){
        if(contact==0)return;
        boolean guard=contact<0;
        int kind=Math.abs(contact);
        b.hp=Math.max(0,b.hp-(guard?2:(kind==1?8:14)));
        b.stun=guard?.09f:.22f;
        if(!guard){b.attack=0;b.attackTime=0;b.block=false;b.hurt=.34f;b.jumpPrep=0;if(b.rolling){b.rolling=false;b.rollCooldown=.28f;b.plantFeet();}}
        sound(guard?BLOCK:(kind==1?PUNCH:KICK),b.x);
        if(!guard)sound(HURT,b.x);
        b.x=clamp(b.x+a.facing*(guard?6:18),65,1215);
        hitX=b.x-a.facing*23;hitY=FLOOR-b.y-132;blockedHit=guard;flash=.2f;freeze=Math.max(freeze,guard?.025f:.055f);hits++;
    }
    public static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
    private static float approach(float value,float target,float amount){return value<target?Math.min(target,value+amount):Math.max(target,value-amount);}
}
