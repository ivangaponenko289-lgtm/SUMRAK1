package ru.shadowduel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Shared deterministic sampler. Frames may be fractional; callers handle time and looping. */
public final class AnimationRuntime {
    private AnimationRuntime(){}
    public static final String[] EASINGS={"linear","ease-in","ease-out","ease-in-out","snap"};
    public static double ease(String easing,double t){
        t=Math.max(0,Math.min(1,t));
        switch(easing){case "linear":return t;case "ease-in":return t*t;case "ease-out":return 1-(1-t)*(1-t);case "ease-in-out":return t*t*(3-2*t);case "snap":return t<1?0:1;default:throw new IllegalArgumentException("Unknown easing: "+easing);}
    }
    public static List<AnimationClip.Key> sortedKeys(AnimationClip anim){List<AnimationClip.Key> keys=new ArrayList<>(anim.keys);Collections.sort(keys,new Comparator<AnimationClip.Key>(){public int compare(AnimationClip.Key a,AnimationClip.Key b){return Integer.compare(a.frame,b.frame);}});return keys;}
    public static AnimationClip.PoseData sampleAnimation(AnimationClip anim,double frame){
        if(!AnimationRig.finite(frame))throw new IllegalArgumentException("Frame must be finite");
        AnimationClip.Key a=null,b=null;
        // No per-frame sorting or temporary key list. Editing may leave keys unordered.
        for(AnimationClip.Key k:anim.keys){if(k.frame<=frame&&(a==null||k.frame>a.frame))a=k;if(k.frame>frame&&(b==null||k.frame<b.frame))b=k;}
        if(a==null&&b==null)throw new IllegalArgumentException("Animation has no keys");
        if(a==null)return pose(anim,b).copy();if(b==null||frame==a.frame)return pose(anim,a).copy();
        AnimationClip.PoseData p=pose(anim,a),q=pose(anim,b);double t=ease(a.easing,(frame-a.frame)/(b.frame-a.frame));
        return blend(p,q,t);
    }
    public static AnimationClip.PoseData blend(AnimationClip.PoseData p,AnimationClip.PoseData q,double t){
        t=Math.max(0,Math.min(1,t));AnimationClip.PoseData out=p.copy();
        out.x=mix(p.x,q.x,t);out.y=mix(p.y,q.y,t);out.socketWeight=mix(p.socketWeight,q.socketWeight,t);
        // Deliberately do not wrap: authored full turns must survive interpolation.
        out.spineBend=mix(p.spineBend,q.spineBend,t);out.shoulderTurn=mix(p.shoulderTurn,q.shoulderTurn,t);
        for(int j=0;j<19;j++){out.angles[j]=mix(p.angles[j],q.angles[j],t);out.projection[j]=mix(p.projection[j],q.projection[j],t);}
        if(p.extended&&q.extended){
            float[] pj=AnimationRig.joints(p),qj=AnimationRig.joints(q);
            double[] pa=AnimationRig.absoluteAngles(p),qa=AnimationRig.absoluteAngles(q);
            for(int leg=0;leg<2;leg++){
                int end=leg==0?7:9,foot=AnimationRig.END[end],toe=12+leg;
                // Interpolate ankle targets, then solve the leg: the arc of an
                // angular blend must not push a grounded sole through the floor.
                AnimationRig.ik(out,end,mix(pj[foot],qj[foot],t)*out.facing,mix(pj[foot+1],qj[foot+1],t),false);
                out.angles[toe]=mix(pa[toe],qa[toe],t)-AnimationRig.absoluteAngles(out)[end];
            }
        }
        return out;
    }
    private static AnimationClip.PoseData pose(AnimationClip a,AnimationClip.Key k){AnimationClip.PoseData p=a.poses.get(k.pose);if(p==null)throw new IllegalArgumentException("Missing pose: "+k.pose);return p;}
    private static double mix(double a,double b,double t){return a+(b-a)*t;}
    /** World-space center and angle for a bone-attached debug/combat volume. */
    public static double[] boxTransform(AnimationClip.PoseData p,AnimationClip.Box box){
        float[] j=AnimationRig.joints(p);double[] s=AnimationRig.start(p,j,box.bone);
        double angle=AnimationRig.absoluteAngles(p)[box.bone],r=Math.toRadians(angle),c=Math.cos(r),v=Math.sin(r);
        double along=box.along*AnimationRig.length(p,box.bone)+box.x;
        return new double[]{(s[0]+along*c-box.y*v)*p.facing,s[1]+along*v+box.y*c,(p.facing==1?angle:180-angle)+box.rotation*p.facing};
    }
    public static boolean boxActive(AnimationClip.Box box,double frame){return frame>=box.start&&frame<=box.end;}
    public static String phase(AnimationClip clip,double frame){return frame<clip.startupEnd?"startup":frame<clip.activeEnd?"active":"recovery";}
}
