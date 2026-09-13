project(
  'break-this-repo',
  'cpp',
  version: '0.1.0',
  default_options: ['cpp_std=c++11'],
)

executable('fozu', 'fozu.cpp')
executable('what', 'what.cpp')

if host_machine.system() == 'windows'
  executable('beep_win', 'beep_win.cpp', win_subsystem: 'windows')
endif