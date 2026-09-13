#include "TouchscreenInput.h"
#include "../../../Options.h"
#include "../../../../platform/input/Multitouch.h"
#include "../../../gui/Gui.h"
#include "../../../renderer/Tesselator.h"
#include "../../../../world/entity/player/Player.h"

#include "../../../Minecraft.h"
#include "../../../../platform/log.h"
#include "../../../renderer/Textures.h"
#include "../../../sound/SoundEngine.h"
#include "client/gui/screens/ScreenChooser.h"


// ARGHHHHHH WHY NOT FUCKING ENUM
static const int AREA_DPAD_FIRST = 100;
static const int AREA_DPAD_N = 100;
static const int AREA_DPAD_S = 101;
static const int AREA_DPAD_W = 102;
static const int AREA_DPAD_E = 103;
static const int AREA_DPAD_C = 104;
static const int AREA_PAUSE = 105;
static const int AREA_CHAT = 106;
static const int AREA_DPAD_FLY_UP   = 107;
static const int AREA_DPAD_FLY_DOWN = 108;

static int cPressed = 0;
static int cReleased = 0;
static int cDiscreet = 0;
static int cPressedPause = 0;
static int cReleasedPause = 0;
//static const int AREA_DPAD_N_JUMP = 105;

//
// TouchscreenInput_TestFps
//

static void Copy(int n, float* x, float* y, float* dx, float* dy) {
	for (int i = 0; i < n; ++i) {
		dx[i] = x[i];
		dy[i] = y[i];
	}
}

static void Translate(int n, float* x, float* y, float xt, float yt) {
	for (int i = 0; i < n; ++i) {
		x[i] += xt;
		y[i] += yt;
	}
}

static void Scale(int n, float* x, float* y, float xt, float yt) {
	for (int i = 0; i < n; ++i) {
		x[i] *= xt;
		y[i] *= yt;
	}
}

static void Transformed(int n, float* x, float* y, float* dx, float* dy, float xt, float yt, float sx=1.0f, float sy=1.0f) {
	Copy(n, x, y, dx, dy);
	Scale(n, dx, dy, sx, sy);
	Translate(n, dx, dy, xt, yt);

	//for (int i = 0; i < n; ++i) {
	//	LOGI("%d. (%f, %f)\n", i, dx[i], dy[i]);
	//}
}

TouchscreenInput_TestFps::TouchscreenInput_TestFps( Minecraft* mc, Options* options )
:	_minecraft(mc),
	_options(options),
	_northJump(false),
	_forward(false),
	_boundingRectangle(0, 0, 1, 1),
	_pressedJump(false),
	_pauseIsDown(false),
	_sneakTapTime(-999),
	aLeft(0),
	aRight(0),
	aUp(0),
	aDown(0),
	aJump(0),
	aUpLeft(0),
	aUpRight(0),
    aFlyUp(nullptr),
    aFlyDown(nullptr),
	_allowHeightChange(false)
{
	releaseAllKeys();
	onConfigChanged( createConfig(mc) );

	Tesselator& t = Tesselator::instance;
	const int alpha = 128;
	t.color( 0xc0c0c0, alpha); cPressed  = t.getColor();
	t.color( 0xffffff, alpha); cReleased = t.getColor();
	t.color( 0xffffff, alpha / 4); cDiscreet = t.getColor();
    t.color( 0xc0c0c0, 80); cPressedPause=t.getColor();
    t.color( 0xffffff, 80); cReleasedPause=t.getColor();
}

TouchscreenInput_TestFps::~TouchscreenInput_TestFps() {
	clear();
}

void TouchscreenInput_TestFps::clear() {
	_model.clear();

	delete aUpLeft; aUpLeft = NULL; // @todo: SAFEDEL
	delete aUpRight; aUpRight = NULL;
}

bool TouchscreenInput_TestFps::isButtonDown(int areaId) {
	return _buttons[areaId - AREA_DPAD_FIRST];
}


