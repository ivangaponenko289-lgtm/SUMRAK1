package ru.shadowduel;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Samples the original procedural forward roll; outputs angular keys, never sprites. */
public final class ImportForwardRoll {
    private ImportForwardRoll(){}
    public static AnimationClip convert(int step){
        int duration=Math.round(Combat.ROLL_DURATION*60);
        if(step<1||step>duration)throw new IllegalArgumentException("Шаг N должен быть 1…"+duration);
        AnimationClip a=new AnimationClip();a.name="Forward roll";a.duration=duration;a.startupEnd=(int)Math.ceil(duration*.20);a.activeEnd=(int)Math.floor(duration*.74)+1;
        Combat g=new Combat();Combat.Fighter f=g.hero;f.x=f.rollOriginX=0;f.facing=1;f.rollDirection=1;f.plantFeet();Pose initial=new Pose();initial.sample(f,0);System.arraycopy(initial.joints,0,f.rollStart,0,22);f.rolling=true;
        AnimationClip.PoseData previous=null;float[][] original=new float[duration+1][];
        double projectionSquared=0,projectionMax=0;int projectedPoints=0;
        HumanRenderer renderer=new HumanRenderer();
        // Visit every tick to unwrap angles continuously, even when a large N skips a full turn.
        for(int frame=0;frame<=duration;frame++){
            f.rollTime=frame/60f;f.x=RollAnimation.travel(f,f.rollTime);float[] j=new float[22];RollAnimation.sample(f,0,j);
            for(int i=0;i<22;i+=2)j[i]+=f.x;original[frame]=j.clone();
            double t=frame/(double)duration,weight=Math.min(Combat.smooth((float)t/.10f),Combat.smooth((float)(1-t)/.14f));
            AnimationClip.PoseData p=AnimationRig.fromJoints(j,weight,previous);p.name=String.format(Locale.ROOT,"Forward #%02d",frame);
            // Preserve hand/foot targets wherever the fixed-length rig can reach them.
            for(int end:new int[]{3,5,7,9}){int joint=AnimationRig.END[end];AnimationRig.ik(p,end,j[joint],j[joint+1],false);}
            AnimationValidation.Bounds bounds=new AnimationValidation.Bounds();float[] normalized=AnimationRig.joints(p);renderer.drawPose(bounds,0,0,1,normalized,(float)weight,0);
            double lift=Math.max(0,Math.max(bounds.bottom,Math.max(normalized[17]+6,normalized[21]+6)));p.y-=lift;
            previous=p;
            if(frame%step==0||frame==duration){
                String id="forward-"+frame;a.poses.put(id,p);a.keys.add(new AnimationClip.Key(frame,id));
                float[] actual=AnimationRig.worldJoints(p);for(int i=0;i<22;i+=2){double distance=Math.hypot(actual[i]-j[i],actual[i+1]-j[i+1]);projectionSquared+=distance*distance;projectionMax=Math.max(projectionMax,distance);projectedPoints++;}
            }
        }
        double squared=0,max=0;int count=0;
        for(int frame=0;frame<=duration;frame++){
            float[] j=AnimationRig.worldJoints(AnimationRuntime.sampleAnimation(a,frame)),source=original[frame];
            for(int i=0;i<22;i+=2){double d=Math.hypot(j[i]-source[i],j[i+1]-source[i+1]);squared+=d*d;max=Math.max(max,d);count++;}
        }
        int warnings=AnimationValidation.validate(a,30).size();
        a.importReport=String.format(Locale.ROOT,"Импорт исходного forward roll: N=%d, %d ключей, %d кадров. Приведение к постоянным длинам: RMS %.2f, max %.2f ед. Отклонение всех кадров с интерполяцией: RMS %.2f, max %.2f ед. Validate: %d замечаний. Старые руки/корпус меняли длину, поэтому импорт — приближение; исходная игра не заменена.",step,a.keys.size(),duration+1,Math.sqrt(projectionSquared/projectedPoints),projectionMax,Math.sqrt(squared/count),max,warnings);
        return a;
    }
    public static void main(String[] args)throws Exception{
        int step=3;File out=null;
        for(int i=0;i<args.length;i++){
            if(args[i].equals("--step")&&i+1<args.length)step=Integer.parseInt(args[++i]);
            else if(args[i].equals("--out")&&i+1<args.length)out=new File(args[++i]);
            else throw new IllegalArgumentException("Usage: ImportForwardRoll --step N --out animation.json");
        }
        if(out==null)throw new IllegalArgumentException("--out is required");AnimationClip a=convert(step);
        File parent=out.getAbsoluteFile().getParentFile();if(!parent.exists()&&!parent.mkdirs())throw new IOException("Cannot create output directory");
        Files.write(out.toPath(),AnimationJson.encode(a).getBytes(StandardCharsets.UTF_8));System.out.println(a.importReport);System.out.println("Saved: "+out.getAbsolutePath());
    }
}
