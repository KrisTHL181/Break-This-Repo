#ifndef NET_MINECRAFT_CLIENT_GUI_SCREENS__DemoChooseLevelScreen_H__
#define NET_MINECRAFT_CLIENT_GUI_SCREENS__DemoChooseLevelScreen_H__

#include "ChooseLevelScreen.h"
#include "../components/TextBox.h"
#include "../components/Button.h"    // for Touch::THeader
class Button;
class ImageButton;

class SimpleChooseLevelScreen: public ChooseLevelScreen
{
public:
	SimpleChooseLevelScreen(const std::string& levelName);

	virtual ~SimpleChooseLevelScreen();

	void init();
	void setupPositions();
	void tick();

	void render(int xm, int ym, float a);

	void buttonClicked(Button* button);
	bool handleBackEvent(bool isDown);
	virtual void keyPressed(int eventKey);
	virtual void mouseClicked(int x, int y, int buttonNum);

private:
	Touch::THeader* bHeader;
	Button* bGamemode;
	Button* bCheats;
	Button* bNoiseMode;          // 新增：噪声模式切换按钮
	ImageButton* bBack;
	Button* bCreate;
	bool hasChosen;

	std::string levelName;
	int gamemode;
	bool cheatsEnabled;
	int noiseMode;               // 0 = 32-bit, 1 = 64-bit, 2 = Double

	TextBox tLevelName;
	TextBox tSeed;
};

#endif /*NET_MINECRAFT_CLIENT_GUI_SCREENS__DemoChooseLevelScreen_H__*/