void TouchscreenInput_TestFps::onConfigChanged(const Config& c) {
    clear();
    const float w = (float)c.width;
    const float h = (float)c.height;
    float Bw = w * 0.11f;
    float Bh = Bw;
    PixelCalc& pc = _minecraft->pixelCalc;
    if (pc.pixelsToMillimeters(Bw) > 200) {
        Bw = Bh = pc.millimetersToPixels(200);
    }

    const bool leftHanded = _options->getBooleanValue(OPTIONS_IS_LEFT_HANDED);
    const float margin = 8.0f;

    // 移动区域基准X（左手模式靠右，右手靠左）
    const float moveBaseX = leftHanded ? (w - margin - 3.0f * Bw) : margin;
    // 旧：
//const float jumpBaseX = leftHanded ? margin : (w - margin - Bw);
// 改成：
const float jumpBaseX = leftHanded ? margin : (w - margin - 2.0f * Bw);

    const float BaseY = -8.0f + h - 3.0f * Bh;

    // 更新移动判定用的包围矩形（保证滑动出方向）
    _boundingRectangle = RectangleArea(moveBaseX, BaseY, moveBaseX + 3.0f * Bw, BaseY + 3.0f * Bh);

    // ---------- 左侧移动按钮 ----------
    float xx, yy;
    xx = moveBaseX + Bw; yy = BaseY;
    _model.addArea(AREA_DPAD_N, aUp = new RectangleArea(xx, yy, xx + Bw, yy + Bh));

    xx = moveBaseX; yy = BaseY;
    aUpLeft = new RectangleArea(xx, yy, xx + Bw, yy + Bh); // 左上（用于滑动）

    xx = moveBaseX + 2.0f * Bw; yy = BaseY;
    aUpRight = new RectangleArea(xx, yy, xx + Bw, yy + Bh); // 右上

    xx = moveBaseX; yy = BaseY + Bh;
    _model.addArea(AREA_DPAD_W, aLeft = new RectangleArea(xx, yy, xx + Bw, yy + Bh));

    xx = moveBaseX + 2.0f * Bw; yy = BaseY + Bh;
    _model.addArea(AREA_DPAD_E, aRight = new RectangleArea(xx, yy, xx + Bw, yy + Bh));

    xx = moveBaseX + Bw; yy = BaseY + 2.0f * Bh;
    _model.addArea(AREA_DPAD_S, aDown = new RectangleArea(xx, yy, xx + Bw, yy + Bh));

    // ---------- 右侧跳跃 & 飞行按钮 ----------
    // 飞行上升（放在跳跃上方）
    xx = jumpBaseX; yy = BaseY;
    _model.addArea(AREA_DPAD_FLY_UP, aFlyUp = new RectangleArea(xx, yy, xx + Bw, yy + Bh));

    // 跳跃（中间）
    xx = jumpBaseX; yy = BaseY + Bh;
    _model.addArea(AREA_DPAD_C, aJump = new RectangleArea(xx, yy, xx + Bw, yy + Bh));

    // 飞行下降（放在跳跃下方）
    xx = jumpBaseX; yy = BaseY + 2.0f * Bh;
    _model.addArea(AREA_DPAD_FLY_DOWN, aFlyDown = new RectangleArea(xx, yy, xx + Bw, yy + Bh));

    // 其他按钮（暂停、聊天）保持不变
    float maxPixels = _minecraft->pixelCalc.millimetersToPixels(10);
    float btnSize = pc.millimetersToPixels(18 * Gui::GuiScale);
    const float spacing = 8.0f;
    const float totalWidth = btnSize * 2.0f + spacing;
    const float baseX = (w - totalWidth) / 2.0f;
    _model.addArea(AREA_CHAT,   aChat  = new RectangleArea(baseX, 4.0f, baseX + btnSize, 4.0f + btnSize));
    _model.addArea(AREA_PAUSE,  aPause = new RectangleArea(baseX + btnSize + spacing, 4.0f, baseX + btnSize + spacing + btnSize, 4.0f + btnSize));
}

