#include "UsernameScreen.h"
#include "StartMenuScreen.h"
#include "../../Minecraft.h"
#include "../Font.h"
#include "../components/Button.h"
#include "../../../platform/input/Keyboard.h"
#include "../../../AppPlatform.h"

UsernameScreen::UsernameScreen()
:   _btnDone(0, "Done"),
    tUsername(0, "Username"),
    _cursorBlink(0)
{
}

UsernameScreen::~UsernameScreen()
{
}

void UsernameScreen::init()
{
    _input = "MFSCelebrate";
    _btnDone.active = true; // disabled until name typed
    buttons.push_back(&_btnDone);
    tabButtons.push_back(&_btnDone);
    textBoxes.push_back(&tUsername);
    setupPositions();
}

void UsernameScreen::setupPositions()
{
    int cx = width / 2;
    int cy = height / 2;

    // Make the done button match the touch-style option tabs
    _btnDone.width  = 66;
    _btnDone.height = 26;
    _btnDone.x = (width - _btnDone.width) / 2;
    _btnDone.y = height / 2 + 52;

    tUsername.width = 120;
    tUsername.height = 20;
    tUsername.x = (width - tUsername.width) / 2;
    tUsername.y = _btnDone.y - 60;
}

void UsernameScreen::tick()
{
    for (auto* tb : textBoxes)
        tb->tick(minecraft);
}

void UsernameScreen::keyPressed(int eventKey)
{
    if (eventKey == Keyboard::KEY_RETURN) {
        if (!tUsername.text.empty())
            buttonClicked(&_btnDone);
    }

    // deliberately do NOT call super::keyPressed — that would close the screen on Escape
    Screen::keyPressed(eventKey);

    // enable the Done button only when there is some text (and ensure it updates after backspace)
    _btnDone.active = tUsername.text.empty();
}

void UsernameScreen::removed()
{
    minecraft->platform()->hideKeyboard();
}

void UsernameScreen::buttonClicked(Button* button)
{
    if (button == &_btnDone && !tUsername.text.empty()) {
        minecraft->options.set(OPTIONS_USERNAME, tUsername.text);
        minecraft->options.save();
        minecraft->setScreen(NULL); // goes to StartMenuScreen
    }
}

void UsernameScreen::render(int xm, int ym, float a)
{
    // Dark dirt background
    renderBackground();

    int cx = width / 2;
    int cy = height / 2;

    // Title
    drawCenteredString(font, "Enter your username", cx, cy - 70, 0xffffffff);

    // Subtitle
    drawCenteredString(font, "Please choose a username so others can easily", cx, cy - 52, 0xffaaaaaa);
    drawCenteredString(font, "identify you in chat. Don't worry, you can", cx, cy - 40, 0xffaaaaaa);
    drawCenteredString(font, "change it anytime.", cx, cy - 28, 0xffaaaaaa);

    // // Hint below box
    // drawCenteredString(font, "Max 16 characters", cx, cy + 20, 0xff808080);

    // Buttons (Done)
    super::render(xm, ym, a);
}
