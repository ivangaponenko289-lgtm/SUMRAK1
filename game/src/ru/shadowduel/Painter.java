package ru.shadowduel;

/** Small drawing interface shared by the phone and desktop preview. */
public interface Painter {
    /** Push the current transform, then translate and uniformly scale. Paired with restore(). */
    void transform(float scale,float x,float y);
    void rotate(float degrees,float x,float y);
    void restore();
    void background(float seconds);
    void rect(float x,float y,float w,float h,int color);
    void oval(float x,float y,float w,float h,int color);
    void line(float x,float y,float xx,float yy,float width,int color);
    void poly(float[] points,int color);
    void text(String value,float x,float y,float size,int color,boolean center);
}
