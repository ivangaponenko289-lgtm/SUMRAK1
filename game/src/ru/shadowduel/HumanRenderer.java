package ru.shadowduel;

/** Fighter silhouette matched to the user's photo, on the existing rig. */
public final class HumanRenderer {
    private final Pose pose=new Pose();
    private Painter p;
    private float x,y,d;
    private float spineBend,frontWrist,backWrist;
    private boolean segmented,groundedCloth,referenceStance;
    private float headTilt,frontPatella,backPatella;
    private static final int SKIN=0xff030303,SHADOW=SKIN,PANTS=SKIN;

    // Traced from photo_2026-10-28_20-20-07.jpg, excluding the overlapping arms.
    // Reference normalized to 1343x1851: floor=1750, 8 image pixels per arena unit.
    // Round the shoulder caps into the upper arms; retain the traced waist below.
    private static final float[] TORSO=ReferenceSilhouette.TORSO;
    private static final float[] NECK={-.1f,0,0,-8,.6f,-6.8f,1,-6,1.1f,0,1,6,.5f,7.5f,0,8.5f};

    // Longitudinal position along a bone, then transverse distance in arena units.
    // Fit the photo's muscle volumes to smooth bone-local envelopes, omitting
    // contour crossings where the photographed upper arm overlaps the forearm.
    private static final float[] UPPER_ARM={-.23f,0,-.08f,-6.5f,.15f,-8.6f,.44f,-9,
        .76f,-7.7f,1,-6.5f,1.12f,0,1,6.5f,.74f,8.3f,.40f,10.1f,.12f,9.3f,-.08f,6};
    private static final float[] FOREARM={-.17f,0,0,-6.3f,.28f,-7.1f,.54f,-6.8f,
        .82f,-4.7f,1,-3.9f,1.06f,0,1,3.8f,.77f,5.1f,.43f,8.4f,.15f,9.4f,-.06f,6.9f};
    // Cloth hangs around thighs and calves rather than tracing every muscle.
    private static final float[] THIGH={-.13f,0, 0,-10.5f, .25f,-12.6f, .56f,-12.4f, .85f,-10.6f, 1,-9.1f,
        1.06f,0, 1,9.3f, .78f,11, .40f,12.7f, .08f,11.8f};
    // Loose trouser legs end in a hem above the ankle, rather than a calf-shaped taper.
    private static final float[] CALF={-.07f,0, 0,-8.8f, .26f,-10.2f, .57f,-9.5f, .80f,-8.6f,
        .87f,-8.7f, .90f,-6.8f, .91f,0, .90f,7, .87f,8.9f,
        .78f,8.7f, .44f,10.3f, .22f,10.5f, .03f,8.7f};
    private static final float[] FIST={2.603f,-5.307f,6.174f,-8.753f,8.559f,-9.816f,12.781f,-7.679f,
        16.093f,-5.291f,17.077f,-1.976f,14.570f,3.855f,12.162f,7.961f,
        9.447f,9.043f,4.545f,7.739f,0.942f,6.029f,-0.256f,3.585f};
    private static final float[] PALM={-4,-4, -1,-6, 3,-2, 7,3, 13,5, 13,7,
        11,8, 2,8, -2,7, -5,4, -6,0, -6,-2};
    // The planted sole is exactly six units below the rig's ankle, as before.
    private static final float[] FOOT={-4.5f,-6, 3,-6, 5,-2.8f, 10,-1, 15.5f,0,
        18,2, 18.5f,4, 16.5f,5.5f, 2,6, -4,5.5f, -6,3, -6,-2};
    // Head traced from photo_2026-10-28_20-33-01.jpg (normalized to 1504x1694).
    // Mirror the left-facing source about pivot (725,310), at 6.775 px/unit.
    // Preserve the visible crown/face; only the rear band knot is replaced by a skull curve.
    private static final float[] HEAD={16.236f,-8.266f,14.760f,-11.070f,11.808f,-14.317f,8.856f,-15.203f,
        5.904f,-16.089f,2.952f,-16.827f,0.000f,-17.269f,-2.952f,-16.384f,
        -5.904f,-15.646f,-9.742f,-12.841f,-13.284f,-6.937f,-15.203f,0.000f,
        -13.579f,2.952f,-11.956f,5.904f,-10.332f,8.856f,-9.742f,10.775f,
        -10.185f,12.989f,-7.528f,16.679f,-2.509f,19.631f,5.166f,20.221f,
        12.251f,18.155f,12.694f,16.236f,13.579f,13.284f,13.579f,11.365f,
        15.055f,10.332f,15.793f,9.446f,15.055f,7.380f,14.317f,5.904f,
        13.875f,4.428f,15.055f,1.771f,15.351f,0.000f,15.055f,-2.952f,
        15.055f,-4.428f};

