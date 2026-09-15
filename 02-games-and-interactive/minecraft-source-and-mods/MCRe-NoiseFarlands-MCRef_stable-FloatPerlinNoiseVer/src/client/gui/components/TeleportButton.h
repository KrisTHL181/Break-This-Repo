#ifndef TELEPORTBUTTON_H
#define TELEPORTBUTTON_H

#include "Button.h"
#include "../../Minecraft.h"

class TeleportButton : public Touch::TButton {
public:
    TeleportButton(int id, Minecraft* minecraft);
    virtual ~TeleportButton();

    virtual void mouseClicked(Minecraft* minecraft, int x, int y, int button) override;

private:
    Minecraft* minecraft;
};

#endif
