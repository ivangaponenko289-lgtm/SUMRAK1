"""Import a supplied track; preserve MP3 for Android, decode PCM for Java desktop."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess

p=argparse.ArgumentParser()
p.add_argument('source',type=Path)
p.add_argument('--ffmpeg',required=True)
a=p.parse_args()
root=Path(__file__).resolve().parents[1]
original=root/'assets'/'audio'/'battle_music.mp3'
desktop=root/'desktop-assets'/'audio'/'battle_music.wav'
desktop.parent.mkdir(parents=True,exist_ok=True)
shutil.copyfile(a.source,original)
subprocess.run([a.ffmpeg,'-hide_banner','-loglevel','error','-y','-i',str(original),
                '-ar','22050','-ac','2','-c:a','pcm_s16le',str(desktop)],check=True)
(root/'audio'/'music.json').write_text(json.dumps({
    'source_filename':a.source.name,
    'sha256':hashlib.sha256(original.read_bytes()).hexdigest(),
    'provenance':'User-supplied track. Android includes the original file unchanged.',
    'desktop':'Full-length PCM conversion, stereo 22050 Hz; no trimming.'
},ensure_ascii=False,indent=2),encoding='utf-8')
print('Imported original MP3 and full-length desktop PCM')
