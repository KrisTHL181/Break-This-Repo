#include "ConsoleScreen.h"
#include "../Gui.h"
#include "../../Minecraft.h"
#include "../../player/LocalPlayer.h"
#include "../../../platform/input/Keyboard.h"
#include "../../../world/level/Level.h"
#include "../../../network/RakNetInstance.h"
#include "../../../network/ServerSideNetworkHandler.h"
#include "../../../network/packet/ChatPacket.h"
#include "../../../platform/log.h"
// ConsoleScreen.cpp 开头部分加入
#include "../../renderer/LevelRenderer.h"

#include <sstream>
#include <cstdlib>
#include <cctype>

ConsoleScreen::ConsoleScreen()
:   _input(""),
    _cursorBlink(0)
{
}

void ConsoleScreen::init()
{
}

void ConsoleScreen::tick()
{
    _cursorBlink++;
}

bool ConsoleScreen::handleBackEvent(bool /*isDown*/)
{
    minecraft->setScreen(NULL);
    return true;
}

void ConsoleScreen::keyPressed(int eventKey) {
    if (eventKey == Keyboard::KEY_ESCAPE) {
        if (!_input.empty()) {
            _input.clear();            // 先清空输入
            return;
        }
        minecraft->setScreen(NULL);   // 输入为空时才关闭
        return;
    // ... 其余代码不变
    } else if (eventKey == Keyboard::KEY_RETURN) {
        execute();
    } else if (eventKey == Keyboard::KEY_BACKSPACE) {
        if (!_input.empty())
            _input.erase(_input.size() - 1, 1);
    } else {
        super::keyPressed(eventKey);
    }
}

void ConsoleScreen::charPressed(char inputChar)
{
    if (inputChar >= 32 && inputChar < 127)
        _input += inputChar;
}

// ---------------------------------------------------------------------------
// execute: run _input as a command, print result, close screen
// ---------------------------------------------------------------------------
void ConsoleScreen::execute()
{
    if (_input.empty()) {
        minecraft->setScreen(NULL);
        return;
    }

    if (_input[0] == '/') {
        // Command
        std::string result = processCommand(_input);
        if (!result.empty())
            minecraft->gui.addMessage(result);
    } else {
        // Chat message: <name> message
        std::string msg = std::string("<") + minecraft->player->name + "> " + _input;
        if (minecraft->netCallback && minecraft->raknetInstance->isServer()) {
            // Hosting a LAN game: displayGameMessage shows locally + broadcasts MessagePacket to clients
            static_cast<ServerSideNetworkHandler*>(minecraft->netCallback)->displayGameMessage(msg);
        } else if (minecraft->netCallback) {
            // Connected client: send ChatPacket to server; server echoes it back as MessagePacket
            ChatPacket chatPkt(msg);
            minecraft->raknetInstance->send(chatPkt);
        } else {
            // Singleplayer: show locally only
            minecraft->gui.addMessage(msg);
        }
    }

    minecraft->setScreen(NULL);
}

// ---------------------------------------------------------------------------
// processCommand
// ---------------------------------------------------------------------------
static std::string trim(const std::string& s) {
    size_t a = s.find_first_not_of(" \t");
    if (a == std::string::npos) return "";
    size_t b = s.find_last_not_of(" \t");
    return s.substr(a, b - a + 1);
}

