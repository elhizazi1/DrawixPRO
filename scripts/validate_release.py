"""Validate public release metadata and optional Android source using Python 3."""
import argparse
import json
import re
from pathlib import Path
from html.parser import HTMLParser
from urllib.parse import urlsplit, unquote

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--source', type=Path)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
def load(name):
    return json.loads((root / name).read_text())
r = load('release.json')
m = load('module.json')
s = load('shizu_store.json')
for path in root.glob('*.json'):
    json.loads(path.read_text())
v = m['versions'][0]
assert m['package'] == s['package_name'] == r['package_name'], 'Package mismatch'
assert (v['versionName'], v['versionCode'], v['minApi'], v['targetApi']) == (r['version_name'], r['version_code'], r['min_sdk'], r['target_sdk']), 'Version/SDK mismatch'
assert v['release'] == r['apk_url'], 'APK URL mismatch'
assert len({item['versionCode'] for item in m['versions']}) == len(m['versions']), 'Duplicate version code'
class Links(HTMLParser):
    def handle_starttag(self, tag, attrs):
        for key, value in attrs:
            if key not in ('src', 'href') or not value:
                continue
            url = urlsplit(value)
            if url.scheme or url.netloc or not url.path:
                continue
            target = root / unquote(url.path.lstrip('/'))
            assert target.exists(), f'Missing local resource: {value}'
for html in root.glob('*.html'):
    Links().feed(html.read_text())
if args.source:
    text = (args.source / 'app/build.gradle.kts').read_text()
    expected = {'applicationId': r['package_name'], 'versionName': r['version_name'], 'versionCode': r['version_code'], 'minSdk': r['min_sdk'], 'targetSdk': r['target_sdk']}
    for key, value in expected.items():
        match = re.search(r'\b' + key + r'\s*=\s*(?:"([^"]+)"|(\d+))', text)
        assert match and (match[1] or match[2]) == str(value), f'Android mismatch: {key}'
workflow = (root / '.github/workflows/android.yml').read_text()
assert 'workflow_dispatch:' in workflow and not re.search(r'^  (push|pull_request):', workflow, re.M), 'Build must be manual'
print('Release metadata, local resources and workflow checks passed.')
