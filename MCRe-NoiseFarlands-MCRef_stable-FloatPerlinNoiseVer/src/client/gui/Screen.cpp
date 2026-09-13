#include "Screen.h"
#include "components/Button.h"
#include "components/TextBox.h"
#include "../Minecraft.h"
#include "../renderer/Tesselator.h"
#include "../sound/SoundEngine.h"
#include "../../platform/input/Keyboard.h"
#include "../../platform/input/Mouse.h"
#include "../renderer/Textures.h"

Screen::Screen()
:   passEvents(false),
	clickedButton(NULL),
	tabButtonIndex(0),
	width(1),
	height(1),
	minecraft(NULL),
	font(NULL)
{
}

void Screen::render( int xm, int ym, float a )
{
	for (unsigned int i = 0; i < buttons.size(); i++) {
		Button* button = buttons[i];
		button->render(minecraft, xm, ym);
	}

	// render any text boxes after buttons
	for (unsigned int i = 0; i < textBoxes.size(); i++) {
		TextBox* textbox = textBoxes[i];
		textbox->render(minecraft, xm, ym);
	}
}

void Screen::init( Minecraft* minecraft, int width, int height )
{
	//particles = /*new*/ GuiParticles(minecraft);
	this->minecraft = minecraft;
	this->font = minecraft->font;
	this->width = width;
	this->height = height;
	init();
	setupPositions();
	updateTabButtonSelection();
}

void Screen::init()
{
}

void Screen::setSize( int width, int height )
{
	this->width = width;
	this->height = height;
	setupPositions();
}

bool Screen::handleBackEvent( bool isDown )
{
	return false;
}

void Screen::updateEvents()
{
	if (passEvents)
		return;

	while (Mouse::next())
		mouseEvent();

	while (Keyboard::next())
		keyboardEvent();
	while (Keyboard::nextTextChar())
		keyboardTextEvent();
}

void Screen::mouseEvent()
{
	const MouseAction& e = Mouse::getEvent();
	// forward wheel events to subclasses
	if (e.action == MouseAction::ACTION_WHEEL) {
		int xm = e.x * width / minecraft->width;
		int ym = e.y * height / minecraft->height - 1;
		mouseWheel(e.dx, e.dy, xm, ym);
		return;
	}

	if (!e.isButton())
		return;

	if (Mouse::getEventButtonState()) {
		int xm = e.x * width / minecraft->width;
		int ym = e.y * height / minecraft->height - 1;
		mouseClicked(xm, ym, Mouse::getEventButton());
	} else {
		int xm = e.x * width / minecraft->width;
		int ym = e.y * height / minecraft->height - 1;
		mouseReleased(xm, ym, Mouse::getEventButton());
	}
}

void Screen::keyboardEvent()
{
	if (Keyboard::getEventKeyState()) {
		//if (Keyboard.getEventKey() == Keyboard.KEY_F11) {
		//    minecraft->toggleFullScreen();
		//    return;
		//}
		keyPressed(Keyboard::getEventKey());
	}
}
void Screen::keyboardTextEvent()
{
	charPressed(Keyboard::getChar());
}
void Screen::renderBackground()
{
	renderBackground(0);
}

void Screen::renderBackground( int vo )
{
	if (minecraft->isLevelGenerated()) {
		fillGradient(0, 0, width, height, 0xc0101010, 0xd0101010);
	} else {
		renderDirtBackground(vo);
	}
}

void Screen::renderDirtBackground( int vo )
{
	//glDisable2(GL_LIGHTING);
	glDisable2(GL_FOG);
	Tesselator& t = Tesselator::instance;
	minecraft->textures->loadAndBindTexture("gui/background.png");
	glColor4f2(1, 1, 1, 1);
	float s = 32;
	float fvo = (float) vo;
	t.begin();
	t.color(0x404040);
	t.vertexUV(0, (float)height, 0, 0, height / s + fvo);
	t.vertexUV((float)width, (float)height, 0, width / s, (float)height / s + fvo);
	t.vertexUV((float)width, 0, 0, (float)width / s, 0 + fvo);
	t.vertexUV(0, 0, 0, 0, 0 + fvo);
	t.draw();
}

bool Screen::isPauseScreen()
{
	return true;
}

bool Screen::isErrorScreen()
{
	return false;
}

bool Screen::isInGameScreen()
{
	return true;
}

bool Screen::closeOnPlayerHurt() {
    return false;
}

