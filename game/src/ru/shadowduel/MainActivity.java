package ru.shadowduel;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.HapticFeedbackConstants;

public final class MainActivity extends Activity {
    private GameView view;
    private AndroidAudio audio;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_FULLSCREEN);
        view=new GameView();
        // Installing this optional asset explicitly opts into an authored forward roll.
        try(java.io.InputStream in=getAssets().open("animations/forward-roll.json")){view.game.forwardRollAnimation=AnimationJson.read(in);}
        catch(java.io.FileNotFoundException absent){}
        catch(Exception invalid){android.util.Log.e("ShadowDuel","Invalid optional forward-roll.json; using procedural roll",invalid);}
        for(int slot=0;slot<3;slot++){
            try(java.io.InputStream in=getAssets().open("animations/"+EditableAnimations.IDS[slot]+".json")){
                EditableAnimations.install(view.game,slot,AnimationJson.read(in));
            }catch(java.io.FileNotFoundException absent){}
            catch(Exception invalid){android.util.Log.e("ShadowDuel","Invalid optional animation",invalid);}
        }
        view.game.soundEnabled=getPreferences(MODE_PRIVATE).getBoolean("sound",true);
        audio=new AndroidAudio(this,view.game);setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
        setContentView(view);immersive();
    }
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);}
    @Override protected void onPause(){super.onPause();view.running=false;view.game.pause();view.controls.clear();view.last=0;audio.suspend();getPreferences(MODE_PRIVATE).edit().putBoolean("sound",view.game.soundEnabled).apply();}
    @Override protected void onDestroy(){if(audio!=null)audio.release();super.onDestroy();}
    @Override protected void onResume(){super.onResume();if(view!=null){view.running=true;view.last=0;view.postInvalidateOnAnimation();}immersive();}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused)immersive();else if(view!=null){view.game.pause();view.controls.clear();if(audio!=null)audio.suspend();}}
    @Override public void onBackPressed(){if(view.game.state==Combat.PLAY){view.controls.clear();view.game.pause();}else super.onBackPressed();}

    final class GameView extends View implements Painter {
        final Combat game=new Combat();final Scene scene=new Scene();final Controls controls=new Controls(game);
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final Path path=new Path();
        final AndroidBackground background=new AndroidBackground(MainActivity.this);
        Canvas canvas;float scale=1,offsetX,offsetY,accumulator;long last;boolean running=true;
        GameView(){super(MainActivity.this);setFocusable(true);paint.setStrokeCap(Paint.Cap.ROUND);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));setContentDescription("Сумрак. Арена боя с сенсорными кнопками.");}
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){scale=Math.min(w/1280f,h/720f);offsetX=(w-1280*scale)/2;offsetY=(h-720*scale)/2;}
        @Override protected void onDraw(Canvas c){
            long now=System.nanoTime();float dt=last==0?0:Math.min(.1f,(now-last)/1e9f);last=now;
            int previousHits=game.hits;accumulator+=dt;
            audio.sync(running&&hasWindowFocus());
            while(accumulator>=1f/60){game.tick(1f/60);accumulator-=1f/60;}
            if(game.hits>previousHits)performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            c.drawColor(0xff09121b);c.save();c.translate(offsetX,offsetY);c.scale(scale,scale);c.clipRect(0,0,1280,720);canvas=c;
            scene.draw(this,game);c.restore();
            if(running)postInvalidateOnAnimation();
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            int action=e.getActionMasked(),index=e.getActionIndex(),id=e.getPointerId(index);
            if(action==MotionEvent.ACTION_CANCEL){controls.clear();return true;}
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN)controls.down(id,(e.getX(index)-offsetX)/scale,(e.getY(index)-offsetY)/scale);
            else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP){controls.up(id);if(action==MotionEvent.ACTION_UP)performClick();}
            else if(action==MotionEvent.ACTION_MOVE)for(int i=0;i<e.getPointerCount();i++)controls.move(e.getPointerId(i),(e.getX(i)-offsetX)/scale,(e.getY(i)-offsetY)/scale);
            return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
        public void background(float seconds){background.draw(canvas,seconds);}
        public void transform(float scale,float x,float y){canvas.save();canvas.translate(x,y);canvas.scale(scale,scale);}
        public void rotate(float degrees,float x,float y){canvas.save();canvas.rotate(degrees,x,y);}
        public void restore(){canvas.restore();}
        public void rect(float x,float y,float w,float h,int color){paint.setColor(color);canvas.drawRect(x,y,x+w,y+h,paint);}
        public void oval(float x,float y,float w,float h,int color){paint.setColor(color);canvas.drawOval(x,y,x+w,y+h,paint);}
        public void line(float x,float y,float xx,float yy,float width,int color){paint.setColor(color);paint.setStrokeWidth(width);canvas.drawLine(x,y,xx,yy,paint);}
        public void poly(float[] points,int color){paint.setColor(color);path.reset();path.moveTo(points[0],points[1]);for(int i=2;i<points.length;i+=2)path.lineTo(points[i],points[i+1]);path.close();canvas.drawPath(path,paint);}
        public void text(String value,float x,float y,float size,int color,boolean center){paint.setColor(color);paint.setTextSize(size);paint.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);canvas.drawText(value,x,y,paint);}
    }
}
