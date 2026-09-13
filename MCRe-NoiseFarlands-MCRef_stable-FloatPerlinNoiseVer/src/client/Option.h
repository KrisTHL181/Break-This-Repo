#pragma once 
#include <sstream>
#include <type_traits>
#include <util/Mth.h>
/*
template<typename T>
struct is_option_type : std::false_type {};

template<> struct is_option_type<bool>  : std::true_type {};
template<> struct is_option_type<int>   : std::true_type {};
template<> struct is_option_type<float> : std::true_type {};
template<> struct is_option_type<std::string> : std::true_type {};

template<typename T>
struct is_min_max_option : std::false_type {};

template<> struct is_min_max_option<int>   : std::true_type {};
template<> struct is_min_max_option<float> : std::true_type {};
*/

class Option {
public:
    Option(const std::string& key) : m_key("options." + key) {}
    virtual ~Option();

    const std::string& getStringId() { return m_key; }

    virtual bool parse(const std::string& value) { return false; }
    virtual std::string serialize() { return m_key + ":"; }

protected:
    std::string m_key;

    template<typename T>
    std::string serialize_value(const T& value) const {
        std::ostringstream ss;
        ss << m_key << ":" << value;
        return ss.str();
    }

    bool parse_bool_like(const std::string& value, bool& out);
};

class OptionFloat : public Option {
public:
    OptionFloat(const std::string& key, float value = 0.f, float min = 0.f, float max = 1.f) : 
        Option(key), m_value(value), m_min(min), m_max(max) {}

    float get() { return m_value; }
    void set(float value) { m_value = Mth::clamp(value, m_min, m_max); }

    float getMin() { return m_min; }
    float getMax() { return m_max; }

    virtual bool parse(const std::string& value);
    virtual std::string serialize() { return serialize_value(m_value); }

private:
    float m_value, m_min, m_max;
};

class OptionInt : public Option {
public:
    OptionInt(const std::string& key, int value = 0, int min = -999999, int max = 999999) : 
        Option(key), m_value(value), m_min(min), m_max(max) {}

    int get() { return m_value; }
    void set(int value) { m_value = Mth::clamp(value, m_min, m_max); }

    int getMin() { return m_min; }
    int getMax() { return m_max; }

    virtual bool parse(const std::string& value);
    virtual std::string serialize() { return serialize_value(m_value); }

private:
    int m_value, m_min, m_max;
};

class OptionBool : public Option {
public:
    OptionBool(const std::string& key, bool value = false) : Option(key), m_value(value) {}

    bool get() { return m_value; }
    void set(int value) { m_value = value; }
    void toggle() { m_value = !m_value; }

    virtual bool parse(const std::string& value);
    virtual std::string serialize() { return serialize_value(m_value); }

private:
    bool m_value;
};

class OptionString : public Option {
public:
    OptionString(const std::string& key, const std::string& str = "") : Option(key), m_value(str) {}

    const std::string& get() { return m_value; }
    void set(const std::string& value) { m_value = value; }

    virtual bool parse(const std::string& value) { m_value = value; return true; }
    virtual std::string serialize() { return m_key + ":" + m_value; }

private:
    std::string m_value;
};
