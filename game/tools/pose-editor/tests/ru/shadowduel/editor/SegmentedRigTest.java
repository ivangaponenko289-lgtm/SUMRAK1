package ru.shadowduel.editor;

import ru.shadowduel.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Check real joint dependencies and render the new controls side by side. */
public final class SegmentedRigTest {
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    static void same(float[] a,float[] b,int from,int to,String label){
        for(int i=from;i<to;i++)check(Math.abs(a[i]-b[i])<.001,label+" at "+i);
    }
    public static void main(String[] args)throws Exception{
        Combat oldGame=new Combat();Pose oldPose=new Pose();oldPose.sample(oldGame.hero,0);
        AnimationClip legacy=AnimationClip.create();legacy.version=2;legacy.poses.put("ready",AnimationRig.fromPose(oldPose,null));
        legacy=AnimationJson.decode(AnimationJson.encode(legacy));float[] oldJoints=AnimationRig.joints(legacy.poses.get("ready"));
        AnimationRig.upgrade(legacy);same(oldJoints,AnimationRig.joints(legacy.poses.get("ready")),0,22,"v2 upgrade preserves pose");
        AnimationClip clip=EditableAnimations.preset(1);
        AnimationClip.PoseData ready=AnimationRuntime.sampleAnimation(clip,0);
        float[] original=AnimationRig.joints(ready);
        AnimationClip.PoseData nod=ready.copy();nod.angles[16]+=30;
        float[] moved=AnimationRig.joints(nod);
        same(original,moved,0,34,"Head tilt must not move neck, spine or limbs");
        check(Math.hypot(moved[34]-original[34],moved[35]-original[35])>5,"Head handle must rotate");
        AnimationClip.PoseData knee=ready.copy();knee.angles[17]+=45;
        moved=AnimationRig.joints(knee);same(original,moved,0,36,"Patella must preserve leg IK");
        check(Math.abs(moved[36]-original[36])+Math.abs(moved[37]-original[37])>3,"Patella control must move");
        for(int bone:new int[]{14,15,0}){
            AnimationClip.PoseData bend=ready.copy();bend.angles[bone]+=25;
            float[] j=AnimationRig.joints(bend);
            check(Math.hypot(j[2]-original[2],j[3]-original[3])>5,"Each spine joint must bend the chain");
            same(original,j,14,22,"Bending spine must leave legs alone");
            check(AnimationValidation.checkLengths(bend,j,0).isEmpty(),"Bent spine retains bone lengths");
        }
        for(int facing:new int[]{-1,1})for(int angle=-45;angle<=45;angle+=5){
            AnimationClip.PoseData p=ready.copy();p.facing=facing;p.angles[14]+=angle;p.angles[15]-=angle*.6;p.angles[0]+=angle*.3;p.angles[16]+=angle;
            check(AnimationValidation.checkLengths(p,AnimationRig.joints(p),0).isEmpty(),"Mirrored bent rig lengths");
            AnimationValidation.Bounds bounds=new AnimationValidation.Bounds();new HumanRenderer().drawPose(bounds,0,0,p);
            check(Double.isFinite(bounds.left)&&Double.isFinite(bounds.bottom),"Finite bent mesh");
        }
        clip.poses.get(clip.keys.get(0).pose).angles[16]+=23;
        AnimationClip loaded=AnimationJson.decode(AnimationJson.encode(clip));
        check(loaded.version==3&&Math.abs(loaded.poses.get(loaded.keys.get(0).pose).angles[16]-clip.poses.get(clip.keys.get(0).pose).angles[16])<.001,"v3 JSON retains new bones");
        AnimationClip.PoseData[] poses={ready,ready.copy(),nod,knee};
        poses[1].angles[14]+=15;poses[1].angles[15]+=25;poses[1].angles[0]-=12;
        BufferedImage image=new BufferedImage(1280,380,BufferedImage.TYPE_INT_RGB);Graphics2D all=image.createGraphics();
        all.setColor(new Color(0xe3e4df));all.fillRect(0,0,1280,380);
        String[] labels={"Стойка — без выступа на спине","Три сегмента спины","Голова отдельно от шеи","Кость надколенника"};
        for(int col=0;col<4;col++){
            Graphics2D g=(Graphics2D)all.create();g.translate(col*320,0);g.setColor(new Color(0x263b36));g.setFont(new Font("Segoe UI",Font.PLAIN,14));g.drawString(labels[col],12,25);
            g.translate(140,335);g.scale(1.35,1.35);CanvasPainter p=new CanvasPainter(g);
            p.line(-100,0,120,0,1,0xffb66e4d);new HumanRenderer().drawPose(p,0,0,poses[col]);
            float[] j=AnimationRig.joints(poses[col]);
            for(int bone=0;bone<19;bone++){
                double[] s=AnimationRig.start(poses[col],j,bone);int e=AnimationRig.END[bone];
                p.line((float)s[0],(float)s[1],j[e],j[e+1],.7f,0xff3c9e8b);p.oval(j[e]-2,j[e+1]-2,4,4,0xff52ddbd);
            }
            g.dispose();
        }
        all.dispose();ImageIO.write(image,"png",new File(args[0]));
        System.out.println("PASS: independent head, three spine segments, patella controls, mirroring, v3 JSON and contour rendering");
    }
}