void TouchscreenInput_TestFps::setKey(int key, bool state)
{
	#ifdef WIN32
		//LOGI("key: %d, %d\n", key, state);

		int id = -1;
		// theres no keyUp etc???
		//if (key == _options->keyUp.key) id = KEY_UP;
		//if (key == _options->keyDown.key) id = KEY_DOWN;
		//if (key == _options->keyLeft.key) id = KEY_LEFT;
		//if (key == _options->keyRight.key) id = KEY_RIGHT;
		//if (key == _options->keyJump.key) id = KEY_JUMP;
		//if (key == _options->keySneak.key) id = KEY_SNEAK;
		//if (key == _options->keyCraft.key) id = KEY_CRAFT;
		//if (id >= 0) {
		//	_keys[id] = state;
		//}

		if (key == _options->getIntValue(OPTIONS_KEY_FORWARD)) id = KEY_UP;
		if (key == _options->getIntValue(OPTIONS_KEY_BACK)) id = KEY_DOWN;
		if (key == _options->getIntValue(OPTIONS_KEY_LEFT)) id = KEY_LEFT;
		if (key == _options->getIntValue(OPTIONS_KEY_RIGHT)) id = KEY_RIGHT;
		if (key == _options->getIntValue(OPTIONS_KEY_JUMP)) id = KEY_JUMP;
		if (key == _options->getIntValue(OPTIONS_KEY_SNEAK)) id = KEY_SNEAK;
		//if (key == _options->getIntValue(OPTIONS_KEY_CRAFT)) id = KEY_CRAFT;
	#endif
}

void TouchscreenInput_TestFps::releaseAllKeys()
{
	xa = 0;
	ya = 0;

	for (int i = 0; i < 9; ++i) _buttons[i] = false;
#ifdef WIN32
	for (int i = 0; i<NumKeys; ++i)
		_keys[i] = false;
#endif
	_pressedJump = false;
	_allowHeightChange = false;
}

void TouchscreenInput_TestFps::tick( Player* player ) {
    xa = 0;
    ya = 0;
    jumping = false;
    wantUp = false;
    wantDown = false;
    isChangingFlightHeight = false;

    bool heldJump = false;
    bool tmpForward = false;
    bool tmpNorthJump = false;

    for (int i = 0; i < 6; ++i)
        _buttons[i] = false;

    const int* pointerIds;
    int pointerCount = Multitouch::getActivePointerIdsThisUpdate(&pointerIds);
    for (int i = 0; i < pointerCount; ++i) {
        int p = pointerIds[i];
        int x = Multitouch::getX(p);
        int y = Multitouch::getY(p);

        if (_boundingRectangle.isInside((float)x, (float)y) && _forward && !isChangingFlightHeight) {
            float angle = Mth::PI + Mth::atan2(y - _boundingRectangle.centerY(), x - _boundingRectangle.centerX());
            ya = Mth::sin(angle);
            xa = Mth::cos(angle);
            tmpForward = true;
        }

        int areaId = _model.getPointerId(x, y, p);
        if (areaId < AREA_DPAD_FIRST) continue;

        // 飞行升降按钮独立处理
        if (player->abilities.flying) {
            if (areaId == AREA_DPAD_FLY_UP) {
                wantUp = true;
                isChangingFlightHeight = true;
                ya = 0;
            }
            if (areaId == AREA_DPAD_FLY_DOWN) {
                wantDown = true;
                isChangingFlightHeight = true;
                ya = 0;
            }
        }

        if (areaId == AREA_DPAD_C) {
            heldJump = true;
            if (player->isInWater()) {
                jumping = true;
            } else if (Multitouch::isPressed(p)) {
                jumping = true;
            }
        }
        if (areaId == AREA_DPAD_N) {
            if (player->isInWater())
                jumping = true;
            else if (!isChangingFlightHeight)
                tmpForward = true;
            ya += 1;
        }
        else if (areaId == AREA_DPAD_S && !_forward) {
            ya -= 1;
        }
        else if (areaId == AREA_DPAD_W && !_forward) {
            xa += 1;
        }
        else if (areaId == AREA_DPAD_E && !_forward) {
            xa -= 1;
        }
        else if (areaId == AREA_PAUSE) {
            if (Multitouch::isReleased(p)) {
                _minecraft->soundEngine->playUI("random.click", 1, 1);
                _minecraft->screenChooser.setScreen(SCREEN_PAUSE);
            }
        }
        else if (areaId == AREA_CHAT) {
            if (Multitouch::isReleased(p)) {
                _minecraft->soundEngine->playUI("random.click", 1, 1);
                _minecraft->screenChooser.setScreen(SCREEN_CONSOLE);
                _minecraft->platform()->showKeyboard();
            }
        }

        _buttons[areaId - AREA_DPAD_FIRST] = true;
    }

    _forward = tmpForward;

    if (tmpNorthJump) {
        if (!_northJump)
            jumping = true;
        _northJump = true;
    }
    else _northJump = false;

    // 将飞行升降状态传递给 IMoveInput 的成员变量
    // 注意：IMoveInput 中已有 wantUp, wantDown, isChangingFlightHeight
    // 这里直接赋值，因为本类继承自 IMoveInput
    this->wantUp = wantUp;
    this->wantDown = wantDown;
    this->isChangingFlightHeight = isChangingFlightHeight;

    _pressedJump = heldJump;
    _renderFlightImage = player->abilities.flying;

    // 原版键盘输入兼容（Windows等）
#ifdef WIN32
    if (_keys[KEY_UP]) ya++;
    if (_keys[KEY_DOWN]) ya--;
    if (_keys[KEY_LEFT]) xa++;
    if (_keys[KEY_RIGHT]) xa--;
    if (_keys[KEY_JUMP]) jumping = true;
    if (_keys[KEY_CRAFT])
        player->startCrafting((int)player->x, (int)player->y, (int)player->z, Recipe::SIZE_2X2);
#endif

    if (sneaking) {
        xa *= 0.3f;
        ya *= 0.3f;
    }
}

