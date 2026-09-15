#include "SharedConstants.h"

namespace Common {

std::string getGameVersionString(const std::string& versionSuffix /* = "" */)
{
	std::string result = std::string("v0.6.1") + versionSuffix;
	// append 64-bit port marker only on Android 64‑bit targets
	#if defined(ANDROID) && (defined(__aarch64__) || defined(__x86_64__))
		result += " (64-bit port)";
	#endif
	result += " alpha";
	return result;
}

};
