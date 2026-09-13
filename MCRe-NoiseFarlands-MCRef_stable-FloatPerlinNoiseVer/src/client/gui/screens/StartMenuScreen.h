#ifndef NET_MINECRAFT_CLIENT_GUI_SCREENS__StartMenuScreen_H__
#define NET_MINECRAFT_CLIENT_GUI_SCREENS__StartMenuScreen_H__

#include "../Screen.h"
#include "../components/Button.h"
#include "../components/ImageButton.h"
// 文件顶部加上
#include "client/renderer/Textures.h"

class StartMenuScreen: public Screen
{
public:
	StartMenuScreen();
	virtual ~StartMenuScreen();

	void init();
	void setupPositions();

	void tick();
	void render(int xm, int ym, float a);

	void buttonClicked(Button* button);
	virtual void mouseClicked(int x, int y, int buttonNum);
	bool handleBackEvent(bool isDown);
	bool isInGameScreen();
private:
    TextureId m_titleBackgroundTexture;

	Button bHost;
	Button bJoin;
	Button bOptions;
	ImageButton bQuit; // X button in top-right corner

	std::string copyright;
	int copyrightPosX;

	std::string version;
	int versionPosX;

	std::string username;
};

#endif /*NET_MINECRAFT_CLIENT_GUI_SCREENS__StartMenuScreen_H__*/
