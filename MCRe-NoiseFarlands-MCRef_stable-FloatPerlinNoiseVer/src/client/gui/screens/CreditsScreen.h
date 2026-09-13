#ifndef NET_MINECRAFT_CLIENT_GUI_SCREENS__CreditsScreen_H__
#define NET_MINECRAFT_CLIENT_GUI_SCREENS__CreditsScreen_H__

#include "../Screen.h"
#include "../components/Button.h"

class ImageButton;

#include <vector>
#include <string>

class CreditsScreen: public Screen {
public:
    typedef Screen super;
    CreditsScreen();
    virtual ~CreditsScreen();
    void init();
    void setupPositions();
    virtual void tick();
    void render(int xm, int ym, float a);
    void buttonClicked(Button* button);
    virtual void mouseClicked(int x, int y, int buttonNum);
private:
    Touch::THeader* bHeader;
    ImageButton* btnBack;

    std::vector<std::string> _lines;
    float _scrollY;
    float _scrollSpeed;
};

#endif /* NET_MINECRAFT_CLIENT_GUI_SCREENS__CreditsScreen_H__ */
