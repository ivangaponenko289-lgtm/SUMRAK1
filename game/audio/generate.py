"""Original, deterministic PCM sound design. No downloaded samples or dependencies."""
import math
import random
import struct
import wave
from pathlib import Path

RATE=22050
DEST=Path(__file__).resolve().parents[1]/'assets'/'audio'
DEST.mkdir(parents=True,exist_ok=True)
rng=random.Random(240)

def write(name,samples,peak):
    highest=max(abs(v) for v in samples) or 1
    gain=peak/highest
    pcm=[int(max(-.99,min(.99,s*gain))*32767) for s in samples]
    with wave.open(str(DEST/(name+'.wav')),'wb') as w:
        w.setparams((1,2,RATE,len(pcm),'NONE','not compressed'))
        w.writeframes(struct.pack('<'+'h'*len(pcm),*pcm))
    print(name,round(len(pcm)/RATE,2),'seconds; peak',round(max(abs(v) for v in pcm)/32767,3))

def impact(duration=.28,heavy=False):
    out=[];low=0;phase=0
    for i in range(int(RATE*duration)):
        t=i/RATE;noise=rng.uniform(-1,1);low+=.17*(noise-low)
        phase+=2*math.pi*(65+(100 if heavy else 155)*math.exp(-t*35))/RATE
        thud=math.sin(phase)*math.exp(-t*(15 if heavy else 23))
        crack=(noise-low)*math.exp(-t*100)*.6
        flesh=low*math.exp(-t*21)*1.7
        out.append((thud*.8+crack+flesh)*min(1,t/.0015))
    return out

punch=impact();kick=impact(.4,True)
write('punch',punch,.8);write('kick',kick,.88)
swish=[];low=0
for i in range(int(RATE*.23)):
    t=i/RATE;noise=rng.uniform(-1,1);low+=.12*(noise-low)
    swish.append((noise-low)*math.sin(math.pi*t/.23)**2*(.22+.78*math.exp(-((t-.065)/.06)**2)))
write('swing',swish,.36)
block=[]
for i in range(int(RATE*.22)):
    t=i/RATE
    block.append((rng.uniform(-1,1)*math.exp(-t*65)+.32*math.sin(2*math.pi*420*t)*math.exp(-t*26)+.2*math.sin(2*math.pi*870*t)*math.exp(-t*40))*min(1,t/.001))
write('block',block,.64)
# Breathy voiced exertion with harmonic/formant shaping, not a spoken recording.
hurt=[];phase=0;low=0
for i in range(int(RATE*.31)):
    t=i/RATE;phase+=2*math.pi*(116-35*t/.31)/RATE
    noise=rng.uniform(-1,1);low+=.23*(noise-low)
    voice=sum(math.sin(phase*h)*math.exp(-((h*110-550)/380)**2)/h for h in range(1,15))
    env=(1-math.exp(-t*90))*math.exp(-t*12)*min(1,(.31-t)/.04)
    hurt.append((voice*.75+low*.7)*env)
write('hurt',hurt,.38)
land=impact(.2,True);write('land',land,.32)
# 16-second loop: wind, subdued war drums, distant clashes and answering thumps.
length=RATE*16;bed=[0.0]*length;low=0
for i in range(length):
    t=i/RATE;low+=.018*(rng.uniform(-1,1)-low)
    bed[i]=low*.17*(.75+.25*math.sin(2*math.pi*t/16))+.012*math.sin(2*math.pi*55*t)
def mix(sample,at,gain):
    start=int(at*RATE)
    for k,v in enumerate(sample):bed[(start+k)%length]+=v*gain
for beat in range(16):
    mix(kick,beat,.13 if beat%4==0 else .055)
    if beat%2==0:mix(punch,beat+.67,.045)
for at in [1.3,3.8,6.2,9.4,12.7,14.6]:
    clash=[]
    for i in range(int(RATE*.65)):
        t=i/RATE
        metal=sum(math.sin(2*math.pi*f*t) for f in (731,1171,1837))/3
        clash.append(metal*math.exp(-t*9)*min(1,t/.007))
    mix(clash,at,.055);mix(clash,at+.19,.016)
# Tiny seam taper avoids a click without creating an audible gap in the loop.
for i in range(220):bed[i]*=i/220;bed[-1-i]*=i/220
write('ambience',bed,.28)
