"""Offline integrity checks; game registry decoding still needs Minecraft."""
import json
import unittest
from pathlib import Path
from zipfile import ZipFile
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
from build_worldgen import build


class WorldgenTests(unittest.TestCase):
    def test_local_registry_references(self):
        entries = list((ROOT / 'worldgen').rglob('*.json'))
        self.assertGreater(len(entries), 5)
        def walk(value):
            if isinstance(value, dict):
                for key, child in value.items():
                    if key in ('noise', 'settings') and isinstance(child, str) and child.startswith('henk:'):
                        registry = 'noise' if key == 'noise' else 'noise_settings'
                        self.assertTrue((ROOT / 'worldgen/data/henk/worldgen' / registry / (child.split(':')[1] + '.json')).is_file())
                    walk(child)
            elif isinstance(value, list):
                for child in value:
                    walk(child)
        for entry in entries:
            walk(json.loads(entry.read_text()))

    def test_preset_and_dimensions(self):
        preset = json.loads((ROOT / 'worldgen/data/henk/worldgen/world_preset/frontier.json').read_text())
        self.assertEqual(set(preset['dimensions']), {'minecraft:overworld', 'minecraft:the_nether', 'minecraft:the_end'})
        self.assertEqual(preset['dimensions']['minecraft:overworld']['generator']['settings'], 'henk:frontier')
        tag = json.loads((ROOT / 'worldgen/data/minecraft/tags/worldgen/world_preset/normal.json').read_text())
        self.assertFalse(tag['replace'])
        self.assertIn('henk:frontier', tag['values'])

    def test_pack_versions(self):
        for folder, version in [('worldgen', 48), ('resourcepack', 34)]:
            self.assertEqual(json.loads((ROOT / folder / 'pack.mcmeta').read_text())['pack']['pack_format'], version)

    def test_archives_are_installable_and_reproducible(self):
        for folder in ['worldgen', 'resourcepack']:
            path = build(folder, f'test-{folder}.zip')
            try:
                first = path.read_bytes()
                with ZipFile(path) as archive:
                    self.assertIsNone(archive.testzip())
                    self.assertIn('pack.mcmeta', archive.namelist())
                    self.assertFalse(any(name.startswith(folder + '/') for name in archive.namelist()))
                self.assertEqual(first, build(folder, path.name).read_bytes())
            finally:
                path.unlink(missing_ok=True)


if __name__ == '__main__':
    unittest.main()
