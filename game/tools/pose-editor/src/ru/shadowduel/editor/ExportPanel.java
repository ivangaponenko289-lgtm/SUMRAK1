package ru.shadowduel.editor;

import ru.shadowduel.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

final class ExportPanel extends JPanel {
    final PoseEditor editor;
    final JSpinner threshold=PoseEditor.number(30,1,360,1),left=PoseEditor.number(-180,-10000,10000,10),top=PoseEditor.number(-260,-10000,10000,10),right=PoseEditor.number(440,-10000,10000,10),bottom=PoseEditor.number(40,-10000,10000,10),sheetStep=PoseEditor.number(6,1,3600,1),columns=PoseEditor.number(4,1,10,1);
    final JCheckBox airborne=new JCheckBox("В прыжке: разрешить стопы ниже Y=0");
    final JTextField animationName=new JTextField("Новая анимация");
    final DefaultListModel<AnimationValidation.Issue> messages=new DefaultListModel<>();final JList<AnimationValidation.Issue> issues=new JList<>(messages);
    final JTextArea report=new JTextArea(4,20);
    boolean syncing;
    ExportPanel(PoseEditor e){
        super(new BorderLayout(4,6));editor=e;JPanel settings=new JPanel(new GridLayout(0,1,3,4));
        settings.add(new JLabel("Имя анимации → Enter"));settings.add(animationName);settings.add(PoseEditor.row("Порог скачка °",threshold));settings.add(PoseEditor.row("Левая граница",left));settings.add(PoseEditor.row("Верхняя граница",top));settings.add(PoseEditor.row("Правая граница",right));settings.add(PoseEditor.row("Нижняя граница",bottom));settings.add(airborne);
        settings.add(PoseEditor.row("Шаг PNG-листа",sheetStep));settings.add(PoseEditor.row("Столбцов PNG",columns));
        editor.button(settings,"validate","Validate — все кадры",()->validateNow());
        editor.button(settings,"sheet","Экспорт контактного листа PNG",()->{try{AnimationClip a=exportClip();List<AnimationValidation.Issue> errors=AnimationValidation.structure(a);if(!errors.isEmpty())throw new IllegalArgumentException(errors.get(0).message);File f=choose(true,"png");if(f!=null){AnimationFiles.contactSheet(a,f,(int)PoseEditor.value(sheetStep),(int)PoseEditor.value(columns));editor.message("PNG сохранён: "+f.getName());}}catch(Exception ex){editor.message("Ошибка: "+ex.getMessage());}});
        add(settings,BorderLayout.NORTH);issues.setVisibleRowCount(7);add(new JScrollPane(issues),BorderLayout.CENTER);report.setEditable(false);report.setLineWrap(true);report.setWrapStyleWord(true);report.setText("Проверяются все кадры. Нажмите строку результата, чтобы перейти к кадру.");add(new JScrollPane(report),BorderLayout.SOUTH);
        issues.addListSelectionListener(ev->{AnimationValidation.Issue i=issues.getSelectedValue();if(!ev.getValueIsAdjusting()&&i!=null&&i.frame>=0)editor.scrub(i.frame);});
        for(JSpinner s:new JSpinner[]{left,top,right,bottom})s.addChangeListener(ev->apply());airborne.addActionListener(ev->apply());
        animationName.addActionListener(ev->{editor.state.checkpoint();editor.state.clip.name=animationName.getText();editor.message("Имя анимации сохранено");});
        editor.button(editor.topActions,"open","Открыть JSON",()->{File f=choose(false,"json");if(f!=null)try{load(f);}catch(Exception ex){editor.message("Ошибка загрузки: "+ex.getMessage());}});
        editor.button(editor.topActions,"save","Сохранить JSON",()->save(false));editor.button(editor.topActions,"saveAs","Сохранить как…",()->save(true));editor.bind("control S","save");editor.bind("control O","open");refresh();
    }
    AnimationClip exportClip(){AnimationClip a=editor.state.clip.copy();if(editor.state.editing)a.poses.put(editor.state.poseId,editor.state.draft.copy());return a;}
    void apply(){if(syncing||editor.syncing)return;editor.state.checkpoint();AnimationClip a=editor.state.clip;a.left=PoseEditor.value(left);a.top=PoseEditor.value(top);a.right=PoseEditor.value(right);a.bottom=PoseEditor.value(bottom);a.airborne=airborne.isSelected();editor.viewport.repaint();}
    void refresh(){syncing=true;AnimationClip a=editor.state.clip;animationName.setText(a.name);left.setValue(a.left);top.setValue(a.top);right.setValue(a.right);bottom.setValue(a.bottom);airborne.setSelected(a.airborne);syncing=false;}
    void validateNow(){messages.clear();AnimationClip a=exportClip();List<AnimationValidation.Issue> result=AnimationValidation.validate(a,PoseEditor.value(threshold));for(AnimationValidation.Issue i:result)messages.addElement(i);
        report.setText(result.isEmpty()?"Ошибок нет. Проверено кадров: "+(a.duration+1):"Найдено: "+result.size()+" (первые 500). Проверяются длины, стопы, весь силуэт и углы между соседними кадрами.");if(!a.importReport.isEmpty())report.append("\n\n"+a.importReport);editor.message(result.isEmpty()?"Validate: ошибок нет":"Validate: найдено "+result.size()+" замечаний");}
    void load(File f)throws Exception{AnimationClip a=AnimationFiles.load(f);AnimationRig.upgrade(a);editor.timer.stop();editor.playing=false;editor.actions.get("play").setText("Play");editor.state.checkpoint();editor.state.clip=a;editor.state.select(AnimationRuntime.sortedKeys(a).get(0).pose);editor.state.frame=0;editor.state.file=f;editor.refresh();editor.fit();editor.message("Открыто: "+f.getName());report.setText(a.importReport.isEmpty()?"Файл загружен. Validate проверит все промежуточные кадры.":a.importReport);}
    void save(boolean chooseNew){try{File f=chooseNew||editor.state.file==null?choose(true,"json"):editor.state.file;if(f==null)return;AnimationClip a=exportClip();List<AnimationValidation.Issue> errors=AnimationValidation.structure(a);if(!errors.isEmpty())throw new IllegalArgumentException(errors.get(0).message);AnimationFiles.save(a,f);if(editor.state.editing)editor.state.savePose();editor.state.file=f;editor.message("JSON сохранён: "+f.getAbsolutePath());}catch(Exception ex){editor.message("Ошибка сохранения: "+ex.getMessage());}}
    File choose(boolean save,String extension){JFileChooser chooser=new JFileChooser(editor.state.file==null?new File("."):editor.state.file.getParentFile());chooser.setFileFilter(new FileNameExtensionFilter(extension.toUpperCase(),extension));if(save)chooser.setSelectedFile(new File(extension.equals("json")?"animation.json":"contact-sheet.png"));int result=save?chooser.showSaveDialog(editor):chooser.showOpenDialog(editor);if(result!=JFileChooser.APPROVE_OPTION)return null;File f=chooser.getSelectedFile();if(save&&!f.getName().toLowerCase(java.util.Locale.ROOT).endsWith("."+extension))f=new File(f.getPath()+"."+extension);if(save&&f.exists()&&JOptionPane.showConfirmDialog(editor,"Заменить "+f.getName()+"?","Сохранение",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return null;return f;}
}
