package ru.shadowduel.editor;

import ru.shadowduel.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class CombatPanel extends JPanel {
    final PoseEditor editor;
    final JCheckBox debug=new JCheckBox("Показывать hit / hurt boxes",true);
    final JSpinner startup=PoseEditor.number(12,0,3600,1),active=PoseEditor.number(30,0,3600,1);
    final DefaultListModel<AnimationClip.Box> items=new DefaultListModel<>();final JList<AnimationClip.Box> list=new JList<>(items);
    final JTextField name=new JTextField(),damageType=new JTextField();
    final JComboBox<String> kind=new JComboBox<>(new String[]{"hit","hurt"}),shape=new JComboBox<>(new String[]{"circle","rect"}),bone=new JComboBox<>(AnimationRig.LABELS);
    final JSpinner start=PoseEditor.number(0,0,3600,1),end=PoseEditor.number(30,0,3600,1),along=PoseEditor.number(1,-5,5,.1),x=PoseEditor.number(0,-1000,1000,1),y=PoseEditor.number(0,-1000,1000,1),rotation=PoseEditor.number(0,-3600,3600,1),radius=PoseEditor.number(18,.1,500,1),width=PoseEditor.number(36,.1,1000,1),height=PoseEditor.number(24,.1,1000,1),damage=PoseEditor.number(10,0,100000,1);
    boolean syncing;
    CombatPanel(PoseEditor e){
        super(new BorderLayout(4,6));editor=e;
        JPanel form=new JPanel(new GridLayout(0,1,3,4));form.add(debug);form.add(PoseEditor.row("Startup → active",startup));form.add(PoseEditor.row("Active → recovery",active));
        form.add(new JLabel("Границы фаз — номера кадров"));list.setVisibleRowCount(4);JScrollPane boxScroll=new JScrollPane(list);boxScroll.setPreferredSize(new Dimension(220,100));
        JPanel top=new JPanel(new BorderLayout());top.add(form,BorderLayout.NORTH);top.add(boxScroll,BorderLayout.CENTER);
        JPanel buttons=new JPanel(new GridLayout(1,3,3,3));
        editor.button(buttons,"addHit","+ Hit",()->add(false));editor.button(buttons,"addHurt","+ Hurt",()->add(true));
        editor.button(buttons,"deleteBox","Удалить",()->{AnimationClip.Box b=selected();if(b!=null){editor.state.checkpoint();editor.state.clip.boxes.remove(b);refresh();editor.viewport.repaint();}});
        top.add(buttons,BorderLayout.SOUTH);add(top,BorderLayout.NORTH);
        JPanel fields=new JPanel(new GridLayout(0,1,3,5));
        fields.add(PoseEditor.row("Имя",name));fields.add(PoseEditor.row("Назначение",kind));fields.add(PoseEditor.row("Форма",shape));fields.add(PoseEditor.row("Кость",bone));
        fields.add(PoseEditor.row("Первый кадр",start));fields.add(PoseEditor.row("Последний кадр",end));fields.add(PoseEditor.row("Доля вдоль кости",along));fields.add(PoseEditor.row("Смещение X",x));fields.add(PoseEditor.row("Смещение Y",y));fields.add(PoseEditor.row("Поворот °",rotation));
        fields.add(PoseEditor.row("Радиус",radius));fields.add(PoseEditor.row("Ширина",width));fields.add(PoseEditor.row("Высота",height));fields.add(PoseEditor.row("Урон",damage));fields.add(PoseEditor.row("Тип урона",damageType));
        fields.add(new JLabel("Объёмы активны в своём диапазоне"));add(fields,BorderLayout.CENTER);
        startup.addChangeListener(ev->phases());active.addChangeListener(ev->phases());debug.addActionListener(ev->editor.viewport.repaint());
        list.addListSelectionListener(ev->{if(!syncing&&!ev.getValueIsAdjusting())loadFields();});
        for(JSpinner s:new JSpinner[]{start,end,along,x,y,rotation,radius,width,height,damage})s.addChangeListener(ev->apply());
        for(JComboBox<?> c:new JComboBox<?>[]{kind,shape,bone})c.addActionListener(ev->apply());
        DocumentListener listener=new DocumentListener(){public void insertUpdate(DocumentEvent e){apply();}public void removeUpdate(DocumentEvent e){apply();}public void changedUpdate(DocumentEvent e){apply();}};
        name.getDocument().addDocumentListener(listener);damageType.getDocument().addDocumentListener(listener);refresh();
    }
    AnimationClip.Box selected(){return list.getSelectedValue();}
    void add(boolean hurt){editor.state.checkpoint();AnimationClip.Box b=new AnimationClip.Box();b.name=hurt?"Хёртбокс головы":"Удар кистью";b.kind=hurt?"hurt":"hit";b.bone=hurt?1:3;b.radius=hurt?22:18;b.damage=hurt?0:10;b.start=hurt?0:editor.state.clip.startupEnd;b.end=hurt?editor.state.clip.duration:Math.max(b.start,editor.state.clip.activeEnd-1);editor.state.clip.boxes.add(b);refresh();list.setSelectedValue(b,true);editor.viewport.repaint();}
    void phases(){if(syncing||editor.syncing)return;editor.state.checkpoint();editor.state.clip.startupEnd=(int)PoseEditor.value(startup);editor.state.clip.activeEnd=(int)PoseEditor.value(active);editor.timeline.repaint();editor.viewport.repaint();}
    void apply(){if(syncing||editor.syncing)return;AnimationClip.Box b=selected();if(b==null)return;editor.state.checkpoint();b.name=name.getText();b.kind=(String)kind.getSelectedItem();b.shape=(String)shape.getSelectedItem();b.bone=bone.getSelectedIndex();b.start=(int)PoseEditor.value(start);b.end=(int)PoseEditor.value(end);b.along=PoseEditor.value(along);b.x=PoseEditor.value(x);b.y=PoseEditor.value(y);b.rotation=PoseEditor.value(rotation);b.radius=PoseEditor.value(radius);b.width=PoseEditor.value(width);b.height=PoseEditor.value(height);b.damage=PoseEditor.value(damage);b.damageType=damageType.getText();list.repaint();editor.viewport.repaint();}
    void refresh(){syncing=true;int index=list.getSelectedIndex();startup.setValue((double)editor.state.clip.startupEnd);active.setValue((double)editor.state.clip.activeEnd);items.clear();for(AnimationClip.Box b:editor.state.clip.boxes)items.addElement(b);if(!items.isEmpty())list.setSelectedIndex(Math.max(0,Math.min(index,items.size()-1)));syncing=false;loadFields();}
    void loadFields(){syncing=true;AnimationClip.Box b=selected();boolean enabled=b!=null;
        for(JComponent c:new JComponent[]{name,kind,shape,bone,start,end,along,x,y,rotation,radius,width,height,damage,damageType})c.setEnabled(enabled);
        if(b!=null){name.setText(b.name);kind.setSelectedItem(b.kind);shape.setSelectedItem(b.shape);bone.setSelectedIndex(b.bone);start.setValue((double)b.start);end.setValue((double)b.end);along.setValue(b.along);x.setValue(b.x);y.setValue(b.y);rotation.setValue(b.rotation);radius.setValue(b.radius);width.setValue(b.width);height.setValue(b.height);damage.setValue(b.damage);damageType.setText(b.damageType);}syncing=false;}
    void draw(Graphics2D graphics,AnimationClip.PoseData pose,double frame){if(!debug.isSelected())return;
        for(AnimationClip.Box b:editor.state.clip.boxes){if(!AnimationRuntime.boxActive(b,frame))continue;double[] t=AnimationRuntime.boxTransform(pose,b);Graphics2D g=(Graphics2D)graphics.create();
            g.translate(editor.viewport.sx(t[0]),editor.viewport.sy(t[1]));g.rotate(Math.toRadians(t[2]));g.scale(editor.zoom,editor.zoom);
            Shape s=b.shape.equals("circle")?new Ellipse2D.Double(-b.radius,-b.radius,b.radius*2,b.radius*2):new Rectangle2D.Double(-b.width/2,-b.height/2,b.width,b.height);
            Color color=b.kind.equals("hit")?new Color(0xe57f58):new Color(0x409ed5);g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),65));g.fill(s);g.setColor(color);g.setStroke(new BasicStroke((float)(b==selected()?2/editor.zoom:1/editor.zoom)));g.draw(s);g.drawLine(-4,0,4,0);g.drawLine(0,-4,0,4);g.dispose();
        }
    }
}
