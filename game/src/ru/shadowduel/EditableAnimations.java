package ru.shadowduel;

/** Shared editor presets and optional game animation installation. */
public final class EditableAnimations {
    public static final String[] IDS={"walk","punch","kick"};
    public static final String[] LABELS={"Ходьба","Удар рукой","Удар ногой"};
    private EditableAnimations(){}
    public static AnimationClip blank(){
        AnimationClip clip=preset(1);AnimationClip.PoseData ready=clip.poses.get(clip.keys.get(0).pose).copy();
        clip.name="Новая анимация";clip.duration=60;clip.startupEnd=12;clip.activeEnd=30;
        clip.poses.clear();clip.keys.clear();clip.poses.put("ready",ready);
        clip.keys.add(new AnimationClip.Key(0,"ready"));clip.keys.add(new AnimationClip.Key(60,"ready"));return clip;
    }
    public static AnimationClip preset(int kind){
        if(kind==0)return ReferenceMotion.walk();
        AnimationClip clip=new AnimationClip();clip.version=2;clip.name=LABELS[kind];
        clip.duration=kind==0?24:kind==1?24:41;
        clip.startupEnd=kind==0?0:kind==1?7:14;
        clip.activeEnd=kind==0?clip.duration:kind==1?11:19;
        clip.left=-140;clip.right=180;clip.top=-230;clip.bottom=30;
        Combat game=new Combat();game.start();game.countdown=0;game.botEnabled=false;
        game.hero.x=500;game.enemy.x=1200;game.hero.plantFeet();
        if(kind==0){game.input.move=1;for(int i=0;i<192;i++)game.tick(1f/60);}
        else game.attack(game.hero,kind);
        AnimationClip.PoseData previous=null;Pose pose=new Pose();
        for(int frame=0;frame<=clip.duration;frame++){
            // Preserve contact frames, while keeping the timeline easy to edit.
            boolean key=kind==0||frame%3==0||frame==clip.duration||frame==clip.startupEnd;
            if(key){
                pose.sample(game.hero,game.elapsed);
                AnimationClip.PoseData data=AnimationRig.fromPose(pose,previous);
                if(frame==clip.duration){
                    data=clip.poses.get("frame-0").copy();
                }
                data.name=LABELS[kind]+" · "+frame;
                String id="frame-"+frame;clip.poses.put(id,data);
                AnimationClip.Key k=new AnimationClip.Key(frame,id);clip.keys.add(k);previous=data;
            }
            game.tick(1f/60);
        }
        AnimationRig.upgrade(clip);return clip;
    }
    public static void install(Combat game,int kind,AnimationClip clip){
        if(kind<0||kind>2)throw new IllegalArgumentException("Unknown animation slot");
        if(!AnimationValidation.structure(clip).isEmpty())throw new IllegalArgumentException("Invalid animation");
        for(AnimationClip.PoseData p:clip.poses.values())if(!p.extended||p.facing!=1)
            throw new IllegalArgumentException("Для ходьбы и ударов нужен расширенный риг без отражения поз");
        game.hero.animations[kind]=clip;game.enemy.animations[kind]=clip;
    }
}
