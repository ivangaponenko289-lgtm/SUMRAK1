package ru.shadowduel.editor;
import ru.shadowduel.*;import java.awt.*;import java.awt.image.*;import java.io.*;import javax.imageio.*;
public class ReferencePreview {
 public static void main(String[] args)throws Exception{
  File dir=new File(args[0]);dir.mkdirs();HumanRenderer renderer=new HumanRenderer();AnimationClip walk=ReferenceMotion.walk();
  for(int frame=-1;frame<37;frame++){
   AnimationClip.PoseData p=frame<0?ReferenceMotion.idle():AnimationRuntime.sampleAnimation(walk,frame);
   BufferedImage im=new BufferedImage(640,720,BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();g.setColor(new Color(0xe8e1d2));g.fillRect(0,0,640,720);g.translate(310,680);g.scale(3,3);
   renderer.drawPose(new CanvasPainter(g),0,0,p);g.dispose();ImageIO.write(im,"png",new File(dir,frame<0?"idle.png":String.format("%03d.png",frame)));
  }
 }
}
