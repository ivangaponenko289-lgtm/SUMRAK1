package ru.shadowduel;

public final class ImportTest {
    public static void main(String[] args){
        int duration=Math.round(Combat.ROLL_DURATION*60);
        for(int step:new int[]{1,3,7,duration}){
            AnimationClip a=ImportForwardRoll.convert(step);if(a.keys.get(a.keys.size()-1).frame!=duration||a.keys.size()!=(duration-1)/step+2)throw new AssertionError("key sampling/end inclusion");
            AnimationJson.decode(AnimationJson.encode(a));
            for(int f=0;f<=duration;f++)if(!AnimationValidation.checkLengths(AnimationRuntime.sampleAnimation(a,f),AnimationRig.joints(AnimationRuntime.sampleAnimation(a,f)),f).isEmpty())throw new AssertionError("import bone lengths");
            System.out.println("N="+step+": "+a.keys.size()+" keys, validation "+AnimationValidation.validate(a,30).size());
            if(step==3){int n=0;for(AnimationValidation.Issue issue:AnimationValidation.validate(a,30))if(n++<8)System.out.println(issue);}
        }
        System.out.println("PASS: procedural import, endpoint inclusion, JSON round trip and all-frame bone lengths");
    }
}
