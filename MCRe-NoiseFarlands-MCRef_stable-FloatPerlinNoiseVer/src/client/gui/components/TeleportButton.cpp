#include "TeleportButton.h"
#include "../screens/TeleportScreen.h"

TeleportButton::TeleportButton(int id, Minecraft* minecraft)
    : Touch::TButton(id, "Teleport"), minecraft(minecraft) {
    // 显式设置按钮尺寸（原版 TButton 默认 66x26，但为了保险再次明确）
    width = 66;
    height = 26;
}

TeleportButton::~TeleportButton() {}

void TeleportButton::mouseClicked(Minecraft* minecraft, int x, int y, int button) {
    // 只有点击在按钮自身区域内时才触发传送界面
    if (pointInside(x, y)) {
        this->minecraft->setScreen(new TeleportScreen(this->minecraft));
    }
}
