#ifndef DEBUGSCREEN_H
#define DEBUGSCREEN_H

#include "../Screen.h"
#include "../components/Button.h"
#include "../../Minecraft.h"
#include <vector>

class DebugScreen : public Screen
{
public:
    DebugScreen(Minecraft* mc);
    virtual ~DebugScreen();

    virtual void init() override;
    virtual void setupPositions() override;
    virtual void render(int xm, int ym, float a) override;

    virtual bool renderGameBehind() override { return true; }
    virtual bool isInGameScreen() override { return false; }
    virtual bool isPauseScreen() override { return false; }

    virtual void tick() override {}
    virtual void keyPressed(int key) override;

protected:
    virtual void buttonClicked(Button* button) override;

private:
    void addDigitButton(int digit);
    void executeExtraAction(int id);

    Minecraft* mc;

    // 0-9 数字按钮（切换调试页）
    Button* digitButtons[10];

    // 额外调试功能按钮
    std::vector<Button*> extraButtons;

    // 额外功能 ID
    static const int ACT_HEAL_RESET       = 100;
    static const int ACT_TOGGLE_GAMEMODE  = 101;
    static const int ACT_ADVANCE_TIME     = 102;
    static const int ACT_OPEN_ARMOR       = 103;
    static const int ACT_HURT_RELOAD      = 104;
    static const int ACT_SPAWN_MOB        = 105;
    static const int ACT_MASSACRE         = 106;
    static const int ACT_REFILL_INV       = 107;
    static const int ACT_PRERENDER        = 108;
    static const int ACT_DROP_INV         = 109;
    static const int ACT_TOGGLE_DIFFICULTY= 110;
    static const int ACT_TOGGLE_3RDPERSON = 111;
    static const int ACT_SPEEDUP          = 112;
    static const int ACT_NOPVP            = 113;
    static const int ACT_NOPVM            = 114;
    static const int ACT_NOMVP            = 115;
    static const int ACT_IMMUTABLE        = 116;
    static const int ACT_NAMETAGS         = 117;
    static const int ACT_PARTICLES        = 118;
    static const int ACT_BACK             = 119;   // 新增
};

#endif // DEBUGSCREEN_H
