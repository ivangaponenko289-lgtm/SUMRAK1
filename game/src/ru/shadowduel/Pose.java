package ru.shadowduel;

/** Local joint positions: pelvis, chest, head, front elbow/hand, back elbow/hand,
 * front knee/foot, back knee/foot. Animation keys match the combat active frames. */
public final class Pose {
    // Intro gestures use planted feet and upper-body keys: wrist check for the
    // player, opening shoulders and raising fists for the opponent.
    private static final float[] RELAX={-8,-101,-3,-153,0,-181, 21,-121,28,-96, -22,-119,-16,-96};
    private static final float[] WRISTS={-7,-99,1,-151,7,-177, 34,-133,34,-157, 4,-135,30,-153};
    private static final float[] SALUTE={-8,-102,-2,-154,2,-182, 35,-143,53,-173, -22,-130,0,-154};
    private static final float[] OPEN={-10,-100,-8,-154,-6,-183, 34,-149,53,-124, -40,-146,-53,-122};
    private static final float[] FISTS={-8,-99,-1,-151,3,-180, 32,-133,35,-174, -27,-134,-6,-170};
    private static final float[] INTRO_TIMES={0,.35f,1.05f,1.65f,2.35f,3};
    // Match the photo's raised guard through rotations, retaining the v1 bone lengths.
    static final float[] READY=referenceReady();
    private static float[] referenceReady(){
        float[] j={-8,-104, -1,-156, 3,-184, 30,-133,54,-148, -22,-133,3,-157, 24,-49,40,-6, -24,-49,-42,-6};
        double front=Math.sqrt(801),upper=Math.sqrt(353),forearm=Math.sqrt(1201);
        j[8]=j[6]+(float)(front*Math.cos(Math.toRadians(-26)));
        j[9]=j[7]+(float)(front*Math.sin(Math.toRadians(-26)));
        j[10]=j[2]-13+(float)(upper*Math.cos(Math.toRadians(145)));
        j[11]=j[3]+6+(float)(upper*Math.sin(Math.toRadians(145)));
        j[12]=j[10]+(float)(forearm*Math.cos(Math.toRadians(-42)));
        j[13]=j[11]+(float)(forearm*Math.sin(Math.toRadians(-42)));
        return j;
    }
    private static final float[][] HERO_INTRO={RELAX,RELAX,WRISTS,SALUTE,READY,READY};
    private static final float[][] ENEMY_INTRO={RELAX,RELAX,OPEN,FISTS,READY,READY};
    static final float[] WIND={-13,-94, -15,-151,-12,-181, -24,-123,-16,-143, -10,-125,30,-153, 21,-50,37,-6,-28,-48,-38,-6};
    static final float[] JAB={5,-91, 24,-147,28,-177, 72,-140,119,-137, 1,-122,29,-145, 31,-48,43,-6,-24,-48,-38,-6};
    static final float[] CHAMBER={-13,-92,-19,-151,-16,-182, 11,-123,24,-153,-36,-124,-6,-151, 29,-115,11,-79,-24,-49,-25,-6};
    static final float[] EXTEND={22,-89,-5,-148,-7,-176, 17,-122,30,-151,-32,-130,-4,-155, 77,-110,124,-126,-12,-49,-15,-6};
    static final float[] GUARD={-10,-101,-7,-152,-7,-182, 34,-134,26,-180, 9,-136,20,-169,24,-49,40,-6,-24,-49,-42,-6};
    static final float[] RECOIL={-12,-88,-28,-139,-41,-167,9,-112,28,-123,-39,-112,-24,-125,18,-46,37,-6,-34,-44,-42,-6};
    static final float[] FALL={-23,-30,-65,-38,-96,-40,-42,-18,-10,-12,-78,-16,-107,-10,27,-19,67,-7,-1,-11,33,-7};
    public final float[] joints=new float[22];
    public float shoulderTurn,frontAngle,backAngle,rotation;
    // Secondary rig: lumbar joint, shoulder girdle and independent wrist rotation.
    public float spineBend,frontWrist,backWrist;
    public float authoredSocketWeight;
    public int authoredFacing=1;
    public AnimationClip.PoseData editedPose;
    public void sampleIntro(Combat.Fighter f,float clock,float remaining,boolean opponent){
        sample(f,clock);
        float t=Combat.clamp(Combat.INTRO_DURATION-remaining,0,Combat.INTRO_DURATION);
        float[][] poses=opponent?ENEMY_INTRO:HERO_INTRO;
        for(int k=1;k<INTRO_TIMES.length;k++)if(t<=INTRO_TIMES[k]){
            float u=Combat.clamp((t-INTRO_TIMES[k-1])/(INTRO_TIMES[k]-INTRO_TIMES[k-1]),0,1);
            u=u*u*(3-2*u);
            float settle=Combat.clamp((t-1.65f)/.7f,0,1);
            for(int i=0;i<14;i++){
                float target=poses[k-1][i]+(poses[k][i]-poses[k-1][i])*u;
                // Match the breathing idle pose exactly before combat unlocks.
                joints[i]=target+(joints[i]-READY[i])*settle;
            }
            break;
        }
        float lower=Math.max(0,Math.max(reachHeight(joints[0]+6,8,16),reachHeight(joints[0]-8,3,20))-joints[1]);
        for(int i=1;i<14;i+=2)joints[i]+=lower;
        knee(joints[0]+6,joints[1]+8,14,16);
        knee(joints[0]-8,joints[1]+3,18,20);
        float[] gesture=joints.clone();
        solveArms();
        float armBlend=Combat.smooth((t-1.65f)/.7f);
        for(int i=6;i<14;i++)joints[i]=gesture[i]+(joints[i]-gesture[i])*armBlend;
    }
    public void sample(Combat.Fighter f,float clock){
        editedPose=null;
        shoulderTurn=frontAngle=backAngle=rotation=0;
        spineBend=frontWrist=backWrist=authoredSocketWeight=0;
        System.arraycopy(READY,0,joints,0,22);
        if(f.hp<=0){System.arraycopy(FALL,0,joints,0,22);return;}
        if(f.rolling){sampleRoll(f,clock);return;}
        // Keep the rear fist in front of the face, not behind the nape.
        joints[12]+=18;
        float breathe=(float)Math.sin(clock*2.7f)*.75f;
        // The ribcage breathes; the pelvis and supporting feet do not bounce.
        for(int i=3;i<14;i+=2)joints[i]+=breathe;
        if(f.block)for(int i=0;i<14;i++)joints[i]=GUARD[i];
        float crouch=0;
        if(f.jumpPrep>0)crouch=17*(float)Math.sin((1-f.jumpPrep/.12f)*Math.PI*.5);
        if(f.landing>0)crouch=18*(float)Math.sin((1-f.landing/.24f)*Math.PI);
        if(f.y>0){
            float flight=Combat.clamp(f.airTime/(1060f/1450),0,1);
            float tuck=(float)Math.sin(Math.PI*flight);
            // Extend after push-off, tuck near the apex, then reach for the floor.
            if(f.airTime<.065f)crouch=17*(1-f.airTime/.065f);
            joints[16]=27-8*tuck;joints[17]=-6-49*tuck;
            joints[20]=-32+11*tuck;joints[21]=-6-34*tuck;
            joints[7]-=6*tuck;joints[9]-=8*tuck;
            joints[11]+=4*tuck;joints[13]-=6*tuck;
            // Lead with the chest and fold the trailing leg on lateral jumps.
            float lateral=Combat.clamp(f.takeoffVx*f.facing/245,-1,1);
            float lean=lateral*(float)Math.sin(Math.PI*flight)*9;
            for(int i=2;i<14;i+=2)joints[i]+=lean;
            joints[16]+=lateral*12*tuck;joints[20]-=lateral*10*tuck;
            joints[17]+=lateral*8*tuck;joints[21]-=lateral*8*tuck;
        }else{
            joints[16]=(f.frontFoot-f.x)*f.facing;joints[17]=-6-f.frontLift;
            joints[20]=(f.backFoot-f.x)*f.facing;joints[21]=-6-f.backLift;
            float weight=f.weightShift;
            float lean=f.motion*f.facing*2.8f;
            joints[0]+=weight;
            // Hips lead; shoulders and head follow by smaller amounts.
            for(int i=2;i<14;i+=2)joints[i]+=weight*.62f+lean;
            joints[4]-=weight*.25f+lean*.35f;
            shoulderTurn=f.bodyTurn;
            joints[6]+=shoulderTurn;joints[8]+=shoulderTurn*.65f;
            joints[10]-=shoulderTurn;joints[12]-=shoulderTurn*.65f;
            joints[7]+=shoulderTurn*.35f;joints[11]-=shoulderTurn*.35f;
            // Damped knee compression loads and releases once per paired shuffle.
            crouch+=f.weightDrop;
            if(f.stepping>=0){
                float u=f.stepTime/f.stepDuration;
                float angle=-9*(float)Math.sin(2*Math.PI*u)*(float)Math.sin(Math.PI*u);
                if(f.stepping==0)frontAngle=angle;else backAngle=angle;
            }
        }
        for(int i=1;i<14;i+=2)joints[i]+=crouch;
        if(f.attack!=0)attack(f);
        else {
            spineBend=-f.bodyTurn*1.5f+f.motion*f.facing*2.4f;
            // The shoulder girdle trails the pelvis; the gaze stays steadier.
            float lag=Combat.clamp(f.turnSpeed*.035f,-1.8f,1.8f);
            joints[2]-=lag;joints[4]+=lag*.45f;
            joints[8]-=f.bodyTurn*1.1f;joints[12]+=f.bodyTurn*.8f;
            frontWrist=-f.bodyTurn*1.8f;backWrist=f.bodyTurn*1.5f;
        }
        if(f.hurt>0){float amount=(float)Math.sin(Math.min(1,f.hurt/.34f)*Math.PI*.5);for(int i=0;i<14;i++)joints[i]+=(RECOIL[i]-joints[i])*amount;}
        // Accommodate wider steps through the pelvis instead of stretching the shin.
        if(f.y==0&&f.attack==0){
            float required=Math.max(reachHeight(joints[0]+6,8,16),reachHeight(joints[0]-8,3,20));
            float lower=Math.max(0,required-joints[1]);
            for(int i=1;i<14;i+=2)joints[i]+=lower;
        }
        knee(joints[0]+6,joints[1]+8,14,16);
        knee(joints[0]-8,joints[1]+3,18,20);
        solveArms();
        sampleEdited(f);
    }
    private void sampleEdited(Combat.Fighter f){
        if(f.y>0||f.jumpPrep>0||f.landing>0||f.hurt>0||f.block)return;
        int kind=f.attack;
        AnimationClip clip=f.animations[kind];
        if(clip==null)return;
        if(kind==0&&clip.walkDistance>0){sampleReferenceWalk(f,clip);return;}
        if(kind==0&&Math.abs(f.motion)<.02f)return;
        double frame=kind==0?(f.walk/.09/138*60)%clip.duration:
            Math.min(clip.duration,f.attackTime/(kind==1?.4:.68)*clip.duration);
        if(kind==0&&f.motion*f.facing<0)frame=(clip.duration-frame)%clip.duration;
        AnimationClip.PoseData data=AnimationRuntime.sampleAnimation(clip,frame);
        float amount=kind==0?Combat.clamp(Math.abs(f.motion)*1.6f,0,1):1;
        if(amount<1){
            AnimationClip.PoseData base=AnimationRig.fromPose(this,null);
            if(data.segmented)AnimationRig.segment(base);
            data.x=base.x+(data.x-base.x)*amount;data.y=base.y+(data.y-base.y)*amount;
            data.spineBend=base.spineBend+(data.spineBend-base.spineBend)*amount;
            data.shoulderTurn=base.shoulderTurn+(data.shoulderTurn-base.shoulderTurn)*amount;
            for(int i=0;i<19;i++)data.angles[i]=base.angles[i]+(AnimationRig.near(data.angles[i],base.angles[i])-base.angles[i])*amount;
        }
        editedPose=data;
        System.arraycopy(AnimationRig.joints(data),0,joints,0,22);
    }
    private void sampleReferenceWalk(Combat.Fighter f,AnimationClip clip){
        double frame=((f.walkSigned/clip.walkDistance*clip.duration)%clip.duration+clip.duration)%clip.duration;
        AnimationClip.PoseData data=AnimationRuntime.sampleAnimation(clip,frame);
        float amount=Combat.clamp(Math.abs(f.motion)*2.4f,0,1);
        AnimationClip.PoseData idle=ReferenceMotion.idle();
        if(amount<1)data=AnimationRuntime.blend(idle,data,amount);
        if(f.recoveryTime>0&&f.recoveryFrom!=null)data=AnimationRuntime.blend(f.recoveryFrom,data,Combat.smooth(1-f.recoveryTime/.18f));
        editedPose=data;System.arraycopy(AnimationRig.joints(data),0,joints,0,22);
    }
    private void attack(Combat.Fighter f){
        float[] base=joints.clone();
        float t=f.attackTime;
        // Follow-through starts immediately after contact; no held cardboard frame.
        float[] follow=(f.attack==1?JAB:EXTEND).clone();
        if(f.attack==1){follow[2]+=3;follow[4]-=2;follow[8]-=9;follow[9]-=4;}
        else {follow[16]-=8;follow[17]+=9;follow[2]+=4;}
        float[] recover=READY.clone();recover[0]+=2;recover[2]+=5;recover[8]+=6;
        if(f.attack==1)keys(t,new float[]{0,.065f,.12f,.16f,.25f,.4f},new float[][]{READY,WIND,JAB,follow,recover,READY});
        else keys(t,new float[]{0,.13f,.24f,.29f,.44f,.68f},new float[][]{READY,CHAMBER,EXTEND,follow,CHAMBER,READY});
        float envelope=Combat.smooth(t/.065f)*Combat.smooth(((f.attack==1?.4f:.68f)-t)/.14f);
        // Apply keys as offsets to the live stance, retaining its ground contacts.
        for(int i=0;i<14;i++)joints[i]+=base[i]-READY[i];
        if(f.y==0){
            joints[20]=base[20];joints[21]=base[21];
            if(f.attack==1){joints[16]=base[16];joints[17]=base[17];}
            else {joints[16]+=base[16]-READY[16];joints[17]+=base[17]-READY[17];}
        }
        float drive=(float)Math.sin(Math.PI*Combat.clamp((t-.065f)/.19f,0,1));
        shoulderTurn=12*drive*envelope;
        spineBend=(-3+8*drive)*envelope;
        frontWrist=-9*envelope*(1-drive);backWrist=5*envelope;
        // Keep planted support reachable even when striking from a wide step.
        float lower=Math.max(0,reachHeight(joints[0]-8,3,20)-joints[1]);
        if(f.attack==1)lower=Math.max(lower,reachHeight(joints[0]+6,8,16)-joints[1]);
        for(int i=1;i<14;i+=2)joints[i]+=lower;
    }
    private void solveArms(){solveArms(joints,shoulderTurn);}
    static void solveArms(float[] joints,float shoulderTurn){
        arm(joints,joints[2]+11+shoulderTurn,joints[3]+5+shoulderTurn*.3f,6,8);
        arm(joints,joints[2]-6-shoulderTurn*.35f,joints[3]+6-shoulderTurn*.105f,10,12);
    }
    private static void arm(float[] joints,float sx,float sy,int elbow,int hand){
        float dx=joints[hand]-sx,dy=joints[hand+1]-sy;
        float distance=Math.max(.001f,(float)Math.hypot(dx,dy));
        float reach=Combat.clamp(distance,4.01f,75.99f);
        float ux=dx/distance,uy=dy/distance;
        if(reach!=distance){joints[hand]=sx+ux*reach;joints[hand+1]=sy+uy*reach;}
        float along=(36*36-40*40+reach*reach)/(2*reach);
        float bend=(float)Math.sqrt(Math.max(0,36*36-along*along));
        joints[elbow]=sx+ux*along-uy*bend;
        joints[elbow+1]=sy+uy*along+ux*bend;
    }
    private void sampleRoll(Combat.Fighter f,float clock){
        if(f.rollAnimation!=null){
            AnimationClip.PoseData sampled=AnimationRuntime.sampleAnimation(f.rollAnimation,f.rollTime*60);
            float[] points=AnimationRig.joints(sampled);float travel=RollAnimation.travel(f,f.rollTime);
            for(int i=0;i<22;i+=2)points[i]-=travel*sampled.facing;
            System.arraycopy(points,0,joints,0,22);authoredSocketWeight=(float)sampled.socketWeight;authoredFacing=sampled.facing;return;
        }
        RollAnimation.sample(f,clock,joints);
    }
    private float reachHeight(float hipX,float offsetY,int foot){
        float dx=joints[foot]-hipX;
        return joints[foot+1]-(float)Math.sqrt(Math.max(0,102*102-dx*dx))-offsetY;
    }
    // Two-bone IK keeps thigh and shin lengths fixed while the foot stays planted.
    private void knee(float hx,float hy,int knee,int foot){
        float dx=joints[foot]-hx,dy=joints[foot+1]-hy;
        float distance=(float)Math.sqrt(dx*dx+dy*dy);
        if(distance<.001f)return;
        float length=Math.min(distance,103.99f);
        float along=(53*53-51*51+length*length)/(2*length);
        float bend=(float)Math.sqrt(Math.max(0,53*53-along*along));
        joints[knee]=hx+dx/distance*along+dy/distance*bend;
        joints[knee+1]=hy+dy/distance*along-dx/distance*bend;
    }
    private void keys(float t,float[] times,float[][] poses){
        for(int i=1;i<times.length;i++)if(t<=times[i]){System.arraycopy(poses[i-1],0,joints,0,22);float u=Combat.clamp((t-times[i-1])/(times[i]-times[i-1]),0,1);blend(joints,poses[i],u*u*(3-2*u));return;}
    }
    private void blend(float[] a,float[] b,float t){for(int i=0;i<22;i++)a[i]+=(b[i]-a[i])*t;}
}
