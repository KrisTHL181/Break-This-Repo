#include "Option.h"
#include <sstream>
#include <cstdio>

Option::~Option() {}

bool Option::parse_bool_like(const std::string& value, bool& out) {
    if (value == "true" || value == "YES") {
        out = true;
        return true;
    }

    if (value == "false" || value == "NO") {
        out = false;
        return true;
    }

    return false;
}

bool OptionFloat::parse(const std::string& value) {
    bool b;
    
    if (parse_bool_like(value, b)) {
        m_value = b ? 1.f : 0.f;
        return true;
    }

    return sscanf(value.c_str(), "%f", &m_value) == 1;
}
bool OptionInt::parse(const std::string& value) {
    bool b;
    if (parse_bool_like(value, b)) {
        m_value = b ? 1 : 0;
        return true;
    }

    return sscanf(value.c_str(), "%d", &m_value) == 1;
}
bool OptionBool::parse(const std::string& value) {
    if (value == "0") {
        m_value = false;
        return true;
    }

    if (value == "1") {
        m_value = true;
        return true;
    }

    return parse_bool_like(value, m_value);
}