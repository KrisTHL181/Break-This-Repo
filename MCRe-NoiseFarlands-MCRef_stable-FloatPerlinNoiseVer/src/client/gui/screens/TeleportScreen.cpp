#include "TeleportScreen.h"
#include "../../Minecraft.h"
#include "../../player/LocalPlayer.h"
#include "../Font.h"
#include "../../../platform/input/Keyboard.h"
#include "../../../world/level/Level.h"
#include <sstream>
#include <cstdlib>
#include <vector>

TeleportScreen::TeleportScreen(Minecraft* minecraft)
    : minecraft(minecraft), textBox(nullptr) {}

TeleportScreen::~TeleportScreen() {
    if (textBox) delete textBox;
}

void TeleportScreen::init() {
    int boxX = width / 2 - 100;
    int boxY = height / 2 - 20;
    textBox = new TextBox(0, boxX, boxY, 200, 20, "");
}

void TeleportScreen::render(int xm, int ym, float a) {
    fill(0, 0, width, height, 0x80000000);
    minecraft->font->draw("Enter coordinates (X Y Z), support ~ for relative:", 
                          width / 2 - 200, height / 2 - 50, 0xffffff);
    if (textBox) textBox->render(minecraft, xm, ym);
}

void TeleportScreen::tick() {
    if (textBox) textBox->tick(minecraft);
}

void TeleportScreen::keyPressed(int key) {
    if (key == Keyboard::KEY_RETURN) {
        teleport();
        minecraft->setScreen(nullptr);
    } else if (textBox) {
        textBox->keyPressed(minecraft, key);
    }
}

void TeleportScreen::charPressed(char inputChar) {
    if (textBox) textBox->charPressed(minecraft, inputChar);
}

void TeleportScreen::mouseClicked(int x, int y, int button) {
    if (textBox) textBox->mouseClicked(minecraft, x, y, button);
}

void TeleportScreen::teleport() {
    std::string input = textBox->text;
    if (input.empty()) return;

    std::stringstream ss(input);
    std::vector<std::string> parts;
    std::string part;
    while (ss >> part) parts.push_back(part);
    if (parts.size() < 3) return;

    LocalPlayer* player = minecraft->player;
    Level* level = minecraft->level;
    if (!player || !level) return;

    Pos spawnPos = level->getSharedSpawnPos();

    double x = player->x;
    double y = player->y;
    double z = player->z;

    auto parseCoord = [&](const std::string& s, double current, double spawnBase) -> double {
        if (s.empty()) return current;
        if (s[0] == '~') {
            if (s.length() == 1) return current;
            const char* numStr = s.c_str() + 1;
            char* endptr;
            double offset = strtod(numStr, &endptr);
            if (endptr == numStr) offset = 0.0;
            return current + offset;
        } else {
            char* endptr;
            double absolute = strtod(s.c_str(), &endptr);
            if (endptr == s.c_str()) return current;
            return absolute + spawnBase;
        }
    };

    x = parseCoord(parts[0], x, (double)spawnPos.x);
    y = parseCoord(parts[1], y, (double)spawnPos.y);
    z = parseCoord(parts[2], z, (double)spawnPos.z);

    player->setPos(x, y, z);
    player->xOld = player->x;
    player->yOld = player->y;
    player->zOld = player->z;
}