static void drawRectangleArea(Tesselator& t, RectangleArea* a, int ux, int vy, float ssz = 64.0f) {
	const float pm = 1.0f / 256.0f;
	const float sz = ssz * pm;
	const float uu = (float)(ux) * pm;
	const float vv = (float)(vy) * pm;
	const float x0 = a->_x0 * Gui::InvGuiScale;
	const float x1 = a->_x1 * Gui::InvGuiScale;
	const float y0 = a->_y0 * Gui::InvGuiScale;
	const float y1 = a->_y1 * Gui::InvGuiScale;

	t.vertexUV(x0, y1, 0, uu,	vv+sz);
	t.vertexUV(x1, y1, 0, uu+sz,vv+sz);
	t.vertexUV(x1, y0, 0, uu+sz,vv);
	t.vertexUV(x0, y0, 0, uu,	vv);
}

static void drawPolygonArea(Tesselator& t, PolygonArea* a, int x, int y) {
	float pm = 1.0f / 256.0f;
	float sz = 64.0f * pm;
	float uu = (float)(x) * pm;
	float vv = (float)(y) * pm;

	float uvs[] = {uu, vv, uu+sz, vv, uu+sz, vv+sz, uu, vv+sz};
	const int o = 0;

	for (int j = 0; j < a->_numPoints; ++j) {
		t.vertexUV(a->_x[j] * Gui::InvGuiScale, a->_y[j] * Gui::InvGuiScale, 0, uvs[(o+j+j)&7], uvs[(o+j+j+1)&7]);
	}
}

