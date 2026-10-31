"""Build reproducible Minecraft packs without third-party dependencies."""
from pathlib import Path
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parents[1]


def build(source, output):
    destination = ROOT / 'dist' / output
    destination.parent.mkdir(exist_ok=True)
    with ZipFile(destination, 'w', compression=ZIP_DEFLATED) as archive:
        for path in sorted((ROOT / source).rglob('*')):
            if path.is_file():
                info = ZipInfo(path.relative_to(ROOT / source).as_posix(), (2026, 1, 1, 0, 0, 0))
                info.compress_type = ZIP_DEFLATED
                info.external_attr = 0o644 << 16
                archive.writestr(info, path.read_bytes())
    return destination


if __name__ == '__main__':
    print(build('worldgen', 'henk-frontier-worldgen-0.1.0.zip'))
    print(build('resourcepack', 'henk-frontier-names-0.1.0.zip'))
