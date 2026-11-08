EAPI=8

inherit cmake

DESCRIPTION="Small C++ binaries from the Break This Repo project"
HOMEPAGE="https://github.com/BL-BlueLighting/Break-This-Repo"
SRC_URI="https://github.com/BL-BlueLighting/Break-This-Repo/archive/refs/tags/v${PV}.tar.gz -> ${P}.tar.gz"

LICENSE="UNKNOWN"
SLOT="0"
KEYWORDS="~amd64 ~arm64"
IUSE=""

DEPEND=""
RDEPEND=""

S="${WORKDIR}/Break-This-Repo-${PV}"