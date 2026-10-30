package ru.shadowduel;

/** Retargeted screen-space motion. Projected lengths retain the turned rear leg. */
public final class ReferenceMotion {
    private ReferenceMotion(){}
    public static AnimationClip walk(){
        AnimationClip clip=new AnimationClip();clip.version=3;clip.name="Ходьба · видео";
        clip.duration=37;clip.startupEnd=0;clip.activeEnd=37;clip.walkDistance=ReferenceSamples.DISTANCE;
        clip.left=-160;clip.right=180;clip.top=-240;clip.bottom=30;
        clip.importReport="Видео 26.10: кадры 19–46, все исходные кадры цикла. Камера компенсирована по груше. Контуры — фото 30.10.";
        for(int frame=0;frame<=clip.duration;frame++){
            double t=frame*27.0/clip.duration;int a=Math.min(26,(int)t);double f=t-a;
            double[] row=new double[14];for(int j=0;j<14;j++)row[j]=ReferenceSamples.DATA[a][j]*(1-f)+ReferenceSamples.DATA[a+1][j]*f;
            AnimationClip.PoseData p=pose(row);p.name="Видео · "+frame;
            String id="frame-"+frame;clip.poses.put(id,p);clip.keys.add(new AnimationClip.Key(frame,id));
        }
        return clip;
    }
    public static AnimationClip.PoseData idle(){
        double[] pixel={795,930,780,510,730,310,421,1130,430,1514,978,1190,972,1514};
        for(int i=0;i<pixel.length;i+=2){pixel[i]=-8+(795-pixel[i])/6.8;pixel[i+1]=(pixel[i+1]-1555)/6.8;}
        pixel[9]=pixel[13]=-6;return pose(pixel);
    }
    private static AnimationClip.PoseData pose(double[] r){
        AnimationClip.PoseData p=new AnimationClip.PoseData();p.extended=p.segmented=true;p.x=r[0];p.y=r[1];
        double spine=angle(r[2]-r[0],r[3]-r[1]),scale=Math.hypot(r[2]-r[0],r[3]-r[1])/AnimationRig.length(0);
        p.projection[14]=p.projection[15]=p.projection[0]=scale;p.angles[14]=spine;
        p.angles[1]=angle(r[4]-r[2],r[5]-r[3])-spine;p.projection[1]=Math.hypot(r[4]-r[2],r[5]-r[3])/AnimationRig.length(1);
        p.angles[16]=-angle(r[4]-r[2],r[5]-r[3]);
        // The guard is held around the chest, with the rear knee opening away from the front knee.
        bone(p,2,r[2]+44,r[3]+30);bone(p,3,r[2]+75,r[3]+15);
        bone(p,4,r[2]-38,r[3]+25);bone(p,5,r[2]-17,r[3]+18);
        bone(p,6,r[6],r[7]);bone(p,7,r[8],r[9]);
        bone(p,8,r[10],r[11]);bone(p,9,r[12],r[13]);
        double[] angles=AnimationRig.absoluteAngles(p);p.angles[12]=-angles[7];p.angles[13]=-angles[9];
        return p;
    }
    private static double angle(double x,double y){return Math.toDegrees(Math.atan2(y,x));}
    private static void bone(AnimationClip.PoseData p,int b,double x,double y){
        float[] j=AnimationRig.joints(p);double[] start=AnimationRig.start(p,j,b),angles=AnimationRig.absoluteAngles(p);
        double abs=angle(x-start[0],y-start[1]);
        double parent=(b==6||b==8)?0:angles[AnimationRig.PARENT[b]];
        p.projection[b]=Math.hypot(x-start[0],y-start[1])/AnimationRig.length(p,b);p.angles[b]=abs-parent;
    }
}
