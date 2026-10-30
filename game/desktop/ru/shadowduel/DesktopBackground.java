package ru.shadowduel;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** This supplied GIF contains 48 full-canvas frames, each lasting 120 ms. */
final class DesktopBackground {
    private final BufferedImage[] frames;
    DesktopBackground(){
        ImageReader reader=ImageIO.getImageReadersByFormatName("gif").next();
        try(InputStream raw=getClass().getResourceAsStream("/backgrounds/bloody_sunset.gif")){
            if(raw==null)throw new IOException("Missing arena GIF");
            try(ImageInputStream in=ImageIO.createImageInputStream(raw)){
                reader.setInput(in);frames=new BufferedImage[reader.getNumImages(true)];
                for(int i=0;i<frames.length;i++)frames[i]=reader.read(i);
            }
        }catch(IOException e){throw new IllegalStateException("Arena background unavailable",e);}
        finally{reader.dispose();}
    }
    void draw(Graphics2D g,float seconds){
        int index=(int)((seconds*1000)%(frames.length*120))/120;
        g.drawImage(frames[index],0,0,1280,576,null);
    }
}
