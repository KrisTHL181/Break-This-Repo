#include <iostream>
using namespace std;
int main() {
#ifdef POTATO_SPROUTING
  static const char SPROUT_MARK[] = "It seems already sprouted.";
  cout << "This is a potato. " << SPROUT_MARK << endl;
#else
  cout << "This is a potato. It looks delicious!" << endl;
#endif
  return 0;
}
