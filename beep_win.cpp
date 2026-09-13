#include <windows.h>
#include <ctime>
#include <random>

int main(){
	std::default_random_engine e;
	std::uniform_int_distribution<int> u(114, 514);
	std::uniform_int_distribution<int> t(114, 514);
	e.seed(time(0));
	
	MessageBox(
	NULL,
	"Start",
	"Give You UP",
	0x00042020L
	);
	for (int i = 0; i < 78919178; i++) {
		Beep(u(e), t(e));
	}
	return 0;
}
