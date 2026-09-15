Name:           break-this-repo
Version:        0.0.0
Release:        1%{?dist}
Summary:        Small C++ binaries from the Break This Repo project
License:        LicenseRef-unknown
URL:            https://github.com/BL-BlueLighting/Break-This-Repo
Source0:        %{name}-%{version}.tar.gz

BuildRequires:  cmake
BuildRequires:  gcc-c++

%description
This package installs the fozu and what command-line programs.

%prep
%autosetup

%build
%cmake
%cmake_build

%install
%cmake_install

%files
/usr/bin/fozu
/usr/bin/what

%changelog
* Sun Sep 13 2026 Break This Repo contributors <noreply@example.invalid> - 0.0.0-1
- Initial package.