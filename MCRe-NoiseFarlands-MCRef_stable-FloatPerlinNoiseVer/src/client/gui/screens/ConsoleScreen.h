#ifndef NET_MINECRAFT_CLIENT_GUI_SCREENS__ConsoleScreen_H__
#define NET_MINECRAFT_CLIENT_GUI_SCREENS__ConsoleScreen_H__

#include "../Screen.h"
#include <string>

class ConsoleScreen: public Screen
{
    typedef Screen super;
public:
    ConsoleScreen();
    virtual ~ConsoleScreen() {}

    void init();
    void render(int xm, int ym, float a);
    void tick();

    virtual bool renderGameBehind() { return true; }
    virtual bool isInGameScreen()   { return true; }
    virtual bool isPauseScreen()    { return false; }

    virtual void keyPressed(int eventKey);
    virtual void charPressed(char inputChar);
    virtual bool handleBackEvent(bool isDown);

private:
    void execute();
    std::string processCommand(const std::string& cmd);

    std::string _input;
    int         _cursorBlink; // tick counter for cursor blink
};

#endif /*NET_MINECRAFT_CLIENT_GUI_SCREENS__ConsoleScreen_H__*/
