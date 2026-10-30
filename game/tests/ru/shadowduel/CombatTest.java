package ru.shadowduel;

public final class CombatTest {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static Combat game(){Combat g=new Combat();g.start();g.countdown=0;g.botEnabled=false;g.hero.x=500;g.enemy.x=600;g.hero.plantFeet();g.enemy.plantFeet();return g;}
    static void advance(Combat g,int ticks){for(int i=0;i<ticks;i++)g.tick(1f/60);}
    static float renderedRollBottom(Combat.Fighter f,float[] joints){
        float time=f.rollTime/Combat.ROLL_DURATION;if(f.rollDirection*f.facing<0)time=1-time;
        AnimationValidation.Bounds bounds=new AnimationValidation.Bounds();
        new HumanRenderer().drawPose(bounds,0,0,f.facing,joints,HumanRenderer.rollWeight(time),0);
        return (float)bounds.bottom;
    }
    public static void main(String[] args){
        Combat training=new Combat();Controls trainingControls=new Controls(training);
        trainingControls.down(1,640,480);
        check(training.training&&training.state==Combat.PLAY&&training.countdown==0,"Menu opens training immediately");
        training.hero.x=760;training.hero.plantFeet();training.input.punch=true;advance(training,45);
        check(training.hits==1&&training.trainingDamage==8&&training.enemy.hp==100,"Bag registers one punch without taking health damage");
        check(Math.abs(training.bagAngle)>.001f,"Bag swings after a hit");
        training.hero.x=training.enemy.x-100;training.input.kick=true;advance(training,45);
        check(training.hits==2&&training.trainingDamage==22,"Bag registers kick damage");
        training.pause();float bagAngle=training.bagAngle;advance(training,60);
        check(training.bagAngle==bagAngle,"Pause freezes bag");training.resume();advance(training,60*90);
        check(training.state==Combat.PLAY&&training.time==75&&training.hero.hp==100&&training.enemy.hp==100,"Training has no timer, damage to player or bot");
        check(Math.abs(training.bagAngle)<.001f,"Bag settles after impacts");
        training.hero.x=65;training.input.punch=true;advance(training,45);check(training.hits==2,"Distant punch misses bag");
        training.pause();trainingControls.down(2,640,480);check(training.state==Combat.MENU&&!training.training,"Pause returns to mode selection");
        trainingControls.down(3,640,400);check(!training.training&&training.countdown>0&&training.hits==0,"Duel resets training state");
        Combat g=game();g.input.punch=true;advance(g,5);check(g.enemy.hp==100,"No damage before active frame");advance(g,20);check(g.enemy.hp==92,"One hit per attack");advance(g,30);check(g.enemy.hp==92,"No repeat damage after recovery");
        g=game();g.enemy.x=900;g.input.kick=true;advance(g,50);check(g.enemy.hp==100,"Out-of-range attacks miss");
        g=game();g.enemy.block=true;g.input.kick=true;advance(g,50);check(g.enemy.hp==98,"Guard reduces damage");
        g=game();g.enemy.y=150;g.enemy.vy=200;g.input.punch=true;advance(g,10);check(g.enemy.hp==100,"Airborne opponent evades low attacks");
        g=game();g.input.jump=true;advance(g,1);check(g.hero.y==0&&g.hero.jumpPrep>0,"Jump begins with anticipation");advance(g,8);check(g.hero.y>0,"Push-off follows anticipation");advance(g,80);check(g.hero.y==0&&g.hero.vy==0,"Jump lands without sinking");
        g=game();g.hero.x=65;g.input.move=-1;advance(g,100);check(g.hero.x==65,"Arena boundary");
        g=game();g.input.move=1;advance(g,30);check(g.enemy.x-g.hero.x>=65.99,"Bodies do not overlap");
        g=game();g.enemy.hp=8;g.input.punch=true;advance(g,30);check(g.state==Combat.END&&g.result.equals("ПОБЕДА"),"KO ends round");float time=g.time;advance(g,100);check(g.time==time,"Finished round stays frozen");
        g=game();g.time=.01f;g.hero.hp=50;advance(g,1);check(g.state==Combat.END&&g.result.equals("ПОРАЖЕНИЕ"),"Timer uses remaining health");
        g=game();g.pause();time=g.time;g.input.punch=true;advance(g,20);check(g.time==time&&g.enemy.hp==100,"Pause freezes combat");g.resume();check(!g.input.punch,"Resume clears stale action");
        g=game();Controls c=new Controls(g);c.down(3,180,620);c.down(8,1182,562);check(g.input.move>0&&g.input.punch,"Multi-touch movement plus attack");c.up(8);check(g.input.move>0,"Releasing attack preserves stick");c.down(9,944,620);check(g.input.block,"Hold guard");c.clear();check(g.input.move==0&&!g.input.block&&!g.input.punch,"Cancel clears all pointers");
        g=game();g.botEnabled=true;advance(g,60*90);check(g.state==Combat.END,"Bot match terminates");check(g.hero.hp>=0&&g.enemy.hp>=0,"Health never negative");
        g.start();check(g.state==Combat.PLAY&&g.hero.hp==100&&g.enemy.hp==100&&g.time==75,"Rematch resets combat");
        g.botEnabled=false;c=new Controls(g);c.down(1,180,620);c.down(2,944,620);advance(g,190);check(g.input.move>0&&g.hero.block,"Held controls survive round countdown");
        final int[] sounds=new int[6];
        Combat.SoundListener listener=(cue,x)->sounds[cue]++;
        g=game();g.soundListener=listener;g.input.punch=true;advance(g,45);
        check(sounds[Combat.SWING]==1&&sounds[Combat.PUNCH]==1&&sounds[Combat.HURT]==1,"One swing, contact and hurt sound per successful punch");
        java.util.Arrays.fill(sounds,0);g=game();g.soundListener=listener;g.enemy.x=1000;g.input.kick=true;advance(g,50);
        check(sounds[Combat.SWING]==1&&sounds[Combat.KICK]==0&&sounds[Combat.HURT]==0,"Miss produces swing only");
        java.util.Arrays.fill(sounds,0);g=game();g.soundListener=listener;g.enemy.block=true;g.input.kick=true;advance(g,50);
        check(sounds[Combat.BLOCK]==1&&sounds[Combat.HURT]==0&&sounds[Combat.KICK]==0,"Blocked contact has its own sound, no hurt sound");
        java.util.Arrays.fill(sounds,0);g=game();g.soundListener=listener;g.soundEnabled=false;g.input.punch=true;advance(g,45);
        check(java.util.Arrays.stream(sounds).sum()==0,"Mute suppresses every effect");
        g=game();g.soundListener=listener;g.input.jump=true;advance(g,80);check(sounds[Combat.LAND]==1,"One landing sound per jump");
        c=new Controls(g);c.down(4,547,70);check(!g.soundEnabled,"Sound button toggles audio");g.start();check(!g.soundEnabled,"Mute survives rematch");
        Pose pose=new Pose();g=game();g.hero.attack=1;g.hero.attackTime=.12f;pose.sample(g.hero,0);check(pose.joints[8]==119,"Punch pose reaches contact at active frame");
        g.hero.attack=2;g.hero.attackTime=.24f;pose.sample(g.hero,0);check(pose.joints[16]==124,"Kick pose reaches contact at active frame");
        for(int kind=1;kind<=2;kind++)for(int frame=0;frame<45;frame++){g.hero.attack=kind;g.hero.attackTime=frame/60f;pose.sample(g.hero,frame/60f);for(float value:pose.joints)if(Float.isNaN(value)||Float.isInfinite(value))throw new AssertionError("Invalid animation coordinate");}
        check(true,"Animation coordinates remain finite across both attacks");
        g=game();g.enemy.x=1150;g.enemy.plantFeet();g.input.move=1;
        boolean supported=true,sliding=false;float longestLeg=0;
        for(int i=0;i<100;i++){
            float front=g.hero.frontFoot,back=g.hero.backFoot;advance(g,1);
            if(g.hero.frontLift>.001f&&g.hero.backLift>.001f)supported=false;
            if(Math.abs(g.hero.frontFoot-front)>.001f&&Math.abs(g.hero.backFoot-back)>.001f)sliding=true;
            pose.sample(g.hero,g.elapsed);
            float dx=pose.joints[14]-(pose.joints[0]+6),dy=pose.joints[15]-(pose.joints[1]+8);
            longestLeg=Math.max(longestLeg,(float)Math.sqrt(dx*dx+dy*dy));
            dx=pose.joints[16]-pose.joints[14];dy=pose.joints[17]-pose.joints[15];
            longestLeg=Math.max(longestLeg,(float)Math.sqrt(dx*dx+dy*dy));
        }
        check(supported&&!sliding,"Walking always has one stationary supporting foot");
        check(longestLeg<54,"Walking preserves anatomical leg lengths");
        check(g.hero.vx<=150,"Forward movement uses controlled combat-step speed");
        g.input.move=0;advance(g,40);check(g.hero.vx==0&&g.hero.frontLift==0&&g.hero.backLift==0,"Stop settles both feet");
        g=game();g.enemy.x=1150;g.enemy.plantFeet();g.input.move=1;advance(g,20);g.input.jump=true;advance(g,9);g.input.move=0;
        float launchX=g.hero.x;advance(g,10);check(g.hero.x>launchX+10,"Jump retains horizontal momentum after releasing stick");
        float maxHeight=g.hero.y;boolean landed=false;for(int i=0;i<80;i++){advance(g,1);maxHeight=Math.max(maxHeight,g.hero.y);if(g.hero.landing>0)landed=true;}
        check(maxHeight>85&&maxHeight<105,"Jump follows the intended ballistic arc");
        check(landed&&g.hero.y==0,"Jump enters absorption phase on landing");
        g=game();g.input.jump=true;advance(g,5);g.input.punch=true;advance(g,1);check(g.hero.attack==0,"Cannot attack while crouching for takeoff");
        g=game();c=new Controls(g);c.down(1,1066,625);
        check(g.input.kick&&!g.input.punch,"Lower-left attack button is now kick");
        c.clear();c.down(2,1182,562);check(g.input.punch&&!g.input.kick,"Upper-right attack button is now punch");
        c.clear();c.down(3,280,620);check(!g.input.jump,"Removed jump button has no touch action");
        c.down(4,130,620);c.move(4,130,580);
        check(g.input.jump&&g.input.stickY<0&&g.input.move==0,"Up gesture jumps without horizontal movement");
        advance(g,1);check(g.hero.jumpPrep>0,"Up gesture reaches combat simulation");
        for(int i=0;i<120;i++){c.move(4,130,578);advance(g,1);}
        check(g.hero.y==0&&g.hero.jumpPrep==0&&!g.input.jump,"Holding up does not repeatedly jump");
        c.move(4,130,596);check(!g.input.jump,"Threshold noise does not rearm jump");
        c.move(4,130,620);c.move(4,165,580);check(g.input.jump&&g.input.move>0,"Return to center rearms a diagonal jump");
        c.down(5,1182,562);check(g.input.punch&&g.input.move>0,"Attack can be pressed while holding diagonal stick");
        c.up(5);check(g.input.move>0&&g.input.stickY<0,"Attack release preserves both joystick axes");
        c.clear();check(g.input.move==0&&g.input.stickY==0&&!g.input.jump,"Cancel clears joystick axes and queued jump");
        c.down(6,130,620);c.move(999,130,550);check(!g.input.jump,"Other finger cannot trigger joystick jump");
        c.move(6,130,580);advance(g,1);c.up(6);c.down(7,130,580);check(g.input.jump,"New joystick touch rearms upward gesture");
        g=game();g.enemy.x=1150;g.enemy.plantFeet();g.input.move=1;
        int first=-1,second=-1;float firstDuration=0,secondDuration=0;
        for(int i=0;i<40;i++){advance(g,1);if(g.hero.stepping>=0){if(first<0){first=g.hero.stepping;firstDuration=g.hero.stepDuration;}else if(g.hero.stepping!=first){second=g.hero.stepping;secondDuration=g.hero.stepDuration;break;}}}
        check(first==0&&second==1&&secondDuration<firstDuration,"Forward shuffle pairs a lead step with a faster trailing step");
        g=game();g.input.move=-1;advance(g,1);check(g.hero.stepping==1,"Backward shuffle leads with rear foot");
        g=game();pose.sample(g.hero,0);
        check(pose.joints[1]<=-102,"Ready stance keeps pelvis above the old deep squat");
        float readyFront=pose.joints[16],readyBack=pose.joints[20];pose.sample(g.hero,1.2f);
        check(pose.joints[16]==readyFront&&pose.joints[20]==readyBack,"Idle breathing leaves feet planted");
        g.enemy.x=1150;g.enemy.plantFeet();float lastHead=g.hero.x+pose.joints[4],largestHeadStep=0,maxBoneError=0,minWidth=1000,maxWidth=0,minTurn=1000,maxTurn=-1000;
        for(int frame=0;frame<270;frame++){
            g.input.move=frame<90?1:frame<125?0:frame<195?-1:frame<235?1:0;
            advance(g,1);pose.sample(g.hero,g.elapsed);
            float worldHead=g.hero.x+pose.joints[4]*g.hero.facing;
            largestHeadStep=Math.max(largestHeadStep,Math.abs(worldHead-lastHead));lastHead=worldHead;
            minWidth=Math.min(minWidth,pose.joints[16]-pose.joints[20]);maxWidth=Math.max(maxWidth,pose.joints[16]-pose.joints[20]);
            minTurn=Math.min(minTurn,pose.shoulderTurn);maxTurn=Math.max(maxTurn,pose.shoulderTurn);
            for(int leg=0;leg<2;leg++){
                int knee=leg==0?14:18,foot=knee+2;float hx=pose.joints[0]+(leg==0?6:-8),hy=pose.joints[1]+(leg==0?8:3);
                float dx=pose.joints[knee]-hx,dy=pose.joints[knee+1]-hy;
                maxBoneError=Math.max(maxBoneError,Math.abs((float)Math.sqrt(dx*dx+dy*dy)-53));
                dx=pose.joints[foot]-pose.joints[knee];dy=pose.joints[foot+1]-pose.joints[knee+1];
                maxBoneError=Math.max(maxBoneError,Math.abs((float)Math.sqrt(dx*dx+dy*dy)-51));
            }
        }
        check(maxBoneError<.1f,"Both legs retain their lengths during forward, reverse and stop transitions");
        check(minWidth>30&&maxWidth<135,"Feet never cross or spread unnaturally during direction changes");
        check(largestHeadStep<9,"Head remains continuous at start, reverse and stop");
        check(minTurn<-.5f&&maxTurn>.5f,"Shoulders counter-rotate across the paired step");
        g=game();g.enemy.x=1150;g.enemy.plantFeet();g.input.move=1;
        float maxLoad=0,largestLoadChange=0,previousLoad=0;
        for(int i=0;i<100;i++){
            advance(g,1);maxLoad=Math.max(maxLoad,g.hero.weightDrop);
            largestLoadChange=Math.max(largestLoadChange,Math.abs(g.hero.weightDrop-previousLoad));previousLoad=g.hero.weightDrop;
        }
        check(maxLoad>2.5f&&maxLoad<8,"Walking visibly loads knees without a deep squat");
        check(largestLoadChange<1.2f,"Body weight changes smoothly between fixed steps");
        g.input.move=0;advance(g,100);check(Math.abs(g.hero.weightDrop)<.1f&&Math.abs(g.hero.weightSpeed)<.1f,"Weight settles after stopping without persistent bouncing");
        g=game();g.attack(g.hero,1);g.attack(g.enemy,1);advance(g,8);
        check(g.hero.hp==92&&g.enemy.hp==92&&g.hits==2,"Simultaneous punches trade symmetrically");
        g=game();g.hero.hp=g.enemy.hp=8;g.attack(g.hero,1);g.attack(g.enemy,1);advance(g,8);
        check(g.state==Combat.END&&g.result.equals("НИЧЬЯ"),"Simultaneous lethal punches produce a draw");
        g=game();g.attack(g.enemy,1);advance(g,3);g.attack(g.hero,1);advance(g,20);
        check(g.hero.hp==92&&g.enemy.hp==100,"Earlier enemy hit still interrupts a later attack");
        g=game();g.attack(g.hero,2);g.hero.attackTime=.23f;g.attack(g.enemy,1);g.enemy.attackTime=.11f;advance(g,1);
        check(g.hero.hp==92&&g.enemy.hp==86,"Trade preserves each attack's damage after interruption");
        for(int side=0;side<2;side++)for(int swap=0;swap<2;swap++){
            g=game();Combat.Fighter left=swap==0?g.hero:g.enemy,right=swap==0?g.enemy:g.hero;
            left.x=side==0?65:1149;right.x=left.x+66;
            g.hero.plantFeet();g.enemy.plantFeet();g.input.move=swap==0?1:-1;
            boolean separated=true;
            for(int i=0;i<120;i++){advance(g,1);separated&=right.x-left.x>=65.99f&&left.x>=65&&right.x<=1215;}
            check(separated,"Body separation holds at either wall and with swapped fighters");
        }
        g=game();g.enemy.x=1000;g.input.punch=true;advance(g,19);g.input.kick=true;advance(g,9);
        check(g.hero.attack==2,"Late attack input starts after recovery");
        advance(g,60);check(g.hero.attack==0,"Buffered action is consumed once");
        g=game();g.enemy.x=1000;g.input.kick=true;advance(g,3);g.input.punch=true;advance(g,55);
        check(g.hero.attack==0,"Early buffered input expires instead of firing much later");
        g=game();g.enemy.x=1000;g.input.punch=true;advance(g,19);g.input.kick=true;advance(g,1);g.pause();g.resume();advance(g,20);
        check(g.hero.attack==0,"Pause clears buffered attacks");
        g=game();g.enemy.x=1000;g.freeze=.05f;g.input.punch=true;advance(g,8);
        check(g.hero.attack==1,"Input during hit freeze is retained");
        g=game();g.enemy.x=1000;g.input.punch=true;advance(g,19);g.input.kick=true;advance(g,1);new Controls(g).clear();advance(g,20);
        check(g.hero.attack==0,"Touch cancellation clears buffered attacks");
        g=new Combat();g.start();g.input.move=1;g.input.punch=g.input.kick=g.input.jump=true;
        advance(g,179);
        check(g.countdown>0&&g.time==75&&g.hero.x==420&&g.enemy.x==860&&g.hits==0,"Intro locks combat for the first 179 frames");
        check(g.hero.attack==0&&g.hero.jumpPrep==0&&!g.input.punch&&!g.input.kick&&!g.input.jump,"Intro discards attack and jump presses");
        float introRemaining=g.countdown;g.pause();advance(g,120);
        check(g.countdown==introRemaining,"Pause freezes intro");g.resume();advance(g,1);
        check(g.countdown==0&&g.time==75,"Intro completes at exactly 180 fixed steps");
        advance(g,1);check(g.time<75,"Round clock starts after intro");
        g.start();check(g.countdown==3,"Rematch repeats intro");
        Pose other=new Pose();boolean finite=true,planted=true;float greatestDelta=0;
        for(boolean opponent:new boolean[]{false,true}){
            float[] previous=null;
            for(int frame=0;frame<=180;frame++){
                float seconds=frame/60f;pose.sampleIntro(g.hero,seconds,3-seconds,opponent);
                for(int i=0;i<22;i++){
                    finite&=!Float.isNaN(pose.joints[i])&&!Float.isInfinite(pose.joints[i]);
                    if(previous!=null)greatestDelta=Math.max(greatestDelta,Math.abs(pose.joints[i]-previous[i]));
                }
                planted&=pose.joints[16]==40&&pose.joints[20]==-42&&pose.joints[17]==-6&&pose.joints[21]==-6;
                previous=pose.joints.clone();
            }
            other.sample(g.hero,3);float difference=0;
            for(int i=0;i<22;i++)difference=Math.max(difference,Math.abs(pose.joints[i]-other.joints[i]));
            check(difference<.001f,"Intro ends in the exact idle pose");
        }
        check(finite&&planted&&greatestDelta<6,"Intro gestures are continuous and keep feet planted");
        pose.sampleIntro(g.hero,1,2,false);other.sampleIntro(g.enemy,1,2,true);
        check(Math.abs(pose.joints[9]-other.joints[9])>10,"Fighters have distinct readiness gestures");
        g=game();c=new Controls(g);c.down(1,52,542);
        check(g.input.move==0&&g.input.stickY==0,"Outside the circular stick does not capture a touch");
        c.down(2,130,620);c.move(2,134,624);
        check(g.input.move==0&&g.input.stickY==0,"Radial dead zone suppresses center noise");
        c.move(2,2000,2500);
        check(g.input.move*g.input.move+g.input.stickY*g.input.stickY<=1.00001f,"Joystick remains inside its circular travel at arbitrary distances");
        int rollFrames=(int)Math.ceil(Combat.ROLL_DURATION*60);
        for(int direction:new int[]{-1,1}){
            g=game();g.enemy.x=1150;g.enemy.plantFeet();c=new Controls(g);
            c.down(1,130,620);c.move(1,130+40*direction,580);advance(g,1);
            check(g.hero.takeoffVx*direction>200,"Diagonal gesture sets lateral jump impulse from rest");
            c.up(1);float startX=g.hero.x;advance(g,65);
            check((g.hero.x-startX)*direction>150&&g.hero.y==0,"Lateral jump travels in either direction after releasing the stick and lands");
            g=game();g.enemy.x=1150;g.enemy.plantFeet();c=new Controls(g);
            c.down(1,130,620);c.move(1,130+40*direction,660);advance(g,1);
            check(g.hero.rolling&&g.hero.rollDirection==direction,"Lower diagonal starts a directed roll");
            c.up(1);startX=g.hero.x;advance(g,rollFrames+8);
            check(!g.hero.rolling&&Math.abs((g.hero.x-startX)-Combat.ROLL_DISTANCE*direction)<.1f,"Roll covers its designed distance once and exits");
            check(g.hero.y==0&&g.hero.vx==0&&g.hero.frontLift==0,"Roll exits grounded with planted feet");
        }
        g=game();g.enemy.x=1150;g.enemy.plantFeet();c=new Controls(g);c.down(1,170,660);advance(g,rollFrames+20);
        check(!g.hero.rolling,"Holding lower diagonal does not repeat rolls");
        c.move(1,130,620);c.move(1,90,660);advance(g,1);
        check(g.hero.rolling&&g.hero.rollDirection==-1,"Neutral gesture rearms roll in the other direction");
        float rollTime=g.hero.rollTime,rollX=g.hero.x;g.pause();advance(g,80);
        check(g.hero.rollTime==rollTime&&g.hero.x==rollX,"Pause freezes roll animation and displacement");
        g.resume();advance(g,rollFrames+8);check(!g.hero.rolling,"Roll finishes after resuming");
        g=game();c=new Controls(g);c.down(1,130,660);
        check(g.input.roll==0,"Straight down alone does not select an arbitrary roll direction");
        c.move(1,170,660);c.down(2,1182,562);
        check(g.input.roll==1&&g.input.punch,"Multitouch preserves roll gesture alongside attack input");
        c.clear();advance(g,1);check(!g.hero.rolling,"Cancelled gesture does not start a roll");
        g=game();g.input.roll=-1;advance(g,1);g.input.jump=g.input.punch=true;g.input.block=true;advance(g,1);
        check(g.hero.rolling&&g.hero.attack==0&&g.hero.jumpPrep==0&&!g.hero.block,"Roll cannot be combined with attack, jump or guard");
        g=game();g.hero.y=80;g.hero.vy=80;g.input.roll=1;advance(g,1);
        check(!g.hero.rolling,"Cannot start a roll in the air");
        g=game();g.hero.stun=.2f;g.input.roll=1;advance(g,1);check(!g.hero.rolling,"Cannot roll out of stun");
        g=game();g.hero.rollCooldown=.2f;g.input.roll=1;advance(g,1);check(!g.hero.rolling,"Roll recovery prevents immediate reuse");
        g=game();g.input.roll=1;advance(g,1);g.hero.rollTime=Combat.ROLL_DURATION*.25f;g.attack(g.enemy,1);g.enemy.attackTime=.12f;advance(g,1);
        check(g.hero.hp==100&&g.hero.rolling,"Tucked roll evades a high punch");
        g=game();g.input.roll=1;advance(g,1);g.attack(g.enemy,1);g.enemy.attackTime=.12f;advance(g,1);
        check(g.hero.hp==92&&!g.hero.rolling,"Roll startup is vulnerable and interrupted by a hit");
        g=game();g.input.roll=1;advance(g,1);g.hero.rollTime=Combat.ROLL_DURATION*.80f;g.attack(g.enemy,1);g.enemy.attackTime=.12f;advance(g,1);
        check(g.hero.hp==92&&!g.hero.rolling,"Roll recovery is vulnerable");
        for(int direction:new int[]{-1,1}){
            g=game();g.hero.x=direction<0?66:1214;g.enemy.x=640;g.hero.plantFeet();g.enemy.plantFeet();g.input.roll=direction;advance(g,rollFrames+8);
            check(g.hero.x>=65&&g.hero.x<=1215&&!g.hero.rolling,"Roll stops at either arena boundary without getting stuck");
        }
        g=game();g.input.roll=1;boolean bodiesApart=true;
        for(int i=0;i<rollFrames+2;i++){advance(g,1);bodiesApart&=g.enemy.x-g.hero.x>=65.99f;}
        check(bodiesApart,"Roll cannot pass through an opponent");
        g.start();g.input.roll=1;advance(g,180);check(!g.hero.rolling&&g.input.roll==0,"Intro discards roll inputs");
        g=game();g.input.roll=1;advance(g,1);float maxRollJump=0;float[] lastWorld=null;boolean rollFinite=true;
        for(int frame=0;frame<=100;frame++){
            g.hero.rollTime=Combat.ROLL_DURATION*frame/100f;g.hero.x=g.hero.rollOriginX+g.hero.rollDirection*RollAnimation.travel(g.hero,g.hero.rollTime);pose.sample(g.hero,0);
            double angle=Math.toRadians(pose.rotation);float[] world=new float[22];
            for(int i=0;i<22;i+=2){
                float xx=pose.joints[i],yy=pose.joints[i+1]+65;
                world[i]=(float)(xx*Math.cos(angle)-yy*Math.sin(angle));
                world[i+1]=-65+(float)(xx*Math.sin(angle)+yy*Math.cos(angle));
                rollFinite&=Float.isFinite(world[i])&&Float.isFinite(world[i+1]);
                if(lastWorld!=null)maxRollJump=Math.max(maxRollJump,(float)Math.hypot(world[i]-lastWorld[i],world[i+1]-lastWorld[i+1]));
            }
            lastWorld=world;
        }
        check(rollFinite&&maxRollJump<20,"Roll pose has no discontinuities through tuck, rotation and recovery");
        g.hero.rolling=false;g.hero.plantFeet();other.sample(g.hero,0);float exitError=0;
        for(int i=0;i<22;i++)exitError=Math.max(exitError,Math.abs(pose.joints[i]-other.joints[i]));
        check(exitError<.1f,"Roll recovery joins the standing pose continuously");
        g=game();c=new Controls(g);c.down(1,170,580);c.up(1);advance(g,1);
        check(g.hero.takeoffVx>200,"Quick diagonal tap retains jump direction even if finger releases before tick");
        g=game();g.input.roll=-1;advance(g,1);g.start();
        check(!g.hero.rolling&&g.hero.rollCooldown==0&&g.input.roll==0,"New match resets roll and pending gestures");
        for(int facing:new int[]{-1,1})for(int direction:new int[]{-1,1}){
            g=game();g.hero.facing=facing;g.hero.plantFeet();pose.sample(g.hero,0);
            System.arraycopy(pose.joints,0,g.hero.rollStart,0,22);
            g.hero.rolling=true;g.hero.rollDirection=direction;
            float[] previous=null;float maxDelta=0,maxEdge=-1000;
            for(int i=0;i<=100;i++){
                g.hero.rollTime=Combat.ROLL_DURATION*i/100f;g.hero.x=g.hero.rollOriginX+g.hero.rollDirection*RollAnimation.travel(g.hero,g.hero.rollTime);pose.sample(g.hero,0);
                for(int j=0;j<22;j+=2){
                    if(previous!=null)maxDelta=Math.max(maxDelta,(float)Math.hypot(pose.joints[j]-previous[j],pose.joints[j+1]-previous[j+1]));
                }
                maxEdge=Math.max(maxEdge,renderedRollBottom(g.hero,pose.joints));
                previous=pose.joints.clone();
            }
            check(maxEdge<.01f,"Roll limbs, head and torso remain above the floor in both directions");
            check(maxDelta<20,"Forward and backward rolls have no knee branch flips");
        }
        for(int facing:new int[]{-1,1})for(int direction:new int[]{-1,1}){
            g=game();g.hero.facing=facing;g.hero.plantFeet();pose.sample(g.hero,0);
            System.arraycopy(pose.joints,0,g.hero.rollStart,0,22);g.hero.rolling=true;g.hero.rollDirection=direction;
            float minReach=1000,referenceBoneError=0,minStagger=1000;
            for(float local:new float[]{.44f,.50f,.56f}){
                g.hero.rollTime=Combat.ROLL_DURATION*(direction*facing<0?1-local:local);g.hero.x=g.hero.rollOriginX+direction*RollAnimation.travel(g.hero,g.hero.rollTime);pose.sample(g.hero,0);
                float[] j=pose.joints;float dx=j[2]-j[0],dy=j[3]-j[1],len=(float)Math.hypot(dx,dy);
                float ux=dx/len,uy=dy/len,rx=-uy,ry=ux;double[] angle=new double[2];
                for(int leg=0;leg<2;leg++){
                    float hx=j[0]+rx*(leg==0?6:-8)-ux*(leg==0?8:3),hy=j[1]+ry*(leg==0?6:-8)-uy*(leg==0?8:3);
                    int knee=leg==0?14:18,foot=knee+2;
                    angle[leg]=Math.atan2(j[knee+1]-hy,j[knee]-hx);
                    minReach=Math.min(minReach,(float)Math.hypot(j[foot]-hx,j[foot+1]-hy));
                    referenceBoneError=Math.max(referenceBoneError,Math.abs((float)Math.hypot(j[knee]-hx,j[knee+1]-hy)-53));
                    referenceBoneError=Math.max(referenceBoneError,Math.abs((float)Math.hypot(j[foot]-j[knee],j[foot+1]-j[knee+1])-51));
                }
                double difference=angle[0]-angle[1];
                minStagger=Math.min(minStagger,(float)Math.abs(Math.toDegrees(Math.atan2(Math.sin(difference),Math.cos(difference)))));
            }
            check(minReach>50&&referenceBoneError<.01f,"Reference roll keeps open bent legs with fixed bone lengths");
            check(minStagger>20,"Reference roll preserves staggered knees instead of folding both legs together");
        }
        for(int direction:new int[]{-1,1}){
            g=game();g.enemy.x=1150;g.enemy.plantFeet();g.input.roll=direction;advance(g,1);
            float palmMin=Float.POSITIVE_INFINITY,palmMax=Float.NEGATIVE_INFINITY;
            float soleMin=Float.POSITIVE_INFINITY,soleMax=Float.NEGATIVE_INFINITY,maxGroundGap=0,maxSupportGap=0;
            for(int frame=1;frame<100;frame++){
                g.hero.rollTime=Combat.ROLL_DURATION*frame/100f;
                g.hero.x=g.hero.rollOriginX+direction*RollAnimation.travel(g.hero,g.hero.rollTime);
                pose.sample(g.hero,0);float phase=direction<0?1-frame/100f:frame/100f;
                float bottom=renderedRollBottom(g.hero,pose.joints);
                maxGroundGap=Math.max(maxGroundGap,Math.abs(bottom));
                if(phase>.21f&&phase<.31f){
                    float palm=g.hero.x+pose.joints[8];palmMin=Math.min(palmMin,palm);palmMax=Math.max(palmMax,palm);
                    maxSupportGap=Math.max(maxSupportGap,Math.abs(pose.joints[9]+8));
                }
                if(phase>.80f){float sole=g.hero.x+pose.joints[16];soleMin=Math.min(soleMin,sole);soleMax=Math.max(soleMax,sole);}
            }
            check(maxGroundGap<.01f,"Every roll frame has a ground contact, not only non-penetration");
            check(palmMax-palmMin<.01f&&maxSupportGap<.01f,"Supporting palm remains planted in world space");
            check(soleMax-soleMin<.01f,"Supporting foot stays planted during standing recovery");
        }
        for(int facing:new int[]{-1,1})for(int direction:new int[]{-1,1}){
            g=game();g.hero.facing=facing;g.hero.plantFeet();pose.sample(g.hero,0);
            System.arraycopy(pose.joints,0,g.hero.rollStart,0,22);
            g.hero.rolling=true;g.hero.rollDirection=direction;g.hero.rollOriginX=g.hero.x;
            double previousAngle=0;float previousHip=0,minSpeed=10000,maxSpeed=0,maxDrop=0,minRise=1000;
            int frames=(int)Math.ceil(Combat.ROLL_DURATION*60);
            for(int frame=0;frame<=frames;frame++){
                float local=Math.min(1,frame/(60*Combat.ROLL_DURATION));
                g.hero.rollTime=Combat.ROLL_DURATION*(direction*facing<0?1-local:local);
                g.hero.x=g.hero.rollOriginX+direction*RollAnimation.travel(g.hero,g.hero.rollTime);
                pose.sample(g.hero,0);
                double angle=Math.atan2(pose.joints[3]-pose.joints[1],pose.joints[2]-pose.joints[0]);
                if(local>.35f&&local<.64f){
                    double delta=angle-previousAngle;
                    float speed=(float)Math.toDegrees(Math.atan2(Math.sin(delta),Math.cos(delta)))*60;
                    minSpeed=Math.min(minSpeed,speed);maxSpeed=Math.max(maxSpeed,speed);
                }
                if(local>.72f){
                    float rise=previousHip-pose.joints[1];
                    maxDrop=Math.max(maxDrop,-rise);
                    if(local<.86f)minRise=Math.min(minRise,rise);
                }
                previousAngle=angle;previousHip=pose.joints[1];
            }
            check(minSpeed>300&&maxSpeed<650,"Spine rotates through the middle without a stall or catch-up snap");
            check(maxDrop<.01f&&minRise>.5f,"Planted recovery rises continuously instead of dropping back into a seated pose");
        }
        System.out.println("PASS: "+checks+" combat and input checks");
    }
}