void TouchscreenInput_TestFps::render( float a ) {
	//return;

	//static Stopwatch sw;
	//sw.start();


	//glColor4f2(1, 0, 1, 1.0f);
	//glDisable2(GL_CULL_FACE);
	glDisable2(GL_ALPHA_TEST);

	glEnable2(GL_BLEND);
	glBlendFunc2(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	_minecraft->textures->loadAndBindTexture("gui/gui.png");
	
	//glDisable2(GL_TEXTURE_2D);

	rebuild();
	//drawArrayVTC(_bufferId, 5 * 2 * 3, 24);

	glDisable2(GL_BLEND);
	//glEnable2(GL_TEXTURE_2D);
	//glEnable2(GL_CULL_FACE);

	//sw.stop();
	//sw.printEvery(100, "buttons");
}

const RectangleArea& TouchscreenInput_TestFps::getRectangleArea()
{
	return _boundingRectangle;
}
const RectangleArea& TouchscreenInput_TestFps::getPauseRectangleArea()
{
    return *aPause;
}

void TouchscreenInput_TestFps::rebuild() {
    if (_options->getBooleanValue(OPTIONS_HIDEGUI))
        return;

    Tesselator& t = Tesselator::instance;
    t.begin();

    const int imageU = 0;
    const int imageV = 107;
    const int imageSize = 26;

    // ===== 左侧移动按钮 =====
    bool northDiagonals = !isChangingFlightHeight && (_northJump || _forward);
    // 上
    if (isButtonDown(AREA_DPAD_N)) t.colorABGR(cPressed);
    else                           t.colorABGR(cReleased);
    drawRectangleArea(t, aUp, imageU + (isChangingFlightHeight ? imageSize * 2 : 0), imageV + (isChangingFlightHeight ? imageSize : 0), (float)imageSize);

    // 左
    if (northDiagonals || isChangingFlightHeight) t.colorABGR(cDiscreet);
    else if (isButtonDown(AREA_DPAD_W)) t.colorABGR(cPressed);
    else                                 t.colorABGR(cReleased);
    drawRectangleArea(t, aLeft, imageU + imageSize, imageV, (float)imageSize);

    // 右
    if (northDiagonals || isChangingFlightHeight) t.colorABGR(cDiscreet);
    else if (isButtonDown(AREA_DPAD_E)) t.colorABGR(cPressed);
    else                                 t.colorABGR(cReleased);
    drawRectangleArea(t, aRight, imageU + imageSize * 3, imageV, (float)imageSize);

    // 下
    if (northDiagonals) t.colorABGR(cDiscreet);
    else if (isButtonDown(AREA_DPAD_S)) t.colorABGR(cPressed);
    else                                 t.colorABGR(cReleased);
    drawRectangleArea(t, aDown, imageU + imageSize * 2 + (isChangingFlightHeight ? imageSize : 0), imageV, (float)imageSize);

    // 左上、右上辅助区域
    if (northDiagonals) {
        t.colorABGR(cReleased);
        drawRectangleArea(t, aUpLeft,  imageU, imageV + imageSize, (float)imageSize);
        drawRectangleArea(t, aUpRight, imageU + imageSize, imageV + imageSize, (float)imageSize);
    }

    // ===== 右侧跳跃 & 飞行按钮 =====
    // 飞行上升（仅在飞行模式显示）
    if (_renderFlightImage) {
        t.colorABGR(isButtonDown(AREA_DPAD_FLY_UP) ? cPressed : cReleased);
        drawRectangleArea(t, aFlyUp, imageU + imageSize * 2, imageV + imageSize, (float)imageSize); // 借用上箭头UV
    }

    // 跳跃按钮（始终显示，飞行时可用不同UV）
    if (_renderFlightImage) {
        t.colorABGR(isButtonDown(AREA_DPAD_C) ? cPressed : cReleased);
        drawRectangleArea(t, aJump, imageU + imageSize * 4, imageV + imageSize, (float)imageSize); // 飞行跳跃图标
    } else {
        t.colorABGR(isButtonDown(AREA_DPAD_C) ? cPressed : cReleased);
        drawRectangleArea(t, aJump, imageU + imageSize * 4, imageV, (float)imageSize); // 普通跳跃图标
    }

    // 飞行下降（仅在飞行模式显示）
    if (_renderFlightImage) {
        t.colorABGR(isButtonDown(AREA_DPAD_FLY_DOWN) ? cPressed : cReleased);
        drawRectangleArea(t, aFlyDown, imageU + imageSize * 3, imageV + imageSize, (float)imageSize); // 借用下箭头UV
    }

    // 暂停、聊天按钮不变
    if (!_minecraft->screen) {
        t.colorABGR(0xFFFFFFFF);
        drawRectangleArea(t, aPause, 200, 64, 18.0f);
        drawRectangleArea(t, aChat,  200, 82, 18.0f);
    }

    t.draw();
}
