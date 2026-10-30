package ru.shadowduel.editor;

import ru.shadowduel.Painter;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayDeque;

final class CanvasPainter implements Painter {
    final Graphics2D g;private final ArrayDeque<AffineTransform> stack=new ArrayDeque<>();
    CanvasPainter(Graphics2D g){this.g=g;g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);}
    public void transform(float scale,float x,float y){stack.push(g.getTransform());g.translate(x,y);g.scale(scale,scale);}
    public void rotate(float a,float x,float y){stack.push(g.getTransform());g.rotate(Math.toRadians(a),x,y);}
    public void restore(){g.setTransform(stack.pop());}
    public void background(float seconds){}
    public void rect(float x,float y,float w,float h,int c){g.setColor(new Color(c,true));g.fill(new Rectangle2D.Float(x,y,w,h));}
    public void oval(float x,float y,float w,float h,int c){g.setColor(new Color(c,true));g.fill(new Ellipse2D.Float(x,y,w,h));}
    public void line(float x,float y,float xx,float yy,float width,int c){g.setColor(new Color(c,true));g.setStroke(new BasicStroke(width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(new Line2D.Float(x,y,xx,yy));}
    public void poly(float[] a,int c){Path2D.Float p=new Path2D.Float();p.moveTo(a[0],a[1]);for(int i=2;i<a.length;i+=2)p.lineTo(a[i],a[i+1]);p.closePath();g.setColor(new Color(c,true));g.fill(p);}
    public void text(String s,float x,float y,float size,int c,boolean center){g.setColor(new Color(c,true));g.setFont(new Font("Segoe UI",Font.PLAIN,Math.round(size)));g.drawString(s,center?x-g.getFontMetrics().stringWidth(s)/2f:x,y);}
}
