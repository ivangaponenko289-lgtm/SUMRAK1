package ru.shadowduel.editor;

import ru.shadowduel.*;
import java.util.*;

final class EditorState {
    AnimationClip clip=AnimationClip.create();
    String poseId="ready";AnimationClip.PoseData draft=clip.poses.get(poseId).copy(),clipboard;
    double frame=0;boolean editing=true;
    java.io.File file;
    int slot=-1;
    private final Deque<Snapshot> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private static final class Snapshot {
        AnimationClip clip;String id;AnimationClip.PoseData draft;double frame;boolean editing;java.io.File file;int slot;
        Snapshot(EditorState s){clip=s.clip.copy();id=s.poseId;draft=s.draft.copy();frame=s.frame;editing=s.editing;file=s.file;slot=s.slot;}
        void restore(EditorState s){s.clip=clip.copy();s.poseId=id;s.draft=draft.copy();s.frame=frame;s.editing=editing;s.file=file;s.slot=slot;}
    }
    void checkpoint(){undo.push(new Snapshot(this));while(undo.size()>80)undo.removeLast();redo.clear();}
    void undo(){if(!undo.isEmpty()){redo.push(new Snapshot(this));undo.pop().restore(this);}}
    void redo(){if(!redo.isEmpty()){undo.push(new Snapshot(this));redo.pop().restore(this);}}
    void select(String id){poseId=id;draft=clip.poses.get(id).copy();editing=true;}
    void savePose(){clip.poses.put(poseId,draft.copy());}
    void duplicate(){String id="pose-"+UUID.randomUUID();draft=draft.copy();draft.name+=" — копия";poseId=id;savePose();editing=true;}
    void delete(){
        if(clip.poses.size()<=1)throw new IllegalArgumentException("Нужна хотя бы одна поза");
        for(AnimationClip.Key k:clip.keys)if(k.pose.equals(poseId))throw new IllegalArgumentException("Поза используется ключами. Сначала удалите или переназначьте их.");
        clip.poses.remove(poseId);select(clip.poses.keySet().iterator().next());
    }
}
