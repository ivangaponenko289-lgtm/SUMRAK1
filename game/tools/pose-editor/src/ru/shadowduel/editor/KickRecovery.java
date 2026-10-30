package ru.shadowduel.editor;

import ru.shadowduel.*;
import java.io.File;
import java.util.Locale;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Reproducible recovery edit; the input is kept as an untouched user backup. */
public final class KickRecovery {
    static double hermite(double a,double b,double va,double vb,double time,double duration){
        double t=Math.max(0,Math.min(1,time/duration)),t2=t*t,t3=t2*t;
        return (2*t3-3*t2+1)*a+(t3-2*t2+t)*duration*va+(-2*t3+3*t2)*b+(t3-t2)*duration*vb;
    }
    static double smooth(double t){t=Math.max(0,Math.min(1,t));return t*t*t*(t*(t*6-15)+10);}
    static double follow(double a,double b,double t){return a+(b-a)*smooth(t);}
    public static AnimationClip refine(AnimationClip source){
        if(source.duration!=41||source.version!=3)throw new IllegalArgumentException("This edit targets the user's 41-frame v3 kick");
        AnimationClip result=source.copy();
        AnimationClip.PoseData start=AnimationRuntime.sampleAnimation(source,16),previous=AnimationRuntime.sampleAnimation(source,15),ready=AnimationRuntime.sampleAnimation(source,41);
        float[] first=AnimationRig.joints(start),prior=AnimationRig.joints(previous),last=AnimationRig.joints(ready);
        double firstFootAngle=AnimationRig.absoluteAngles(start)[12];
        result.keys.removeIf(k->k.frame>16);
        for(int frame=17;frame<=41;frame++){
            AnimationClip.PoseData p=start.copy();double upper=(frame-16)/25.0;
            p.name="Возврат · "+frame;
            for(int b=0;b<19;b++)p.angles[b]=follow(start.angles[b],AnimationRig.near(ready.angles[b],start.angles[b]),upper);
            p.spineBend=follow(start.spineBend,ready.spineBend,upper);
            p.shoulderTurn=follow(start.shoulderTurn,ready.shoulderTurn,upper);
            if(frame<=32){
                p.x=hermite(start.x,-4,start.x-previous.x,0,frame-16,16);
                p.y=hermite(start.y,-101,start.y-previous.y,0,frame-16,16);
            }else if(frame<=35){
                // Contact is followed by a small knee compression and forward load.
                p.x=follow(-4,-1.5,(frame-32)/3.0);p.y=follow(-101,-98.8,(frame-32)/3.0);
            }else{
                p.x=follow(-1.5,ready.x,(frame-35)/6.0);p.y=follow(-98.8,ready.y,(frame-35)/6.0);
            }
            double fx,fy;
            if(frame<=24){
                fx=hermite(first[16],64,first[16]-prior[16],-4,frame-16,8);
                fy=hermite(first[17],-50,first[17]-prior[17],6,frame-16,8);
            }else{
                fx=hermite(64,last[16],-4,0,frame-24,8);
                fy=hermite(-50,last[17],6,0,frame-24,8);
            }
            AnimationRig.ik(p,7,fx,fy,false);
            AnimationRig.ik(p,9,last[20],last[21],false);
            double[] a=AnimationRig.absoluteAngles(p);
            p.angles[12]=follow(firstFootAngle,0,(frame-16)/16.0)-a[7];
            p.angles[13]=-a[9];
            if(frame==41)p=ready.copy();
            String id="recovery-028-"+frame;result.poses.put(id,p);result.keys.add(new AnimationClip.Key(frame,id));
        }
        return result;
    }
    static void verify(AnimationClip before,AnimationClip after){
        for(int i=0;i<=64;i++){
            double frame=i*.25;
            String a=AnimationJson.encodePose(AnimationRuntime.sampleAnimation(before,frame));
            String b=AnimationJson.encodePose(AnimationRuntime.sampleAnimation(after,frame));
            if(!a.equals(b))throw new AssertionError("User's strike changed at "+frame);
        }
        if(!AnimationValidation.validate(after,60).isEmpty())throw new AssertionError("Invalid recovery: "+AnimationValidation.validate(after,60));
        double peakBefore=0,peakAfter=0,hipBefore=0,hipAfter=0;
        float[] priorBefore=null,priorAfter=null;
        for(int frame=16;frame<=41;frame++){
            AnimationClip.PoseData p=AnimationRuntime.sampleAnimation(after,frame);
            float[] a=AnimationRig.joints(AnimationRuntime.sampleAnimation(before,frame)),b=AnimationRig.joints(p);
            if(priorBefore!=null){peakBefore=Math.max(peakBefore,Math.abs(a[17]-priorBefore[17]));peakAfter=Math.max(peakAfter,Math.abs(b[17]-priorAfter[17]));hipBefore=Math.max(hipBefore,Math.hypot(a[0]-priorBefore[0],a[1]-priorBefore[1]));hipAfter=Math.max(hipAfter,Math.hypot(b[0]-priorAfter[0],b[1]-priorAfter[1]));}
            if(Math.abs(b[20]+42)>.01||Math.abs(b[21]+6)>.01)throw new AssertionError("Support foot drift");
            if(frame>=32&&(Math.abs(b[16]-40)>.01||Math.abs(b[17]+6)>.01))throw new AssertionError("Landing foot drift");
            priorBefore=a;priorAfter=b;
        }
        if(peakAfter>=peakBefore*.6||hipAfter>=hipBefore*.6)throw new AssertionError("Recovery is still abrupt");
        float[] end=AnimationRig.joints(AnimationRuntime.sampleAnimation(after,41)),original=AnimationRig.joints(AnimationRuntime.sampleAnimation(before,41));
        for(int i=0;i<end.length;i++)if(Math.abs(end[i]-original[i])>.001)throw new AssertionError("Finish pose changed");
        Combat game=new Combat();game.start();game.countdown=0;game.botEnabled=false;game.enemy.x=1200;
        EditableAnimations.install(game,2,after);game.attack(game.hero,2);Pose rendered=new Pose();float[] previous=null;
        for(int frame=0;frame<48;frame++){
            rendered.sample(game.hero,game.elapsed);
            if(previous!=null&&frame>=18)for(int j=0;j<22;j+=2)
                if(Math.hypot(rendered.joints[j]-previous[j],rendered.joints[j+1]-previous[j+1])>20)
                    throw new AssertionError("Game recovery/idle transition jumps at "+frame+" joint "+j);
            previous=rendered.joints.clone();game.tick(1f/60);
        }
        System.out.printf(Locale.ROOT,"PASS: strike 0-16 unchanged; feet planted; max foot fall %.2f -> %.2f, hip step %.2f -> %.2f units/frame%n",peakBefore,peakAfter,hipBefore,hipAfter);
    }
    static void preview(AnimationClip before,AnimationClip after,File dir)throws Exception{
        dir.mkdirs();
        for(int frame=0;frame<=60;frame++){
            BufferedImage image=new BufferedImage(800,340,BufferedImage.TYPE_INT_RGB);Graphics2D all=image.createGraphics();all.setColor(new Color(0xe3e4df));all.fillRect(0,0,800,340);
            for(int col=0;col<2;col++){
                Graphics2D g=(Graphics2D)all.create();g.translate(col*400,0);g.setColor(new Color(0x263b36));g.setFont(new Font("Segoe UI",Font.PLAIN,16));g.drawString(col==0?"До":"После — опускание и перенос веса",16,25);
                g.translate(170,310);g.scale(1.3,1.3);CanvasPainter painter=new CanvasPainter(g);painter.line(-100,0,160,0,1,0xffb66e4d);
                new HumanRenderer().drawPose(painter,0,0,AnimationRuntime.sampleAnimation(col==0?before:after,Math.min(frame,41)));g.dispose();
            }
            all.dispose();ImageIO.write(image,"png",new File(dir,String.format(Locale.ROOT,"%03d.png",frame)));
        }
        AnimationFiles.contactSheet(after,new File(dir,"contact.png"),3,6);
    }
    public static void main(String[] args)throws Exception{
        if(args[0].equals("--verify")){verify(AnimationFiles.load(new File(args[1])),AnimationFiles.load(new File(args[2])));return;}
        AnimationClip clip=AnimationFiles.load(new File(args[0]));
        if(args.length==3){
            AnimationClip refined=refine(clip);verify(clip,refined);preview(clip,refined,new File(args[2]));
            AnimationFiles.save(refined,new File(args[1]));return;
        }
        for(int frame:new int[]{15,16,17,18,21,24,25,28,30,41}){
            AnimationClip.PoseData p=AnimationRuntime.sampleAnimation(clip,frame);float[] j=AnimationRig.joints(p);
            double[] a=AnimationRig.absoluteAngles(p);
            System.out.printf(Locale.ROOT,"%d hip %.2f %.2f foot %.2f %.2f angle %.2f support %.2f %.2f%n",frame,p.x,p.y,j[16],j[17],a[12],j[20],j[21]);
        }
        System.out.println("Validation: "+AnimationValidation.validate(clip,60));
    }
}