void Screen::keyPressed(int eventKey) {
    // 检查是否有任何 TextBox 拥有焦点
    bool hasFocusedTextBox = false;
    for (std::vector<TextBox*>::iterator it = textBoxes.begin(); it != textBoxes.end(); ++it) {
        if ((*it)->focused) {
            hasFocusedTextBox = true;
            break;
        }
    }
    if (hasFocusedTextBox && (eventKey == Keyboard::KEY_RETURN || (eventKey >= '0' && eventKey <= '9'))) {
        return;   // 让文本框自行处理回车和数字
	}
	
    // 如果有焦点文本框，并且按的是数字键，则直接返回，不触发屏幕关闭/按钮点击
    if (hasFocusedTextBox && eventKey >= '0' && eventKey <= '9') {
        return;
    }
    
    // 原有逻辑（例如 ESC 关闭屏幕、按钮导航等）
    // ...
	if (eventKey == Keyboard::KEY_ESCAPE) {
		minecraft->setScreen(NULL);
		//minecraft->grabMouse();
	}

	// pass key events to any text boxes first
	for (auto& textbox : textBoxes) {
		textbox->keyPressed(minecraft, eventKey);
	}

#ifdef TABBING
	if (minecraft->useTouchscreen())
		return;


	// "Tabbing" the buttons (walking with keys)
	const int tabButtonCount = tabButtons.size();
	if (!tabButtonCount)
		return;

	Options& o = minecraft->options;
	if (eventKey == o.getIntValue(OPTIONS_KEY_MENU_NEXT))
		if (++tabButtonIndex == tabButtonCount) tabButtonIndex = 0;
	if (eventKey == o.getIntValue(OPTIONS_KEY_MENU_PREV))
		if (--tabButtonIndex == -1) tabButtonIndex = tabButtonCount-1;
	if (eventKey == o.getIntValue(OPTIONS_KEY_MENU_OK)) {
		Button* button = tabButtons[tabButtonIndex];
		if (button->active) {
			minecraft->soundEngine->playUI("random.click", 1, 1);
			buttonClicked(button);
		}
	}

	updateTabButtonSelection();
#endif
}

void Screen::charPressed(char inputChar) {
	for (auto& textbox : textBoxes) {
		textbox->charPressed(minecraft, inputChar);
	}
}

void Screen::updateTabButtonSelection()
{
#ifdef TABBING
	if (minecraft->useTouchscreen())
		return;

	for (unsigned int i = 0; i < tabButtons.size(); ++i)
		tabButtons[i]->selected = (i == tabButtonIndex);
#endif
}

void Screen::mouseClicked( int x, int y, int buttonNum )
{
	if (buttonNum == MouseAction::ACTION_LEFT) {
		for (unsigned int i = 0; i < buttons.size(); ++i) {
			Button* button = buttons[i];
            //LOGI("Hit-testing button: %p\n", button);
			if (button->clicked(minecraft, x, y)) {
                button->setPressed();

                //LOGI("Hit-test successful: %p\n", button);
				clickedButton = button;
/*
#if !defined(ANDROID) && !defined(__APPLE__) //if (!minecraft->isTouchscreen()) {
					minecraft->soundEngine->playUI("random.click", 1, 1);
					buttonClicked(button);
#endif }
*/
			}
		}
	}

	// let textboxes see the click regardless
	for (auto& textbox : textBoxes) {
		textbox->mouseClicked(minecraft, x, y, buttonNum);
	}
}

void Screen::mouseReleased( int x, int y, int buttonNum )
{
	//LOGI("b_id: %d, (%p), text: %s\n", buttonNum, clickedButton, clickedButton?clickedButton->msg.c_str():"<null>");
	if (!clickedButton || buttonNum != MouseAction::ACTION_LEFT) return;

#if 1
//#if defined(ANDROID) || defined(__APPLE__) //if (minecraft->isTouchscreen()) {
		for (unsigned int i = 0; i < buttons.size(); ++i) {
			Button* button = buttons[i];
			if (clickedButton == button && button->clicked(minecraft, x, y)) {
				buttonClicked(button);
				minecraft->soundEngine->playUI("random.click", 1, 1);
				clickedButton->released(x, y);
			}
		}
# else //	} else {
		clickedButton->released(x, y);
#endif // }
	clickedButton = NULL;
}

bool Screen::renderGameBehind() {
	return true;
}

bool Screen::hasClippingArea( IntRectangle& out )
{
	return false;
}

void Screen::lostFocus() {
	for(std::vector<TextBox*>::iterator it = textBoxes.begin(); it != textBoxes.end(); ++it) {
		TextBox* tb = *it;
		tb->loseFocus(minecraft);
	}
}

void Screen::toGUICoordinate( int& x, int& y ) {
	x = x * width / minecraft->width;
	y = y * height / minecraft->height - 1;
}
