package ru.shadowduel.editor;

import ru.shadowduel.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Standalone Swing authoring tool. Uses the game's renderer and angular rig adapter. */
public final class PoseEditor extends JPanel {
    final EditorState state=new EditorState();
    final Map<String,JButton> actions=new LinkedHashMap<>();
    final DefaultListModel<String> poseItems=new DefaultListModel<>();
    final JList<String> poseList=new JList<>(poseItems);
    final JLabel status=new JLabel("Перетащите сустав: FK. Кисти и стопы поддерживают IK.");
    final JCheckBox armIK=new JCheckBox("IK рук",true),legIK=new JCheckBox("IK ног",true),handles=new JCheckBox("Суставы",true);
    final JSpinner rootX=number(-8,-10000,10000,.5),rootY=number(-104,-10000,10000,.5),weight=number(0,0,1,.05);
    final JSpinner[] angleFields=new JSpinner[19];
    final JTextField poseName=new JTextField();
    final Viewport viewport=new Viewport();
    final JPanel bottomPanel=new JPanel(new BorderLayout()),topActions=new JPanel(new FlowLayout(FlowLayout.LEFT,5,4));
    final JTabbedPane inspector=new JTabbedPane();
    boolean syncing=false;double zoom=2.0;int selectedBone=-1;
    final Timeline timeline=new Timeline();
    final JSpinner duration=number(60,1,3600,1),frameField=number(0,0,3600,1);
    final JComboBox<String> easing=new JComboBox<>(AnimationRuntime.EASINGS);
    final JSlider speed=new JSlider(25,200,100);
    final JCheckBox loop=new JCheckBox("Loop",true),onion=new JCheckBox("Onion skin",true);
    final JLabel speedLabel=new JLabel("1.00×");
    boolean playing=false;long lastTick;
    final javax.swing.Timer timer=new javax.swing.Timer(16,e->tickPlayback());
    CombatPanel combatPanel;
    ExportPanel exportPanel;

