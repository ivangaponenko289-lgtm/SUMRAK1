package ru.shadowduel;

/** Paired combat shuffle. Lead step and shorter trailing step share one beat;
 * support stays fixed in world space, and torso motion uses the same phase. */
public final class Gait {
    public static void update(Combat.Fighter f,float dt){
        if(f.feetFacing!=f.facing||Math.abs(f.frontFoot-f.x)>125||Math.abs(f.backFoot-f.x)>125)f.plantFeet();
        float front=f.x+f.facing*40,back=f.x-f.facing*42;
        boolean moving=Math.abs(f.vx)>10;
        int direction=f.vx*f.facing>=0?1:-1;
        if(moving&&direction!=f.gaitDirection){f.pendingFoot=-1;f.gaitDirection=direction;}
        if(f.stepping<0){
            if(moving){
                boolean follow=f.pendingFoot>=0;
                int foot=follow?f.pendingFoot:(direction>0?0:1);
                float duration=(follow?10f/60:14f/60)/Combat.animationSpeed(f,0);
                begin(f,foot,(foot==0?front:back)+f.vx*duration,duration,follow?2.5f:5);
                f.pendingFoot=follow?-1:1-foot;
            }else{
                f.pendingFoot=-1;
                float a=Math.abs(f.frontFoot-front),b=Math.abs(f.backFoot-back);
                if(Math.max(a,b)>10)begin(f,a>b?0:1,a>b?front:back,.18f,2.5f);
            }
        }
        if(f.stepping>=0){
            f.stepTime=Math.min(f.stepDuration,f.stepTime+dt);
            if(f.stepDuration-f.stepTime<.000001f)f.stepTime=f.stepDuration;
            float u=f.stepTime/f.stepDuration;
            float at=f.stepStart+(f.stepTarget-f.stepStart)*(u*u*u*(u*(u*6-15)+10));
            f.stepPulse=(float)Math.sin(Math.PI*u);
            float lift=f.stepPulse*f.stepPulse*f.stepLift;
            if(f.stepping==0){f.frontFoot=at;f.frontLift=lift;}else{f.backFoot=at;f.backLift=lift;}
            if(u>=1){f.stepping=-1;f.frontLift=f.backLift=f.stepPulse=0;}
        }else f.stepPulse=0;
        // One weight transfer per paired step, with spring-damped absorption.
        float speed=Combat.clamp(Math.abs(f.vx)/(138*Combat.animationSpeed(f,0)),0,1);
        float load=0;
        if(moving){
            // Lead-foot landing loads the knees; the trailing foot completes the push.
            boolean lead=f.pendingFoot>=0;
            float phase=f.stepping<0?1:f.stepTime/f.stepDuration;
            load=lead?3+phase:4-phase;
            load*=speed;
        }
        f.weightSpeed+=(210*(load-f.weightDrop)-27*f.weightSpeed)*dt;
        f.weightDrop+=f.weightSpeed*dt;
        float center=((f.frontFoot+f.backFoot)*.5f-f.x)*f.facing+1;
        float shift=Combat.clamp(center*.28f,-10,10);
        f.shiftSpeed+=(230*(shift-f.weightShift)-29*f.shiftSpeed)*dt;
        f.weightShift+=f.shiftSpeed*dt;
        float turn=(f.stepping==0?1:f.stepping==1?-1:0)*f.stepPulse*speed*2.8f;
        f.turnSpeed+=(220*(turn-f.bodyTurn)-27*f.turnSpeed)*dt;
        f.bodyTurn+=f.turnSpeed*dt;
    }
    private static void begin(Combat.Fighter f,int foot,float target,float duration,float lift){
        // Bound the landing against the planted foot, including direction reversals.
        float support=foot==0?f.backFoot:f.frontFoot;
        float separation=(target-support)*f.facing*(foot==0?1:-1);
        target=support+f.facing*(foot==0?1:-1)*Combat.clamp(separation,65,124);
        f.stepping=foot;f.stepTime=0;f.stepStart=foot==0?f.frontFoot:f.backFoot;
        f.stepTarget=target;f.stepDuration=duration;f.stepLift=lift;
    }
}
