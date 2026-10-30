"""Measure every decoded frame of the forward shuffle and trace the supplied photo.
Run from project root; pass --frames with the decoded native video frame directory.
"""
from pathlib import Path
import cv2,numpy as np,json,argparse
from scipy.ndimage import gaussian_filter1d
parser=argparse.ArgumentParser();parser.add_argument('--frames',type=Path,default=Path('.cache/reference-frames'));args=parser.parse_args()
base=args.frames;rows=[]
# Knee heights read from the enlarged source frames; horizontal centers measured below.
knots=np.array([[19,162,165],[23,162,166],[27,164,168],[31,164,169],[36,164,168],[40,163,166],[44,162,165],[46,162,165]])
for n in range(19,47):
 a=cv2.imread(str(base/f'{n:04d}.png'));mask=np.max(a,axis=2)<95
 def runs(y):
  xs=np.flatnonzero(mask[int(round(y)),145:238])+145
  return [r for r in np.split(xs,np.where(np.diff(xs)>1)[0]+1) if len(r)>3]
 waist=np.mean([np.mean(max(runs(y),key=len)) for y in range(139,144)])
 fy=np.interp(n,knots[:,0],knots[:,1]);by=np.interp(n,knots[:,0],knots[:,2])
 fk=np.mean(runs(fy)[-1]);bk=np.mean(runs(by)[0])
 feet=runs(184);back=np.mean(feet[0]);front=np.mean(feet[-1])
 bag=np.where(mask[120:170,270:310])[1].mean()+270
 head=np.argwhere(mask[96:114,165:210]);hy=max(108,head[:,0].min()+96+9);hx=head[:,1].mean()+165
 crotch=next(y for y in range(147,159) if len(runs(y))>=2)
 rows.append([n,waist,crotch-5,hx,hy,fk,fy,front,bk,by,back,bag])
r=np.array(rows,float);raw=r.copy();r[:,1:]=gaussian_filter1d(r[:,1:],.65,axis=0,mode='nearest')
# Correct foreground camera motion using the stationary punching bag.
cam=r[0,-1]-r[:,-1];world=r[:,1]+cam
stride=(world[-1]-world[0])*2.3
print('Reference stride',stride,'units; period',27/44.29,'seconds')
# Data is pelvis/chest/head and projected knee/ankle targets, relative to a linear root path.
records=[]
for i,row in enumerate(r):
 n,px,py,hx,hy,fkx,fky,fx,bkx,bky,bx,bag=row
 origin=world[0]+(world[-1]-world[0])*i/(len(r)-1)
 def x(v):return (v+cam[i]-origin)*2.3-8
 # Foot clearance: source front transfer 20-30, rear transfer 33-44, only 1-2 native pixels.
 fl=1.8*np.sin(np.pi*np.clip((n-20)/11,0,1))**2
 bl=1.1*np.sin(np.pi*np.clip((n-33)/11,0,1))**2
 records.append([x(px),(py-190)*2.3,x(px+(hx-px)*.65),(hy+12-190)*2.3,x(hx),(hy-190)*2.3,x(fkx),(fky-190)*2.3,x(fx),-6-fl*2.3,x(bkx),(bky-190)*2.3,x(bx),-6-bl*2.3])
# Close the measured cycle with a small, distributed correction for source pixel quantization.
data=np.array(records);delta=data[-1]-data[0];data-=np.arange(len(data))[:,None]/(len(data)-1)*delta
out=Path('tools/reference-motion');np.savetxt(out/'measured-frames.csv',raw,delimiter=',',header='source_frame,pelvis_x,pelvis_y,head_x,head_y,front_knee_x,front_knee_y,front_ankle_x,back_knee_x,back_knee_y,back_ankle_x,bag_x',comments='')
(out/'retarget.json').write_text(json.dumps({'sourceFrames':[19,46],'scale':2.3,'stride':stride,'samples':data.tolist()},indent=2))
java='package ru.shadowduel;\n\n/** Per-frame measurements from source video frames 19–46; see tools/reference-motion. */\nfinal class ReferenceSamples {\n    static final double DISTANCE='+str(round(stride,4))+';\n    static final double[][] DATA={\n'
java+=',\n'.join('        {'+','.join(f'{v:.5f}' for v in row)+'}' for row in data)+'\n    };\n}\n'
Path('src/ru/shadowduel/ReferenceSamples.java').write_text(java,encoding='utf-8')
# The new photo, normalized to 1504 x 1694. Each outline is traced in image pixels.
profiles={
 'TORSO':([(652,442),(603,476),(644,640),(650,718),(658,805),(675,854),(640,929),(674,995),(791,1023),(940,974),(958,904),(928,856),(930,810),(941,749),(968,636),(1000,552),(958,463),(910,418),(785,401)],(795,930),(780,510)),
 'FRONT_THIGH':([(674,854),(536,925),(471,966),(411,1000),(352,1031),(335,1070),(333,1118),(346,1168),(484,1186),(540,1125),(555,1107),(662,1059),(791,1023)],(754,984),(421,1130)),
 'BACK_THIGH':([(831,873),(928,856),(980,950),(1025,1040),(1036,1088),(1062,1147),(1058,1230),(920,1234),(911,1186),(856,1123),(791,1023)],(849,950),(978,1190)),
 'FRONT_CALF':([(335,1108),(343,1282),(362,1322),(362,1444),(380,1450),(500,1435),(505,1366),(493,1310),(487,1200),(518,1140)],(421,1130),(430,1514)),
 'BACK_CALF':([(912,1175),(904,1330),(888,1400),(881,1449),(1023,1454),(1051,1328),(1059,1212),(1061,1140)],(978,1190),(972,1514))}
j='package ru.shadowduel;\n\n/** Bone-local contours traced from photo_2026-10-30_19-03-48.jpg. */\nfinal class ReferenceSilhouette {\n'
for name,(points,a,b) in profiles.items():
 a=np.array(a,float);b=np.array(b,float);v=b-a;length=np.linalg.norm(v);u=v/length;normal=np.array([-u[1],u[0]])
 values=[]
 for pt in points:
  q=np.array(pt)-a
  # Reflection into right-facing canonical space flips the transverse coordinate.
  values += [np.dot(q,u)/length,-np.dot(q,normal)/6.8]
 j+='    static final float[] '+name+'={'+','.join(f'{v:.5f}f' for v in values)+'};\n'
j+='}\n';Path('src/ru/shadowduel/ReferenceSilhouette.java').write_text(j,encoding='utf-8')
