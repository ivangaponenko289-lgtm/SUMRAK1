package ru.shadowduel;

import java.io.*;
import java.util.*;

/** Strict, dependency-free JSON codec shared by Android and the authoring tool. */
public final class AnimationJson {
    private AnimationJson(){}
    public static AnimationClip read(InputStream in)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
        while((n=in.read(buffer))!=-1){out.write(buffer,0,n);if(out.size()>8*1024*1024)throw new IOException("Animation exceeds 8 MB");}
        return decode(new String(out.toByteArray(),"UTF-8"));
    }
    public static String encode(AnimationClip a){
        Map<String,Object> root=obj("format","shadowduel-animation","version",a.version,"fps",a.fps,"name",a.name,"durationFrames",a.duration,"duringJump",a.airborne,"playbackSpeed",a.playbackSpeed);if(a.walkDistance>0)root.put("walkDistance",a.walkDistance);
        AnimationClip.PoseData metric=new AnimationClip.PoseData();metric.extended=a.version>=2;metric.segmented=a.version==3;
        Map<String,Object> lengths=new LinkedHashMap<>();for(int i=0;i<(a.version==3?19:a.version==2?14:10);i++)lengths.put(AnimationRig.BONES[i],AnimationRig.length(metric,i));
        root.put("rig",obj("id",a.version==3?AnimationRig.SEGMENTED_ID:a.version==2?AnimationRig.EXTENDED_ID:AnimationRig.ID,"lengths",lengths));root.put("bounds",obj("left",a.left,"top",a.top,"right",a.right,"bottom",a.bottom));
        Map<String,Object> poses=new LinkedHashMap<>();for(Map.Entry<String,AnimationClip.PoseData> e:a.poses.entrySet())poses.put(e.getKey(),poseMap(e.getValue()));root.put("poses",poses);
        List<Object> keys=new ArrayList<>();for(AnimationClip.Key k:AnimationRuntime.sortedKeys(a))keys.add(obj("frame",k.frame,"pose",k.pose,"easing",k.easing));root.put("keys",keys);
        root.put("phases",obj("startupEnd",a.startupEnd,"activeEnd",a.activeEnd));List<Object> boxes=new ArrayList<>();
        for(AnimationClip.Box b:a.boxes)boxes.add(obj("name",b.name,"kind",b.kind,"shape",b.shape,"bone",AnimationRig.BONES[b.bone],"start",b.start,"end",b.end,"along",b.along,"offsetX",b.x,"offsetY",b.y,"rotation",b.rotation,"radius",b.radius,"width",b.width,"height",b.height,"damage",b.damage,"damageType",b.damageType));
        root.put("hitboxes",boxes);if(!a.importReport.isEmpty())root.put("importReport",a.importReport);
        StringBuilder result=new StringBuilder();write(root,result,0);return result.append('\n').toString();
    }
    public static String encodePose(AnimationClip.PoseData p){StringBuilder b=new StringBuilder();write(poseMap(p),b,0);return b.toString();}
    public static AnimationClip.PoseData decodePose(String text){
        AnimationClip.PoseData p=pose(map(new Parser(text).parse()));
        AnimationClip check=AnimationClip.create();check.poses.put("ready",p);
        List<AnimationValidation.Issue> errors=AnimationValidation.structure(check);
        if(!errors.isEmpty())throw new IllegalArgumentException(errors.get(0).message);
        return p;
    }
    private static Map<String,Object> poseMap(AnimationClip.PoseData p){Map<String,Object> angles=new LinkedHashMap<>();for(int i=0;i<19;i++)angles.put(AnimationRig.BONES[i],p.angles[i]);Map<String,Object> result=obj("name",p.name,"root",obj("x",p.x,"y",p.y),"facing",p.facing,"socketWeight",p.socketWeight,"extended",p.extended,"segmented",p.segmented,"spineBend",p.spineBend,"shoulderTurn",p.shoulderTurn,"angles",angles);Map<String,Object> projection=new LinkedHashMap<>();for(int i=0;i<19;i++)if(p.projection[i]!=1)projection.put(AnimationRig.BONES[i],p.projection[i]);if(!projection.isEmpty())result.put("projection",projection);return result;}
    public static AnimationClip decode(String text){
        Map<String,Object> root=map(new Parser(text).parse());if(!str(root,"format").equals("shadowduel-animation"))throw new IllegalArgumentException("Unknown animation format");
        AnimationClip a=new AnimationClip();a.version=integer(root,"version");if(a.version!=1&&a.version!=2&&a.version!=3)throw new IllegalArgumentException("Unsupported animation version: "+a.version);
        a.fps=integer(root,"fps");a.name=str(root,"name");a.duration=integer(root,"durationFrames");a.airborne=bool(root,"duringJump");if(root.containsKey("playbackSpeed"))a.playbackSpeed=num(root,"playbackSpeed");if(root.containsKey("walkDistance"))a.walkDistance=num(root,"walkDistance");
        Map<String,Object> rig=map(root.get("rig")),lengths=map(rig.get("lengths"));if(!str(rig,"id").equals(a.version==3?AnimationRig.SEGMENTED_ID:a.version==2?AnimationRig.EXTENDED_ID:AnimationRig.ID))throw new IllegalArgumentException("Rig does not match this game");
        AnimationClip.PoseData metric=new AnimationClip.PoseData();metric.extended=a.version>=2;metric.segmented=a.version==3;
        for(int i=0;i<(a.version==3?19:a.version==2?14:10);i++)if(Math.abs(num(lengths,AnimationRig.BONES[i])-AnimationRig.length(metric,i))>.00001)throw new IllegalArgumentException("Bone length changed: "+AnimationRig.BONES[i]);
        Map<String,Object> bounds=map(root.get("bounds"));a.left=num(bounds,"left");a.top=num(bounds,"top");a.right=num(bounds,"right");a.bottom=num(bounds,"bottom");
        Map<String,Object> poses=map(root.get("poses"));if(poses.size()>4000)throw new IllegalArgumentException("Too many poses");for(Map.Entry<String,Object> e:poses.entrySet())a.poses.put(e.getKey(),pose(map(e.getValue())));
        for(Object o:list(root.get("keys"))){Map<String,Object> k=map(o);AnimationClip.Key key=new AnimationClip.Key(integer(k,"frame"),str(k,"pose"));key.easing=str(k,"easing");a.keys.add(key);}
        Map<String,Object> phases=map(root.get("phases"));a.startupEnd=integer(phases,"startupEnd");a.activeEnd=integer(phases,"activeEnd");
        for(Object o:list(root.get("hitboxes"))){Map<String,Object> m=map(o);AnimationClip.Box b=new AnimationClip.Box();b.name=str(m,"name");b.kind=str(m,"kind");b.shape=str(m,"shape");b.bone=Arrays.asList(AnimationRig.BONES).indexOf(str(m,"bone"));
            b.start=integer(m,"start");b.end=integer(m,"end");b.along=num(m,"along");b.x=num(m,"offsetX");b.y=num(m,"offsetY");b.rotation=num(m,"rotation");b.radius=num(m,"radius");b.width=num(m,"width");b.height=num(m,"height");b.damage=num(m,"damage");b.damageType=str(m,"damageType");a.boxes.add(b);}
        if(root.containsKey("importReport"))a.importReport=str(root,"importReport");
        List<AnimationValidation.Issue> errors=AnimationValidation.structure(a);if(!errors.isEmpty())throw new IllegalArgumentException(errors.get(0).message);return a;
    }
    private static AnimationClip.PoseData pose(Map<String,Object> m){AnimationClip.PoseData p=new AnimationClip.PoseData();p.name=str(m,"name");if(m.containsKey("projection")){Map<String,Object> projection=map(m.get("projection"));for(int i=0;i<19;i++)if(projection.containsKey(AnimationRig.BONES[i]))p.projection[i]=num(projection,AnimationRig.BONES[i]);}Map<String,Object> r=map(m.get("root")),angles=map(m.get("angles"));p.x=num(r,"x");p.y=num(r,"y");p.facing=integer(m,"facing");p.socketWeight=num(m,"socketWeight");for(int i=0;i<10;i++)p.angles[i]=num(angles,AnimationRig.BONES[i]);if(m.containsKey("segmented"))p.segmented=bool(m,"segmented");if(m.containsKey("extended"))p.extended=bool(m,"extended");if(m.containsKey("spineBend"))p.spineBend=num(m,"spineBend");if(m.containsKey("shoulderTurn"))p.shoulderTurn=num(m,"shoulderTurn");for(int i=10;i<19;i++)if(angles.containsKey(AnimationRig.BONES[i]))p.angles[i]=num(angles,AnimationRig.BONES[i]);if(p.facing!=1&&p.facing!=-1||p.socketWeight<0||p.socketWeight>1)throw new IllegalArgumentException("Invalid pose orientation");return p;}
    private static Map<String,Object> obj(Object... values){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)m.put((String)values[i],values[i+1]);return m;}
    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object v){if(!(v instanceof Map))throw new IllegalArgumentException("Expected JSON object");return (Map<String,Object>)v;}
    @SuppressWarnings("unchecked") private static List<Object> list(Object v){if(!(v instanceof List))throw new IllegalArgumentException("Expected JSON array");return (List<Object>)v;}
    private static String str(Map<String,Object> m,String k){Object v=m.get(k);if(!(v instanceof String))throw new IllegalArgumentException("Expected string: "+k);return (String)v;}
    private static double num(Map<String,Object> m,String k){Object v=m.get(k);if(!(v instanceof Number)||!AnimationRig.finite(((Number)v).doubleValue()))throw new IllegalArgumentException("Expected finite number: "+k);return ((Number)v).doubleValue();}
    private static int integer(Map<String,Object> m,String k){double v=num(m,k);if(v!=Math.rint(v)||Math.abs(v)>1000000)throw new IllegalArgumentException("Expected integer: "+k);return (int)v;}
    private static boolean bool(Map<String,Object> m,String k){Object v=m.get(k);if(!(v instanceof Boolean))throw new IllegalArgumentException("Expected boolean: "+k);return (Boolean)v;}
    private static void write(Object value,StringBuilder b,int depth){
        if(value instanceof Map){b.append('{');boolean first=true;for(Map.Entry<?,?> e:((Map<?,?>)value).entrySet()){if(!first)b.append(',');indent(b,depth+1);quote((String)e.getKey(),b);b.append(": ");write(e.getValue(),b,depth+1);first=false;}if(!first)indent(b,depth);b.append('}');}
        else if(value instanceof List){b.append('[');boolean first=true;for(Object v:(List<?>)value){if(!first)b.append(',');indent(b,depth+1);write(v,b,depth+1);first=false;}if(!first)indent(b,depth);b.append(']');}
        else if(value instanceof String)quote((String)value,b);
        else if(value instanceof Number){if(!AnimationRig.finite(((Number)value).doubleValue()))throw new IllegalArgumentException("Non-finite JSON number");b.append(value);}
        else b.append(value);
    }
    private static void indent(StringBuilder b,int n){b.append('\n');for(int i=0;i<n;i++)b.append("  ");}
    private static void quote(String s,StringBuilder b){b.append('"');for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c=='"'||c=='\\')b.append('\\').append(c);else if(c<' ')b.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else b.append(c);}b.append('"');}
    private static final class Parser {
        final String s;int at=0;
        Parser(String s){if(s.length()>8*1024*1024)throw new IllegalArgumentException("JSON too large");this.s=s.startsWith("\ufeff")?s.substring(1):s;}
        RuntimeException error(){return new IllegalArgumentException("Invalid JSON at character "+at);}
        void ws(){while(at<s.length()&&" \r\n\t".indexOf(s.charAt(at))>=0)at++;}
        Object parse(){Object v=value(0);ws();if(at!=s.length())throw error();return v;}
        Object value(int depth){if(depth>32)throw error();ws();if(at>=s.length())throw error();char c=s.charAt(at++);
            if(c=='{'){Map<String,Object> m=new LinkedHashMap<>();ws();if(take('}'))return m;do{ws();if(!take('"'))throw error();String key=string();ws();if(!take(':')||m.containsKey(key))throw error();m.put(key,value(depth+1));ws();if(take('}'))return m;}while(take(','));throw error();}
            if(c=='['){List<Object> a=new ArrayList<>();ws();if(take(']'))return a;do{a.add(value(depth+1));ws();if(take(']'))return a;}while(take(','));throw error();}
            if(c=='"')return string();at--;
            for(String word:new String[]{"true","false","null"})if(s.startsWith(word,at)){at+=word.length();return word.equals("null")?null:Boolean.valueOf(word);}
            int start=at;while(at<s.length()&&"0123456789.eE+-".indexOf(s.charAt(at))>=0)at++;
            String number=s.substring(start,at);if(!number.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?"))throw error();try{double n=Double.parseDouble(number);if(!AnimationRig.finite(n))throw error();return n;}catch(NumberFormatException ex){throw error();}
        }
        boolean take(char c){if(at<s.length()&&s.charAt(at)==c){at++;return true;}return false;}
        String string(){StringBuilder b=new StringBuilder();while(at<s.length()){char c=s.charAt(at++);if(c=='"')return b.toString();if(c<' ')throw error();if(c=='\\'){if(at>=s.length())throw error();c=s.charAt(at++);switch(c){case '"':case '\\':case '/':break;case 'b':c='\b';break;case 'f':c='\f';break;case 'n':c='\n';break;case 'r':c='\r';break;case 't':c='\t';break;case 'u':if(at+4>s.length())throw error();try{c=(char)Integer.parseInt(s.substring(at,at+4),16);}catch(NumberFormatException e){throw error();}at+=4;break;default:throw error();}}b.append(c);}throw error();}
    }
}
