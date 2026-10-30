package ru.shadowduel;

/** Saved tempo must keep combat contacts and distance-driven footsteps in sync. */
public final class PlaybackSpeedTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static Combat game(){Combat g=new Combat();g.start();g.countdown=0;g.botEnabled=false;g.hero.x=400;g.enemy.x=1100;g.hero.plantFeet();g.enemy.plantFeet();return g;}
    public static void main(String[] args){
        String legacy=AnimationJson.encode(EditableAnimations.preset(0)).replace("  \"playbackSpeed\": 1.0,\n", "");
        check(!legacy.contains("playbackSpeed"),"Legacy fixture omits tempo field");
        AnimationClip old=AnimationJson.decode(legacy);
        check(old.playbackSpeed==1,"Legacy files default to normal tempo");
        AnimationClip cycle=EditableAnimations.preset(0);
        float[] previous=AnimationRig.joints(AnimationRuntime.sampleAnimation(cycle,0));
        for(int frame=1;frame<=cycle.duration*2;frame++){
            float[] joints=AnimationRig.joints(AnimationRuntime.sampleAnimation(cycle,frame%cycle.duration));
            double supportDrift=Math.min(Math.abs(joints[16]-previous[16]+cycle.walkDistance/cycle.duration),Math.abs(joints[20]-previous[20]+cycle.walkDistance/cycle.duration));
            check(supportDrift<1,"Support must stay planted across walk loop: frame "+frame+", drift "+supportDrift);
            previous=joints;
        }
        for(double speed:new double[]{.25,.5,1,2}){
            AnimationClip walk=EditableAnimations.preset(0);walk.playbackSpeed=speed;
            check(walk.copy().playbackSpeed==speed,"Copy retains tempo");
            check(AnimationJson.decode(AnimationJson.encode(walk)).playbackSpeed==speed,"JSON retains tempo");
            for(int direction:new int[]{-1,1}){
                Combat g=game();EditableAnimations.install(g,0,walk);g.input.move=direction;
                for(int i=0;i<45;i++)g.tick(1f/60);
                check(Math.abs(g.hero.vx-direction*Combat.walkSpeed(g.hero)*(direction==1?1:104.0/138)*speed)<.01,"Walking speed matches saved tempo");
                float distance=g.hero.x,phase=g.hero.walk;
                for(int i=0;i<15;i++)g.tick(1f/60);
                check(Math.abs((g.hero.walk-phase)/.09-Math.abs(g.hero.x-distance))<.01,"Footstep phase follows distance at every tempo");
                check(Math.abs(g.hero.motion)>.45,"Slow playback preserves full walking pose");
            }
            for(int kind:new int[]{1,2}){
                Combat g=game();AnimationClip clip=EditableAnimations.preset(kind);clip.playbackSpeed=speed;EditableAnimations.install(g,kind,clip);g.attack(g.hero,kind);
                int frames=0,contact=0;
                while(g.hero.attack!=0&&frames<200){g.tick(1f/60);frames++;if(g.hero.hit&&contact==0)contact=frames;}
                double duration=kind==1?.4:.68,active=kind==1?.12:.24;
                check(Math.abs(frames/60.0-duration/speed)<.035,"Attack ends at scaled duration");
                check(Math.abs(contact/60.0-active/speed)<.035,"Contact follows scaled animation clock");
            }
        }
        AnimationClip invalid=EditableAnimations.preset(0);invalid.playbackSpeed=0;
        check(!AnimationValidation.structure(invalid).isEmpty(),"Zero tempo rejected");
        invalid.playbackSpeed=Double.NaN;check(!AnimationValidation.structure(invalid).isEmpty(),"Non-finite tempo rejected");
        System.out.println("PASS: saved tempo, legacy default, copy, four playback rates, both movement directions, attack contact and recovery timing");
    }
}
