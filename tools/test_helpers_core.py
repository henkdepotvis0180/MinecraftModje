"""Compile and execute dependency-free Java tests, even without a javac launcher."""
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
sources = [root / 'helpers/src/main/java/nl/henk/helpers' / name for name in ['TaskPlan.java', 'Blueprint.java']]
sources.append(root / 'helpers/src/test/java/nl/henk/helpers/CoreTest.java')
with tempfile.TemporaryDirectory(prefix='henk-java-') as classes:
    subprocess.run(['java', '-m', 'jdk.compiler/com.sun.tools.javac.Main', '-d', classes, *map(str, sources)], check=True)
    subprocess.run(['java', '-cp', classes, 'nl.henk.helpers.CoreTest'], check=True)
