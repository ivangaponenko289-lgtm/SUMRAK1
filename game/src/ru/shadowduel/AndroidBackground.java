package ru.shadowduel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.RectF;
import java.io.InputStream;
import java.io.IOException;

/** Decode into one reusable software bitmap, then upload to the hardware canvas.
 * Movie cannot draw reliably straight onto a hardware-accelerated Canvas. */
final class AndroidBackground {
    private final Movie movie;
    private final Bitmap frame;
    private final Canvas buffer;
    private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF destination=new RectF(0,0,1280,576);
    private int lastFrame=-1;
    AndroidBackground(Context context){
        try(InputStream in=context.getAssets().open("backgrounds/bloody_sunset.gif")){
            movie=Movie.decodeStream(in);
            if(movie==null)throw new IOException("Cannot decode arena GIF");
        }catch(IOException e){throw new IllegalStateException("Arena background unavailable",e);}
        frame=Bitmap.createBitmap(movie.width(),movie.height(),Bitmap.Config.ARGB_8888);
        buffer=new Canvas(frame);
    }
    void draw(Canvas canvas,float seconds){
        int duration=movie.duration()>0?movie.duration():5760;
        int millis=(int)((seconds*1000)%duration);
        int index=millis/120;
        if(index!=lastFrame){
            frame.eraseColor(0xff080709);
            movie.setTime(millis);movie.draw(buffer,0,0);
            lastFrame=index;
        }
        canvas.drawBitmap(frame,null,destination,paint);
    }
}
