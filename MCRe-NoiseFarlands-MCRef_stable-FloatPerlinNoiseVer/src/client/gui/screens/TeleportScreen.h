#ifndef TELEPORTSCREEN_H
#define TELEPORTSCREEN_H

#include "../Screen.h"
#include "../components/TextBox.h"

class Minecraft;

class TeleportScreen : public Screen {
public:
    TeleportScreen(Minecraft* minecraft);
    virtual ~TeleportScreen();

    virtual void init();
    virtual void render(int xm, int ym, float a);
    virtual void tick();
    virtual void keyPressed(int key);
    virtual void charPressed(char inputChar);
    virtual void mouseClicked(int x, int y, int button);

private:
    Minecraft* minecraft;
    TextBox* textBox;
    void teleport();
};

#endif
