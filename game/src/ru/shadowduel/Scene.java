package ru.shadowduel;

public final class Scene {
    static final int INK=0xff0a1420,WHITE=0xffecede2,TEAL=0xff75e0d0,GOLD=0xffffbf7d;
    private static final float CLOSE_ZOOM=1.35f,SCREEN_FLOOR=515;
    private Painter p;
    public void draw(Painter painter,Combat g){
        p=painter;
        p.rect(0,0,1280,720,0xff080709);
        // Frame both fighters while keeping the panorama's edges outside the viewport.
        float separation=Math.abs(g.hero.x-g.enemy.x);
        float zoom=Combat.clamp(1120f/Math.max(1,separation),1,CLOSE_ZOOM);
        float halfWidth=640/zoom;
        float center=Combat.clamp((g.hero.x+g.enemy.x)*.5f,halfWidth,1280-halfWidth);
        p.transform(zoom,640-center*zoom,SCREEN_FLOOR-Combat.FLOOR*zoom);
        p.background(g.elapsed);
        if(g.training)bag(g);else fighter(g.enemy,GOLD,g);
        fighter(g.hero,TEAL,g);
        if(g.flash>0){
            float t=1-g.flash/.2f;
            for(int i=0;i<9;i++){double a=i*Math.PI*2/9;float r=15+t*45;p.line(g.hitX+(float)Math.cos(a)*r*.5f,g.hitY+(float)Math.sin(a)*r*.5f,g.hitX+(float)Math.cos(a)*r,g.hitY+(float)Math.sin(a)*r,3,g.blockedHit?TEAL:GOLD);}
            p.text(g.blockedHit?"БЛОК":"",g.hitX,g.hitY-42-t*20,16,TEAL,true);
        }
        p.restore();
        p.rect(0,0,1280,110,0x68201016);
        hud(g);controls(g);
        if(g.state==Combat.PLAY&&g.countdown>0){
            boolean fight=g.countdown<=.45f;
            p.text(fight?"БОЙ":"ПРИГОТОВЬСЯ",640,227,fight?64:29,fight?GOLD:WHITE,true);
            if(!fight){
                p.rect(550,245,180,3,0xff354450);
                p.rect(550,245,180*(1-g.countdown/Combat.INTRO_DURATION),3,TEAL);
            }
        }
        if(g.state==Combat.MENU)panel("СУМРАК","ВЫБЕРИ РЕЖИМ","Держи дистанцию. Лови момент для удара.","НАЧАТЬ БОЙ","ТРЕНИРОВКА С ГРУШЕЙ");
        if(g.state==Combat.PAUSE)panel("ПАУЗА",g.training?"ТРЕНИРОВКА":"ПЕРЕВЕДИ ДЫХАНИЕ","Продолжи занятие или вернись к выбору режима.","ПРОДОЛЖИТЬ","В МЕНЮ");
        if(g.state==Combat.END)panel(g.result,"ДУЭЛЬ ЗАВЕРШЕНА",g.hero.hp>g.enemy.hp?"Точность сильнее спешки.":"Попробуй блок и контратаку после замаха.","ЕЩЁ БОЙ","В МЕНЮ");
    }
    private void bag(Combat g){
        float x=Combat.BAG_X,y=Combat.BAG_PIVOT_Y;
        p.line(x-105,y-12,x+105,y-12,10,0xff26343a);
        p.oval(x-8,y-8,16,16,0xff9ba9a6);
        p.rotate((float)(-g.bagAngle*180/Math.PI),x,y);
        p.line(x,y,x-28,275,4,0xffa4aaa4);p.line(x,y,x+28,275,4,0xffa4aaa4);
        p.rect(x-42,285,84,145,0xff713b32);
        p.oval(x-42,265,84,40,0xff925444);p.oval(x-42,410,84,40,0xff713b32);
        p.rect(x-42,300,84,12,0xff28343a);p.rect(x-42,405,84,12,0xff28343a);
        p.line(x+26,320,x+26,395,3,0xffb0795b);
        p.text("СУМРАК",x,365,13,0xffe4b38b,true);
        p.restore();
    }
    private void hud(Combat g){
        p.text("СУМРАК / ПРОТОТИП 24",640,30,13,0xffadc0c1,true);
        if(g.training){
            p.text("ТРЕНИРОВКА",50,47,19,TEAL,false);
            p.text("УДАРЫ: "+g.hits,50,83,25,WHITE,false);
            p.text("УРОН: "+g.trainingDamage,1010,83,22,GOLD,false);
            p.text("БЕЗ ЛИМИТА",640,102,12,WHITE,true);
        }else{
        p.text("ТЫ",50,47,19,WHITE,false);p.text("СТРАЖ",1010,47,19,WHITE,false);
        p.rect(50,61,440,17,0xff283841);p.rect(50,61,440*g.hero.hp/100,17,TEAL);
        p.rect(790,61,440,17,0xff283841);p.rect(1230-440*g.enemy.hp/100,61,440*g.enemy.hp/100,17,GOLD);
        p.text(""+(int)Math.ceil(g.hero.hp),50,100,14,0xffb9cccb,false);p.text(""+(int)Math.ceil(g.enemy.hp),1195,100,14,0xffb9cccb,false);
        p.text(String.format(java.util.Locale.ROOT,"%02d",(int)Math.ceil(g.time)),640,81,39,WHITE,true);
        p.text("ДУЭЛЬ",640,102,11,0xffb8c9c7,true);
        }
        p.rect(526,48,44,44,0xff263c46);
        p.poly(new float[]{535,65,541,65,548,58,548,82,541,75,535,75},g.soundEnabled?TEAL:0xff819095);
        if(g.soundEnabled){p.line(554,63,558,70,2,TEAL);p.line(558,70,554,77,2,TEAL);}
        else p.line(532,82,562,56,2,0xffe2b291);
        if(g.state==Combat.PLAY){p.rect(707,48,44,44,0xff263c46);p.line(722,61,722,79,4,WHITE);p.line(735,61,735,79,4,WHITE);}
    }
    private void controls(Combat g){
        float cx=Controls.STICK_X,cy=Controls.STICK_Y;
        p.oval(cx-82,cy-82,164,164,0xff78908c);
        p.oval(cx-79,cy-79,158,158,0xe010191e);
        p.oval(cx-56,cy-56,112,112,0xff26343a);
        p.oval(cx-53,cy-53,106,106,0xff121c22);
        for(int i=0;i<8;i++){
            double a=i*Math.PI/4;
            float dx=(float)Math.cos(a),dy=(float)Math.sin(a);
            int color=dy<-.5?TEAL:dy>.5?GOLD:WHITE;
            p.line(cx+dx*64,cy+dy*64,cx+dx*70,cy+dy*70,3,color);
        }
        float knobX=cx+g.input.move*Controls.STICK_TRAVEL,knobY=cy+g.input.stickY*Controls.STICK_TRAVEL;
        p.oval(knobX-26,knobY-26,52,52,0xff75918e);
        p.oval(knobX-23,knobY-23,46,46,0xff30494d);
        p.oval(knobX-6,knobY-6,12,12,TEAL);
        p.text("↖  ↑  ↗  ПРЫЖОК",cx,525,12,TEAL,true);
        p.text("↙  ↘  КУВЫРОК",cx,718,12,GOLD,true);
        button(944,620,48,"◇","БЛОК",g.input.block,TEAL);
        button(1066,625,55,"II","НОГА",g.hero.attack==2,GOLD);
        button(1182,562,59,"I","РУКА",g.hero.attack==1,WHITE);
        p.text("АРЕНА / КРОВАВЫЙ ЗАКАТ",640,688,12,0xff79939c,true);
    }
    private void button(float x,float y,float r,String icon,String label,boolean held,int accent){
        p.oval(x-r,y-r,r*2,r*2,held?accent:0xff49616a);p.oval(x-r+2,y-r+2,r*2-4,r*2-4,held?0xff314b53:0xff1b303b);
        p.text(icon,x,y+5,31,accent,true);p.text(label,x,y+27,11,accent,true);
    }
    private void panel(String title,String eyebrow,String detail,String action,String secondary){
        p.rect(0,0,1280,720,0xb0091520);p.rect(330,166,620,396,0xf0122632);
        p.rect(330,166,620,3,TEAL);p.text(eyebrow,640,211,13,TEAL,true);
        p.text(title,640,285,52,WHITE,true);p.text(detail,640,333,18,0xffadbfbe,true);
        p.rect(466,374,348,70,TEAL);p.text(action,640,418,21,INK,true);
        p.rect(466,454,348,56,0xff304953);p.text(secondary,640,490,18,WHITE,true);
        p.text("↖ ↑ ↗ — прыжок   •   ← → — шаг   •   ↙ ↘ — кувырок",640,540,15,0xff8ba4aa,true);
    }
    private final HumanRenderer humans=new HumanRenderer();
    private void fighter(Combat.Fighter f,int accent,Combat g){humans.draw(p,f,accent,g.elapsed,g.state==Combat.MENU?0:g.countdown,f==g.enemy);}
}
