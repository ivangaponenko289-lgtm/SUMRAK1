package ru.shadowduel;

/** Check emitted geometry, including muscle contours, hands and soles between key poses. */
public final class RendererTest {
    private static final class Geometry implements Painter {
        final AnimationValidation.Bounds bounds=new AnimationValidation.Bounds();
        int depth;
        void reset(){bounds.reset();depth=0;}
        void finite(float... values){for(float v:values)if(!Float.isFinite(v))throw new AssertionError("Non-finite rendered geometry");}
        public void transform(float s,float x,float y){finite(s,x,y);depth++;bounds.transform(s,x,y);}
        public void rotate(float a,float x,float y){finite(a,x,y);depth++;bounds.rotate(a,x,y);}
        public void restore(){if(--depth<0)throw new AssertionError("Unbalanced renderer transform");bounds.restore();}
        public void background(float seconds){}
        public void rect(float x,float y,float w,float h,int c){finite(x,y,w,h);bounds.rect(x,y,w,h,c);}
        public void oval(float x,float y,float w,float h,int c){finite(x,y,w,h);if(c!=0x65000000)bounds.oval(x,y,w,h,c);}
        public void line(float x,float y,float xx,float yy,float w,int c){finite(x,y,xx,yy,w);bounds.line(x,y,xx,yy,w,c);}
        public void poly(float[] a,int c){finite(a);bounds.poly(a,c);}
        public void text(String s,float x,float y,float size,int c,boolean center){finite(x,y,size);}
    }
    public static void main(String[] args){
        int frames=0;HumanRenderer renderer=new HumanRenderer();Geometry geometry=new Geometry();
        String[] actions={"intro","walk","backstep","jab","kick","guard","jump","forward-roll","backward-roll"};
        for(int facing:new int[]{-1,1})for(String action:actions){
            Combat game=new Combat();game.start();game.botEnabled=false;game.hero.x=640;game.enemy.x=facing==1?1150:130;
            game.hero.facing=facing;game.hero.plantFeet();game.enemy.plantFeet();if(!action.equals("intro"))game.countdown=0;
            if(action.equals("walk"))game.input.move=facing;if(action.equals("backstep"))game.input.move=-facing;
            if(action.equals("jab"))game.input.punch=true;if(action.equals("kick"))game.input.kick=true;
            if(action.equals("guard"))game.input.block=true;if(action.equals("jump"))game.input.jump=true;
            if(action.equals("forward-roll"))game.input.roll=facing;if(action.equals("backward-roll"))game.input.roll=-facing;
            for(int frame=0;frame<190;frame++){
                geometry.reset();renderer.draw(geometry,game.hero,0xff75e0d0,game.elapsed,game.countdown,false);
                if(geometry.depth!=0||!Double.isFinite(geometry.bounds.bottom))throw new AssertionError("Invalid render state");
                if(geometry.bounds.bottom>Combat.FLOOR+.25)throw new AssertionError(action+" facing="+facing+" frame="+frame+" penetrates floor by "+(geometry.bounds.bottom-Combat.FLOOR));
                if(game.hero.rolling&&Math.abs(geometry.bounds.bottom-Combat.FLOOR)>.02)throw new AssertionError("Roll loses visible support: "+action+" #"+frame);
                game.tick(1f/60);frames++;
            }
        }
        System.out.println("PASS: "+frames+" rendered frames, both facings, finite contours, ground contact and balanced transforms");
    }
}
