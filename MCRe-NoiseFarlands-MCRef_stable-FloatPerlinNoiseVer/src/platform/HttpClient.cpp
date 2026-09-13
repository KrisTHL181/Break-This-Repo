#include "HttpClient.h"
#include "log.h"

#include <algorithm>
#include <cctype>
#include <cstring>
#include <string>
#include <sstream>
#include <vector>

#if defined(_WIN32)
#ifndef NOMINMAX
#define NOMINMAX
#endif
#include <winsock2.h>
#include <ws2tcpip.h>
#include <winhttp.h>
#pragma comment(lib, "ws2_32.lib")
#pragma comment(lib, "winhttp.lib")
#else
#include <arpa/inet.h>
#include <netdb.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>

// if cmake detected opensll, we define HTTPCLIENT_USE_OPENSSL and include the openssl headers
// most linux distros should have openssl right
#if defined(HTTPCLIENT_USE_OPENSSL)
#include <openssl/err.h>
#include <openssl/ssl.h>
#endif
#endif

namespace {
bool stringStartsWith(const std::string& str, const std::string& prefix) {
    return str.size() >= prefix.size() && std::equal(prefix.begin(), prefix.end(), str.begin());
}

bool parseUrl(const std::string& url, std::string& schemeOut, std::string& hostOut, int& portOut, std::string& pathOut);

// forward declarations for helper functions used by https implementation
bool resolveAndConnect(const std::string& host, int port, int& outSocket);
bool extractStatusCode(const std::string& headers, int& outStatus);

#if defined(_WIN32)

// download an https url using windows winhttp 
// this is only used on windows because the rest of the code is a simple raw tcp http client
bool downloadHttpsWinHttp(const std::string& url, std::vector<unsigned char>& outputBody) {
    // gotta start clear
    outputBody.clear();

    std::string scheme;
    std::string host;
    std::string path;
    int port = 0;

    // split into scheme/host/port/path
    if (!parseUrl(url, scheme, host, port, path)) {
        return false;
    }

    // creating an http session
    HINTERNET session = WinHttpOpen(L"MinecraftPE/0.6.1", WINHTTP_ACCESS_TYPE_DEFAULT_PROXY, WINHTTP_NO_PROXY_NAME, WINHTTP_NO_PROXY_BYPASS, 0);
    if (!session) {
        return false;
    }
    HINTERNET connectHandle = WinHttpConnect(session, std::wstring(host.begin(), host.end()).c_str(), port, 0);
    if (!connectHandle) {
        WinHttpCloseHandle(session);
        return false;
    }

    HINTERNET requestHandle = WinHttpOpenRequest(
        connectHandle,
        L"GET",
        std::wstring(path.begin(), path.end()).c_str(),
        NULL,
        WINHTTP_NO_REFERER,
        WINHTTP_DEFAULT_ACCEPT_TYPES,
        WINHTTP_FLAG_SECURE
    );

    if (!requestHandle) {
        WinHttpCloseHandle(connectHandle);
        WinHttpCloseHandle(session);
        return false;
    }

    DWORD redirectPolicy = WINHTTP_OPTION_REDIRECT_POLICY_ALWAYS;
    WinHttpSetOption(requestHandle, WINHTTP_OPTION_REDIRECT_POLICY, &redirectPolicy, sizeof(redirectPolicy));

    BOOL sendResult = WinHttpSendRequest(requestHandle, WINHTTP_NO_ADDITIONAL_HEADERS, 0, WINHTTP_NO_REQUEST_DATA, 0, 0, 0);
    if (!sendResult) {
        WinHttpCloseHandle(requestHandle);
        WinHttpCloseHandle(connectHandle);
        WinHttpCloseHandle(session);
        return false;
    }

    BOOL receiveResult = WinHttpReceiveResponse(requestHandle, NULL);
    if (!receiveResult) {
        WinHttpCloseHandle(requestHandle);
        WinHttpCloseHandle(connectHandle);
        WinHttpCloseHandle(session);
        return false;
    }

    DWORD bytesAvailable = 0;
    while (WinHttpQueryDataAvailable(requestHandle, &bytesAvailable) && bytesAvailable > 0) {
        std::vector<unsigned char> buffer(bytesAvailable);
        DWORD bytesRead = 0;
        if (!WinHttpReadData(requestHandle, buffer.data(), bytesAvailable, &bytesRead) || bytesRead == 0)
            break;
        outputBody.insert(outputBody.end(), buffer.begin(), buffer.begin() + bytesRead);
    }

    WinHttpCloseHandle(requestHandle);
    WinHttpCloseHandle(connectHandle);
    WinHttpCloseHandle(session);

    return !outputBody.empty();
}

#endif

#if defined(HTTPCLIENT_USE_OPENSSL) && !defined(_WIN32)
bool downloadHttpsOpenSSL(const std::string& url, std::vector<unsigned char>& outputBody) {
    outputBody.clear();

    std::string scheme;
    std::string host;
    std::string path;
    int port = 0;

    // split into scheme/host/port/path
    if (!parseUrl(url, scheme, host, port, path)) {
        return false;
    }

    int socketFd = -1;
    if (!resolveAndConnect(host, port, socketFd)) {
        return false;
    }

    SSL_library_init();
    SSL_load_error_strings();
    OpenSSL_add_all_algorithms();

    SSL_CTX* ctx = SSL_CTX_new(TLS_client_method());
    if (!ctx) {
        close(socketFd);
        return false;
    }

    // do not validate certificates we donst ship ca roots.
    SSL_CTX_set_verify(ctx, SSL_VERIFY_NONE, nullptr);

    SSL* ssl = SSL_new(ctx);
    if (!ssl) {
        SSL_CTX_free(ctx);
        close(socketFd);
        return false;
    }

    SSL_set_fd(ssl, socketFd);
    SSL_set_tlsext_host_name(ssl, host.c_str());

    if (SSL_connect(ssl) != 1) {
        SSL_free(ssl);
        SSL_CTX_free(ctx);
        close(socketFd);
        return false;
    }

    std::string httpRequest;
    httpRequest += "GET ";
    httpRequest += path;
    httpRequest += " HTTP/1.1\r\n";
    httpRequest += "Host: ";
    httpRequest += host;
    httpRequest += "\r\n";
    httpRequest += "User-Agent: MinecraftPE\r\n";
    httpRequest += "Connection: close\r\n";
    httpRequest += "\r\n";

    if (SSL_write(ssl, httpRequest.data(), (int)httpRequest.size()) <= 0) {
        SSL_shutdown(ssl);
        SSL_free(ssl);
        SSL_CTX_free(ctx);
        close(socketFd);
        return false;
    }

    std::vector<unsigned char> rawResponse;

    const int BUFFER_SIZE = 4096;
    unsigned char buffer[BUFFER_SIZE];
    while (true) {
        int bytesRead = SSL_read(ssl, buffer, BUFFER_SIZE);
        if (bytesRead <= 0)
            break;
        rawResponse.insert(rawResponse.end(), buffer, buffer + bytesRead);
    }

    SSL_shutdown(ssl);
    SSL_free(ssl);
    SSL_CTX_free(ctx);
    close(socketFd);

    if (rawResponse.empty()) {
        return false;
    }

    const std::string headerDelimiter = "\r\n\r\n";
    auto headerEndIt = std::search(rawResponse.begin(), rawResponse.end(), headerDelimiter.begin(), headerDelimiter.end());
    if (headerEndIt == rawResponse.end()) {
        return false;
    }

    size_t headerLength = headerEndIt - rawResponse.begin();
    std::string headers(reinterpret_cast<const char*>(rawResponse.data()), headerLength);
    size_t bodyStartIndex = headerLength + headerDelimiter.size();

    int statusCode = 0;
    if (!extractStatusCode(headers, statusCode)) {
        return false;
    }

    if (statusCode != 200) {
        std::string bodySnippet;
        size_t len = rawResponse.size() < 1024 ? rawResponse.size() : 1024;
        bodySnippet.assign(rawResponse.begin(), rawResponse.begin() + len);
        LOGW("[HttpClient] HTTP status %d for %s\n", statusCode, url.c_str());
        LOGW("[HttpClient] Headers:\n%s\n", headers.c_str());
        LOGW("[HttpClient] Body (up to 1024 bytes):\n%s\n", bodySnippet.c_str());
        return false;
    }

    outputBody.assign(rawResponse.begin() + bodyStartIndex, rawResponse.end());
    return true;
}
#endif

std::string toLower(const std::string& input) {
    std::string output = input;
    std::transform(output.begin(), output.end(), output.begin(), ::tolower);
    return output;
}

bool parseUrl(const std::string& url, std::string& schemeOut, std::string& hostOut, int& portOut, std::string& pathOut) {
    schemeOut.clear();
    hostOut.clear();
    pathOut.clear();
    portOut = 0;

    size_t schemeSep = url.find("://");
    if (schemeSep == std::string::npos) {
        return false;
    }

    schemeOut = toLower(url.substr(0, schemeSep));
    size_t hostStart = schemeSep + 3;

    // split host/port from the path
    size_t pathStart = url.find('/', hostStart);
    std::string hostPort;
    if (pathStart == std::string::npos) {
        // no path part, so just use / as the default
        hostPort = url.substr(hostStart);
        pathOut = "/";
    } else {
        hostPort = url.substr(hostStart, pathStart - hostStart);
        pathOut = url.substr(pathStart);
    }

    // if the host includes a ":port", split it out
    size_t portSep = hostPort.find(':');
    if (portSep != std::string::npos) {
        hostOut = hostPort.substr(0, portSep);
        portOut = atoi(hostPort.c_str() + portSep + 1);
    } else {
        hostOut = hostPort;
    }

    // fill in default ports for known schemes
    if (schemeOut == "http") {
        if (portOut == 0) portOut = 80;
    } else if (schemeOut == "https") {
        if (portOut == 0) portOut = 443;
    }

    // return success only if we got at a scheme and host
    return !hostOut.empty() && !schemeOut.empty();
}

// read all available data from a tcp socket until the connection is closed
// data is appended to outData
bool readAll(int socketFd, std::vector<unsigned char>& outData) {
    const int BUFFER_SIZE = 4096;
    unsigned char buffer[BUFFER_SIZE];

    while (true) {
        int bytesRead = recv(socketFd, (char*)buffer, BUFFER_SIZE, 0);
        if (bytesRead <= 0)
            break;
        outData.insert(outData.end(), buffer, buffer + bytesRead);
    }

    return true;
}

// resolve a hostname and connect a tcp socket to the given host:port
// on windows this also makes sure winsock is initialized
// if successful, outSocket will contain a connected socket descriptor
bool resolveAndConnect(const std::string& host, int port, int& outSocket) {
#if defined(_WIN32)
    static bool wsaStarted = false;
    if (!wsaStarted) {
        WSADATA wsaData;
        WSAStartup(MAKEWORD(2, 2), &wsaData);
        wsaStarted = true;
    }
#endif

    struct addrinfo hints;
    struct addrinfo* result = NULL;

    memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_UNSPEC;   // ipv4 ipv6
    hints.ai_socktype = SOCK_STREAM; // tcp

    // getaddrinfo expects strings for port and host
    std::ostringstream portStream;
    portStream << port;
    const std::string portString = portStream.str();

    if (getaddrinfo(host.c_str(), portString.c_str(), &hints, &result) != 0) {
        return false;
    }

    int socketFd = -1;

    // try each resolved address until we successfully connect
    for (struct addrinfo* addr = result; addr != NULL; addr = addr->ai_next) {
        socketFd = socket(addr->ai_family, addr->ai_socktype, addr->ai_protocol);
        if (socketFd < 0) {
            continue;
        }

        if (connect(socketFd, addr->ai_addr, (int)addr->ai_addrlen) == 0) {
            // connected! yay!
            break;
        }

        // failed to connect, try next
#if defined(_WIN32)
        closesocket(socketFd);
#else
        close(socketFd);
#endif
        socketFd = -1;
    }

    freeaddrinfo(result);

    if (socketFd < 0) {
        return false;
    }

    outSocket = socketFd;
    return true;
}

std::string getHeaderValue(const std::string& headers, const std::string& key) {
    std::string headersLower = toLower(headers);
    std::string keyLower = toLower(key);

    size_t pos = headersLower.find(keyLower);
    if (pos == std::string::npos) {
        return "";
    }

    size_t colonPos = headersLower.find(':', pos + keyLower.size());
    if (colonPos == std::string::npos) {
        return "";
    }

    size_t valueStart = colonPos + 1;
    while (valueStart < headersLower.size() && (headersLower[valueStart] == ' ' || headersLower[valueStart] == '\t')) {
        valueStart++;
    }

    size_t valueEnd = headersLower.find('\r', valueStart);
    if (valueEnd == std::string::npos) {
        valueEnd = headersLower.find('\n', valueStart);
    }
    if (valueEnd == std::string::npos) {
        valueEnd = headersLower.size();
    }

    return headers.substr(valueStart, valueEnd - valueStart);
}

// wxtract the http status code from the first response line (foex example "HTTP/1.1 200 OK")
// returns false on malformed responses.
bool extractStatusCode(const std::string& headers, int& outStatus) {
    size_t firstSpace = headers.find(' ');
    if (firstSpace == std::string::npos) {
        return false;
    }

    size_t secondSpace = headers.find(' ', firstSpace + 1);
    if (secondSpace == std::string::npos) {
        return false;
    }

    std::string code = headers.substr(firstSpace + 1, secondSpace - firstSpace - 1);
    outStatus = atoi(code.c_str());
    return true;
}

} // anonymous namespace

