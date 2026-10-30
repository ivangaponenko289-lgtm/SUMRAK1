package ru.shadowduel;

/** Platform-independent tests for the editor/game animation contract. */
public final class AnimationTest {
    static int checks;
    static void check(boolean v,String s){checks++;if(!v)throw new AssertionError(s);}
    static void close(double a,double b,String s){check(Math.abs(a-b)<.001,s+": "+a+" != "+b);}
    static boolean has(java.util.List<AnimationValidation.Issue> issues,String code){for(AnimationValidation.Issue i:issues)if(i.code.equals(code))return true;return false;}
    static void rejects(Runnable operation,String message){boolean caught=false;try{operation.run();}catch(IllegalArgumentException ex){caught=true;}check(caught,message);}
    public static void main(String[] args){
        AnimationClip a=AnimationClip.create();AnimationClip.PoseData q=AnimationRig.bindPose();q.x=92;q.angles[0]+=360;a.poses.put("end",q);a.keys.get(1).pose="end";
        close(AnimationRuntime.sampleAnimation(a,30).x,42,"linear midpoint");
        close(AnimationRuntime.sampleAnimation(a,30).angles[0],q.angles[0]-180,"unwrapped full turn");
        close(AnimationRuntime.sampleAnimation(a,-10).x,-8,"before first key");close(AnimationRuntime.sampleAnimation(a,99).x,92,"after last key");
        for(String e:AnimationRuntime.EASINGS){a.keys.get(0).easing=e;close(AnimationRuntime.sampleAnimation(a,60).x,92,"exact last key "+e);}
        a.keys.get(0).easing="snap";close(AnimationRuntime.sampleAnimation(a,59.99).x,-8,"snap holds previous pose");
        close(AnimationRuntime.ease("ease-in",.5),.25,"ease in");close(AnimationRuntime.ease("ease-out",.5),.75,"ease out");close(AnimationRuntime.ease("ease-in-out",.5),.5,"ease in out");
        AnimationClip.PoseData sample=AnimationRuntime.sampleAnimation(a,0);sample.x=999;close(a.poses.get("ready").x,-8,"sampler does not mutate input");
        for(int facing:new int[]{-1,1})for(int end:new int[]{3,5,7,9}){
            AnimationClip.PoseData p=AnimationRig.bindPose();p.facing=facing;
            AnimationRig.ik(p,end,facing*35,-55,false);float[] j=AnimationRig.joints(p);
            for(int b=0;b<10;b++){double[] s=AnimationRig.start(j,b,p.socketWeight);int e=AnimationRig.END[b];close(Math.hypot(j[e]-s[0],j[e+1]-s[1]),AnimationRig.length(b),"IK bone length "+b);}
            AnimationRig.ik(p,end,facing*10000,-10000,false);j=AnimationRig.joints(p);for(float v:j)check(Float.isFinite(v),"unreachable IK stays finite");
        }
        AnimationClip.PoseData p=AnimationRig.bindPose();float[] before=AnimationRig.worldJoints(p);AnimationRig.mirror(p);float[] after=AnimationRig.worldJoints(p);
        for(int i=0;i<22;i+=2){close(after[i],-before[i],"mirror X");close(after[i+1],before[i+1],"mirror Y");}
        AnimationClip valid=AnimationClip.create();check(AnimationValidation.validate(valid,30).isEmpty(),"idle passes validation");
        valid.poses.get("ready").y+=20;check(has(AnimationValidation.validate(valid,30),"ground"),"feet below ground detected");
        valid.airborne=true;check(!has(AnimationValidation.validate(valid,30),"ground"),"jump exemption applies only to ground check");
        valid=AnimationClip.create();valid.left=-43;check(has(AnimationValidation.validate(valid,30),"frame"),"actual foot outline is included in frame bounds");
        valid=AnimationClip.create();float[] corrupt=AnimationRig.joints(valid.poses.get("ready"));corrupt[8]+=8;
        check(has(AnimationValidation.checkLengths(valid.poses.get("ready"),corrupt,0),"length"),"changed bone length detected");
        valid=AnimationClip.create();AnimationClip.PoseData start=valid.poses.get("ready");start.angles[6]=-start.angles[0];start.angles[7]=0;
        AnimationClip.PoseData finish=start.copy();finish.angles[6]+=180;valid.poses.put("end",finish);valid.keys.get(1).pose="end";
        java.util.List<AnimationValidation.Issue> issues=AnimationValidation.validate(valid,30);boolean middle=false,endpoints=false;
        for(AnimationValidation.Issue i:issues)if(i.code.equals("ground")){middle|=i.frame>0&&i.frame<60;endpoints|=i.frame==0||i.frame==60;}
        check(middle&&!endpoints,"validator finds interpolation penetration between valid keys");
        valid.keys.get(0).easing="snap";check(has(AnimationValidation.validate(valid,30),"angle"),"snap angular jump detected");
        check(!has(AnimationValidation.validate(valid,360),"angle"),"angular threshold is configurable");
        valid.keys.add(new AnimationClip.Key(0,"ready"));check(has(AnimationValidation.structure(valid),"key"),"duplicate frames rejected");
        valid=AnimationClip.create();valid.startupEnd=40;valid.activeEnd=10;check(has(AnimationValidation.structure(valid),"phases"),"unordered phases rejected");
        valid=AnimationClip.create();valid.poses.get("ready").angles[0]=Double.NaN;check(has(AnimationValidation.structure(valid),"pose"),"non-finite angles rejected");
        AnimationClip original=AnimationClip.create();original.name="Тест \"поза\"\nUnicode";AnimationClip.Box box=new AnimationClip.Box();original.boxes.add(box);
        String json=AnimationJson.encode(original);AnimationClip restored=AnimationJson.decode(json);
        check(restored.name.equals(original.name)&&restored.boxes.size()==1,"JSON Unicode/escapes and volumes round trip");
        close(AnimationRuntime.sampleAnimation(restored,12.5).y,-104,"JSON runtime sample");
        rejects(()->AnimationJson.decode(json.replace("\"version\": 1,","\"version\": 99,")),"unknown version rejected");
        rejects(()->AnimationJson.decode(json.replace("\"frontThigh\": 53.0","\"frontThigh\": 52.0")),"rig length edits rejected on import");
        rejects(()->AnimationJson.decode(json+" garbage"),"trailing JSON garbage rejected");
        rejects(()->AnimationJson.decode(json.replace("\"fps\": 60,","\"fps\": 60, \"fps\": 60,")),"duplicate JSON properties rejected");
        rejects(()->AnimationRuntime.sampleAnimation(original,Double.NaN),"non-finite sample frame rejected");
        AnimationClip.PoseData clipboard=AnimationRig.bindPose();clipboard.x=10001;
        rejects(()->AnimationJson.decodePose(AnimationJson.encodePose(clipboard)),"clipboard root obeys editor limits");
        clipboard.x=0;clipboard.angles[0]=100001;
        rejects(()->AnimationJson.decodePose(AnimationJson.encodePose(clipboard)),"clipboard angles obey editor limits");
        for(int facing:new int[]{1,-1}){
            AnimationClip.PoseData bp=AnimationRig.bindPose();bp.facing=facing;bp.angles[0]+=30;
            double[] center=AnimationRuntime.boxTransform(bp,box);float[] j=AnimationRig.worldJoints(bp);
            close(center[0],j[8],"bone attachment follows mirrored wrist X");close(center[1],j[9],"bone attachment follows wrist Y");
        }
        check(AnimationRuntime.boxActive(box,box.start)&&AnimationRuntime.boxActive(box,box.end)&&!AnimationRuntime.boxActive(box,box.end+1),"hitbox frame range inclusive");
        check(AnimationRuntime.phase(original,11).equals("startup")&&AnimationRuntime.phase(original,12).equals("active")&&AnimationRuntime.phase(original,30).equals("recovery"),"phase boundaries");
        for(int facing:new int[]{1,-1}){
            AnimationClip clip=AnimationClip.create();clip.duration=12;clip.startupEnd=2;clip.activeEnd=8;clip.keys.get(1).frame=12;
            AnimationClip.PoseData ending=clip.poses.get("ready").copy();ending.x+=120;ending.angles[2]+=15;clip.poses.put("end",ending);clip.keys.get(1).pose="end";
            Combat game=new Combat();game.start();game.countdown=0;game.botEnabled=false;game.hero.x=640;game.enemy.x=facing==1?1100:200;game.hero.facing=facing;game.hero.plantFeet();game.enemy.plantFeet();
            game.forwardRollAnimation=clip;game.input.roll=facing;game.tick(1f/60);
            check(game.hero.rollAnimation==clip&&game.hero.rolling,"game explicitly selects authored forward roll");
            for(int n=1;n<6;n++)game.tick(1f/60);
            Pose gamePose=new Pose();gamePose.sample(game.hero,0);AnimationClip.PoseData rendered=AnimationRuntime.sampleAnimation(clip,game.hero.rollTime*60);float[] expected=AnimationRig.worldJoints(rendered);
            for(int i=0;i<22;i+=2){close(game.hero.x+gamePose.joints[i]*facing*gamePose.authoredFacing,640+expected[i]*facing,"game/editor joint X parity");close(gamePose.joints[i+1],expected[i+1],"game/editor joint Y parity");}
            check(Combat.rollEvades(game.hero),"authored active phase provides roll evasion");
            for(int n=0;n<8;n++)game.tick(1f/60);check(!game.hero.rolling,"authored roll uses its own duration");close(game.hero.x,640+120*facing,"authored root motion distance");
            game.start();game.countdown=0;game.botEnabled=false;game.forwardRollAnimation=clip;game.input.roll=-1;game.tick(1f/60);check(game.hero.rollAnimation==null,"backward roll retains original sampler");
        }
        System.out.println("PASS: "+checks+" animation/rig checks");
    }
}