std::string ConsoleScreen::processCommand(const std::string& raw)
{
    // strip leading '/'
    std::string line = raw;
    if (!line.empty() && line[0] == '/') line = line.substr(1);
    line = trim(line);

    // tokenise
    std::vector<std::string> args;
    {
        std::istringstream ss(line);
        std::string tok;
        while (ss >> tok) args.push_back(tok);
    }

    if (args.empty()) return "";

    Level* level = minecraft->level;
    if (!level) return "No level loaded.";

    

    // -----------------------------------------------------------------------
    // /time ...
    // -----------------------------------------------------------------------
    if (args[0] == "time") {
        if (args.size() < 2)
            return "Usage: /time <add|set|query> ...";

        const std::string& sub = args[1];

        // -- time add <value> -----------------------------------------------
        if (sub == "add") {
            if (args.size() < 3) return "Usage: /time add <value>";
            long delta = std::atol(args[2].c_str());
            long newTime = level->getTime() + delta;
            level->setTime(newTime);
            std::ostringstream out;
            out << "Set the time to " << (newTime % Level::TICKS_PER_DAY);
            return out.str();
        }

        // -- time set <value|day|night|noon|midnight> -----------------------
        if (sub == "set") {
            if (args.size() < 3) return "Usage: /time set <value|day|night|noon|midnight>";
            const std::string& val = args[2];

            long t = -1;
            if      (val == "day")      t = 1000;
            else if (val == "noon")     t = 6000;
            else if (val == "night")    t = 13000;
            else if (val == "midnight") t = 18000;
            else {
                // numeric — accept positive integers only
                bool numeric = true;
                for (char c : val)
                    if (!std::isdigit((unsigned char)c)) { numeric = false; break; }
                if (!numeric) return std::string("Unknown value: ") + val;
                t = std::atol(val.c_str());
            }

            // Preserve the total day count so only the time-of-day changes
            long dayCount = level->getTime() / Level::TICKS_PER_DAY;
            long newTime  = dayCount * Level::TICKS_PER_DAY + (t % Level::TICKS_PER_DAY);
            level->setTime(newTime);
            std::ostringstream out;
            out << "Set the time to " << t;
            return out.str();
        }

        // -- time query <daytime|gametime|day> ------------------------------
        if (sub == "query") {
            if (args.size() < 3) return "Usage: /time query <daytime|gametime|day>";
            const std::string& what = args[2];

            long total   = level->getTime();
            long daytime = total % Level::TICKS_PER_DAY;
            long day     = total / Level::TICKS_PER_DAY;

            std::ostringstream out;
            if      (what == "daytime")  { out << "The time of day is " << daytime; }
            else if (what == "gametime") { out << "The game time is "   << total;   }
            else if (what == "day")      { out << "The day is "         << day;     }
            else return std::string("Unknown query: ") + what;
            return out.str();
        }

        return "Unknown sub-command. Usage: /time <add|set|query> ...";
    }
     else if (args[0] == "help") {
    return "Available commands:\n"
           "/time add <value>\n"
           "/time set <value|day|night|noon|midnight>\n"
           "/time query <daytime|gametime|day>\n"
           "/noclip <true|false> - Toggle spectator-like mode\n"
           "/help - Show this help";
} else if (args[0] == "noclip") {
    if (args.size() < 2) return "Usage: /noclip <true|false>";
    if (!level) return "No level loaded.";

    bool enable;
    if (args[1] == "true")        enable = true;
    else if (args[1] == "false")  enable = false;
    else                          return "Usage: /noclip <true|false>";

    Player* player = minecraft->player;
    if (!player) return "No local player.";

    static bool savedFlying = false;
    static bool savedInvul = false;
    static bool savedTick = true;
    static long savedTime = 0;

    if (enable) {
    // … 保存状态 …
    player->abilities.flying = true;
    player->abilities.invulnerable = true;
    player->noPhysics = true;
    minecraft->noclip = true;
    level->adventureSettings.doTickTime = false;   // 停止时间
    level->setTime(6000);                         // 正午
    // 不再需要 level->skyDarken = 0 和 updateSkyBrightness()
} else {
    // 恢复
    player->abilities.flying = savedFlying;
    player->abilities.invulnerable = savedInvul;
    player->noPhysics = false;
    minecraft->noclip = false;
    level->adventureSettings.doTickTime = savedTick;
    level->setTime(savedTime);
    level->updateSkyBrightness();                 // 恢复正常时间亮度
    }

    // 立即刷新区块渲染，避免残留黑暗或闪烁
    if (minecraft->levelRenderer) {
        minecraft->levelRenderer->allChanged();
    }

    std::ostringstream out;
    out << "Noclip mode " << (enable ? "enabled" : "disabled");
    return out.str();
}

    return std::string("Unknown command: /") + args[0];
}

// ---------------------------------------------------------------------------
// render
// ---------------------------------------------------------------------------
void ConsoleScreen::render(int /*xm*/, int /*ym*/, float /*a*/)
{
    // Dim the game world slightly
    fillGradient(0, 0, width, height, 0x00000000, 0x40000000);

    const int boxH  = 12;
    const int boxY  = height - boxH - 2;
    const int boxX0 = 2;
    const int boxX1 = width - 2;

    // Input box background
    fill(boxX0, boxY, boxX1, boxY + boxH, 0xc0000000);
    // Border
    fill(boxX0,     boxY,          boxX1, boxY + 1,   0xff808080);
    fill(boxX0,     boxY + boxH - 1, boxX1, boxY + boxH, 0xff808080);
    fill(boxX0,     boxY,          boxX0 + 1, boxY + boxH, 0xff808080);
    fill(boxX1 - 1, boxY,          boxX1,     boxY + boxH, 0xff808080);

    // Input text + blinking cursor
    std::string displayed = _input;
    if ((_cursorBlink / 10) % 2 == 0)
        displayed += '_';

    // Placeholder hint when empty
    font->drawShadow(displayed, (float)(boxX0 + 2), (float)(boxY + 2), 0xffffffff);
}