    public void draw(Painter painter,Combat.Fighter f,int accent,float clock){draw(painter,f,accent,clock,0,false);}
    public void draw(Painter painter,Combat.Fighter f,int accent,float clock,float intro,boolean opponent){
        p=painter;x=f.x;y=Combat.FLOOR-f.y;d=f.facing;
        referenceStance=false;segmented=false;headTilt=frontPatella=backPatella=0;
        if(intro>0)pose.sampleIntro(f,clock,intro,opponent);else pose.sample(f,clock);
        spineBend=pose.spineBend;frontWrist=pose.frontWrist;backWrist=pose.backWrist;
        float shadow=1-f.y/400;p.oval(x-47*shadow,Combat.FLOOR-6,94*shadow,12,0x65000000);
        if(pose.editedPose!=null){
            AnimationClip.PoseData data=pose.editedPose.copy();data.facing=f.facing;data.x*=f.facing;
            drawPose(painter,f.x,Combat.FLOOR-f.y,data);return;
        }
        if(f.rolling){
            if(f.rollAnimation!=null){d*=pose.authoredFacing;body(pose.joints,pose.authoredSocketWeight,clock,0,true,0,0);}
            else{
                float time=Combat.clamp(f.rollTime/Combat.ROLL_DURATION,0,1);
                if(f.rollDirection*f.facing<0)time=1-time;
                body(pose.joints,rollWeight(time),clock,0,true,0,0);
            }
            return;
        }
        p.rotate(pose.rotation,x,Combat.FLOOR-65);
        body(pose.joints,0,clock,pose.shoulderTurn,false,pose.frontAngle,pose.backAngle);
        if(f.block){line(53,-177,63,-143,2,accent);line(63,-143,53,-113,2,accent);}
        if(f.attack!=0){float active=f.attack==1?.12f:.24f;if(Math.abs(f.attackTime-active)<.065f){
            int a=f.attack==1?8:16;float tx=pose.joints[a],ty=pose.joints[a+1];
            line(tx-26,ty+15,tx-8,ty+5,3,0x5575e0d0);line(tx-38,ty+23,tx-20,ty+13,2,0x3375e0d0);
        }}
        p.restore();
    }
    static float rollWeight(float time){return Math.min(Combat.smooth(time/.10f),Combat.smooth((1-time)/.14f));}
    /** The game, authoring tool and support measurement all draw this same mesh. */
    public void drawPose(Painter painter,float originX,float floor,int facing,float[] joints,float socketWeight,float clock){
        spineBend=frontWrist=backWrist=0;
        referenceStance=false;segmented=false;headTilt=frontPatella=backPatella=0;
        p=painter;x=originX;y=floor;d=facing;body(joints,socketWeight,clock,0,true,0,0);
    }
    public void drawPose(Painter painter,float originX,float floor,AnimationClip.PoseData data){
        if(!data.extended){drawPose(painter,originX,floor,data.facing,AnimationRig.joints(data),(float)data.socketWeight,0);return;}
        p=painter;x=originX;y=floor;d=data.facing;
        spineBend=(float)data.spineBend;frontWrist=(float)data.angles[10];backWrist=(float)data.angles[11];
        double[] angles=AnimationRig.absoluteAngles(data);
        referenceStance=data.projection[8]!=1;segmented=data.segmented;headTilt=segmented?(float)angles[16]:0;
        frontPatella=(float)angles[17];backPatella=(float)angles[18];
        body(AnimationRig.joints(data),(float)data.socketWeight,0,(float)data.shoulderTurn,false,(float)angles[12],(float)angles[13]);
    }
    private void body(float[] j,float weight,float clock,float shoulderTurn,boolean articulated,float frontAngle,float backAngle){
        float hx=j[0],hy=j[1],cx=j[2],cy=j[3],nx=j[4],ny=j[5];
        float dx=cx-hx,dy=cy-hy,len=Math.max(.001f,(float)Math.hypot(dx,dy));
        float ux=dx/len,uy=dy/len,rx=1+(-uy-1)*weight,ry=ux*weight;
        ux*=weight;uy=-1+(uy+1)*weight;
        float farX=cx-6*rx-6*ux-shoulderTurn*.35f,farY=cy-6*ry-6*uy-shoulderTurn*.105f;
        float nearX=cx+11*rx-5*ux+shoulderTurn,nearY=cy+11*ry-5*uy+shoulderTurn*.3f;
        arm(farX,farY,j[10],j[11],j[12],j[13],SHADOW,weight*Combat.smooth((j[13]-hy-20)/20),backWrist);
        leg(hx-8*rx-3*ux,hy-8*ry-3*uy,j[18],j[19],j[20],j[21],SHADOW,
            articulated?footAngle(j[18],j[19],j[20],j[21]):backAngle,backPatella,true);

        // A broad, continuous chest/waist outline follows the reference's silhouette.
        torso(hx,hy,cx,cy,j);
        // The sternomastoid and trapezius join the skull to the shoulder girdle.
        bone(cx-2*rx+4*ux,cy-2*ry+4*uy,nx-3*rx-8*ux,ny-3*ry-8*uy,NECK,SKIN);

        leg(hx+6*rx-8*ux,hy+6*ry-8*uy,j[14],j[15],j[16],j[17],PANTS,
            articulated?footAngle(j[14],j[15],j[16],j[17]):frontAngle,frontPatella,false);

        float angle=(float)Math.toDegrees(Math.atan2((nx-cx)*d,-(ny-cy)))*weight;
        if(segmented)p.rotate(headTilt*d,x+d*(nx-3),y+ny+10);
        else p.rotate(angle,x+d*nx,y+ny);
        head(nx,ny);p.restore();
        arm(nearX,nearY,j[6],j[7],j[8],j[9],SKIN,weight*Combat.smooth((j[9]-hy-20)/20),frontWrist);
    }
    private void torso(float hx,float hy,float cx,float cy,float[] joints){
        // Quadratic skinning around the lumbar joint: pelvis and ribcage keep
        // their own orientation instead of rotating one rigid torso cutout.
        float dx=cx-hx,dy=cy-hy;
        float[] nodes=new float[TORSO.length];
        for(int i=0;i<nodes.length;i+=2){
            float t=TORSO[i],width=TORSO[i+1];
            float tangentX=dx+4*(1-2*t)*spineBend;
            float tangentLength=Math.max(.001f,(float)Math.hypot(tangentX,dy));
            nodes[i]=hx+t*dx+4*t*(1-t)*spineBend-width*dy/tangentLength;
            nodes[i+1]=hy+t*dy+width*tangentX/tangentLength;
            if(segmented){
                float[] center=spinePoint(joints,t),before=spinePoint(joints,t-.002f),after=spinePoint(joints,t+.002f);
                float vx=after[0]-before[0],vy=after[1]-before[1],length=Math.max(.001f,(float)Math.hypot(vx,vy));
                nodes[i]=center[0]+4*t*(1-t)*spineBend-width*vy/length;
                nodes[i+1]=center[1]+width*vx/length;
            }
        }
        smooth(nodes,SKIN);
    }
    private static float[] spinePoint(float[] j,float t){
        int[] point={0,30,32,2};float u=t*3;
        int segment=Math.max(0,Math.min(2,(int)Math.floor(u)));float f=u-segment;
        float[] result=new float[2];
        for(int axis=0;axis<2;axis++){
            float b=j[point[segment]+axis],c=j[point[segment+1]+axis];
            float a=segment==0?2*b-c:j[point[segment-1]+axis];
            float e=segment==2?2*c-b:j[point[segment+2]+axis];
            result[axis]=.5f*(2*b+(-a+c)*f+(2*a-5*b+4*c-e)*f*f+(-a+3*b-3*c+e)*f*f*f);
        }
        return result;
    }
    private void bodyLine(float cx,float cy,float ax,float ay,float bx,float by,float rx,float ry,float ux,float uy,float width,int color){
        line(cx+ax*rx-ay*ux,cy+ax*ry-ay*uy,cx+bx*rx-by*ux,cy+bx*ry-by*uy,width,color);
    }
    private void arm(float sx,float sy,float ex,float ey,float wx,float wy,int color,float open,float wrist){
        bone(sx,sy,ex,ey,UPPER_ARM,color);bone(ex,ey,wx,wy,FOREARM,color);
        // Cover the join as the forearm turns, so the elbow has a continuous outline.
        oval(ex-6.5f,ey-6.5f,13,13,color);
        float dx=wx-ex,dy=wy-ey,len=Math.max(.001f,(float)Math.hypot(dx,dy)),ux=dx/len,uy=dy/len;
        double r=Math.toRadians(wrist);float tx=ux;
        ux=tx*(float)Math.cos(r)-uy*(float)Math.sin(r);uy=tx*(float)Math.sin(r)+uy*(float)Math.cos(r);
        float[] hand=new float[FIST.length];
        for(int i=0;i<hand.length;i+=2){float a=FIST[i],b=FIST[i+1];
            hand[i]=wx+(a*ux-b*uy)*(1-open)+PALM[i]*open;
            hand[i+1]=wy+(a*uy+b*ux)*(1-open)+PALM[i+1]*open;
        }
        // A polygon preserves the planted palm's perfectly flat contact edge.
        if(open==0)smooth(hand,color);else polygon(hand,color);
    }
    private void leg(float sx,float sy,float kx,float ky,float fx,float fy,int color,float angle,float patella,boolean backLeg){
        // Ankle continues beneath the trouser hem into the foot in every pose.
        line(kx,ky,fx,fy,8,color);
        groundedCloth=fy>=-10;
        bone(sx,sy,kx,ky,backLeg?ReferenceSilhouette.BACK_THIGH:ReferenceSilhouette.FRONT_THIGH,color);
        bone(kx,ky,fx,fy,backLeg?ReferenceSilhouette.BACK_CALF:ReferenceSilhouette.FRONT_CALF,color);
        // The fabric folds around the knee rather than pinching between two segments.
        if(segmented){
            double r=Math.toRadians(patella);float ux=(float)Math.cos(r),uy=(float)Math.sin(r);
            float[] cap=new float[24];
            for(int i=0;i<12;i++){
                double a=i*Math.PI/6;float along=2+(referenceStance?4:8)*(float)Math.cos(a),across=(referenceStance?4:6)*(float)Math.sin(a);
                cap[i*2]=kx+along*ux-across*uy;cap[i*2+1]=ky+along*uy+across*ux;
            }
            smooth(cap,color);
        }else oval(kx-9.2f,ky-9.2f,18.4f,18.4f,color);
        groundedCloth=false;
        double r=Math.toRadians(angle);float c=(float)Math.cos(r),s=(float)Math.sin(r);float[] foot=new float[FOOT.length];
        for(int i=0;i<foot.length;i+=2){float toe=FOOT[i]*(backLeg&&referenceStance?-1:1);foot[i]=fx+toe*c-FOOT[i+1]*s;foot[i+1]=fy+toe*s+FOOT[i+1]*c;}
        polygon(foot,color);
    }
    static float footAngle(float kx,float ky,float fx,float fy){
        double a=Math.atan2(fy-ky,fx-kx)-Math.PI/2;
        // Longer anatomical toes need room to level out before reaching the floor.
        return (float)Math.toDegrees(Math.atan2(Math.sin(a),Math.cos(a)))*(1-Combat.smooth((fy+40)/34));
    }
    static double footBottom(float fy,float angle){
        double r=Math.toRadians(angle),bottom=-Double.MAX_VALUE;
        for(int i=0;i<FOOT.length;i+=2)bottom=Math.max(bottom,fy+FOOT[i]*Math.sin(r)+FOOT[i+1]*Math.cos(r));
        return bottom;
    }
    private void head(float nx,float ny){
        float[] outline=HEAD.clone();for(int i=0;i<outline.length;i+=2){outline[i]+=nx;outline[i+1]+=ny;}smooth(outline,SKIN);
    }
    private void bone(float ax,float ay,float bx,float by,float[] profile,int color){
        float dx=bx-ax,dy=by-ay,len=(float)Math.hypot(dx,dy);if(len<.001f)return;
        float nx=-dy/len,ny=dx/len;float[] nodes=new float[profile.length];
        for(int i=0;i<nodes.length;i+=2){nodes[i]=ax+profile[i]*dx+profile[i+1]*nx;nodes[i+1]=ay+profile[i]*dy+profile[i+1]*ny;}
        smooth(nodes,color);
    }
    private void line(float ax,float ay,float bx,float by,float width,int color){p.line(x+d*ax,y+ay,x+d*bx,y+by,width,color);}
    private void oval(float ax,float ay,float w,float h,int color){p.oval(x+(d>0?ax:-ax-w),y+ay,w,h,color);}
    private void polygon(float[] pts,int color){for(int i=0;i<pts.length;i+=2){pts[i]=x+d*pts[i];pts[i+1]+=y;}p.poly(pts,color);}
    private void smooth(float[] nodes,int color){
        int n=nodes.length/2;float[] points=new float[n*10];
        for(int i=0;i<n;i++)for(int s=0;s<5;s++){float t=s/5f;for(int k=0;k<2;k++){
            float a=nodes[((i+n-1)%n)*2+k],b=nodes[i*2+k],c=nodes[((i+1)%n)*2+k],e=nodes[((i+2)%n)*2+k];
            points[(i*5+s)*2+k]=.5f*((2*b)+(-a+c)*t+(2*a-5*b+4*c-e)*t*t+(-a+3*b-3*c+e)*t*t*t);
        }}if(groundedCloth)for(int i=1;i<points.length;i+=2)points[i]=Math.min(0,points[i]);polygon(points,color);
    }
}
