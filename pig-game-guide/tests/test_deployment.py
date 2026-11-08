"""Public entry points work outside the developer workspace and fail helpfully."""
import argparse
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

from runtime import interval_seconds,platform_error,state_directory

ROOT=Path(__file__).resolve().parents[1]


class DeploymentTests(unittest.TestCase):
    def test_help_works_without_site_packages(self):
        result=subprocess.run([sys.executable,'-S',str(ROOT/'guide.py'),'--help'],capture_output=True,text=True)
        self.assertEqual(result.returncode,0,result.stderr)
        self.assertIn('--demo',result.stdout)
        self.assertIn('--doctor',result.stdout)

    def test_invalid_intervals_are_rejected_before_native_imports(self):
        for value in ('0','-1','nan','inf','3601','text'):
            with self.subTest(value=value):
                result=subprocess.run([sys.executable,'-S',str(ROOT/'guide.py'),'--interval',value],capture_output=True,text=True)
                self.assertEqual(result.returncode,2)
                self.assertNotIn('Traceback',result.stderr)
        self.assertEqual(interval_seconds('.1'),.1)
        self.assertEqual(interval_seconds('3600'),3600.)

    def test_demo_cannot_accidentally_enable_phone_clicking(self):
        result=subprocess.run([sys.executable,'-S',str(ROOT/'guide.py'),'--demo','--auto-start'],capture_output=True,text=True)
        self.assertEqual(result.returncode,2)
        self.assertIn('--auto-start 只用于实时模式',result.stderr)

    def test_demo_works_from_an_unrelated_directory(self):
        with tempfile.TemporaryDirectory(prefix='pig demo ') as temp:
            output=Path(temp)/'result'
            result=subprocess.run([sys.executable,str(ROOT/'guide.py'),'--demo','--out',str(output)],cwd=temp,capture_output=True,text=True,timeout=30)
            self.assertEqual(result.returncode,0,result.stderr)
            plan=json.loads((output/'plan.json').read_text())
            self.assertEqual(plan['states'][-1],[])
            self.assertEqual(len(plan['actions']),120)
            self.assertTrue((output/'next.png').is_file())

    def test_launcher_handles_spaces_and_forwards_arguments(self):
        with tempfile.TemporaryDirectory(prefix='pig tool ') as temp:
            folder=Path(temp);shutil.copy2(ROOT/'start.command',folder/'start.command')
            (folder/'.venv').mkdir();(folder/'.venv'/'bin').mkdir()
            (folder/'.venv'/'bin'/'python').symlink_to(sys.executable)
            (folder/'guide.py').write_text('import json,sys; print(json.dumps(sys.argv[1:]))')
            result=subprocess.run(['sh',str(folder/'start.command'),'--analyze','a b.png'],cwd='/',capture_output=True,text=True)
            self.assertEqual(result.returncode,0,result.stderr)
            self.assertEqual(json.loads(result.stdout),['--analyze','a b.png'])

    def test_uninstalled_launcher_gives_install_step(self):
        with tempfile.TemporaryDirectory(prefix='pig missing ') as temp:
            script=Path(temp)/'start.command';shutil.copy2(ROOT/'start.command',script)
            result=subprocess.run(['sh',str(script)],capture_output=True,text=True)
            self.assertEqual(result.returncode,1)
            self.assertIn('install.command',result.stderr)

    def test_platform_and_state_path_are_explicit(self):
        with patch('runtime.sys.platform','win32'):
            self.assertIn('macOS',platform_error())
        with patch('runtime.sys.platform','darwin'),patch('runtime.platform.mac_ver',return_value=('14.0','','')):
            self.assertIn('macOS 15',platform_error())
        with patch('runtime.Path.home',return_value=Path('/tmp/example-user')):
            self.assertEqual(state_directory(),Path('/tmp/example-user/Library/Application Support/Pig Game Guide'))


if __name__=='__main__':unittest.main()
