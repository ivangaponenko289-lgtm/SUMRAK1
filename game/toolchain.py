"""Local JDK/Android SDK paths; tools are shared, never copied into source folders."""
import json
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parent

def configured_path(name):
    variables = {'jdk': 'JAVA_HOME', 'build_tools': 'SHADOWDUEL_BUILD_TOOLS', 'android_jar': 'SHADOWDUEL_ANDROID_JAR'}
    value = os.environ.get(variables[name])
    config = ROOT / 'toolchain.local.json'
    if not value and config.is_file():
        value = json.loads(config.read_text(encoding='utf-8-sig')).get(name)
    if not value:
        return None
    path = Path(value).expanduser()
    return path if path.is_absolute() else ROOT / path
