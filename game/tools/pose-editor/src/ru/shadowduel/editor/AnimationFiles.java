package ru.shadowduel.editor;

import ru.shadowduel.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

final class AnimationFiles {
    static void save(AnimationClip clip,File file)throws IOException{
        Path target=file.toPath().toAbsolutePath(),temp=Files.createTempFile(target.getParent(),"animation-",".tmp");
        try{Files.write(temp,AnimationJson.encode(clip).getBytes(StandardCharsets.UTF_8));try{Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException ex){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}
    }
    static AnimationClip load(File file)throws IOException{try(InputStream in=new FileInputStream(file)){return AnimationJson.read(in);}}
    static void contactSheet(AnimationClip clip,File file,int step,int columns)throws IOException{
        if(step<1||columns<1||columns>10)throw new IllegalArgumentException("Шаг ≥ 1, столбцов 1–10");
        List<Integer> frames=new ArrayList<>();for(int i=0;i<clip.duration;i+=step)frames.add(i);frames.add(clip.duration);
        int cellW=420,cellH=300,rows=(frames.size()+columns-1)/columns;
        if((long)columns*cellW*rows*cellH>64000000)throw new IllegalArgumentException("Лист слишком большой: увеличьте шаг кадров");
        BufferedImage sheet=new BufferedImage(columns*cellW,rows*cellH,BufferedImage.TYPE_INT_RGB);Graphics2D all=sheet.createGraphics();all.setColor(new Color(0xe3e4df));all.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
        HumanRenderer renderer=new HumanRenderer();
        for(int i=0;i<frames.size();i++){
            int frame=frames.get(i);Graphics2D g=(Graphics2D)all.create();g.translate((i%columns)*cellW,(i/columns)*cellH);g.clipRect(0,0,cellW,cellH);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setFont(new Font("Segoe UI",Font.BOLD,12));g.setColor(new Color(0x31443e));
            g.drawString(String.format(java.util.Locale.ROOT,"%s #%d  %.3fs",clip.name,frame,frame/60.0),10,20);
            double scale=Math.min((cellW-20)/(clip.right-clip.left),(cellH-44)/(clip.bottom-clip.top));g.translate(10-clip.left*scale,34-clip.top*scale);g.scale(scale,scale);
            CanvasPainter painter=new CanvasPainter(g);painter.line((float)clip.left,0,(float)clip.right,0,(float)(1/scale),0xffb66e4d);
            AnimationClip.PoseData pose=AnimationRuntime.sampleAnimation(clip,frame);renderer.drawPose(painter,0,0,pose);g.dispose();
        }all.dispose();ImageIO.write(sheet,"png",file);
    }
}
