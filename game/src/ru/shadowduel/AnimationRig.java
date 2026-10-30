package ru.shadowduel;

/** Angular adapter for Pose's existing 11 joints and HumanRenderer's sockets.
 * Bone lengths are taken from the actual idle rig; legs retain the 53/51 IK lengths. */
public final class AnimationRig {
    private AnimationRig(){}
    public static final String ID="shadowduel-11-v1",EXTENDED_ID="shadowduel-15-v2",SEGMENTED_ID="shadowduel-20-v3";
    public static final String[] BONES={"spine","neck","frontUpperArm","frontForearm","backUpperArm","backForearm","frontThigh","frontShin","backThigh","backShin","frontHand","backHand","frontFoot","backFoot","lumbar","thoracic","head","frontPatella","backPatella"};
    public static final String[] LABELS={"Верх спины / корпус","Шея","Ближнее плечо","Ближнее предплечье","Дальнее плечо","Дальнее предплечье","Ближнее бедро","Ближняя голень","Дальнее бедро","Дальняя голень","Ближняя кисть","Дальняя кисть","Ближняя стопа","Дальняя стопа","Поясница","Грудной отдел","Голова (отдельно)","Ближний надколенник","Дальний надколенник"};
    public static final int[] PARENT={-1,0,0,2,0,4,0,6,0,8,3,5,7,9,-1,14,1,6,8};
    public static final int[] END={2,4,6,8,10,12,14,16,18,20,22,24,26,28,30,32,34,36,38};
    private static final double[] LENGTHS=new double[19];
    private static final AnimationClip.PoseData BIND;
    static {
        float[] j=Pose.READY.clone();
        LENGTHS[0]=Math.hypot(j[2]-j[0],j[3]-j[1]);LENGTHS[1]=Math.hypot(j[4]-j[2],j[5]-j[3]);
        LENGTHS[2]=Math.hypot(j[6]-(j[2]+11),j[7]-(j[3]+5));
        LENGTHS[3]=Math.hypot(j[8]-j[6],j[9]-j[7]);
        LENGTHS[4]=Math.hypot(j[10]-(j[2]-13),j[11]-(j[3]+6));
        LENGTHS[5]=Math.hypot(j[12]-j[10],j[13]-j[11]);
        LENGTHS[14]=LENGTHS[15]=LENGTHS[0]/3;LENGTHS[16]=16;LENGTHS[17]=LENGTHS[18]=9;
        LENGTHS[10]=LENGTHS[11]=14;LENGTHS[12]=LENGTHS[13]=18;
        LENGTHS[6]=LENGTHS[8]=53;LENGTHS[7]=LENGTHS[9]=51;
        BIND=fromJoints(j,0,null);
        ik(BIND,7,40,-6,false);ik(BIND,9,-42,-6,false);
    }
    public static double length(AnimationClip.PoseData p,int bone){return (p.segmented&&bone==0?LENGTHS[0]/3:p.extended&&bone>=2&&bone<=5?(bone%2==0?36:40):LENGTHS[bone])*p.projection[bone];}
    public static double length(int bone){return LENGTHS[bone];}
    public static boolean finite(double value){return !Double.isNaN(value)&&!Double.isInfinite(value);}
    public static double[] lengths(){return LENGTHS.clone();}
    public static AnimationClip.PoseData bindPose(){return BIND.copy();}
    public static double near(double value,double reference){return value+360*Math.rint((reference-value)/360);}
    public static double[] absoluteAngles(AnimationClip.PoseData p){
        double[] a=new double[19];
        a[14]=p.segmented?p.angles[14]:p.angles[0]+p.angles[14];a[15]=a[14]+p.angles[15];
        for(int i=0;i<14;i++)a[i]=i==0&&p.segmented?a[15]+p.angles[0]:p.angles[i]+(PARENT[i]<0?0:p.segmented&&(i==6||i==8)?0:a[PARENT[i]]);
        a[16]=a[1]+p.angles[16];
        a[17]=(a[6]+a[7])*.5-90+p.angles[17];a[18]=(a[8]+a[9])*.5-90+p.angles[18];
        return a;
    }
    /** Canonical (unmirrored) points, suitable for HumanRenderer with facing=p.facing. */
    public static float[] joints(AnimationClip.PoseData p){
        float[] j=new float[40];j[0]=(float)(p.x*p.facing);j[1]=(float)p.y;
        double[] a=absoluteAngles(p);
        for(int b:new int[]{14,15,0,1,2,3,4,5,6,7,8,9,10,11,12,13,16,17,18}){
            double[] s=start(p,j,b);int e=END[b];double r=Math.toRadians(a[b]);
            j[e]=(float)(s[0]+length(p,b)*Math.cos(r));j[e+1]=(float)(s[1]+length(p,b)*Math.sin(r));
        }return j;
    }
    public static float[] worldJoints(AnimationClip.PoseData p){float[] j=joints(p);for(int i=0;i<j.length;i+=2)j[i]*=p.facing;return j;}
    public static double[] start(float[] j,int bone,double weight){return start(j,bone,weight,0);}
    public static double[] start(float[] j,int bone,double weight,double turn){
        if(bone==14)return new double[]{j[0],j[1]};
        if(bone==15)return new double[]{j[30],j[31]};
        if(bone==16)return new double[]{j[4]-3,j[5]+10};
        if(bone==17||bone==18){int e=bone==17?14:18;return new double[]{j[e],j[e+1]};}
        if(bone>=10){int e=END[PARENT[bone]];return new double[]{j[e],j[e+1]};}
        if(bone==0)return new double[]{j[0],j[1]};
        if(bone==1)return new double[]{j[2],j[3]};
        if(bone==3||bone==5||bone==7||bone==9){int i=END[bone-1];return new double[]{j[i],j[i+1]};}
        double dx=j[2]-j[0],dy=j[3]-j[1],len=Math.max(.000001,Math.hypot(dx,dy));
        double ux=dx/len,uy=dy/len,rx=1+(-uy-1)*weight,ry=ux*weight;
        ux*=weight;uy=-1+(uy+1)*weight;
        int anchor=bone<6?2:0;
        double sx=bone==2?11:bone==4?-6:bone==6?6:-8;
        double sy=bone==2?5:bone==4?6:bone==6?8:3;
        double shift=bone==2?turn:bone==4?-turn*.35:0;
        return new double[]{j[anchor]+sx*rx-sy*ux+shift,j[anchor+1]+sx*ry-sy*uy+shift*.3};
    }
    public static double[] start(AnimationClip.PoseData p,float[] j,int bone){
        if(p.segmented&&bone==0)return new double[]{j[32],j[33]};
        return start(j,bone,p.socketWeight,p.shoulderTurn);
    }
    public static void fk(AnimationClip.PoseData p,int bone,double worldX,double worldY){
        float[] j=joints(p);double[] s=start(p,j,bone),a=absoluteAngles(p);
        double dx=worldX*p.facing-s[0],dy=worldY-s[1];if(Math.hypot(dx,dy)<.001)return;
        double absolute=Math.toDegrees(Math.atan2(dy,dx));
        double parent=bone==14?(p.segmented?0:p.angles[0]):bone==17?(a[6]+a[7])*.5-90:bone==18?(a[8]+a[9])*.5-90:
            p.segmented&&(bone==6||bone==8)?0:PARENT[bone]<0?0:a[PARENT[bone]];
        double target=absolute-parent;
        if(p.segmented&&bone==0)target=absolute-a[15];
        p.angles[bone]=near(target,p.angles[bone]);
    }
    /** Two-bone IK; clamp unreachable targets and preserve the current elbow/knee branch. */
    public static void ik(AnimationClip.PoseData p,int endBone,double worldX,double worldY,boolean flip){
        int upper=endBone-1;if(endBone!=3&&endBone!=5&&endBone!=7&&endBone!=9)throw new IllegalArgumentException("IK endpoint required");
        float[] j=joints(p);double[] s=start(p,j,upper),a=absoluteAngles(p);
        double dx=worldX*p.facing-s[0],dy=worldY-s[1],dist=Math.hypot(dx,dy);
        if(dist<.000001){dx=Math.cos(Math.toRadians(a[upper]));dy=Math.sin(Math.toRadians(a[upper]));dist=1;}
        double l1=length(p,upper),l2=length(p,endBone),d=Math.max(Math.abs(l1-l2)+.00001,Math.min(l1+l2-.00001,dist));
        int e=END[upper],f=END[endBone];double cross=(j[e]-s[0])*(j[f+1]-j[e+1])-(j[e+1]-s[1])*(j[f]-j[e]);
        double sign=cross<0?-1:1;if(flip)sign=-sign;
        double bend=sign*Math.acos(Math.max(-1,Math.min(1,(d*d-l1*l1-l2*l2)/(2*l1*l2))));
        double first=Math.atan2(dy,dx)-Math.atan2(l2*Math.sin(bend),l1+l2*Math.cos(bend));
        double parent=p.segmented&&(upper==6||upper==8)?0:a[PARENT[upper]];
        p.angles[upper]=near(Math.toDegrees(first)-parent,p.angles[upper]);
        p.angles[endBone]=near(Math.toDegrees(bend),p.angles[endBone]);
    }
    public static AnimationClip.PoseData fromJoints(float[] j,double weight,AnimationClip.PoseData previous){
        AnimationClip.PoseData p=new AnimationClip.PoseData();p.x=j[0];p.y=j[1];p.socketWeight=weight;
        double[] a=new double[19];
        for(int b=0;b<10;b++){
            double[] s=start(j,b,weight);int e=END[b];a[b]=Math.toDegrees(Math.atan2(j[e+1]-s[1],j[e]-s[0]));
            double relative=a[b]-(PARENT[b]<0?0:a[PARENT[b]]);
            p.angles[b]=near(relative,previous==null?0:previous.angles[b]);
        }return p;
    }
    public static void mirror(AnimationClip.PoseData p){p.x=-p.x;p.facing=-p.facing;}
    public static void segment(AnimationClip.PoseData p){
        if(p.segmented)return;
        p.angles[16]=-absoluteAngles(p)[1];
        p.angles[6]+=p.angles[0];p.angles[8]+=p.angles[0];
        p.angles[14]=p.angles[0];p.angles[15]=0;p.angles[0]=0;p.segmented=true;
    }
    public static void upgrade(AnimationClip clip){
        if(clip.version==3)return;
        // Only extended (v2) clips can retain their arm lengths during upgrade.
        if(clip.version!=2)return;
        for(AnimationClip.PoseData p:clip.poses.values()){
            segment(p);
        }
        clip.version=3;
    }
    /** Import the current procedural silhouette into the extended fixed-length rig. */
    public static AnimationClip.PoseData fromPose(Pose pose,AnimationClip.PoseData previous){
        float[] j=pose.joints;AnimationClip.PoseData p=fromJoints(j,0,previous);
        p.extended=true;p.spineBend=pose.spineBend;p.shoulderTurn=pose.shoulderTurn;
        double[] a=new double[10];
        for(int b=0;b<10;b++){
            double[] s=start(j,b,0,p.shoulderTurn);int e=END[b];
            a[b]=Math.toDegrees(Math.atan2(j[e+1]-s[1],j[e]-s[0]));
            p.angles[b]=near(a[b]-(PARENT[b]<0?0:a[PARENT[b]]),previous==null?0:previous.angles[b]);
        }
        for(int end:new int[]{3,5,7,9})ik(p,end,j[END[end]],j[END[end]+1],false);
        p.angles[10]=pose.frontWrist;p.angles[11]=pose.backWrist;
        double[] absolute=absoluteAngles(p);
        p.angles[12]=pose.frontAngle-absolute[7];p.angles[13]=pose.backAngle-absolute[9];
        return p;
    }
}