namespace HttpClient {

bool download(const std::string& url, std::vector<unsigned char>& outBody) {
    outBody.clear();

    // keep a copy of the current url so we can follow redirects
    std::string urlToDownload = url;

    // follow up to 3 redirects (301/302/307/308)
    // if a redirect is encountered we loop again with the new loaction url
    for (int redirectCount = 0; redirectCount < 3; ++redirectCount) {
        std::string urlScheme;
        std::string urlHost;
        std::string urlPath;
        int urlPort = 0;

        // parse the url into its components so we can open a socket/start an https request
        if (!parseUrl(urlToDownload, urlScheme, urlHost, urlPort, urlPath)) {
            LOGW("[HttpClient] parseUrl failed for '%s'\n", urlToDownload.c_str());
            return false;
        }

        // for https we delegate to winhttp on windows, since this simple client
        // only supports plain http over raw sockets
        if (urlScheme == "https") {
#if defined(_WIN32)
            LOGI("[HttpClient] using WinHTTP for HTTPS URL %s\n", urlToDownload.c_str());
            return downloadHttpsWinHttp(urlToDownload, outBody);
#elif defined(HTTPCLIENT_USE_OPENSSL)
            LOGI("[HttpClient] using OpenSSL for HTTPS URL %s\n", urlToDownload.c_str());
            return downloadHttpsOpenSSL(urlToDownload, outBody);
#else
            LOGW("[HttpClient] HTTPS not supported on this platform: %s\n", urlToDownload.c_str());
            return false;
#endif
        }

        // we only support plain http for all non-windows platforms for nw
        if (urlScheme != "http") {
            LOGW("[HttpClient] unsupported scheme '%s' for URL '%s'\n", urlScheme.c_str(), urlToDownload.c_str());
            return false;
        }

        int socketFd = -1;
        if (!resolveAndConnect(urlHost, urlPort, socketFd)) {
            LOGW("[HttpClient] resolve/connect failed for %s:%d\n", urlHost.c_str(), urlPort);
            return false;
        }

        std::string httpRequest;
        httpRequest += "GET ";
        httpRequest += urlPath;
        httpRequest += " HTTP/1.1\r\n";
        httpRequest += "Host: ";
        httpRequest += urlHost;
        httpRequest += "\r\n";
        httpRequest += "User-Agent: MinecraftPE\r\n";
        httpRequest += "Connection: close\r\n";
        httpRequest += "\r\n";

        send(socketFd, httpRequest.c_str(), (int)httpRequest.size(), 0);

        std::vector<unsigned char> rawResponse;
        readAll(socketFd, rawResponse);

#if defined(_WIN32)
        closesocket(socketFd);
#else
        close(socketFd);
#endif

        if (rawResponse.empty()) {
            LOGW("[HttpClient] no response data from %s\n", urlToDownload.c_str());
            return false;
        }

        // find the end of the headers (\r\n\r\n) so we can split headers and body
        const std::string headerDelimiter = "\r\n\r\n";
        auto headerEndIt = std::search(rawResponse.begin(), rawResponse.end(), headerDelimiter.begin(), headerDelimiter.end());
        if (headerEndIt == rawResponse.end()) {
            // we didn't find the end of headers :(
            return false;
        }

        // extract the header block as a string so we can inspect it
        size_t headerLength = headerEndIt - rawResponse.begin();
        std::string headers(reinterpret_cast<const char*>(rawResponse.data()), headerLength);
        size_t bodyStartIndex = headerLength + headerDelimiter.size();

        int statusCode = 0;
        if (!extractStatusCode(headers, statusCode)) {
            return false;
        }

        if (statusCode == 301 || statusCode == 302 || statusCode == 307 || statusCode == 308) {
            std::string location = getHeaderValue(headers, "Location");
            if (location.empty()) {
                LOGW("[HttpClient] redirect without Location header for %s\n", urlToDownload.c_str());
                return false;
            }
            LOGI("[HttpClient] redirect %s -> %s\n", urlToDownload.c_str(), location.c_str());
            urlToDownload = location;
            continue;
        }

        if (statusCode != 200) {
            // if we got any status other than 200 OK, log what happened
            std::string bodySnippet;
            if (!outBody.empty()) {
                size_t len = outBody.size() < 1024 ? outBody.size() : 1024;
                bodySnippet.assign(outBody.begin(), outBody.begin() + len);
            }
            LOGW("[HttpClient] HTTP status %d for %s\n", statusCode, urlToDownload.c_str());
            LOGW("[HttpClient] Headers:\n%s\n", headers.c_str());
            LOGW("[HttpClient] Body (up to 1024 bytes):\n%s\n", bodySnippet.c_str());
            return false;
        }

        // everything looks good! copy just the body bytes (after the headers) into outBody.
        outBody.assign(rawResponse.begin() + bodyStartIndex, rawResponse.end());
        return true;
    }

    return false;
}

} // namespace HttpClient
