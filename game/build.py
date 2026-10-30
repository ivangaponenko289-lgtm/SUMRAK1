"""Build APK, desktop preview, and run the shared combat tests. Requires Python 3 + JDK 17 + Android SDK 35."""
import argparse
import os
import pathlib
import subprocess
import zipfile
import sys
sys.dont_write_bytecode = True
from toolchain import configured_path

ROOT = pathlib.Path(__file__).resolve().parent
parser = argparse.ArgumentParser()
parser.add_argument('--jdk', type=pathlib.Path, default=configured_path('jdk'))
parser.add_argument('--build-tools', type=pathlib.Path, default=configured_path('build_tools'))
parser.add_argument('--android-jar', type=pathlib.Path, default=configured_path('android_jar'))
parser.add_argument('--check-only', action='store_true', help='Run shared tests and compile Android/DEX without replacing APK/JAR outputs')
parser.add_argument('--work', type=pathlib.Path, default=ROOT / '.cache' / 'build')
args = parser.parse_args()
if any(p is None for p in (args.jdk, args.build_tools, args.android_jar)):
    parser.error("Set toolchain.local.json or pass --jdk, --build-tools and --android-jar")
jdk, bt, android, work = (p.resolve() for p in (args.jdk, args.build_tools, args.android_jar, args.work))
out = ROOT / 'dist'
out.mkdir(exist_ok=True)
env = os.environ.copy()
env['JAVA_HOME'] = str(jdk)
env['PATH'] = str(jdk / 'bin') + os.pathsep + env.get('PATH', '')
def run(*cmd):
    # aapt2 on Windows can reject non-ASCII absolute paths; use paths relative to the project.
    argv = [str(cmd[0])] + [os.path.relpath(v, ROOT) if isinstance(v, pathlib.Path) else str(v) for v in cmd[1:]]
    subprocess.run(argv, check=True, env=env, cwd=ROOT)
def java(name):
    return jdk / 'bin' / (name + '.exe' if os.name == 'nt' else name)
def sdk(name):
    return bt / (name + '.exe' if os.name == 'nt' else name)
for folder in ('android', 'desktop', 'dex'):
    (work / folder).mkdir(parents=True, exist_ok=True)
run(sys.executable, ROOT / 'audio' / 'generate.py')
shared = [p for p in (ROOT / 'src').rglob('*.java') if p.name not in ('MainActivity.java','AndroidAudio.java','AndroidBackground.java')]
run(java('javac'), '-encoding', 'UTF-8', '--release', '8', '-d', work / 'desktop', *shared,
    *list((ROOT / 'desktop').rglob('*.java')), *list((ROOT / 'tests').rglob('*.java')))
run(java('java'), '-cp', work / 'desktop', 'ru.shadowduel.CombatTest')
run(java('java'), '-cp', work / 'desktop', 'ru.shadowduel.PlaybackSpeedTest')
run(java('java'), '-cp', work / 'desktop', 'ru.shadowduel.ReferenceMotionTest')
run(java('java'), '-cp', work / 'desktop', 'ru.shadowduel.AnimationTest')
run(java('java'), '-cp', work / 'desktop', 'ru.shadowduel.RendererTest')
run(java('java'), '-cp', work / 'desktop', 'ru.shadowduel.MotionQualityTest')
if args.check_only:
    run(java('javac'), '-encoding', 'UTF-8', '-source', '8', '-target', '8', '-classpath', android,
        '-d', work / 'android', *list((ROOT / 'src').rglob('*.java')))
    run(java('jar'), 'cf', work / 'classes.jar', '-C', work / 'android', '.')
    run(java('java'), '-cp', bt / 'lib' / 'd8.jar', 'com.android.tools.r8.D8', '--lib', android,
        '--min-api', '23', '--output', work / 'dex', work / 'classes.jar')
    print('PASS: shared tests, Android compilation and DEX; existing APK/JAR outputs preserved')
    sys.exit(0)
run(java('jar'), 'cfe', out / 'Sumrak-desktop.jar', 'ru.shadowduel.Desktop', '-C', work / 'desktop', '.', '-C', ROOT / 'assets', '.', '-C', ROOT / 'desktop-assets', '.')
run(java('java'), '-Djava.awt.headless=true', '-cp', str(work / 'desktop') + os.pathsep + str(ROOT / 'assets'), 'ru.shadowduel.Desktop', work / 'Sumrak-preview.png')
run(java('javac'), '-encoding', 'UTF-8', '-source', '8', '-target', '8', '-classpath', android,
    '-d', work / 'android', *list((ROOT / 'src').rglob('*.java')))
run(java('jar'), 'cf', work / 'classes.jar', '-C', work / 'android', '.')
run(java('java'), '-cp', bt / 'lib' / 'd8.jar', 'com.android.tools.r8.D8', '--lib', android,
    '--min-api', '23', '--output', work / 'dex', work / 'classes.jar')
run(sdk('aapt2'), 'compile', '--dir', ROOT / 'res', '-o', work / 'resources.zip')
run(sdk('aapt2'), 'link', '-o', work / 'resource-package.apk', '-I', android,
    '--manifest', ROOT / 'AndroidManifest.xml', work / 'resources.zip')
with zipfile.ZipFile(work / 'resource-package.apk') as resources, zipfile.ZipFile(work / 'unsigned.apk', 'w') as archive:
    for entry in resources.infolist():
        archive.writestr(entry.filename, resources.read(entry), compress_type=entry.compress_type)
    archive.write(work / 'dex' / 'classes.dex', 'classes.dex', compress_type=zipfile.ZIP_DEFLATED)
    for asset in (ROOT / 'assets').rglob('*'):
        if asset.is_file():
            archive.write(asset, 'assets/' + asset.relative_to(ROOT / 'assets').as_posix(), compress_type=zipfile.ZIP_STORED)
run(sdk('zipalign'), '-f', '4', work / 'unsigned.apk', work / 'aligned.apk')
# Local prototype signing key, intentionally outside source and deliverable archives.
key = ROOT / '.local' / 'signing.p12'
key.parent.mkdir(exist_ok=True)
if not key.exists():
    run(java('keytool'), '-genkeypair', '-keystore', key, '-storepass', 'android', '-keypass', 'android',
        '-alias', 'prototype', '-dname', 'CN=Sumrak Prototype', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '3650')
apk = out / 'Sumrak.apk'
run(java('java'), '-jar', bt / 'lib' / 'apksigner.jar', 'sign', '--ks', key,
    '--ks-pass', 'pass:android', '--v4-signing-enabled', 'false', '--out', apk, work / 'aligned.apk')
run(java('java'), '-jar', bt / 'lib' / 'apksigner.jar', 'verify', '--verbose', apk)
run(sdk('zipalign'), '-c', '4', apk)
run(sdk('aapt2'), 'dump', 'badging', apk)
run(sys.executable, ROOT / 'tests' / 'verify_audio.py')
print('Built:', apk)
