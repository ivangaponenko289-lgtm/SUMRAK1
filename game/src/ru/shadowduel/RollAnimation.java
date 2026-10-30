package ru.shadowduel;

/** Reference: user video_2026-10-27_17-03-28.mp4, first forward roll.
 * Open, staggered legs carry across the back, then a foot reaches forward.
 * A continuous spine angle carries momentum through the shoulder/back contact. */
final class RollAnimation {
    private static final HumanRenderer SUPPORT_RENDERER=new HumanRenderer();
    private static final AnimationValidation.Bounds SUPPORT_BOUNDS=new AnimationValidation.Bounds();
    private static final float[] TIMES={0,.10f,.20f,.32f,.44f,.56f,.68f,.78f,.86f,1};
    private static final float[][] KEYS={
        Pose.READY,
        {-8,-91,16,-135,31,-160, 40,-113,49,-92,-15,-111,4,-113, 30,-45,40,-6,-36,-44,-42,-6},
        {-10,-70,29,-73,45,-71, 49,-43,58,-8,14,-45,36,-10, 30,-28,12,-6,-58,-37,-64,-6},
        {-8,-80,16,-34,20,-25, 13,-16,-14,-8,3,-34,-29,-13, -48,-46,-99,-46,-61,-80,-61,-131},
        {8,-58,-25,-26,-43,-34, -12,-50,8,-59,-37,-46,-18,-62, -30,-98,10,-119,-17,-101,18,-133},
        {15,-44,-26,-24,-49,-39, -7,-52,17,-67,-45,-47,-39,-73, -11,-90,38,-103,-28,-81,9,-118},
        {-8,-38,-28,-85,-15,-115, 5,-84,18,-108,-40,-61,-32,-23, 10,-77,65,-6,-17,-75,12,-38},
        {-8,-65,-11,-117,0,-145, 26,-99,51,-117,-28,-98,-7,-122, 39,-52,46,-6,-29,-25,-36,-6},
        {-8,-92,-1,-144,3,-172, 29,-122,53,-141,-23,-121,2,-145, 30,-45,40,-6,-29,-43,-42,-6},
        Pose.READY
    };
    private static final float[][] SPINE=new float[TIMES.length][1];
    private static final float[][] BODY=new float[TIMES.length][14];
    private static final float[] LEG_TIMES={.20f,.32f,.44f,.56f,.68f,.78f,.86f};
    // Unwrapped angles: each thigh sweeps in one direction through the roll.
    private static final float[][] LEG_ANGLES={
        {0,0,0,0}, {140,180,180,270}, {220,310,185,275},
        {265,360,230,320}, {333,410,295,390}, {385,470,390,495}, {0,0,0,0}
    };
    static {
        for(int key=0;key<TIMES.length;key++){
            float[] j=KEYS[key];
            float angle=(float)Math.toDegrees(Math.atan2(j[3]-j[1],j[2]-j[0]));
            SPINE[key][0]=key==0?angle:unwrap(angle,SPINE[key-1][0]);
            double radians=Math.toRadians(angle);
            float c=(float)Math.cos(radians),s=(float)Math.sin(radians);
            for(int i=2;i<14;i+=2){
                float x=j[i]-j[0],y=j[i+1]-j[1];
                BODY[key][i]=x*c+y*s;BODY[key][i+1]=-x*s+y*c;
            }
        }
        // Even angular progress across the back; Cartesian interpolation had
        // only 18 degrees here followed by a 93-degree catch-up in the next span.
        SPINE[4][0]=129;SPINE[5][0]=196;SPINE[6][0]=260;
        // Carry the chest over the leading foot before opening into the guard.
        SPINE[7][0]=296;SPINE[8][0]=285;
        for(int leg=0;leg<2;leg++){
            float[] entry=angles(KEYS[2],leg),exit=angles(KEYS[8],leg);
            for(int bone=0;bone<2;bone++){
                int index=leg*2+bone;
                LEG_ANGLES[0][index]=unwrap(entry[bone],LEG_ANGLES[1][index]);
                LEG_ANGLES[6][index]=unwrap(exit[bone],LEG_ANGLES[5][index]);
            }
        }
    }
    private static final float[][] TRAVEL={{0},{0},{12},{84},{135},{182},{215},{234},{240},{240}};
    static float travel(Combat.Fighter f,float seconds){
        if(f.rollAnimation!=null)return (float)(AnimationRuntime.sampleAnimation(f.rollAnimation,seconds*60).x-AnimationRuntime.sampleAnimation(f.rollAnimation,0).x);
        float phase=Combat.clamp(seconds/Combat.ROLL_DURATION,0,1);
        boolean backward=f.rollDirection*f.facing<0;
        return backward?Combat.ROLL_DISTANCE-curve(TIMES,TRAVEL,1-phase,0):curve(TIMES,TRAVEL,phase,0);
    }
    static void sample(Combat.Fighter f,float clock,float[] out){
        boolean backward=f.rollDirection*f.facing<0;
        float phase=Combat.clamp(f.rollTime/Combat.ROLL_DURATION,0,1);
        float time=backward?1-phase:phase;
        for(int i=0;i<22;i++)out[i]=curve(TIMES,KEYS,time,i);
        double radians=Math.toRadians(curve(TIMES,SPINE,time,0));
        float c=(float)Math.cos(radians),s=(float)Math.sin(radians);
        for(int i=2;i<14;i+=2){
            float x=curve(TIMES,BODY,time,i),y=curve(TIMES,BODY,time,i+1);
            out[i]=out[0]+x*c-y*s;out[i+1]=out[1]+x*s+y*c;
        }
        float entry=1-Combat.smooth(phase/.10f),exit=Combat.smooth((phase-.86f)/.14f);
        for(int i=0;i<22;i++)out[i]+=(f.rollStart[i]-Pose.READY[i])*entry;
        for(int i=3;i<14;i+=2)out[i]+=(float)Math.sin(clock*2.7f)*.75f*exit;
        float nominal=curve(TIMES,TRAVEL,time,0)-(backward?Combat.ROLL_DISTANCE:0);
        float actual=(f.x-f.rollOriginX)*f.facing;
        // World-space anchors prevent planted palms/soles sliding on the terrace.
        // Limit correction if a wall prevents the intended travel.
        float root=nominal+Combat.clamp(actual-nominal,-24,24);
        float offset=backward?-Combat.ROLL_DISTANCE:0;
        anchor(out,8,70+offset-root,-8,window(time,.17f,.20f,.32f,.40f));
        anchor(out,12,48+offset-root,-8,window(time,.17f,.20f,.28f,.36f));
        out[9]=Math.min(out[9],-8);out[13]=Math.min(out[13],-8);
        float dx=out[2]-out[0],dy=out[3]-out[1],len=(float)Math.hypot(dx,dy);
        float ux=dx/len,uy=dy/len;
        float weight=Math.min(Combat.smooth(time/.10f),Combat.smooth((1-time)/.14f));
        float rx=1+(-uy-1)*weight,ry=ux*weight;ux*=weight;uy=-1+(uy+1)*weight;
        for(int leg=0;leg<2;leg++){
            float hx=out[0]+rx*(leg==0?6:-8)-ux*(leg==0?8:3);
            float hy=out[1]+ry*(leg==0?6:-8)-uy*(leg==0?8:3);
            int knee=leg==0?14:18,foot=knee+2;
            if(time>.20f&&time<.86f){
                double thigh=Math.toRadians(curve(LEG_TIMES,LEG_ANGLES,time,leg*2));
                double shin=Math.toRadians(curve(LEG_TIMES,LEG_ANGLES,time,leg*2+1));
                out[knee]=hx+53*(float)Math.cos(thigh);out[knee+1]=hy+53*(float)Math.sin(thigh);
                out[foot]=out[knee]+51*(float)Math.cos(shin);out[foot+1]=out[knee+1]+51*(float)Math.sin(shin);
                if(out[foot+1]>-6){out[foot+1]=-6;solve(out,hx,hy,knee,foot);}
            }else solve(out,hx,hy,knee,foot);
            float entering=1-Combat.smooth((time-(leg==0?.10f:.16f))/.10f);
            float leaving=Combat.smooth((time-(leg==0?.62f:.70f))/(leg==0?.10f:.14f));
            float planted=leg==0?40:-42;
            if(entering>0){anchor(out,foot,planted+offset-root,-6,entering);solve(out,hx,hy,knee,foot);}
            if(leaving>0){anchor(out,foot,Combat.ROLL_DISTANCE+planted+offset-root,-6,leaving);solve(out,hx,hy,knee,foot);}

        }
        float[] relaxed=Pose.READY.clone();relaxed[12]+=18;
        for(int i=3;i<14;i+=2)relaxed[i]+=(float)Math.sin(clock*2.7f)*.75f;
        Pose.solveArms(relaxed,0);
        for(int i=6;i<14;i++)out[i]+=(relaxed[i]-out[i])*exit;
        // Project the complete pose onto its lowest support instead of merely
        // forbidding penetration. Contact passes from hand to shoulder to hip.
        // Use the actual anatomical outline: the old joint radii described the
        // larger head and capsule limbs and would leave the new body floating.
        // Re-evaluate as palms flatten and ankles settle near the ground.
        for(int pass=0;pass<6;pass++){
            SUPPORT_BOUNDS.reset();SUPPORT_RENDERER.drawPose(SUPPORT_BOUNDS,0,0,1,out,weight,clock);
            float bottom=(float)SUPPORT_BOUNDS.bottom;
            if(Math.abs(bottom)<.001f)break;
            for(int i=1;i<22;i+=2)out[i]-=bottom;
        }
    }
    private static float window(float t,float in0,float in1,float out0,float out1){
        return Combat.smooth((t-in0)/(in1-in0))*(1-Combat.smooth((t-out0)/(out1-out0)));
    }
    private static void anchor(float[] j,int index,float x,float y,float amount){
        j[index]+=(x-j[index])*amount;j[index+1]+=(y-j[index+1])*amount;
    }
    private static float curve(float[] times,float[][] values,float t,int joint){
        int k=1;while(k<times.length-1&&t>times[k])k++;
        float h=times[k]-times[k-1],u=Combat.clamp((t-times[k-1])/h,0,1);
        float a=values[k-1][joint],b=values[k][joint],slope=(b-a)/h;
        float m0=k==1?0:tangent((a-values[k-2][joint])/(times[k-1]-times[k-2]),slope);
        float m1=k==times.length-1?0:tangent(slope,(values[k+1][joint]-b)/(times[k+1]-times[k]));
        float u2=u*u,u3=u2*u;
        return (2*u3-3*u2+1)*a+(u3-2*u2+u)*h*m0+(-2*u3+3*u2)*b+(u3-u2)*h*m1;
    }
    private static float tangent(float a,float b){return a*b<=0?0:2*a*b/(a+b);}
    private static float unwrap(float angle,float near){return near+(float)Math.toDegrees(Math.atan2(Math.sin(Math.toRadians(angle-near)),Math.cos(Math.toRadians(angle-near))));}
    private static float[] angles(float[] key,int leg){
        float[] j=key.clone();float dx=j[2]-j[0],dy=j[3]-j[1],len=(float)Math.hypot(dx,dy);
        float ux=dx/len,uy=dy/len,rx=-uy,ry=ux;
        float hx=j[0]+rx*(leg==0?6:-8)-ux*(leg==0?8:3),hy=j[1]+ry*(leg==0?6:-8)-uy*(leg==0?8:3);
        int knee=leg==0?14:18,foot=knee+2;solve(j,hx,hy,knee,foot);
        return new float[]{(float)Math.toDegrees(Math.atan2(j[knee+1]-hy,j[knee]-hx)),
            (float)Math.toDegrees(Math.atan2(j[foot+1]-j[knee+1],j[foot]-j[knee]))};
    }
    private static void solve(float[] j,float hx,float hy,int knee,int foot){
        float dx=j[foot]-hx,dy=j[foot+1]-hy,actual=(float)Math.hypot(dx,dy);
        if(actual<.001f)return;
        float distance=Combat.clamp(actual,2.01f,103.99f),along=(53*53-51*51+distance*distance)/(2*distance);
        float bend=(float)Math.sqrt(Math.max(0,53*53-along*along));
        j[knee]=hx+dx/actual*along+dy/actual*bend;j[knee+1]=hy+dy/actual*along-dx/actual*bend;
    }
}
