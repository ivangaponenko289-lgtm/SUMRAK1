package ru.shadowduel;

/** Regression checks for retargeting, editor files and signed walk phase. */
public final class ReferenceMotionTest {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    public static void main(String[] args){
        AnimationClip clip=ReferenceMotion.walk();
        AnimationClip restored=AnimationJson.decode(AnimationJson.encode(clip));
        check(restored.walkDistance==clip.walkDistance,"Stride survives save/load");
        check(AnimationValidation.validate(restored,60).isEmpty(),"All reference frames validate");
        for(int frame=0;frame<=clip.duration;frame++){
            AnimationClip.PoseData a=AnimationRuntime.sampleAnimation(clip,frame),b=AnimationRuntime.sampleAnimation(restored,frame);
            float[] aj=AnimationRig.joints(a),bj=AnimationRig.joints(b);
            for(int i=0;i<40;i++)check(Math.abs(aj[i]-bj[i])<.001,"Save/load retains projected joints");
            check(aj[18]<aj[0]-8,"Rear knee opens outward");
            check(aj[14]>aj[0]+6,"Front knee opens outward");
            check(aj[16]-aj[20]>65,"Stable stance width");
        }
        float[] first=AnimationRig.joints(AnimationRuntime.sampleAnimation(clip,0)),last=AnimationRig.joints(AnimationRuntime.sampleAnimation(clip,clip.duration));
        for(int i=0;i<40;i++)check(Math.abs(first[i]-last[i])<.001,"Seamless loop");
        Combat game=new Combat();game.start();game.countdown=0;game.botEnabled=false;game.hero.x=450;game.enemy.x=1150;game.hero.plantFeet();EditableAnimations.install(game,0,clip);
        Pose pose=new Pose();float[] previous=null;float previousX=game.hero.x;float maxStep=0;
        for(int frame=0;frame<240;frame++){
            game.input.move=frame<70?1:frame<140?-1:frame<190?1:0;game.tick(1f/60);pose.sample(game.hero,game.elapsed);
            check(pose.editedPose!=null,"Standing and moving use reference rig");
            float[] joints=pose.joints;
            if(previous!=null)for(int foot:new int[]{16,20})maxStep=Math.max(maxStep,Math.abs(joints[foot]+game.hero.x-previous[foot]-previousX));
            previous=joints.clone();previousX=game.hero.x;
        }
        check(maxStep<12,"Reversal/stop must not teleport feet: "+maxStep);
        check(game.hero.recoveryTime==0,"No spurious attack recovery");
        System.out.println("PASS: reference projection round trip, outward knees, loop closure, signed direction changes, idle and stop; max foot step "+maxStep);
    }
}
