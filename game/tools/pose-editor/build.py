"""Build the standalone editor using the game's Java sources. No external libraries."""
from pathlib import Path
import argparse, os, subprocess, sys

root=Path(__file__).resolve().parent
project=root.parent.parent
sys.dont_write_bytecode=True
sys.path.insert(0,str(project))
from toolchain import configured_path
parser=argparse.ArgumentParser()
parser.add_argument('--jdk',type=Path)
parser.add_argument('--smoke',action='store_true')
args=parser.parse_args()
jdk=args.jdk or configured_path('jdk')
if jdk is None:parser.error('Set toolchain.local.json or pass --jdk')

def tool(name):return str(jdk/'bin'/(name+'.exe' if os.name=='nt' else name))
build=project/'.cache/editor';build.mkdir(parents=True,exist_ok=True)
reports=project/'.cache/reports/editor';reports.mkdir(parents=True,exist_ok=True)
dist=project/'dist';dist.mkdir(exist_ok=True)
sources=[p for p in (project/'src').rglob('*.java') if p.name not in ('MainActivity.java','AndroidAudio.java','AndroidBackground.java')]
sources+=list((root/'src').rglob('*.java'))+list((root/'tests').rglob('*.java'))+list((project/'tests').rglob('*.java'))
subprocess.run([tool('javac'),'-encoding','UTF-8','--release','8','-d',str(build),*[str(p) for p in sources]],check=True)
subprocess.run([tool('jar'),'cfe',str(dist/'AnimationEditor.jar'),'ru.shadowduel.editor.PoseEditor','-C',str(build),'.'],check=True)
subprocess.run([tool('java'),'-cp',str(build),'ru.shadowduel.AnimationTest'],check=True)
subprocess.run([tool('java'),'-cp',str(build),'ru.shadowduel.CombatTest'],check=True)
subprocess.run([tool('java'),'-cp',str(build),'ru.shadowduel.PlaybackSpeedTest'],check=True)
subprocess.run([tool('java'),'-cp',str(build),'ru.shadowduel.ReferenceMotionTest'],check=True)
subprocess.run([tool('java'),'-cp',str(build),'ru.shadowduel.RendererTest'],check=True)
subprocess.run([tool('java'),'-cp',str(build),'ru.shadowduel.ImportTest'],check=True)
subprocess.run([tool('java'),'-Djava.awt.headless=true','-cp',str(build),'ru.shadowduel.editor.SimpleEditorTest',str(reports/'simple-editor-preview.png')],check=True)
subprocess.run([tool('java'),'-Djava.awt.headless=true','-cp',str(build),'ru.shadowduel.editor.SegmentedRigTest',str(reports/'rig.png')],check=True)
subprocess.run([tool('java'),'-Djava.awt.headless=true','-cp',str(build),'ru.shadowduel.editor.KickRecovery','--verify',str(project/'tests/fixtures/kick-original.json'),str(project/'tests/fixtures/kick-recovery.json')],check=True)
if args.smoke:subprocess.run([tool('java'),'-Djava.awt.headless=true','-cp',str(build),'ru.shadowduel.editor.PoseEditor','--smoke',str(reports/'editor-preview.png')],check=True)
print('Built:',dist/'AnimationEditor.jar')
