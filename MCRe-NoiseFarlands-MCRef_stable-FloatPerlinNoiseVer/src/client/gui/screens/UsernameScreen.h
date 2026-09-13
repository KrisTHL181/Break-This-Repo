#ifndef NET_MINECRAFT_CLIENT_GUI_SCREENS__UsernameScreen_H__
#define NET_MINECRAFT_CLIENT_GUI_SCREENS__UsernameScreen_H__

#include "../Screen.h"
#include "../components/Button.h"
#include "client/gui/components/TextBox.h"
#include <string>

class UsernameScreen : public Screen
{
    typedef Screen super;
public:
    UsernameScreen();
    virtual ~UsernameScreen();

    void init() override;
    virtual void setupPositions() override;
    void render(int xm, int ym, float a) override;
    void tick() override;

    virtual bool isPauseScreen() override { return false; }

    virtual void keyPressed(int eventKey) override;
    virtual bool handleBackEvent(bool isDown) override { return true; } // block back/escape
    virtual void removed() override;

protected:
    virtual void buttonClicked(Button* button) override;

private:
    Touch::TButton _btnDone;
    TextBox tUsername;
    std::string _input;
    int _cursorBlink;
};

#endif /*NET_MINECRAFT_CLIENT_GUI_SCREENS__UsernameScreen_H__*/
