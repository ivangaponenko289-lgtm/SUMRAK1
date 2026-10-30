package ru.shadowduel;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Desktop build uses exactly the same scene and combat model as Android. */
public final class Desktop extends JPanel implements Painter {
    final Combat game=new Combat();final Scene scene=new Scene();final Controls controls=new Controls(game);
    final DesktopBackground background=new DesktopBackground();
    private final java.util.ArrayDeque<AffineTransform> transforms=new java.util.ArrayDeque<>();
    private boolean rollHeld,jumpHeld;
    Graphics2D g;boolean left,right;long last=System.nanoTime();float accumulator;DesktopAudio audio;
    Desktop(){
        try(java.io.InputStream in=getClass().getResourceAsStream("/animations/forward-roll.json")){if(in!=null)game.forwardRollAnimation=AnimationJson.read(in);}
        catch(Exception invalid){System.err.println("Invalid optional forward-roll.json: "+invalid.getMessage());}
        for(int slot=0;slot<3;slot++){
            String name=EditableAnimations.IDS[slot]+".json";
            try{
                File location=new File(Desktop.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                File project=location.isFile()?location.getParentFile():location;
                while(project!=null&&!new File(project,"AndroidManifest.xml").isFile())project=project.getParentFile();
                File override=project==null?new File(location.isFile()?location.getParentFile():location,"animations/"+name):new File(project,"assets/animations/"+name);
                try(java.io.InputStream in=override.isFile()?new java.io.FileInputStream(override):getClass().getResourceAsStream("/animations/"+name)){
                    if(in!=null)EditableAnimations.install(game,slot,AnimationJson.read(in));
                }
            }catch(Exception invalid){System.err.println("Invalid optional "+name+": "+invalid.getMessage());}
        }
        setPreferredSize(new Dimension(1280,720));setFocusable(true);
        addMouseListener(new MouseAdapter(){public void mousePressed(MouseEvent e){requestFocusInWindow();controls.down(0,px(e),py(e));}public void mouseReleased(MouseEvent e){controls.up(0);}});
        addMouseMotionListener(new MouseMotionAdapter(){public void mouseDragged(MouseEvent e){controls.move(0,px(e),py(e));}});
        addFocusListener(new FocusAdapter(){public void focusLost(FocusEvent e){left=right=rollHeld=jumpHeld=false;controls.clear();game.pause();if(audio!=null)audio.sync(false);}});
        addKeyListener(new KeyAdapter(){
            public void keyPressed(KeyEvent e){switch(e.getKeyCode()){
                case KeyEvent.VK_ENTER:if(game.state==Combat.PAUSE)game.resume();else if(game.state!=Combat.PLAY)game.start();break;
                case KeyEvent.VK_ESCAPE:if(game.state==Combat.PLAY)game.pause();else game.resume();break;
                case KeyEvent.VK_M:game.soundEnabled=!game.soundEnabled;break;
                case KeyEvent.VK_A:case KeyEvent.VK_LEFT:left=true;break;
                case KeyEvent.VK_D:case KeyEvent.VK_RIGHT:right=true;break;
                case KeyEvent.VK_W:case KeyEvent.VK_UP:case KeyEvent.VK_SPACE:if(!jumpHeld){game.input.jump=true;game.input.jumpDirection=left?-1:right?1:0;jumpHeld=true;}break;
                case KeyEvent.VK_S:case KeyEvent.VK_DOWN:if(!rollHeld){game.input.roll=left?-1:right?1:game.hero.facing;rollHeld=true;}break;
                case KeyEvent.VK_J:game.input.punch=true;break;case KeyEvent.VK_K:game.input.kick=true;break;case KeyEvent.VK_L:game.input.block=true;break;
            }if(left||right)game.input.move=(right?1:0)-(left?1:0);}
            public void keyReleased(KeyEvent e){switch(e.getKeyCode()){
                case KeyEvent.VK_A:case KeyEvent.VK_LEFT:left=false;game.input.move=right?1:0;break;
                case KeyEvent.VK_D:case KeyEvent.VK_RIGHT:right=false;game.input.move=left?-1:0;break;
                case KeyEvent.VK_L:game.input.block=false;break;
                case KeyEvent.VK_S:case KeyEvent.VK_DOWN:rollHeld=false;break;
                case KeyEvent.VK_W:case KeyEvent.VK_UP:case KeyEvent.VK_SPACE:jumpHeld=false;break;
            }}
        });
    }
    float scale(){return Math.min(getWidth()/1280f,getHeight()/720f);}
    float px(MouseEvent e){return (e.getX()-(getWidth()-1280*scale())/2)/scale();}
    float py(MouseEvent e){return (e.getY()-(getHeight()-720*scale())/2)/scale();}
    @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);g=(Graphics2D)graphics.create();g.setColor(new Color(0xff09121b,true));g.fillRect(0,0,getWidth(),getHeight());g.translate((getWidth()-1280*scale())/2,(getHeight()-720*scale())/2);g.scale(scale(),scale());g.clipRect(0,0,1280,720);render();g.dispose();}
    void render(){g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);scene.draw(this,game);}
    public void rect(float x,float y,float w,float h,int c){g.setColor(new Color(c,true));g.fill(new Rectangle2D.Float(x,y,w,h));}
    public void background(float seconds){background.draw(g,seconds);}
    public void transform(float scale,float x,float y){transforms.push(g.getTransform());g.translate(x,y);g.scale(scale,scale);}
    public void rotate(float degrees,float x,float y){transforms.push(g.getTransform());g.rotate(Math.toRadians(degrees),x,y);}
    public void restore(){g.setTransform(transforms.pop());}
    public void oval(float x,float y,float w,float h,int c){g.setColor(new Color(c,true));g.fill(new Ellipse2D.Float(x,y,w,h));}
    public void line(float x,float y,float xx,float yy,float width,int c){g.setColor(new Color(c,true));g.setStroke(new BasicStroke(width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(new Line2D.Float(x,y,xx,yy));}
    public void poly(float[] a,int c){g.setColor(new Color(c,true));Path2D.Float p=new Path2D.Float();p.moveTo(a[0],a[1]);for(int i=2;i<a.length;i+=2)p.lineTo(a[i],a[i+1]);p.closePath();g.fill(p);}
    public void text(String v,float x,float y,float size,int c,boolean center){g.setColor(new Color(c,true));g.setFont(new Font("SansSerif",Font.BOLD,Math.round(size)));g.drawString(v,center?x-g.getFontMetrics().stringWidth(v)/2f:x,y);}
    public static void main(String[] args)throws Exception{
        final AnimationClip authored;
        if(args.length==2&&args[0].equals("--roll-animation")){try(java.io.InputStream in=new java.io.FileInputStream(args[1])){authored=AnimationJson.read(in);}}
        else authored=null;
        if(args.length>0&&authored==null){Desktop d=new Desktop();d.game.start();d.game.countdown=0;d.game.hero.x=570;d.game.enemy.x=715;d.game.hero.attack=2;d.game.hero.attackTime=.3f;d.game.enemy.block=true;d.game.hero.plantFeet();d.game.enemy.plantFeet();
            BufferedImage img=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);d.g=img.createGraphics();d.render();d.g.dispose();ImageIO.write(img,"png",new File(args[0]));return;}
        SwingUtilities.invokeLater(()->{Desktop d=new Desktop();if(authored!=null)d.game.forwardRollAnimation=authored;JFrame f=new JFrame("Сумрак 0.31 / A,D · Space · J,K,L · M: звук");f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);f.setContentPane(d);f.pack();f.setLocationRelativeTo(null);f.setVisible(true);d.requestFocusInWindow();d.audio=new DesktopAudio(d.game);
            f.addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){d.audio.close();}});
            new Timer(16,e->{long now=System.nanoTime();d.accumulator+=Math.min(.1f,(now-d.last)/1e9f);d.last=now;d.audio.sync(f.isFocused());while(d.accumulator>=1f/60){d.game.tick(1f/60);d.accumulator-=1f/60;}d.repaint();}).start();});
    }
}
