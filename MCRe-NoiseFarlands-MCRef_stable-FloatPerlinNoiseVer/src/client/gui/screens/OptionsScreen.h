#ifndef NET_MINECRAFT_CLIENT_GUI_SCREENS__OptionsScreen_H__
#define NET_MINECRAFT_CLIENT_GUI_SCREENS__OptionsScreen_H__

#include "../Screen.h"
#include "../components/Button.h"
#include "../components/OptionsGroup.h"
#include "client/renderer/Textures.h"   // <--- 新增，提供 TextureId 和 Textures 完整声明

class ImageButton;
class OptionsPane;

class OptionsScreen: public Screen
{
	typedef Screen super;

	void init() override;
	void generateOptionScreens();

public:
	OptionsScreen();
	~OptionsScreen();

	void setupPositions() override;
	void buttonClicked(Button* button) override;
	void render(int xm, int ym, float a) override;
	void removed() override;
	void selectCategory(int index);

	virtual void mouseClicked(int x, int y, int buttonNum) override;
	virtual void mouseReleased(int x, int y, int buttonNum) override;
	virtual void mouseWheel(int dx, int dy, int xm, int ym) override;
	virtual void keyPressed(int eventKey) override;
	virtual void charPressed(char inputChar) override;
	
	virtual void tick() override;
	virtual void lostFocus() override;

// 在类定义里加个成员，避免每帧重复加载
private:
    TextureId m_backgroundTexture;
	Touch::THeader* bHeader;
	ImageButton* btnClose;
	Button* btnCredits;

	std::vector<Touch::TButton*> categoryButtons;
	std::vector<OptionsGroup*> optionPanes;
	OptionsGroup* currentOptionsGroup;
	int selectedCategory;

	// 滚动相关
	float scrollOffset;
	float maxScrollOffset;
	float scrollVelocity;
	float lastMouseY;
	bool isDragging;
	bool isScrollbarVisible;

	void updateMaxScrollOffset();
	void applyScrollLimits();
	float getContentHeight() const;
	bool isPointInScrollArea(int x, int y) const;
	void transformMouseForScroll(int& x, int& y) const;
};

#endif /*NET_MINECRAFT_CLIENT_GUI_SCREENS__OptionsScreen_H__*/
