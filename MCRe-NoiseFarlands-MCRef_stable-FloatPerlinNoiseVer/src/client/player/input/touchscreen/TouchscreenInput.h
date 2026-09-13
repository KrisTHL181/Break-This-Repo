#ifndef NET_MINECRAFT_CLIENT_PLAYER_INPUT_TOUCHSCREEN_TouchscreenInput_H__
#define NET_MINECRAFT_CLIENT_PLAYER_INPUT_TOUCHSCREEN_TouchscreenInput_H__

//package net.minecraft.client.player;

#include "../IMoveInput.h"
#include "../../../gui/GuiComponent.h"
#include "TouchAreaModel.h"
#include "../../../renderer/RenderChunk.h"

class Options;
class Player;
class Minecraft;
class PolygonArea;

// @todo: extract a separate MoveInput (-> merge XperiaPlayInput)
class TouchscreenInput_TestFps:	public IMoveInput,
								public GuiComponent
{
public:
    static const int KEY_UP = 0;
    static const int KEY_DOWN = 1;
    static const int KEY_LEFT = 2;
    static const int KEY_RIGHT = 3;
    static const int KEY_JUMP = 4;
    static const int KEY_SNEAK = 5;
	static const int KEY_CRAFT = 6;
	static const int NumKeys = 7;

    TouchscreenInput_TestFps(Minecraft* mc, Options* options);
	~TouchscreenInput_TestFps();

	void onConfigChanged(const Config& c);

	void tick(Player* player);
	void render(float a);

	void setKey(int key, bool state);
    void releaseAllKeys();

	const RectangleArea& getRectangleArea();
    const RectangleArea& getPauseRectangleArea();
    RectangleArea* aJump;
RectangleArea* aFlyUp;      // 右侧飞行上升按钮
    RectangleArea* aFlyDown;    // 右侧飞行下降按钮
    // 在 TouchscreenInput_TestFps 类的 public 区域添加
const RectangleArea& getJumpRectangleArea() const { return *aJump; }
const RectangleArea& getFlyUpRectangleArea() const { return *aFlyUp; }
const RectangleArea& getFlyDownRectangleArea() const { return *aFlyDown; }
const RectangleArea& getPauseRectangleArea() const { return *aPause; }
const RectangleArea& getChatRectangleArea() const { return *aChat; }

private:
    // 原移动按钮区域指针保留（aUp, aDown, aLeft, aRight, aJump, ...）
    

    // 新增区域ID
    static const int AREA_DPAD_FLY_UP   = 107;
    static const int AREA_DPAD_FLY_DOWN = 108;
	void clear();

	RectangleArea _boundingRectangle;

	bool _keys[NumKeys];
	Options* _options;

	bool _pressedJump;
	bool _forward;
	bool _northJump;
	bool _renderFlightImage;
	TouchAreaModel _model;
	Minecraft* _minecraft;

	RectangleArea* aLeft;
	RectangleArea* aRight;
	RectangleArea* aUp;
	RectangleArea* aDown;
	RectangleArea* aPause;
	RectangleArea* aChat;
	//RectangleArea* aUpJump;
	
	RectangleArea* aUpLeft;
	RectangleArea* aUpRight;
	bool _pauseIsDown;

	RenderChunk _render;
	bool _allowHeightChange;
	float _sneakTapTime;

	// 找到 _buttons 的定义，扩大至至少 9（或者直接用 109-100=9）
bool _buttons[9];   // 原来是 8
	bool isButtonDown(int areaId);
	void rebuild();
};

#endif /*NET_MINECRAFT_CLIENT_PLAYER_INPUT_TOUCHSCREEN_TouchscreenInput_H__*/
