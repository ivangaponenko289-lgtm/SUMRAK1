package ru.shadowduel;

import java.util.*;

/** Validates the sampled frames, not merely the stored key poses. */
public final class AnimationValidation {
    private AnimationValidation(){}
    public static final class Issue {
        public final String code,message;public final int frame;
        public Issue(String code,int frame,String message){this.code=code;this.frame=frame;this.message=message;}
        public String toString(){return (frame<0?"Данные":"Кадр "+frame)+" · "+message;}
    }
    private static void add(List<Issue> issues,String code,int frame,String text){if(issues.size()<500)issues.add(new Issue(code,frame,text));}
    public static List<Issue> structure(AnimationClip a){
        List<Issue> out=new ArrayList<>();
        if((a.version!=1&&a.version!=2&&a.version!=3)||a.fps!=60||a.duration<1||a.duration>3600)add(out,"format",-1,"Нужны версия 1–3, 60 fps, длительность 1–3600 кадров");
        if(!finite(a.walkDistance)||a.walkDistance<0||a.walkDistance>500)add(out,"stride",-1,"Некорректная длина шага");
        if(!finite(a.playbackSpeed)||a.playbackSpeed<.25||a.playbackSpeed>2)add(out,"speed",-1,"Скорость должна быть от 0,25 до 2,00");
        if(!limited(10000,a.left,a.top,a.right,a.bottom)||a.left>=a.right||a.top>=a.bottom)add(out,"bounds",-1,"Некорректные границы кадра (предел ±10000)");
        if(a.startupEnd<0||a.activeEnd<a.startupEnd||a.activeEnd>a.duration)add(out,"phases",-1,"Фазы должны идти startup → active → recovery внутри длительности");
        if(a.poses.isEmpty()||a.keys.isEmpty())add(out,"keys",-1,"Нужны хотя бы одна поза и один ключ");
        for(Map.Entry<String,AnimationClip.PoseData> e:a.poses.entrySet()){
            AnimationClip.PoseData p=e.getValue();
            if(p!=null){if(p.projection==null||p.projection.length!=19)add(out,"projection",-1,"Некорректная проекция");else for(double v:p.projection)if(!finite(v)||v<.4||v>1.5)add(out,"projection",-1,"Проекция вне диапазона");}
            if(p==null||p.angles==null||p.angles.length!=19||a.version>=2&&!p.extended||a.version==3&&!p.segmented||!limited(10000,p.x,p.y)||!limited(30,p.spineBend,p.shoulderTurn)||!finite(p.socketWeight)||!limited(100000,p.angles)||p.socketWeight<0||p.socketWeight>1||(p.facing!=1&&p.facing!=-1))add(out,"pose",-1,"Некорректная поза: "+e.getKey());
        }
        Set<Integer> frames=new HashSet<>();for(AnimationClip.Key k:a.keys){if(k.frame<0||k.frame>a.duration||!frames.add(k.frame)||!a.poses.containsKey(k.pose)||!Arrays.asList(AnimationRuntime.EASINGS).contains(k.easing))add(out,"key",k.frame,"Некорректный / повторный ключ, ссылка на позу или easing");}
        for(AnimationClip.Box b:a.boxes){if(b.bone<0||b.bone>=19||!Arrays.asList("hit","hurt").contains(b.kind)||!Arrays.asList("circle","rect").contains(b.shape)||b.start<0||b.end<b.start||b.end>a.duration||!limited(5,b.along)||!limited(1000,b.x,b.y,b.width,b.height)||!limited(3600,b.rotation)||!limited(500,b.radius)||!limited(100000,b.damage)||b.radius<=0||b.width<=0||b.height<=0||b.damage<0)add(out,"box",-1,"Некорректный объём: "+b.name);}
        return out;
    }
    private static boolean finite(double... values){for(double v:values)if(!AnimationRig.finite(v))return false;return true;}
    private static boolean limited(double limit,double... values){for(double v:values)if(!AnimationRig.finite(v)||Math.abs(v)>limit)return false;return true;}
    public static List<Issue> validate(AnimationClip a,double maxAngle){
        List<Issue> out=structure(a);if(!finite(maxAngle)||maxAngle<=0)add(out,"threshold",-1,"Порог угла должен быть положительным");if(!out.isEmpty())return out;
        double[] previous=null;HumanRenderer renderer=new HumanRenderer();
        for(int frame=0;frame<=a.duration;frame++){
            AnimationClip.PoseData p=AnimationRuntime.sampleAnimation(a,frame);float[] j=AnimationRig.joints(p);
            for(Issue issue:checkLengths(p,j,frame))add(out,issue.code,frame,issue.message);
            if(!a.airborne&&(footBottom(p,j,16)>.15||footBottom(p,j,20)>.15))add(out,"ground",frame,"Стопа ниже линии земли");
            Bounds bounds=new Bounds();renderer.drawPose(bounds,0,0,p);
            if(bounds.left<a.left-.01||bounds.top<a.top-.01||bounds.right>a.right+.01||bounds.bottom>a.bottom+.01)add(out,"frame",frame,"Силуэт выходит за границы кадра");
            double[] angles=AnimationRig.absoluteAngles(p);if(p.facing<0)for(int b=0;b<(p.segmented?19:14);b++)angles[b]=180-angles[b];
            if(previous!=null)for(int b=0;b<(p.segmented?19:14);b++)if(Math.abs(angles[b]-previous[b])>maxAngle)add(out,"angle",frame,AnimationRig.LABELS[b]+": скачок "+Math.round(Math.abs(angles[b]-previous[b]))+"°");previous=angles;
        }return out;
    }
    public static List<Issue> checkLengths(AnimationClip.PoseData p,float[] j,int frame){List<Issue> out=new ArrayList<>();for(int b=0;b<(p.segmented?19:14);b++){double[] s=AnimationRig.start(p,j,b);int e=AnimationRig.END[b];double len=Math.hypot(j[e]-s[0],j[e+1]-s[1]);if(!AnimationRig.finite(len)||Math.abs(len-AnimationRig.length(p,b))>.01)add(out,"length",frame,"Изменилась длина: "+AnimationRig.LABELS[b]);}return out;}
    private static double footBottom(AnimationClip.PoseData p,float[] j,int foot){float angle=p.extended?(float)AnimationRig.absoluteAngles(p)[foot==16?12:13]:HumanRenderer.footAngle(j[foot-2],j[foot-1],j[foot],j[foot+1]);if(foot==20&&p.projection[8]!=1)angle=-angle;return HumanRenderer.footBottom(j[foot+1],angle);}
    /** Measures the actual renderer primitives, including cloth, toes, head rotation and stroke width. */
    public static final class Bounds implements Painter {
        public double left=Double.POSITIVE_INFINITY,top=Double.POSITIVE_INFINITY,right=Double.NEGATIVE_INFINITY,bottom=Double.NEGATIVE_INFINITY;
        private double[] m={1,0,0,1,0,0};private final Deque<double[]> stack=new ArrayDeque<>();
        public void reset(){left=top=Double.POSITIVE_INFINITY;right=bottom=Double.NEGATIVE_INFINITY;stack.clear();m=new double[]{1,0,0,1,0,0};}
        public void transform(float scale,float x,float y){stack.push(m.clone());double[] o=m;m=new double[]{o[0]*scale,o[1]*scale,o[2]*scale,o[3]*scale,o[0]*x+o[2]*y+o[4],o[1]*x+o[3]*y+o[5]};}
        private void point(double x,double y,double r){double xx=m[0]*x+m[2]*y+m[4],yy=m[1]*x+m[3]*y+m[5],rx=r*Math.hypot(m[0],m[2]),ry=r*Math.hypot(m[1],m[3]);left=Math.min(left,xx-rx);right=Math.max(right,xx+rx);top=Math.min(top,yy-ry);bottom=Math.max(bottom,yy+ry);}
        public void rotate(float degrees,float x,float y){stack.push(m.clone());double r=Math.toRadians(degrees),c=Math.cos(r),s=Math.sin(r),tx=x-c*x+s*y,ty=y-s*x-c*y;double[] o=m;m=new double[]{o[0]*c+o[2]*s,o[1]*c+o[3]*s,-o[0]*s+o[2]*c,-o[1]*s+o[3]*c,o[0]*tx+o[2]*ty+o[4],o[1]*tx+o[3]*ty+o[5]};}
        public void restore(){m=stack.pop();}public void background(float seconds){}
        public void rect(float x,float y,float w,float h,int color){point(x,y,0);point(x+w,y,0);point(x,y+h,0);point(x+w,y+h,0);}
        public void oval(float x,float y,float w,float h,int color){double cx=m[0]*(x+w/2)+m[2]*(y+h/2)+m[4],cy=m[1]*(x+w/2)+m[3]*(y+h/2)+m[5];double rx=Math.hypot(m[0]*w/2,m[2]*h/2),ry=Math.hypot(m[1]*w/2,m[3]*h/2);left=Math.min(left,cx-rx);right=Math.max(right,cx+rx);top=Math.min(top,cy-ry);bottom=Math.max(bottom,cy+ry);}
        public void line(float x,float y,float xx,float yy,float width,int color){point(x,y,width/2);point(xx,yy,width/2);}
        public void poly(float[] points,int color){for(int i=0;i<points.length;i+=2)point(points[i],points[i+1],0);}
        public void text(String text,float x,float y,float size,int color,boolean center){}
    }
}
