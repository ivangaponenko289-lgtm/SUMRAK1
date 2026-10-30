"""Validate PCM delivery, levels and APK streaming requirements."""
from pathlib import Path
import array
import math
import wave
import zipfile
import hashlib
import json

root=Path(__file__).resolve().parents[1]
files=list((root/'assets'/'audio').glob('*.wav'))
assert len(files)==7
for path in files:
    with wave.open(str(path),'rb') as w:
        assert (w.getnchannels(),w.getsampwidth(),w.getframerate())==(1,2,22050)
        pcm=array.array('h',w.readframes(w.getnframes()))
        peak=max(abs(v) for v in pcm)/32768
        rms=math.sqrt(sum((v/32768)**2 for v in pcm)/len(pcm))
        assert .005<rms<.4 and peak<.99,(path.name,peak,rms)
        assert abs(pcm[0])<100 and abs(pcm[-1])<1000,(path.name,'hard boundary')
        print(path.name,'peak',round(peak,3),'RMS',round(rms,3))
with zipfile.ZipFile(root/'dist/Sumrak.apk') as z:
    assert z.testzip() is None
    for path in files:
        entry=z.getinfo('assets/audio/'+path.name)
        assert entry.compress_type==zipfile.ZIP_STORED,'Android openFd needs uncompressed assets'
        assert z.read(entry)==path.read_bytes()
    track=root/'assets'/'audio'/'battle_music.mp3'
    metadata=json.loads((root/'audio'/'music.json').read_text(encoding='utf-8'))
    entry=z.getinfo('assets/audio/battle_music.mp3')
    assert entry.compress_type==zipfile.ZIP_STORED
    assert hashlib.sha256(z.read(entry)).hexdigest()==metadata['sha256']
    assert z.read(entry)==track.read_bytes()
with wave.open(str(root/'desktop-assets'/'audio'/'battle_music.wav'),'rb') as w:
    assert w.getnchannels()==2 and w.getframerate()==22050
    print('User music decoded for desktop:',round(w.getnframes()/w.getframerate(),2),'seconds')
print('PASS: original user MP3 is included unchanged')
print('PASS: all 7 WAV assets are valid, unclipped, and stored intact in APK')
