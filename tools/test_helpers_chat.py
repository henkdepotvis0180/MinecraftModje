"""Run standalone HTTP integration tests with the Gson JAR shipped by Minecraft.

Usage: python3 tools/test_helpers_chat.py /path/to/gson-2.10.1.jar
No dependencies are downloaded by this test runner.
"""
from pathlib import Path
import subprocess
import tempfile
import sys

if len(sys.argv) != 2:
    raise SystemExit('Usage: python3 tools/test_helpers_chat.py /path/to/gson-2.10.1.jar')
jar = str(Path(sys.argv[1]).resolve(strict=True))
root = Path(__file__).resolve().parents[1]
sources = [root / 'helpers/src/main/java/nl/henk/helpers' / (name + '.java')
           for name in ['TaskPlan', 'ChatReply', 'ChatMemory', 'OllamaClient']]
sources.append(root / 'helpers/src/test/java/nl/henk/helpers/ChatTest.java')
with tempfile.TemporaryDirectory(prefix='henk-chat-') as classes:
    subprocess.run(['java', '-m', 'jdk.compiler/com.sun.tools.javac.Main', '--add-modules', 'jdk.httpserver',
                    '-cp', jar, '-d', classes, *map(str, sources)], check=True)
    import os
    subprocess.run(['java', '--add-modules', 'jdk.httpserver', '-cp', os.pathsep.join([classes, jar]),
                    'nl.henk.helpers.ChatTest'], check=True)
