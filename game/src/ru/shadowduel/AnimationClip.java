package ru.shadowduel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Editable, versioned animation data. No platform or editor dependencies. */
public final class AnimationClip {
    public int version=1,fps=60,duration=60,startupEnd=12,activeEnd=30;
    public String name="Новая анимация";
    public boolean airborne=false;
    public double playbackSpeed=1;
    public double walkDistance=0;
    public double left=-180,top=-260,right=440,bottom=40;
    public final Map<String,PoseData> poses=new LinkedHashMap<>();
    public final List<Key> keys=new ArrayList<>();
    public final List<Box> boxes=new ArrayList<>();
    public String importReport="";
    public static final class PoseData {
        public String name="Стойка";
        public double x=-8,y=-104,socketWeight=0;
        public int facing=1;
        public boolean extended=false,segmented=false;
        public double spineBend=0,shoulderTurn=0;
        public double[] angles=new double[19];
        // Projected bone lengths for a turned stance; 1 preserves existing animations.
        public double[] projection=ones();
        private static double[] ones(){double[] a=new double[19];java.util.Arrays.fill(a,1);return a;}
        public PoseData copy(){PoseData p=new PoseData();p.name=name;p.x=x;p.y=y;p.socketWeight=socketWeight;p.facing=facing;p.angles=angles.clone();p.projection=projection.clone();p.extended=extended;p.segmented=segmented;p.spineBend=spineBend;p.shoulderTurn=shoulderTurn;return p;}
    }
    public static final class Key {
        public int frame;public String pose,easing="linear";
        public Key(int frame,String pose){this.frame=frame;this.pose=pose;}
        public Key copy(){Key k=new Key(frame,pose);k.easing=easing;return k;}
    }
    public static final class Box {
        public String name="Хитбокс",kind="hit",shape="circle",damageType="strike";
        public int bone=3,start=12,end=29;
        public double along=1,x=0,y=0,rotation=0,radius=18,width=36,height=24,damage=10;
        public Box copy(){Box b=new Box();b.name=name;b.kind=kind;b.shape=shape;b.damageType=damageType;b.bone=bone;b.start=start;b.end=end;b.along=along;b.x=x;b.y=y;b.rotation=rotation;b.radius=radius;b.width=width;b.height=height;b.damage=damage;return b;}
        public String toString(){return name+" ["+kind+", "+start+"–"+end+"]";}
    }
    public AnimationClip copy(){
        AnimationClip a=new AnimationClip();a.version=version;a.fps=fps;a.duration=duration;a.startupEnd=startupEnd;a.activeEnd=activeEnd;a.name=name;a.airborne=airborne;a.playbackSpeed=playbackSpeed;a.walkDistance=walkDistance;
        a.left=left;a.top=top;a.right=right;a.bottom=bottom;a.importReport=importReport;
        for(Map.Entry<String,PoseData> e:poses.entrySet())a.poses.put(e.getKey(),e.getValue().copy());
        for(Key k:keys)a.keys.add(k.copy());for(Box b:boxes)a.boxes.add(b.copy());return a;
    }
    public static AnimationClip create(){AnimationClip a=new AnimationClip();a.poses.put("ready",AnimationRig.bindPose());a.keys.add(new Key(0,"ready"));a.keys.add(new Key(60,"ready"));return a;}
}
