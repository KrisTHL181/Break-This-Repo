#ifndef HTTPCLIENT_H__
#define HTTPCLIENT_H__

#include <string>
#include <vector>

namespace HttpClient {

bool download(const std::string& url, std::vector<unsigned char>& outBody);

} // namespace HttpClient

#endif /* HTTPCLIENT_H__ */
