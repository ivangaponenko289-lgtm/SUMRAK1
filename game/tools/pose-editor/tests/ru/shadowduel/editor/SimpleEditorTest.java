package ru.shadowduel.editor;

import ru.shadowduel.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

public final class SimpleEditorTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        SwingUtilities.invokeAndWait(()->{try{
            PoseEditor.theme();PoseEditor editor=new PoseEditor();editor.simpleMode(true);
            editor.setSize(1440,920);PoseEditor.layoutTree(editor);editor.loadMotion(1,false);
            check(!editor.libraryPanel.isVisible(),"Simple mode hides pose library");
            editor.scrub(5);editor.angleFields[10].setValue(editor.state.draft.angles[10]+22);
            check(editor.keyAt(5)!=null,"Editing an intermediate frame creates a key");
            editor.scrub(5);double wrist=editor.displayPose().angles[10];
            editor.speed.setValue(50);
            check(editor.state.clip.playbackSpeed==.5,"Slider stores playback speed");
            check(AnimationJson.decode(AnimationJson.encode(editor.state.clip)).playbackSpeed==.5,"Speed survives file round trip");
            editor.state.undo();editor.refresh();
            check(editor.speed.getValue()==100,"Undo restores speed and slider");
            editor.state.redo();editor.refresh();
            check(editor.speed.getValue()==50,"Redo restores speed and slider");
            editor.loadMotion(0,false);editor.loadMotion(1,false);editor.scrub(5);
            check(editor.speed.getValue()==50,"Switching documents retains their speed");
            check(Math.abs(editor.displayPose().angles[10]-wrist)<.001,"Switching motion retains edits");
            for(int kind=0;kind<3;kind++){
                AnimationClip clip=EditableAnimations.preset(kind);
                clip=AnimationJson.decode(AnimationJson.encode(clip));
                for(int frame=0;frame<=clip.duration;frame++){
                    AnimationClip.PoseData p=AnimationRuntime.sampleAnimation(clip,frame);
                    check(AnimationValidation.checkLengths(p,AnimationRig.joints(p),frame).isEmpty(),"Extended rig length drift");
                }
                check(AnimationValidation.validate(clip,60).isEmpty(),"Preset "+kind+" must pass validation: "+AnimationValidation.validate(clip,60));
                File examples=new File(new File(args[0]).getParentFile(),"examples");examples.mkdirs();
                AnimationFiles.save(clip,new File(examples,EditableAnimations.IDS[kind]+"-0.31.json"));
                for(int facing:new int[]{-1,1}){
                    Combat game=new Combat();game.start();game.countdown=0;game.hero.facing=facing;game.hero.plantFeet();
                    EditableAnimations.install(game,kind,clip);
                    game.hero.attack=kind;game.hero.attackTime=.12f;game.hero.motion=1;game.hero.walk=1.5f;game.hero.walkSigned=16;
                    Pose pose=new Pose();pose.sample(game.hero,0);
                    check(pose.editedPose!=null,"Game must use installed motion");
                    double at=kind==0?(game.hero.walkSigned/clip.walkDistance*clip.duration)%clip.duration:game.hero.attackTime/(kind==1?.4:.68)*clip.duration;
                    
                    float[] expected=AnimationRig.joints(AnimationRuntime.sampleAnimation(clip,at));
                    for(int i=0;i<22;i++)check(Math.abs(expected[i]-pose.joints[i])<.001,"Game/editor pose parity");
                }
            }
            editor.loadMotion(1,true);editor.scrub(7);editor.fit();
            BufferedImage image=new BufferedImage(1440,920,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();editor.paint(g);g.dispose();
            ImageIO.write(image,"png",new File(args[0]));
            System.out.println("PASS: simple UI, auto keys, retained documents, extended rig JSON and game parity");
        }catch(Exception ex){throw new RuntimeException(ex);}});
    }
}