    final JComboBox<String> motion=new JComboBox<>(EditableAnimations.LABELS);
    final Map<Integer,AnimationClip> documents=new HashMap<>();
    int motionSlot=-1;
    JPanel libraryPanel;
    Component fullInspector;
    final JPanel simpleInspector=new JPanel(new GridLayout(0,1,4,9));
    final JComboBox<String> simpleBone=new JComboBox<>(AnimationRig.LABELS);
    final JSpinner simpleAngle=number(0,-100000,100000,1),simpleSpine=number(0,-30,30,.5),simpleShoulder=number(0,-30,30,.5);
    final JSpinner spine=number(0,-30,30,.5),shoulders=number(0,-30,30,.5);
    final JCheckBox advanced=new JCheckBox("Расширенный режим",false);
    PoseEditor(){
        super(new BorderLayout(8,8));setBorder(new EmptyBorder(12,12,12,12));
        JPanel header=new JPanel(new BorderLayout());JLabel title=new JLabel("СУМРАК  /  РЕДАКТОР ПОЗ И АНИМАЦИЙ");title.setFont(title.getFont().deriveFont(Font.BOLD,19));header.add(title,BorderLayout.NORTH);
        JPanel quick=new JPanel(new FlowLayout(FlowLayout.LEFT,6,4));
        quick.add(new JLabel("Анимация:"));quick.add(motion);
        button(quick,"preset","Восстановить заготовку",()->loadMotion(motion.getSelectedIndex(),true));
        button(quick,"applyGame","Применить к игре",()->{try{publishMotion();}catch(Exception ex){throw new IllegalArgumentException(ex.getMessage(),ex);}});
        button(quick,"testGame","Проверить в игре",()->{try{publishMotion();launchGame();}catch(Exception ex){throw new IllegalArgumentException(ex.getMessage(),ex);}});
        quick.add(advanced);advanced.addActionListener(e->simpleMode(!advanced.isSelected()));
        motion.addActionListener(e->{if(!syncing)loadMotion(motion.getSelectedIndex(),false);});
        header.add(quick,BorderLayout.CENTER);
        header.add(topActions,BorderLayout.SOUTH);add(header,BorderLayout.NORTH);
        button(topActions,"undo","Undo",()->{state.undo();refresh();});button(topActions,"redo","Redo",()->{state.redo();refresh();});
        button(topActions,"copy","Копировать",()->{state.clipboard=displayPose().copy();try{Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(AnimationJson.encodePose(state.clipboard)),null);}catch(HeadlessException|IllegalStateException ignored){}message("Поза скопирована");});
        button(topActions,"paste","Вставить",()->{AnimationClip.PoseData copied=state.clipboard;try{Object text=Toolkit.getDefaultToolkit().getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor);copied=AnimationJson.decodePose((String)text);}catch(HeadlessException|IllegalStateException ignored){}catch(Exception ex){throw new IllegalArgumentException("В буфере нет JSON позы");}if(copied!=null){edit();state.draft=copied.copy();refresh();}});
        button(topActions,"mirror","Отразить",()->{edit();AnimationRig.mirror(state.draft);refresh();});
        button(topActions,"fit","Вписать",()->{fit();viewport.repaint();});
        JPanel left=new JPanel(new BorderLayout(5,8));libraryPanel=left;left.setPreferredSize(new Dimension(215,500));
        left.add(new JLabel("БИБЛИОТЕКА ПОЗ"),BorderLayout.NORTH);left.add(new JScrollPane(poseList),BorderLayout.CENTER);
        poseList.setCellRenderer(new DefaultListCellRenderer(){public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean s,boolean f){
            Component c=super.getListCellRendererComponent(l,v,i,s,f);AnimationClip.PoseData p=state.clip.poses.get(v);setText(p==null?"":p.name);setBorder(new EmptyBorder(7,8,7,8));return c;}});
        poseList.addListSelectionListener(e->{if(!syncing&&!e.getValueIsAdjusting()&&poseList.getSelectedValue()!=null){stopPlayback();if(state.editing)state.savePose();state.select(poseList.getSelectedValue());refresh();}});
        JPanel poseActions=new JPanel(new GridLayout(0,1,4,5));
        button(poseActions,"store","Сохранить позу",()->{edit();state.draft.name=poseName.getText().trim();if(state.draft.name.isEmpty())state.draft.name="Поза";state.savePose();refresh();message("Поза сохранена в библиотеке");});
        button(poseActions,"duplicate","Дублировать",()->{state.checkpoint();if(state.editing)state.savePose();state.duplicate();refresh();});
        button(poseActions,"deletePose","Удалить позу",()->{state.checkpoint();state.delete();refresh();});
        left.add(poseActions,BorderLayout.SOUTH);add(left,BorderLayout.WEST);
        JPanel posePanel=new JPanel(new GridLayout(0,1,3,3));
        posePanel.add(new JLabel("Имя (переименовать → сохранить)"));posePanel.add(poseName);
        posePanel.add(row("Таз X",rootX));posePanel.add(row("Таз Y",rootY));posePanel.add(row("Поворот креплений",weight));
        for(int i=0;i<19;i++){final int b=i;angleFields[i]=number(0,-100000,100000,1);posePanel.add(row(AnimationRig.LABELS[i],angleFields[i]));angleFields[i].addChangeListener(e->{if(!syncing){edit();state.draft.angles[b]=value(angleFields[b]);viewport.repaint();}});}
        posePanel.add(row("Изгиб поясницы",spine));posePanel.add(row("Разворот плеч",shoulders));
        spine.addChangeListener(e->{if(!syncing){edit();state.draft.spineBend=value(spine);state.draft.extended=true;viewport.repaint();}});
        shoulders.addChangeListener(e->{if(!syncing){edit();state.draft.shoulderTurn=value(shoulders);state.draft.extended=true;viewport.repaint();}});
        posePanel.add(armIK);posePanel.add(legIK);posePanel.add(handles);handles.addActionListener(e->viewport.repaint());
        posePanel.add(new JLabel("Shift + IK: другой сгиб"));posePanel.add(new JLabel("Колесо: масштаб · ПКМ: сдвиг вида"));
        fullInspector=new JScrollPane(posePanel);
        simpleInspector.setBorder(new EmptyBorder(12,10,12,10));
        simpleInspector.add(new JLabel("Выбранная кость"));simpleInspector.add(simpleBone);
        simpleInspector.add(row("Угол, °",simpleAngle));
        simpleInspector.add(row("Изгиб поясницы",simpleSpine));simpleInspector.add(row("Разворот плеч",simpleShoulder));
        simpleInspector.add(new JLabel("<html>1. Выберите кадр внизу.<br>2. Тяните сустав мышью.<br>3. Нажмите Play или пробел.<br>4. Примените результат к игре.</html>"));
        simpleInspector.add(new JLabel("<html>Таз — оранжевая точка.<br>Кисти и стопы — крайние точки.<br>Ctrl+Z — отменить изменение.<br>Ctrl+S — сохранить файл.</html>"));
        simpleBone.addActionListener(e->{if(!syncing){selectedBone=simpleBone.getSelectedIndex();syncing=true;simpleAngle.setValue(displayPose().angles[selectedBone]);syncing=false;viewport.repaint();}});
        simpleAngle.addChangeListener(e->{if(!syncing)angleFields[simpleBone.getSelectedIndex()].setValue(value(simpleAngle));});
        simpleSpine.addChangeListener(e->{if(!syncing)spine.setValue(value(simpleSpine));});
        simpleShoulder.addChangeListener(e->{if(!syncing)shoulders.setValue(value(simpleShoulder));});
        Component[] simpleRows=simpleInspector.getComponents();simpleInspector.removeAll();
        simpleInspector.setLayout(new BoxLayout(simpleInspector,BoxLayout.Y_AXIS));
        for(Component item:simpleRows){
            if(item instanceof JComponent){JComponent component=(JComponent)item;component.setAlignmentX(Component.LEFT_ALIGNMENT);component.setMaximumSize(new Dimension(10000,component.getPreferredSize().height));}
            simpleInspector.add(item);simpleInspector.add(Box.createVerticalStrut(12));
        }
        inspector.addTab("Поза",fullInspector);inspector.setPreferredSize(new Dimension(290,500));add(inspector,BorderLayout.EAST);
        rootX.addChangeListener(e->{if(!syncing){edit();state.draft.x=value(rootX);viewport.repaint();}});
        rootY.addChangeListener(e->{if(!syncing){edit();state.draft.y=value(rootY);viewport.repaint();}});
        weight.addChangeListener(e->{if(!syncing){edit();state.draft.socketWeight=value(weight);viewport.repaint();}});
        add(viewport,BorderLayout.CENTER);bottomPanel.add(status,BorderLayout.SOUTH);add(bottomPanel,BorderLayout.SOUTH);
        bind("control Z","undo");bind("control Y","redo");bind("control C","copy");bind("control V","paste");installTimeline();
        combatPanel=new CombatPanel(this);inspector.addTab("Бой",new JScrollPane(combatPanel));
        exportPanel=new ExportPanel(this);inspector.addTab("Проверки",new JScrollPane(exportPanel));refresh();
        button(topActions,"importRoll","Импорт кувырка…",()->{JSpinner step=number(3,1,Math.round(Combat.ROLL_DURATION*60),1);if(JOptionPane.showConfirmDialog(this,row("Каждый N-й кадр",step),"Импорт forward roll",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){stopPlayback();state.checkpoint();state.clip=ImportForwardRoll.convert((int)value(step));state.select(state.clip.keys.get(0).pose);state.frame=0;state.file=null;refresh();fit();exportPanel.validateNow();message("Forward roll импортирован; точность указана во вкладке «Проверки»");}});
        button(topActions,"new","Новая",()->{stopPlayback();state.checkpoint();state.clip=EditableAnimations.blank();state.select("ready");state.frame=0;state.file=null;refresh();fit();message("Новая анимация");});
    }
    void loadMotion(int slot,boolean reset){
        stopPlayback();
        if(motionSlot>=0){if(state.editing)state.savePose();documents.put(motionSlot,state.clip.copy());}
        state.checkpoint();state.clip=reset||!documents.containsKey(slot)?EditableAnimations.preset(slot):documents.get(slot).copy();
        if(!reset&&!documents.containsKey(slot))try{
            File saved=new File(projectDirectory(),"assets/animations/"+EditableAnimations.IDS[slot]+".json");
            if(saved.isFile())state.clip=AnimationFiles.load(saved);
        }catch(Exception invalid){message("Не удалось открыть сохранённую анимацию: "+invalid.getMessage());}
        AnimationRig.upgrade(state.clip);
        motionSlot=slot;state.slot=slot;syncing=true;motion.setSelectedIndex(slot);syncing=false;state.file=null;state.frame=0;state.select(state.clip.keys.get(0).pose);
        refresh();fit();message("Тяните кисть или стопу. Правка кадра создаёт ключ. Ctrl+S — сохранить JSON.");
    }
    void simpleMode(boolean simple){
        libraryPanel.setVisible(!simple);
        if(simple){JPanel wrapper=new JPanel(new BorderLayout());wrapper.add(simpleInspector,BorderLayout.NORTH);inspector.setComponentAt(0,wrapper);}
        else inspector.setComponentAt(0,fullInspector);
        for(String id:new String[]{"copy","paste","mirror","importRoll","new"})if(actions.containsKey(id))actions.get(id).setVisible(!simple);
        for(int i=1;i<inspector.getTabCount();i++)inspector.setEnabledAt(i,!simple);
        if(simple)inspector.setSelectedIndex(0);
        onion.setSelected(!simple);revalidate();viewport.repaint();
    }
    File projectDirectory()throws Exception{
        File location=new File(PoseEditor.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        File directory=location.isFile()?location.getParentFile():location;
        while(directory!=null){if(new File(directory,"AndroidManifest.xml").isFile())return directory;directory=directory.getParentFile();}
        throw new java.io.IOException("Не найдена папка проекта с AndroidManifest.xml");
    }
    void publishMotion()throws Exception{
        if(state.editing)state.savePose();
        AnimationClip clip=state.clip.copy();EditableAnimations.install(new Combat(),motion.getSelectedIndex(),clip);
        File project=projectDirectory(),assets=new File(project,"assets/animations");
        if(!assets.isDirectory()&&!assets.mkdirs())throw new java.io.IOException("Не удалось создать папку анимаций");
        String name=EditableAnimations.IDS[motion.getSelectedIndex()]+".json";
        AnimationFiles.save(clip,new File(assets,name));
        message("Применено: перезапустите игру на ПК. Для Android нужно пересобрать APK.");
    }
    void launchGame()throws Exception{
        File jar=new File(projectDirectory(),"dist/Sumrak-desktop.jar");
        if(!jar.isFile())throw new java.io.IOException("Сначала соберите desktop-игру");
        File java=new File(System.getProperty("java.home"),"bin/"+(System.getProperty("os.name").startsWith("Windows")?"javaw.exe":"java"));
        new ProcessBuilder(java.getAbsolutePath(),"-jar",jar.getAbsolutePath()).directory(jar.getParentFile()).start();
    }
    void installTimeline(){
        JPanel controls=new JPanel(new FlowLayout(FlowLayout.LEFT,5,4));
        button(controls,"play","Play",()->{playing=!playing;lastTick=System.nanoTime();if(playing){if(state.editing)state.savePose();state.editing=false;timer.start();}else timer.stop();actions.get("play").setText(playing?"Pause":"Play");});
        button(controls,"addKey","+ Ключ",()->{
            state.checkpoint();int frame=(int)Math.round(state.frame);
            if(!state.editing){state.draft=displayPose().copy();state.draft.name="Поза #"+frame;state.duplicate();}
            state.savePose();AnimationClip.Key found=keyAt(frame);
            if(found==null)state.clip.keys.add(new AnimationClip.Key(frame,state.poseId));else found.pose=state.poseId;
            state.frame=frame;refresh();
        });
        button(controls,"deleteKey","− Ключ",()->{AnimationClip.Key k=keyAt((int)Math.round(state.frame));if(k!=null){if(state.clip.keys.size()==1)throw new IllegalArgumentException("Нужен хотя бы один ключ");state.checkpoint();state.clip.keys.remove(k);scrub(state.frame);}});
        controls.add(new JLabel("Кадр"));controls.add(frameField);controls.add(new JLabel("Длина"));controls.add(duration);controls.add(easing);controls.add(loop);controls.add(onion);
        speed.setPreferredSize(new Dimension(100,25));speedLabel.setPreferredSize(new Dimension(60,25));controls.add(new JLabel("Темп"));controls.add(speed);controls.add(speedLabel);
        speed.setToolTipText("Сохраняется в JSON и применяется в игре. Ходьба: темп шага и скорость передвижения.");
        speed.addChangeListener(e->{
            speedLabel.setText(String.format(java.util.Locale.ROOT,"%.2f×",speed.getValue()/100.0));
            if(!syncing){stopPlayback();if(!speed.getValueIsAdjusting()){state.checkpoint();state.clip.playbackSpeed=speed.getValue()/100.0;message("Темп сохранится в JSON и будет применён в игре");}}
        });
        onion.addActionListener(e->viewport.repaint());
        duration.addChangeListener(e->{if(!syncing){int d=(int)value(duration);for(AnimationClip.Key k:state.clip.keys)if(k.frame>d){message("Сначала перенесите ключи за пределами новой длительности");refresh();return;}state.checkpoint();state.clip.duration=d;state.clip.startupEnd=Math.min(state.clip.startupEnd,d);state.clip.activeEnd=Math.min(state.clip.activeEnd,d);scrub(Math.min(state.frame,d));}});
        frameField.addChangeListener(e->{if(!syncing)scrub(value(frameField));});
        easing.addActionListener(e->{if(!syncing){AnimationClip.Key k=keyAt((int)Math.round(state.frame));if(k!=null){state.checkpoint();k.easing=(String)easing.getSelectedItem();}}});
        JPanel body=new JPanel(new BorderLayout());body.add(controls,BorderLayout.NORTH);body.add(timeline,BorderLayout.CENTER);bottomPanel.add(body,BorderLayout.CENTER);
        bind("SPACE","play");
    }
    AnimationClip.Key keyAt(int frame){for(AnimationClip.Key k:state.clip.keys)if(k.frame==frame)return k;return null;}
    void scrub(double frame){if(state.editing)state.savePose();state.frame=Math.max(0,Math.min(state.clip.duration,frame));state.editing=false;state.draft=AnimationRuntime.sampleAnimation(state.clip,state.frame);AnimationClip.Key key=keyAt((int)Math.round(state.frame));if(key!=null&&Math.abs(key.frame-state.frame)<.0001)state.poseId=key.pose;refresh();}
    void stopPlayback(){playing=false;timer.stop();if(actions.containsKey("play"))actions.get("play").setText("Play");}
    void tickPlayback(){long now=System.nanoTime();double dt=Math.min(.1,(now-lastTick)/1e9);lastTick=now;if(!playing)return;
        double next=state.frame+dt*60*state.clip.playbackSpeed;
        if(next>state.clip.duration){if(loop.isSelected())next%=state.clip.duration;else{next=state.clip.duration;playing=false;timer.stop();actions.get("play").setText("Play");}}
        scrub(next);
    }
    final class Timeline extends JPanel {
        AnimationClip.Key dragging;int initialFrame;
        Timeline(){setPreferredSize(new Dimension(600,78));setBackground(new Color(0x182329));
            MouseAdapter m=new MouseAdapter(){
                public void mousePressed(MouseEvent e){playing=false;timer.stop();if(actions.containsKey("play"))actions.get("play").setText("Play");
                    int f=frameAt(e.getX());dragging=null;
                    for(AnimationClip.Key k:state.clip.keys)if(Math.abs(px(k.frame)-e.getX())<9&&e.getY()>24){dragging=k;break;}
                    if(dragging!=null){if(state.editing)state.savePose();state.checkpoint();initialFrame=dragging.frame;state.frame=dragging.frame;state.select(dragging.pose);refresh();}else scrub(f);
                }
                public void mouseDragged(MouseEvent e){int f=frameAt(e.getX());if(dragging!=null){AnimationClip.Key occupied=keyAt(f);if(occupied==null||occupied==dragging){dragging.frame=f;state.frame=f;refresh();}}else scrub(f);}
                public void mouseReleased(MouseEvent e){dragging=null;}
                public void mouseClicked(MouseEvent e){if(e.getClickCount()==2&&e.getY()<26)actions.get("addKey").doClick();}
            };addMouseListener(m);addMouseMotionListener(m);
        }
        int px(double f){return 18+(int)((getWidth()-36)*f/Math.max(1,state.clip.duration));}
        int frameAt(int x){return Math.max(0,Math.min(state.clip.duration,(int)Math.round((x-18)*state.clip.duration/(double)Math.max(1,getWidth()-36))));}
        protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int activeStart=px(state.clip.startupEnd),recovery=px(state.clip.activeEnd);
            g.setColor(new Color(0x465a78));g.fillRect(px(0),56,activeStart-px(0),18);g.setColor(new Color(0x9b6346));g.fillRect(activeStart,56,recovery-activeStart,18);g.setColor(new Color(0x39665c));g.fillRect(recovery,56,px(state.clip.duration)-recovery,18);
            int step=Math.max(1,(int)Math.ceil(state.clip.duration/20.0));g.setFont(new Font("Segoe UI",Font.PLAIN,11));
            for(int f=0;f<=state.clip.duration;f+=step){g.setColor(new Color(0x44545a));g.drawLine(px(f),24,px(f),68);g.setColor(new Color(0xc7d8d4));g.drawString(""+f,px(f)-5,17);}
            for(AnimationClip.Key k:state.clip.keys){int x=px(k.frame);g.setColor(k.frame==Math.round(state.frame)?new Color(0xffbb70):new Color(0x52ddbd));g.fillPolygon(new int[]{x,x+7,x,x-7},new int[]{34,43,52,43},4);}
            g.setColor(new Color(0xf5e4c8));g.drawLine(px(state.frame),22,px(state.frame),70);g.dispose();
        }
    }
    static JSpinner number(double initial,double min,double max,double step){JSpinner s=new JSpinner(new SpinnerNumberModel(initial,min,max,step));s.setPreferredSize(new Dimension(95,25));return s;}
    static double value(JSpinner s){return ((Number)s.getValue()).doubleValue();}
    static JPanel row(String label,JComponent input){JPanel p=new JPanel(new BorderLayout(5,0));p.add(new JLabel(label),BorderLayout.CENTER);p.add(input,BorderLayout.EAST);return p;}
    JButton button(JPanel p,String id,String label,Runnable action){JButton b=new JButton(label);b.setName(id);b.addActionListener(e->{try{if(!id.equals("play"))stopPlayback();action.run();}catch(Exception ex){message("Ошибка: "+ex.getMessage());}});actions.put(id,b);p.add(b);return b;}
    void bind(String key,String action){getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key),action);getActionMap().put(action,new AbstractAction(){public void actionPerformed(ActionEvent e){actions.get(action).doClick();}});}
    void edit(){stopPlayback();state.checkpoint();if(!state.editing){state.draft=displayPose().copy();AnimationClip.Key key=keyAt((int)Math.round(state.frame));if(key!=null&&Math.abs(key.frame-state.frame)<.0001)state.poseId=key.pose;else{state.poseId="pose-"+UUID.randomUUID();state.draft.name="Поза #"+Math.round(state.frame);state.clip.poses.put(state.poseId,state.draft.copy());state.frame=Math.round(state.frame);state.clip.keys.add(new AnimationClip.Key((int)state.frame,state.poseId));}}state.editing=true;}
    void message(String s){status.setText(s);}
    void refresh(){
        syncing=true;if(state.slot>=0){motionSlot=state.slot;motion.setSelectedIndex(state.slot);}poseItems.clear();for(String id:state.clip.poses.keySet())poseItems.addElement(id);poseList.setSelectedValue(state.poseId,true);
        poseName.setText(state.draft.name);rootX.setValue(state.draft.x);rootY.setValue(state.draft.y);weight.setValue(state.draft.socketWeight);
        for(int i=0;i<19;i++)angleFields[i].setValue(state.draft.angles[i]);
        simpleSpine.setValue(state.draft.spineBend);simpleShoulder.setValue(state.draft.shoulderTurn);
        if(selectedBone>=0)simpleBone.setSelectedIndex(selectedBone);
        simpleAngle.setValue(state.draft.angles[simpleBone.getSelectedIndex()]);
        spine.setValue(state.draft.spineBend);shoulders.setValue(state.draft.shoulderTurn);
        speed.setValue((int)Math.round(state.clip.playbackSpeed*100));
        duration.setValue((double)state.clip.duration);frameField.setValue((double)Math.round(state.frame));
        AnimationClip.Key key=keyAt((int)Math.round(state.frame));easing.setEnabled(key!=null);if(key!=null)easing.setSelectedItem(key.easing);
        if(combatPanel!=null)combatPanel.refresh();if(exportPanel!=null)exportPanel.refresh();syncing=false;viewport.repaint();timeline.repaint();
    }
    AnimationClip.PoseData displayPose(){return state.editing?state.draft:AnimationRuntime.sampleAnimation(state.clip,state.frame);}
    void fit(){if(state.clip.right<=state.clip.left||state.clip.bottom<=state.clip.top){message("Исправьте границы кадра во вкладке «Проверки»");return;}double w=Math.max(1,viewport.getWidth()-60),h=Math.max(1,viewport.getHeight()-70);zoom=Math.min(w/(state.clip.right-state.clip.left),h/(state.clip.bottom-state.clip.top));viewport.panX=-(state.clip.left+state.clip.right)*zoom/2;viewport.panY=-(state.clip.top+state.clip.bottom)*zoom/2-viewport.getHeight()*.24;}
    final class Viewport extends JPanel {
        double panX=0,panY=0;int drag=-2;Point last;boolean flipApplied;
        Viewport(){
            setPreferredSize(new Dimension(720,600));setBackground(new Color(0xe3e4df));
            MouseAdapter mouse=new MouseAdapter(){
                public void mousePressed(MouseEvent e){requestFocusInWindow();last=e.getPoint();if(SwingUtilities.isRightMouseButton(e)){drag=-3;return;}
                    AnimationClip.PoseData p=displayPose();float[] j=AnimationRig.worldJoints(p);double nearest=14;drag=-2;
                    for(int i=0;i<(p.segmented?40:30);i+=2){double dist=Math.hypot(sx(j[i])-e.getX(),sy(j[i+1])-e.getY());if(dist<nearest){nearest=dist;drag=i==0?-1:i/2-1;}}
                    if(drag>=-1){edit();state.draft=p.copy();selectedBone=drag;flipApplied=false;refresh();}
                }
                public void mouseDragged(MouseEvent e){
                    if(drag==-3){panX+=e.getX()-last.x;panY+=e.getY()-last.y;last=e.getPoint();repaint();return;}
                    if(drag==-2)return;double x=wx(e.getX()),y=wy(e.getY());
                    if(drag==-1){state.draft.x=x;state.draft.y=y;}else{
                        boolean ik=(drag==3||drag==5)&&armIK.isSelected()||(drag==7||drag==9)&&legIK.isSelected();
                        if(ik){AnimationRig.ik(state.draft,drag,x,y,e.isShiftDown()&&!flipApplied);flipApplied|=e.isShiftDown();}
                        else AnimationRig.fk(state.draft,drag,x,y);
                    }refresh();
                }
                public void mouseReleased(MouseEvent e){drag=-2;}
                public void mouseWheelMoved(MouseWheelEvent e){zoom=Math.max(.3,Math.min(5,zoom*Math.pow(1.1,-e.getPreciseWheelRotation())));repaint();}
            };addMouseListener(mouse);addMouseMotionListener(mouse);addMouseWheelListener(mouse);
        }
        double ox(){return getWidth()/2.0+panX;}double oy(){return getHeight()*.74+panY;}
        double sx(double x){return ox()+x*zoom;}double sy(double y){return oy()+y*zoom;}
        double wx(double x){return (x-ox())/zoom;}double wy(double y){return (y-oy())/zoom;}
        protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(new Color(0xd1d5cf));
            for(int x=-2000;x<=2000;x+=25)g.drawLine((int)sx(x),0,(int)sx(x),getHeight());
            for(int y=-1500;y<=1000;y+=25)g.drawLine(0,(int)sy(y),getWidth(),(int)sy(y));
            g.setColor(new Color(0xb66e4d));g.setStroke(new BasicStroke(2));g.drawLine(0,(int)sy(0),getWidth(),(int)sy(0));
            g.setColor(new Color(0x8d9b96));g.setStroke(new BasicStroke(1));g.drawRect((int)sx(state.clip.left),(int)sy(state.clip.top),(int)((state.clip.right-state.clip.left)*zoom),(int)((state.clip.bottom-state.clip.top)*zoom));
            AnimationClip.PoseData p=displayPose();
            if(onion.isSelected()){
                if(state.frame>0)drawCharacter(g,AnimationRuntime.sampleAnimation(state.clip,state.frame-1),.17f);
                if(state.frame<state.clip.duration)drawCharacter(g,AnimationRuntime.sampleAnimation(state.clip,state.frame+1),.17f);
            }
            drawCharacter(g,p,1);
            if(combatPanel!=null)combatPanel.draw(g,p,state.frame);
            if(handles.isSelected()){
                float[] j=AnimationRig.worldJoints(p);float[] local=AnimationRig.joints(p);
                g.setColor(new Color(0x8850c8b3,true));g.setStroke(new BasicStroke(1));
                for(int b=0;b<(p.segmented?19:14);b++){double[] s=AnimationRig.start(p,local,b);int e=AnimationRig.END[b];g.drawLine((int)sx(s[0]*p.facing),(int)sy(s[1]),(int)sx(j[e]),(int)sy(j[e+1]));}
                for(int i=0;i<(p.segmented?40:30);i+=2){int x=(int)sx(j[i]),y=(int)sy(j[i+1]);g.setColor(i==0?new Color(0xf2af61):new Color(0x52ddbd));g.fillOval(x-5,y-5,10,10);g.setColor(new Color(0x142f2c));g.drawOval(x-5,y-5,10,10);}
            }
            g.setColor(new Color(0x40544f));g.drawString("Земля  Y = 0     ·     "+String.format(java.util.Locale.ROOT,"%.0f%%",zoom*100)+"     ·     "+AnimationRuntime.phase(state.clip,state.frame),14,22);g.dispose();
        }
        void drawCharacter(Graphics2D target,AnimationClip.PoseData p,float alpha){Graphics2D g=(Graphics2D)target.create();g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,alpha));g.translate(ox(),oy());g.scale(zoom,zoom);new HumanRenderer().drawPose(new CanvasPainter(g),0,0,p);g.dispose();}
    }
    static void theme(){
        Color bg=new Color(0x202b31),panel=new Color(0x28353b),fg=new Color(0xe3ece8);
        UIManager.put("Panel.background",bg);UIManager.put("Label.foreground",fg);UIManager.put("CheckBox.background",bg);UIManager.put("CheckBox.foreground",fg);
        UIManager.put("List.background",panel);UIManager.put("List.foreground",fg);UIManager.put("List.selectionBackground",new Color(0x386c61));UIManager.put("List.selectionForeground",Color.WHITE);
        for(String k:new String[]{"Label.font","Button.font","CheckBox.font","List.font","TextField.font","Spinner.font","TabbedPane.font"})UIManager.put(k,new Font("Segoe UI",Font.PLAIN,12));
    }
    static void layoutTree(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layoutTree((Container)child);}
    public static void main(String[] args)throws Exception{
        theme();
        if(args.length==3&&args[0].equals("--preview")){SwingUtilities.invokeAndWait(()->{try{PoseEditor e=new PoseEditor();e.setSize(1440,920);layoutTree(e);e.exportPanel.load(new File(args[1]));e.scrub(Math.round(e.state.clip.duration*.46));e.fit();BufferedImage img=new BufferedImage(1440,920,BufferedImage.TYPE_INT_RGB);Graphics2D g=img.createGraphics();e.paint(g);g.dispose();ImageIO.write(img,"png",new File(args[2]));AnimationFiles.contactSheet(e.state.clip,new File(new File(args[2]).getParentFile(),"forward-roll-contact.png"),3,6);}catch(Exception ex){throw new RuntimeException(ex);}});return;}
        if(args.length>0&&args[0].equals("--smoke")){SwingUtilities.invokeAndWait(()->{try{PoseEditor e=new PoseEditor();e.setSize(1360,900);layoutTree(e);e.fit();
            double before=e.state.draft.angles[3];e.state.checkpoint();AnimationRig.fk(e.state.draft,3,85,-110);if(e.state.draft.angles[3]==before)throw new AssertionError("FK drag");
            e.actions.get("undo").doClick();if(e.state.draft.angles[3]!=before)throw new AssertionError("undo");
            e.actions.get("duplicate").doClick();if(e.state.clip.poses.size()!=2)throw new AssertionError("duplicate");
            e.actions.get("mirror").doClick();if(e.state.draft.facing!=-1)throw new AssertionError("mirror");e.actions.get("undo").doClick();
            e.actions.get("store").doClick();e.scrub(30);e.actions.get("addKey").doClick();if(e.keyAt(30)==null)throw new AssertionError("add timeline key");
            e.easing.setSelectedItem("ease-in-out");if(!e.keyAt(30).easing.equals("ease-in-out"))throw new AssertionError("easing control");
            e.scrub(10);e.actions.get("play").doClick();e.lastTick-=100000000;e.tickPlayback();e.actions.get("play").doClick();if(e.state.frame<=10||e.timer.isRunning())throw new AssertionError("play/pause");
            e.scrub(30);e.actions.get("deleteKey").doClick();if(e.keyAt(30)!=null)throw new AssertionError("delete key");e.actions.get("undo").doClick();if(e.keyAt(30)==null)throw new AssertionError("undo key deletion");
            double firstX=e.state.clip.poses.get(e.keyAt(0).pose).x;e.scrub(30);if(!e.state.poseId.equals(e.keyAt(30).pose))throw new AssertionError("scrubbing selects correct key pose");
            e.rootX.setValue(e.state.draft.x+7);e.actions.get("store").doClick();if(e.state.clip.poses.get(e.keyAt(0).pose).x!=firstX)throw new AssertionError("editing another key overwrote first pose");
            e.actions.get("undo").doClick();e.actions.get("undo").doClick();e.scrub(30);
            int tx=e.timeline.px(30),target=e.timeline.px(36);e.timeline.dispatchEvent(new MouseEvent(e.timeline,MouseEvent.MOUSE_PRESSED,0,0,tx,43,1,false,MouseEvent.BUTTON1));e.timeline.dispatchEvent(new MouseEvent(e.timeline,MouseEvent.MOUSE_DRAGGED,0,MouseEvent.BUTTON1_DOWN_MASK,target,43,0,false,MouseEvent.BUTTON1));e.timeline.dispatchEvent(new MouseEvent(e.timeline,MouseEvent.MOUSE_RELEASED,0,0,target,43,1,false,MouseEvent.BUTTON1));if(e.keyAt(36)==null)throw new AssertionError("drag timeline key");e.actions.get("undo").doClick();e.scrub(0);
            float[] joints=AnimationRig.worldJoints(e.displayPose());int mx=(int)e.viewport.sx(joints[0]),my=(int)e.viewport.sy(joints[1]);double root=e.state.draft.x;
            e.viewport.dispatchEvent(new MouseEvent(e.viewport,MouseEvent.MOUSE_PRESSED,0,0,mx,my,1,false,MouseEvent.BUTTON1));e.viewport.dispatchEvent(new MouseEvent(e.viewport,MouseEvent.MOUSE_DRAGGED,0,MouseEvent.BUTTON1_DOWN_MASK,mx+25,my,0,false,MouseEvent.BUTTON1));e.viewport.dispatchEvent(new MouseEvent(e.viewport,MouseEvent.MOUSE_RELEASED,0,0,mx+25,my,1,false,MouseEvent.BUTTON1));if(e.state.draft.x<=root+10)throw new AssertionError("root mouse drag");e.actions.get("undo").doClick();e.scrub(0);
            e.actions.get("addHit").doClick();e.actions.get("addHurt").doClick();if(e.state.clip.boxes.size()!=2)throw new AssertionError("combat volumes");
            e.combatPanel.startup.setValue(10.0);if(e.state.clip.startupEnd!=10)throw new AssertionError("phase boundary");
            e.actions.get("validate").doClick();if(!e.exportPanel.messages.isEmpty())throw new AssertionError("valid default clip");
            File json=new File(new File(args[1]).getParentFile(),"smoke-animation.json");AnimationFiles.save(e.exportPanel.exportClip(),json);e.exportPanel.load(json);
            if(e.state.clip.boxes.size()!=2||e.state.clip.keys.size()!=3)throw new AssertionError("JSON round trip");
            AnimationFiles.contactSheet(e.state.clip,new File(json.getParentFile(),"smoke-contact.png"),15,3);
            e.scrub(0);String editedId=e.state.poseId;e.rootX.setValue(e.state.draft.x+11);double editedX=e.state.draft.x;
            int otherKey=e.timeline.px(30);e.timeline.dispatchEvent(new MouseEvent(e.timeline,MouseEvent.MOUSE_PRESSED,0,0,otherKey,43,1,false,MouseEvent.BUTTON1));e.timeline.dispatchEvent(new MouseEvent(e.timeline,MouseEvent.MOUSE_RELEASED,0,0,otherKey,43,1,false,MouseEvent.BUTTON1));
            if(e.state.clip.poses.get(editedId).x!=editedX)throw new AssertionError("selecting key lost edited pose");
            e.actions.get("play").doClick();e.rootX.setValue(e.state.draft.x+1);if(e.playing||e.timer.isRunning())throw new AssertionError("editing must pause playback");e.exportPanel.load(json);
            BufferedImage image=new BufferedImage(1360,900,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();e.paint(g);g.dispose();ImageIO.write(image,"png",new File(args[1]));System.out.println("PASS: editor launch, pose operations, timeline keys/easing, playback, undo, render");
        }catch(Exception ex){throw new RuntimeException(ex);}});return;}
        SwingUtilities.invokeLater(()->{PoseEditor editor=new PoseEditor();editor.simpleMode(true);JFrame frame=new JFrame("Сумрак — редактор поз и анимаций");frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);frame.setContentPane(editor);frame.setSize(1400,920);frame.setMinimumSize(new Dimension(1050,720));frame.setLocationRelativeTo(null);frame.setVisible(true);editor.fit();if(args.length==0)editor.loadMotion(1,false);if(args.length>0)try{editor.exportPanel.load(new File(args[0]));}catch(Exception ex){editor.message("Ошибка открытия: "+ex.getMessage());}});
    }
}
