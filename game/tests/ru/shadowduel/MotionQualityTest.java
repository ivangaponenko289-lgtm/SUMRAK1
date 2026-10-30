package ru.shadowduel;

/** Regression checks for the live procedural rig, independently of editor keys. */
public final class MotionQualityTest {
    static void near(float actual,float expected,String label){
        if(Math.abs(actual-expected)>.015f)throw new AssertionError(label+": "+actual+" != "+expected);
    }
    public static void main(String[] args){
        int frames=0;
        for(int facing:new int[]{-1,1})for(int action=0;action<4;action++){
            Combat g=new Combat();g.start();g.countdown=0;g.botEnabled=false;
            g.hero.x=640;g.enemy.x=facing>0?1200:80;g.hero.facing=facing;g.hero.plantFeet();
            if(action==0)g.input.move=facing;
            if(action==1)g.input.move=-facing;
            if(action>=2)g.attack(g.hero,action-1);
            Pose p=new Pose();
            for(int frame=0;frame<90;frame++){
                p.sample(g.hero,g.elapsed);float[] j=p.joints;
                for(int arm=0;arm<2;arm++){
                    int e=arm==0?6:10,w=e+2;
                    float sx=j[2]+(arm==0?11+p.shoulderTurn:-6-p.shoulderTurn*.35f);
                    float sy=j[3]+(arm==0?5+p.shoulderTurn*.3f:6-p.shoulderTurn*.105f);
                    near((float)Math.hypot(j[e]-sx,j[e+1]-sy),36,"Upper arm length");
                    near((float)Math.hypot(j[w]-j[e],j[w+1]-j[e+1]),40,"Forearm length");
                }
                if(g.hero.attack!=0){
                    near(j[20],(g.hero.backFoot-g.hero.x)*facing,"Support foot x");
                    near(j[21],-6,"Support foot y");
                }
                g.tick(1f/60);frames++;
            }
        }
        Combat g=new Combat();g.start();g.countdown=0;Pose a=new Pose(),b=new Pose();
        g.hero.attack=1;g.hero.attackTime=.12f;a.sample(g.hero,0);
        g.hero.attackTime=.16f;b.sample(g.hero,0);
        if(Math.abs(a.joints[8]-b.joints[8])<5)throw new AssertionError("Punch freezes after contact");
        System.out.println("PASS: "+frames+" motion frames: fixed arm lengths, planted support, punch follow-through");
    }
}
