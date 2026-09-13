#include "Gui.h"
#include "Font.h"
#include "client/Options.h"
#include "platform/input/Keyboard.h"
#include "screens/IngameBlockSelectionScreen.h"
#include "screens/ChatScreen.h"
#include "screens/ConsoleScreen.h"
#include "../Minecraft.h"
#include "../player/LocalPlayer.h"
#include "../renderer/Tesselator.h"
#include "../renderer/TileRenderer.h"
#include "../renderer/LevelRenderer.h"
#include "../renderer/GameRenderer.h"
#include "../renderer/entity/ItemRenderer.h"
#include "../player/input/IInputHolder.h"
#include "../gamemode/GameMode.h"
#include "../gamemode/CreativeMode.h"
#include "../renderer/Textures.h"
#include "../../AppConstants.h"
#include "../../world/entity/player/Inventory.h"
#include "../../world/level/material/Material.h"
#include "../../world/item/Item.h"
#include "../../world/item/ItemInstance.h"
#include "../../platform/input/Mouse.h"
#include "../../world/level/Level.h"
#include "../../world/PosTranslator.h"
#include "../../platform/time.h"
#include "../sound/SoundEngine.h"
#include <cmath>
#include <algorithm>
#include <sstream>

// 新增：用于获取地形偏移量 Important!!!
#include "../../util/WorldCoordinate.h"
#include "../../world/level/levelgen/RandomLevelSource.h"
#include "../../world/level/chunk/ChunkCache.h"

#include "../../world/level/levelgen/TheEndLevelSource.h"

float Gui::InvGuiScale = 1.0f / 3.0f;
float Gui::GuiScale = 1.0f / Gui::InvGuiScale;
const float Gui::DropTicks = 40.0f;

//#include <android/log.h>

Gui::Gui(Minecraft* minecraft)
:	minecraft(minecraft),
	tickCount(0),
	progress(0),
	overlayMessageTime(0),
	animateOverlayMessageColor(false),
	chatScrollOffset(0),
	tbr(1),
	_inventoryNeedsUpdate(true),
	_flashSlotId(-1),
	_flashSlotStartTime(-1),
	_slotFont(NULL),
	_numSlots(4),
	_currentDropTicks(-1),
	_currentDropSlot(-1),
	MAX_MESSAGE_WIDTH(240),
	itemNameOverlayTime(2),
    _debugBtnRect(0.0f, 0.0f, 0.0f, 0.0f),   // 显式初始化
    _debugBtnPressed(false),
	_openInventorySlot(minecraft->useTouchscreen())
{
	glGenBuffers2(1, &_inventoryRc.vboId);
	glGenBuffers2(1, &rcFeedbackInner.vboId);
	glGenBuffers2(1, &rcFeedbackOuter.vboId);
	//Gui::InvGuiScale = 1.0f / (int) (3 * Minecraft::width / 854);
}

Gui::~Gui()
{
	if (_slotFont)
		delete _slotFont;

	glDeleteBuffers(1, &_inventoryRc.vboId);
}

void Gui::render(float a, bool mouseFree, int xMouse, int yMouse) {

	if (!minecraft->level || !minecraft->player)
		return;

	//minecraft->gameRenderer->setupGuiScreen();
	Font* font = minecraft->font;

	const bool isTouchInterface = minecraft->useTouchscreen();
	
	const int screenWidth = (int)(minecraft->width * InvGuiScale);
	const int screenHeight = (int)(minecraft->height * InvGuiScale);
	blitOffset = -90;
	renderProgressIndicator(isTouchInterface, screenWidth, screenHeight, a);

	glColor4f2(1, 1, 1, 1);

	// H: 4
    // T: 7
    // L: 6
    // F: 3
	int ySlot = screenHeight - 16 - 3;

	if (!minecraft->options.getBooleanValue(OPTIONS_HIDEGUI)) {
		if (minecraft->gameMode->canHurtPlayer()) {
			minecraft->textures->loadAndBindTexture("gui/icons.png");
			Tesselator& t = Tesselator::instance;
			t.beginOverride();
			t.colorABGR(0xffffffff);
			renderHearts();
			renderBubbles();
			t.endOverrideAndDraw();
		}
	}

	if(minecraft->player->getSleepTimer() > 0) {
		glDisable(GL_DEPTH_TEST);
		glDisable(GL_ALPHA_TEST);

		renderSleepAnimation(screenWidth, screenHeight);

		glEnable(GL_ALPHA_TEST);
		glEnable(GL_DEPTH_TEST);
	}
	if (!minecraft->options.getBooleanValue(OPTIONS_HIDEGUI)) {
	renderToolBar(a, ySlot, screenWidth);

	glEnable(GL_BLEND);
	bool isChatting = (minecraft->screen && (dynamic_cast<ChatScreen*>(minecraft->screen) || dynamic_cast<ConsoleScreen*>(minecraft->screen)));
	unsigned int max = 10;
	if (isChatting) {
		int lineHeight = 9;
		max = (screenHeight - 48) / lineHeight;
		if (max < 1) max = 1;
		int maxScroll = (int)guiMessages.size() - (int)max;
		if (maxScroll < 0) maxScroll = 0;
		if (chatScrollOffset > maxScroll) chatScrollOffset = maxScroll;
	} else {
		chatScrollOffset = 0;
	}
	renderChatMessages(screenHeight, max, isChatting, font);
#if !defined(RPI)
	renderOnSelectItemNameText(screenWidth, font, ySlot);
#endif
#if defined(RPI)
	renderDebugInfo();
#endif

	if (Keyboard::isKeyDown(Keyboard::KEY_TAB)) {
		renderPlayerList(font, screenWidth, screenHeight);
	}

	if (minecraft->options.getBooleanValue(OPTIONS_RENDER_DEBUG))
		renderDebugInfo();
	}

	// 绘制调试按钮（右下角）
if (!minecraft->screen) {
    float invScale = InvGuiScale;
    float x0 = _debugBtnRect._x0 * invScale;
    float y0 = _debugBtnRect._y0 * invScale;
    float x1 = _debugBtnRect._x1 * invScale;
    float y1 = _debugBtnRect._y1 * invScale;

    // 半透明背景
    fillGradient(x0, y0, x1, y1, 0x800044FF, 0x800022AA);
    // 文字
    const char* label = "Debug";
    Font* font = minecraft->font;
    float tx = (x0 + x1 - font->width(label)) / 2.0f;
    float ty = (y0 + y1 - font->lineHeight) / 2.0f;
    font->drawShadow(label, tx, ty, 0xFFFFFFFF);
}

    glDisable(GL_BLEND);
	glEnable2(GL_ALPHA_TEST);
}

int Gui::getSlotIdAt(int x, int y) {
	int screenWidth = (int)(minecraft->width * InvGuiScale);
	int screenHeight = (int)(minecraft->height * InvGuiScale);
	x = (int)(x * InvGuiScale);
	y = (int)(y * InvGuiScale);

	if (y < (screenHeight - 16 - 3) || y > screenHeight)
		return -1;

	int xBase = 2 + screenWidth / 2 - getNumSlots() * 10;
	int xRel  = (x - xBase);
	if (xRel < 0)
		return -1;

	int slot = xRel / 20;
	return (slot >= 0 && slot < getNumSlots())? slot : -1;
}

bool Gui::isInside(int x, int y) {
	return getSlotIdAt(x, y) != -1;
}

int Gui::getNumSlots() {
	return _numSlots;
}

void Gui::flashSlot(int slotId) {
	_flashSlotId = slotId;
	_flashSlotStartTime = getTimeS();
}

void Gui::getSlotPos(int slot, int& posX, int& posY) {
	int screenWidth = (int)(minecraft->width * InvGuiScale);
	int screenHeight = (int)(minecraft->height * InvGuiScale);
	posX = screenWidth / 2 - getNumSlots() * 10 + slot * 20, 
	posY = screenHeight - 22;
}

RectangleArea Gui::getRectangleArea(int extendSide) {
	const int Spacing = 3;
	const float pCenterX   = 2.0f + (float)(minecraft->width / 2);
	const float pHalfWidth = (1.0f + (getNumSlots() * 10 + Spacing)) * Gui::GuiScale;
	const float pHeight    = (22 + Spacing) * Gui::GuiScale;

	if (extendSide < 0)
		return RectangleArea(0, (float)minecraft->height-pHeight, pCenterX+pHalfWidth+2, (float)minecraft->height);
	if (extendSide > 0)
		return RectangleArea(pCenterX-pHalfWidth, (float)minecraft->height-pHeight, (float)minecraft->width, (float)minecraft->height);
	
	return RectangleArea(pCenterX-pHalfWidth, (float)minecraft->height-pHeight, pCenterX+pHalfWidth+2, (float)minecraft->height);
}

void Gui::handleClick(int button, int x, int y) {
    if (button != MouseAction::ACTION_LEFT) return;

    // Debug 按钮直接用屏幕坐标检测（不用转换成 Gui 逻辑坐标）
    if (x >= _debugBtnRect._x0 && x < _debugBtnRect._x1 &&
        y >= _debugBtnRect._y0 && y < _debugBtnRect._y1) {
        minecraft->soundEngine->playUI("random.click", 1, 1);
        minecraft->screenChooser.setScreen(SCREEN_DEBUG);
        return;
    }

    // 其他 Gui 元素才需要转换为逻辑坐标
    int guiX = (int)(x * InvGuiScale);
    int guiY = (int)(y * InvGuiScale);
    // ... 后面的代码不变 ...

	int slot = getSlotIdAt(x, y);
	if (slot != -1) {
		if (_openInventorySlot && slot == (getNumSlots()-1)) {
			minecraft->screenChooser.setScreen(SCREEN_BLOCKSELECTION);
		} else {
			minecraft->player->inventory->selectSlot(slot);
			itemNameOverlayTime = 0;
		}
	}
}

void Gui::handleKeyPressed(int key)
{
	bool isChatting = (minecraft->screen && (dynamic_cast<ChatScreen*>(minecraft->screen) || dynamic_cast<ConsoleScreen*>(minecraft->screen)));
	if (isChatting) {
		// Allow scrolling the chat history with the mouse/keyboard when chat is open
		if (key == 38) { // VK_UP
			scrollChat(1);
			return;
		} else if (key == 40) { // VK_DOWN
			scrollChat(-1);
			return;
		} else if (key == 33) { // VK_PRIOR (Page Up)
			// Scroll by a page
			int screenHeight = (int)(minecraft->height * InvGuiScale);
			int maxVisible = (screenHeight - 48) / 9;
			scrollChat(maxVisible);
			return;
		} else if (key == 34) { // VK_NEXT (Page Down)
			int screenHeight = (int)(minecraft->height * InvGuiScale);
			int maxVisible = (screenHeight - 48) / 9;
			scrollChat(-maxVisible);
			return;
		}
	}

	if (key == Keyboard::KEY_F1) {
		minecraft->options.toggle(OPTIONS_HIDEGUI);
	}
	
	if (key == 99)
	{
		if (minecraft->player->inventory->selected > 0)
		{
			minecraft->player->inventory->selected--;
		}
	}
	else if (key == 4)
	{
		if (minecraft->player->inventory->selected < (getNumSlots() - 2))
		{
			minecraft->player->inventory->selected++;
		}
	}
	else if (key == 100)
	{
		minecraft->screenChooser.setScreen(SCREEN_BLOCKSELECTION);
	}
	else if (key == minecraft->options.getIntValue(OPTIONS_KEY_DROP)) 
	{
		minecraft->player->inventory->dropSlot(minecraft->player->inventory->selected, false);
	}
}

void Gui::scrollChat(int delta) {
	if (delta == 0)
		return;

	int screenHeight = (int)(minecraft->height * InvGuiScale);
	int maxVisible = (screenHeight - 48) / 9;
	if (maxVisible <= 0)
		return;

	int maxScroll = (int)guiMessages.size() - maxVisible;
	if (maxScroll < 0) maxScroll = 0;
	int desired = chatScrollOffset + delta;
	if (desired < 0) desired = 0;
	if (desired > maxScroll) desired = maxScroll;
	chatScrollOffset = desired;
}

void Gui::tick() {
	if (overlayMessageTime > 0) overlayMessageTime--;
	tickCount++;
	if(itemNameOverlayTime < 2)
		itemNameOverlayTime += 1.0f / SharedConstants::TicksPerSecond;
	for (unsigned int i = 0; i < guiMessages.size(); i++) {
	    guiMessages.at(i).ticks++;
	}

    if (!minecraft->isCreativeMode())
        tickItemDrop();
}

void Gui::addMessage(const std::string& _string) {
	if (!minecraft->font)
		return;

	std::string string = _string;
	while (minecraft->font->width(string) > MAX_MESSAGE_WIDTH) {
		unsigned int i = 1;
		while (i < string.length() && minecraft->font->width(string.substr(0, i + 1)) <= MAX_MESSAGE_WIDTH) {
			i++;
		}
		addMessage(string.substr(0, i));
		string = string.substr(i);
	}
	GuiMessage message;
	message.message = string;
	message.ticks = 0;
	guiMessages.insert(guiMessages.begin(), message);

	// Keep a larger history so users can scroll through the full chat
	const unsigned int MaxHistoryLines = 200;
	while (guiMessages.size() > MaxHistoryLines) {
		guiMessages.pop_back();
	}

	// If the user has scrolled up, keep their window fixed (new messages shift older ones down)
	if (chatScrollOffset > 0) {
		chatScrollOffset++;
	}
}

void Gui::clearMessages() {
	guiMessages.clear();
	chatScrollOffset = 0;
}

void Gui::setNowPlaying(const std::string& string) {
	overlayMessageString = "Now playing: " + string;
	overlayMessageTime = 20 * 3;
	animateOverlayMessageColor = true;
}

void Gui::displayClientMessage(const std::string& messageId) {
	//Language language = Language.getInstance();
	//std::string languageString = language.getElement(messageId);
	addMessage(messageId);
}

void Gui::renderVignette(float br, int w, int h) {
	br = 1 - br;
	if (br < 0) br = 0;
	if (br > 1) br = 1;
	tbr += (br - tbr) * 0.01f;

	glDisable(GL_DEPTH_TEST);
	glDepthMask(false);
	glBlendFunc2(GL_ZERO, GL_ONE_MINUS_SRC_COLOR);
	glColor4f2(tbr, tbr, tbr, 1);
	minecraft->textures->loadAndBindTexture("misc/vignette.png");

	Tesselator& t = Tesselator::instance;
	t.begin();
	t.vertexUV(0, (float)h, -90, 0, 1);
	t.vertexUV((float)w, (float)h, -90, 1, 1);
	t.vertexUV((float)w, 0, -90, 1, 0);
	t.vertexUV(0, 0, -90, 0, 0);
	t.draw();
	glDepthMask(true);
	glEnable(GL_DEPTH_TEST);
	glColor4f2(1, 1, 1, 1);
	glBlendFunc2(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
}

void Gui::renderSlot(int slot, int x, int y, float a) {
	ItemInstance* item = minecraft->player->inventory->getItem(slot);
	if (!item) {
		//LOGW("Warning: item @ Gui::renderSlot is NULL\n");
		return;
	}

	const bool fancy = true;
	ItemRenderer::renderGuiItem(minecraft->font, minecraft->textures, item, (float)x, (float)y, fancy);
}

void Gui::renderSlotText( const ItemInstance* item, float x, float y, bool hasFinite, bool shadow )
{
	//if (!item || item->getItem()->getMaxStackSize() <= 1) {
	if (item->count <= 1) {
		return;
	}

	int c = item->count;

	char buffer[4] = {0,0,0,0};
	if (hasFinite)
		itemCountItoa(buffer, c);
	else
		buffer[0] = (char)157;

	//LOGI("slot: %d - %s\n", slot, buffer);
	if (shadow)
		minecraft->font->drawShadow(buffer, x, y, item->count>0?0xffcccccc:0x60cccccc);
	else
		minecraft->font->draw(buffer, x, y, item->count>0?0xffcccccc:0x60cccccc);
}

void Gui::inventoryUpdated() {
	_inventoryNeedsUpdate = true;
}

void Gui::onGraphicsReset() {
    inventoryUpdated();
}

void Gui::texturesLoaded( Textures* textures ) {
	//_slotFont = new Font(&minecraft->options, "gui/gui_blocks.png", textures, 0, 504, 10, 1, '0');
}

void Gui::onConfigChanged( const Config& c ) {
	Tesselator& t = Tesselator::instance;
	t.begin();

	//
	// Create outer feedback circle
	//
#ifdef ANDROID
	const float mm = 50; //20
#else
	const float mm = 50; //20
#endif
	const float maxRadius = minecraft->pixelCalcUi.millimetersToPixels(mm);
	const float radius = Mth::Min(80.0f/2, maxRadius);
	//LOGI("radius, maxradius: %f, %f\n", radius, maxRadius);
	const float radiusInner = radius * 0.95f;

	const int steps = 24;
	const float fstep = Mth::TWO_PI / steps;
	for (int i = 0; i < steps; ++i) {
		float a = i * fstep;;
		float b = a + fstep;

		float aCos = Mth::cos(a);
		float bCos = Mth::cos(b);
		float aSin = Mth::sin(a);
		float bSin = Mth::sin(b);
		float x00 = radius * aCos;
		float x01 = radiusInner * aCos;
		float x10 = radius * bCos;
		float x11 = radiusInner * bCos;
		float y00 = radius * aSin;
		float y01 = radiusInner * aSin;
		float y10 = radius * bSin;
		float y11 = radiusInner * bSin;

		t.vertexUV(x01, y01, 0, 0, 1);
		t.vertexUV(x11, y11, 0, 1, 1);
		t.vertexUV(x10, y10, 0, 1, 0);
		t.vertexUV(x00, y00, 0, 0, 0);
	}
	rcFeedbackOuter = t.end(true, rcFeedbackOuter.vboId);

	//
	// Create the inner feedback ring
	//
	t.begin(GL_TRIANGLE_FAN);
	t.vertex(0, 0, 0);
	for (int i = 0; i < steps + 1; ++i) {
		float a = -i * fstep;
		float xx = radiusInner * Mth::cos(a);
		float yy = radiusInner * Mth::sin(a);
		t.vertex(xx, yy, 0);
		//LOGI("x,y: %f, %f\n", xx, yy);
	}
	rcFeedbackInner = t.end(true, rcFeedbackInner.vboId);

	if (c.minecraft->useTouchscreen()) {
		// I'll bump this up to 6.
		int num = 6; // without "..." dots
		if (!c.minecraft->options.getBooleanValue(OPTIONS_IS_JOY_TOUCH_AREA) && c.width > 480) {
			while (num < Inventory::MAX_SELECTION_SIZE - 1) {
				int x0, x1, y;
				getSlotPos(0, x0, y);
				getSlotPos(num, x1, y);
				int width = x1 - x0;
				float leftoverPixels = c.width - c.guiScale*width;
				if (c.pixelCalc.pixelsToMillimeters(leftoverPixels) < 80)
					break;
				num++;
			}
		}
		_numSlots = num;
#if defined(__APPLE__)
		_numSlots = Mth::Min(7, _numSlots);
#endif
	} else {
		_numSlots = Inventory::MAX_SELECTION_SIZE; // Xperia Play
	}
	MAX_MESSAGE_WIDTH = c.guiWidth;
// 原来
//_debugBtnRect = RectangleArea(minecraft->width - 115, 70, minecraft->width - 25, 100);
// 改为
_debugBtnRect = RectangleArea(minecraft->width - 115, 90, minecraft->width - 25, 120);
}

float Gui::floorAlignToScreenPixel(float v) {
	return (int)(v * Gui::GuiScale) * Gui::InvGuiScale;
}

int Gui::itemCountItoa( char* buffer, int count )
{
	if (count < 0)
		return 0;

	if (count < 10) { // 1 digit
		buffer[0] = '0' + count;
		buffer[1] = 0;
		return 1;
	} else if (count < 100) { // 2 digits
		int digit = count/10;
		buffer[0] = '0' + digit;
		buffer[1] = '0' + count - digit*10;
		buffer[2] = 0;
	} else { // 3 digits -> "99+"
		buffer[0] = buffer[1] = '9';
		buffer[2] = '+';
		buffer[3] = 0;
		return 3;
	}
	return 2;
}

void Gui::tickItemDrop()
{
	// Handle item drop
	static bool isCurrentlyActive = false;
	isCurrentlyActive = false;

	int slots = getNumSlots() - _openInventorySlot;

	if (Mouse::isButtonDown(MouseAction::ACTION_LEFT)) {
		int slot = getSlotIdAt(Mouse::getX(), Mouse::getY());
		if (slot >= 0 && slot < slots) {
			if (slot != _currentDropSlot) {
				_currentDropTicks = 0;
				_currentDropSlot = slot;
			}
			isCurrentlyActive = true;
			if ((_currentDropTicks += 1.0f) >= DropTicks) {
				minecraft->player->inventory->dropSlot(slot, false);
				minecraft->level->playSound(minecraft->player, "random.pop", 0.3f, 1);
				isCurrentlyActive = false;
			}
		}
	}
	if (!isCurrentlyActive) {
		_currentDropSlot = -1;
		_currentDropTicks = -1;
	}
}

void Gui::postError( int errCode )
{
	static std::set<int> posted;
	if (posted.find(errCode) != posted.end())
		return;

	posted.insert(errCode);

	std::stringstream s;
	s << "Something went wrong! (errcode " << errCode << ")\n";
	addMessage(s.str());
}

void Gui::setScissorRect( const IntRectangle& bbox )
{
	GLuint x = (GLuint)(GuiScale * bbox.x);
	GLuint y = minecraft->height - (GLuint)(GuiScale * (bbox.y + bbox.h));
	GLuint w = (GLuint)(GuiScale * bbox.w);
	GLuint h = (GLuint)(GuiScale * bbox.h);
	glScissor(x, y, w, h);
}

float Gui::cubeSmoothStep(float percentage, float min, float max) {
	//percentage = percentage * percentage;
	//return (min * percentage) + (max * (1 - percentage));
	return (percentage) * (percentage) * (3 - 2 * (percentage));
}

void Gui::renderProgressIndicator( const bool isTouchInterface, const int screenWidth, const int screenHeight, float a ) {
	ItemInstance* currentItem = minecraft->player->inventory->getSelected();
	bool bowEquipped = currentItem != NULL ? currentItem->getItem() == Item::bow : false;
	bool itemInUse = currentItem != NULL ? currentItem->getItem() == minecraft->player->getUseItem()->getItem() : false;
	if ((!isTouchInterface || minecraft->options.getBooleanValue(OPTIONS_IS_JOY_TOUCH_AREA) 
	|| (bowEquipped && itemInUse)) && !minecraft->options.getBooleanValue(OPTIONS_HIDEGUI)) {
		minecraft->textures->loadAndBindTexture("gui/icons.png");
		glEnable(GL_BLEND);
		glBlendFunc2(GL_ONE_MINUS_DST_COLOR, GL_ONE_MINUS_SRC_COLOR);
		blit(screenWidth/2 - 8, screenHeight/2 - 8, 0, 0, 16, 16);
		glDisable(GL_BLEND);
	} else if(!bowEquipped) {
		const float tprogress = minecraft->gameMode->destroyProgress;
		const float alpha = Mth::clamp(minecraft->inputHolder->alpha, 0.0f, 1.0f);
		//LOGI("alpha: %f\n", alpha);

		if (tprogress <= 0 && minecraft->inputHolder->alpha >= 0) {
			glDisable2(GL_TEXTURE_2D);
			glEnable2(GL_BLEND);
			glBlendFunc2(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			if (minecraft->hitResult.isHit())
				glColor4f2(1, 1, 1, 0.8f * alpha);
			else
				glColor4f2(1, 1, 1, Mth::Min(0.4f, alpha*0.4f));

			//LOGI("alpha2: %f\n", alpha);
			const float x = InvGuiScale * minecraft->inputHolder->mousex;
			const float y = InvGuiScale * minecraft->inputHolder->mousey;
			glTranslatef2(x, y, 0);
			drawArrayVT(rcFeedbackOuter.vboId, rcFeedbackOuter.vertexCount, 24);
			glTranslatef2(-x, -y, 0);

			glEnable2(GL_TEXTURE_2D);
			glDisable(GL_BLEND);
		} else if (tprogress > 0) {
			const float oProgress = minecraft->gameMode->oDestroyProgress;
			const float progress = 0.5f * (oProgress + (tprogress - oProgress) * a);

			//static Stopwatch w;
			//w.start();

			glDisable2(GL_TEXTURE_2D);
			glColor4f2(1, 1, 1, 0.8f * alpha);
			glEnable(GL_BLEND);
			glBlendFunc2(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

			const float x = InvGuiScale * minecraft->inputHolder->mousex;
			const float y = InvGuiScale * minecraft->inputHolder->mousey;
			glPushMatrix2();
			glTranslatef2(x, y, 0);
			drawArrayVT(rcFeedbackOuter.vboId, rcFeedbackOuter.vertexCount, 24);
			glScalef2(0.5f + progress, 0.5f + progress, 1);
			//glDisable2(GL_CULL_FACE);
			glColor4f2(1, 1, 1, 1);
			glBlendFunc2(GL_ONE_MINUS_DST_COLOR, GL_ONE_MINUS_SRC_COLOR);
			drawArrayVT(rcFeedbackInner.vboId, rcFeedbackInner.vertexCount, 24, GL_TRIANGLE_FAN);
			glPopMatrix2();

			glDisable(GL_BLEND);
			glEnable2(GL_TEXTURE_2D);

			//w.stop();
			//w.printEvery(100, "feedback-r ");
		}
	}
}

void Gui::renderHearts() {
	bool blink = (minecraft->player->invulnerableTime / 3) % 2 == 1;
	if (minecraft->player->invulnerableTime < 10) blink = false;
	int h = minecraft->player->health;
	int oh = minecraft->player->lastHealth;
	random.setSeed(tickCount * 312871);

	int screenWidth = (int)(minecraft->width * InvGuiScale);
	int screenHeight = (int)(minecraft->height * InvGuiScale);

	int xx = (minecraft->options.getBooleanValue(OPTIONS_BAR_ON_TOP)) ? screenWidth / 2 - getNumSlots() * 10 - 1 : 2;

	int armor = minecraft->player->getArmorValue();
	for (int i = 0; i < Player::MAX_HEALTH / 2; i++) {
		int yo = (minecraft->options.getBooleanValue(OPTIONS_BAR_ON_TOP)) ? screenHeight - 32 : 2;
		int ip2 = i + i + 1;

		if (armor > 0) {
		    int xo = xx + 80 + i * 8 + 4;
		    if (ip2 < armor) blit(xo, yo, 16 + 2 * 9, 9 * 1, 9, 9);
		    else if (ip2 == armor) blit(xo, yo, 16 + 4 * 9, 9 * 1, 9, 9);
		    else if (ip2 > armor) blit(xo, yo, 16 + 0 * 9, 9 * 1, 9, 9);
		}

		int bg = 0;
		if (blink) bg = 1;
		int xo = xx + i * 8;
		if (h <= 4) {
			yo = yo + random.nextInt(2) - 1;
		}
		blit(xo, yo, 16 + bg * 9, 9 * 0, 9, 9);
		if (blink) {
			if (ip2 < oh) blit(xo, yo, 16 + 6 * 9, 9 * 0, 9, 9);
			else if (ip2 == oh) blit(xo, yo, 16 + 7 * 9, 9 * 0, 9, 9);
		}
		if (ip2 < h) blit(xo, yo, 16 + 4 * 9, 9 * 0, 9, 9);
		else if (ip2 == h) blit(xo, yo, 16 + 5 * 9, 9 * 0, 9, 9);
	}
}

void Gui::renderBubbles() {
	if (minecraft->player->isUnderLiquid(Material::water)) {
		int screenWidth = (int)(minecraft->width * InvGuiScale);
		int screenHeight = (int)(minecraft->height * InvGuiScale);
		
		int xx = (minecraft->options.getBooleanValue(OPTIONS_BAR_ON_TOP)) ? screenWidth / 2 - getNumSlots() * 10 - 1 : 2;
		int yo = (minecraft->options.getBooleanValue(OPTIONS_BAR_ON_TOP)) ? screenHeight - 42 : 12;
		int count = (int) std::ceil((minecraft->player->airSupply - 2) * 10.0f / Player::TOTAL_AIR_SUPPLY);
		int extra = (int) std::ceil((minecraft->player->airSupply) * 10.0f / Player::TOTAL_AIR_SUPPLY) - count;
		for (int i = 0; i < count + extra; i++) {
			int xo =  i * 8 + xx;
			if (i < count) blit(xo, yo, 16, 9 * 2, 9, 9);
			else blit(xo, yo, 16 + 9, 9 * 2, 9, 9);
		}
	}
}

static OffsetPosTranslator posTranslator;
void Gui::onLevelGenerated() {
    if (Level* level = minecraft->level) {
        Pos p = level->getSharedSpawnPos();
        posTranslator = OffsetPosTranslator((double)-p.x, (double)-p.y, (double)-p.z);
    }
}


void Gui::renderDebugInfo() {
    // ===================== FPS =====================
    static float fps = 0.0f;
    static float fpsLastTime = 0.0f;
    static int fpsFrames = 0;
    float now = getTimeS();
    fpsFrames++;
    if (now - fpsLastTime >= 1.0f) {
        fps = fpsFrames / (now - fpsLastTime);
        fpsFrames = 0;
        fpsLastTime = now;
    }

    LocalPlayer* p = minecraft->player;
    Level* lvl = minecraft->level;

    // ===================== 偏移/缩放 =====================
    double terrainOffsetX = 0.0, terrainOffsetY = 0.0, terrainOffsetZ = 0.0;
    double worldScaleX = 1.0, worldScaleY = 1.0, worldScaleZ = 1.0;
    std::string sOffX, sOffY, sOffZ, sSclX, sSclY, sSclZ;

    // ===================== BigWorldCoordinate =====================
    double pxo = 0.0, pyo = 0.0, pzo = 0.0;
    bool showBig = false;
    std::string pxoBigStr, pyoBigStr, pzoBigStr;

    double px = p->x;
    double py = p->y - p->heightOffset + 64;
    double pz = p->z;
    posTranslator.to(px, py, pz);

    BigWorldCoordinate bigPX = p->getBigAbsX();
    BigWorldCoordinate bigPY = p->getBigAbsY();
    BigWorldCoordinate bigPZ = p->getBigAbsZ();
    std::string bigPXStr = worldCoordBigToString(bigPX);
    std::string bigPYStr = worldCoordBigToString(bigPY);
    std::string bigPZStr = worldCoordBigToString(bigPZ);

    // ===================== 生成器检测 =====================
    RandomLevelSource* rls = nullptr;
    TheEndLevelSource* els = nullptr;
    bool isEnd = false;

    if (lvl && lvl->getChunkSource()) {
        ChunkCache* cache = dynamic_cast<ChunkCache*>(lvl->getChunkSource());
        if (cache) {
            rls = dynamic_cast<RandomLevelSource*>(cache->getSource());
            if (rls) {
                terrainOffsetX = rls->getWorldOffsetX();
                terrainOffsetY = rls->getWorldOffsetY();
                terrainOffsetZ = rls->getWorldOffsetZ();
                worldScaleX   = rls->getWorldScaleX();
                worldScaleY   = rls->getWorldScaleY();
                worldScaleZ   = rls->getWorldScaleZ();
            }
        }
        if (!rls) {
            els = dynamic_cast<TheEndLevelSource*>(lvl->getChunkSource());
            if (els) {
                isEnd = true;
                terrainOffsetX = els->getWorldOffsetX();
                terrainOffsetY = els->getWorldOffsetY();
                terrainOffsetZ = els->getWorldOffsetZ();
                worldScaleX   = els->getWorldScaleX();
                worldScaleY   = els->getWorldScaleY();
                worldScaleZ   = els->getWorldScaleZ();
            }
        }
    }

    // 偏移/缩放字符串（从 Options 读，无浮点舍入）
    if (rls || els) {
        sOffX = minecraft->options.getStringValue(OPTIONS_WORLD_OFFSET_X);
        sOffY = minecraft->options.getStringValue(OPTIONS_WORLD_OFFSET_Y);
        sOffZ = minecraft->options.getStringValue(OPTIONS_WORLD_OFFSET_Z);
        sSclX = minecraft->options.getStringValue(OPTIONS_WORLD_SCALE_X);
        sSclY = minecraft->options.getStringValue(OPTIONS_WORLD_SCALE_Y);
        sSclZ = minecraft->options.getStringValue(OPTIONS_WORLD_SCALE_Z);
    }

    // ===================== pxo/pyo/pzo =====================
    double estPxo = px * worldScaleX + terrainOffsetX * worldScaleX;
    double estPyo = py * worldScaleY + terrainOffsetY * worldScaleY;
    double estPzo = pz * worldScaleZ + terrainOffsetZ * worldScaleZ;
    showBig = needsBigWorldCoord(estPxo) || needsBigWorldCoord(estPyo) || needsBigWorldCoord(estPzo);

    if (rls || els) {
        if (showBig) {
            BigWorldCoordinate bxo = computeWorldCoordBig(px, worldScaleX, terrainOffsetX);
            BigWorldCoordinate byo = computeWorldCoordBig(py, worldScaleY, terrainOffsetY);
            BigWorldCoordinate bzo = computeWorldCoordBig(pz, worldScaleZ, terrainOffsetZ);
            pxo = worldCoordBigToDouble(bxo);
            pyo = worldCoordBigToDouble(byo);
            pzo = worldCoordBigToDouble(bzo);
            pxoBigStr = worldCoordBigToString(bxo);
            pyoBigStr = worldCoordBigToString(byo);
            pzoBigStr = worldCoordBigToString(bzo);
        } else {
            pxo = estPxo;
            pyo = estPyo;
            pzo = estPzo;
        }
    }

    // ===================== 基础信息 =====================
    int seaLevel = 63;
    if (minecraft->options.getOpt(OPTIONS_SEA_LEVEL)) {
        std::string slStr = minecraft->options.getStringValue(OPTIONS_SEA_LEVEL);
        if (!slStr.empty()) seaLevel = atoi(slStr.c_str());
    }

    int64_t bx = Mth::floor64(px), by = Mth::floor64(py), bz = Mth::floor64(pz);
    int64_t cx = bx >> 4, cz = bz >> 4;

    float yMod = fmodf(p->yRot, 360.0f);
    if (yMod < 0) yMod += 360.0f;
    const char* facing; const char* axis;
    if (yMod < 45 || yMod >= 315)      { facing = "South"; axis = "+Z"; }
    else if (yMod < 135)                { facing = "West";  axis = "-X"; }
    else if (yMod < 225)                { facing = "North"; axis = "-Z"; }
    else                                { facing = "East";  axis = "+X"; }

    const char* biomeName = "unknown";
    if (lvl) {
        Biome* biome = lvl->getBiome(bx, bz);
        if (biome) biomeName = biome->name.c_str();
    }

    long worldTime = lvl ? lvl->getTime() : 0;
    long dayTime = worldTime % Level::TICKS_PER_DAY;
    long day = worldTime / Level::TICKS_PER_DAY;
    long seed = lvl ? lvl->getSeed() : 0;

    bool fringeEnabled = false;
    if (minecraft->options.getOpt(OPTIONS_SIXTYFOUR_FARLANDS)) {
        fringeEnabled = minecraft->options.getBooleanValue(OPTIONS_SIXTYFOUR_FARLANDS);
    }

    float debugScale = 1.0f;
    std::string scaleStr = minecraft->options.getStringValue(OPTIONS_DEBUG_SCREEN_SIZE);
    if (!scaleStr.empty()) debugScale = (float)atof(scaleStr.c_str());

    // ===================== 噪声计算 =====================
    double noiseVals[8] = {0.0};
    float  noiseValsF[8] = {0.0f};
    const char* noiseLabels[8] = { nullptr };
    bool showNoise = false;

    if (rls) {
        showNoise = true;
        noiseLabels[0] = "Low-Noise"; noiseLabels[1] = "High-Noise";
        noiseLabels[2] = "Selector-Noise"; noiseLabels[3] = "Sand-Noise";
        noiseLabels[4] = "Gravel-Noise"; noiseLabels[5] = "Scale-Noise";
        noiseLabels[6] = "Depth-Noise"; noiseLabels[7] = "Tree-Density-Noise";

        const double s = 684.412;
        const double sXZ = s / 80.0;
        const double sY  = s / 160.0;

        // Double 精度噪声
        double nX = pxo * sXZ, nY = pyo * sY, nZ = pzo * sXZ;
        noiseVals[0] = rls->getLPerlinNoise1(nX, nY, nZ);
noiseVals[1] = rls->getLPerlinNoise2(nX, nY, nZ);
noiseVals[2] = rls->getPerlinNoise1(nX, nY, nZ);
noiseVals[3] = rls->getPerlinNoise2(pxo / 32.0, pzo / 32.0);
noiseVals[4] = rls->getPerlinNoise3(pxo / 64.0, pzo / 64.0);
noiseVals[5] = rls->getScaleNoise(pxo / 80.0, pzo / 80.0);
noiseVals[6] = rls->getDepthNoise(pxo / 200.0, pzo / 200.0);
noiseVals[7] = rls->getForestNoise(pxo * 0.5, pzo * 0.5);

        // Float 精度噪声
        double fX = px * worldScaleX + terrainOffsetX * worldScaleX;
        double fY = py * worldScaleY + terrainOffsetY * worldScaleY;
        double fZ = pz * worldScaleZ + terrainOffsetZ * worldScaleZ;
        noiseValsF[0] = (float)rls->getLPerlinNoise1(fX * sXZ, fY * sY, fZ * sXZ);
        noiseValsF[1] = (float)rls->getLPerlinNoise2(fX * sXZ, fY * sY, fZ * sXZ);
        noiseValsF[2] = (float)rls->getPerlinNoise1(fX * sXZ, fY * sY, fZ * sXZ);
        noiseValsF[3] = (float)rls->getPerlinNoise2(fX / 32.0, fZ / 32.0);
        noiseValsF[4] = (float)rls->getPerlinNoise3(fX / 64.0, fZ / 64.0);
        noiseValsF[5] = (float)rls->getScaleNoise(fX / 80.0, fZ / 80.0);
        noiseValsF[6] = (float)rls->getDepthNoise(fX / 200.0, fZ / 200.0);
        noiseValsF[7] = (float)rls->getForestNoise(fX * 0.5, fZ * 0.5);
    } else if (els) {
        showNoise = true;
        noiseLabels[0] = "Low-Noise";  noiseLabels[1] = "High-Noise";
        noiseLabels[2] = "Selector-Noise"; noiseLabels[3] = "OuterIslandDetection-Noise";
        noiseLabels[4] = "FinalDensity-Value";      noiseLabels[5] = "IslandHeight-Value";
        noiseLabels[6] = nullptr;        noiseLabels[7] = nullptr;

        // Double 精度噪声
        double nX = pxo * 1368.824, nY = pyo * 684.412, nZ = pzo * 1368.824;
        noiseVals[0] = els->getPNoise1().getValue(nX, nY, nZ);
        noiseVals[1] = els->getPNoise2().getValue(nX, nY, nZ);
        noiseVals[2] = els->getPNoise3().getValue(pxo * 17.1103, pyo * 4.277575, pzo * 17.1103);
        noiseVals[3] = els->getSNoise1().getValue(pxo / 16.0, pzo / 16.0);
        noiseVals[4] = els->sampleDensityAt(px, py, pz);
        noiseVals[5] = els->debugIslandHeightValue(cx, cz);
        noiseVals[6] = 0.0; noiseVals[7] = 0.0;

        // Float 精度噪声
        double fX = px * worldScaleX + terrainOffsetX * worldScaleX;
        double fY = py * worldScaleY + terrainOffsetY * worldScaleY;
        double fZ = pz * worldScaleZ + terrainOffsetZ * worldScaleZ;
        noiseValsF[0] = (float)els->getPNoise1().getValue(fX * 1368.824, fY * 684.412, fZ * 1368.824);
        noiseValsF[1] = (float)els->getPNoise2().getValue(fX * 1368.824, fY * 684.412, fZ * 1368.824);
        noiseValsF[2] = (float)els->getPNoise3().getValue(fX * 17.1103, fY * 4.277575, fZ * 17.1103);
        noiseValsF[3] = (float)els->getSNoise1().getValue(fX / 16.0, fZ / 16.0);
        noiseValsF[4] = (float)els->sampleDensityAt(px, py, pz);
        noiseValsF[5] = (float)els->debugIslandHeightValue(cx, cz);
        noiseValsF[6] = 0.0f; noiseValsF[7] = 0.0f;
    }

    // ===================== 构建噪声字符串 =====================
    static char firstPartD[2048], secondPartD[2048];
    static char firstPartF[2048], secondPartF[2048];
    firstPartD[0] = '\0'; secondPartD[0] = '\0';
    firstPartF[0] = '\0'; secondPartF[0] = '\0';

    if (showNoise) {
        for (int i = 0; i < 4; i++) {
            if (!noiseLabels[i]) continue;
            char tmp[64];
            bool bad = (std::isnan(noiseVals[i]) || std::isinf(noiseVals[i]));
            snprintf(tmp, sizeof(tmp), "%s%s: %.4f  ", bad ? "*" : "", noiseLabels[i], noiseVals[i]);
            strcat(firstPartD, tmp);
        }
        for (int i = 4; i < 8; i++) {
            if (!noiseLabels[i]) continue;
            char tmp[64];
            bool bad = (std::isnan(noiseVals[i]) || std::isinf(noiseVals[i]));
            snprintf(tmp, sizeof(tmp), "%s%s: %.4f  ", bad ? "*" : "", noiseLabels[i], noiseVals[i]);
            strcat(secondPartD, tmp);
        }
        for (int i = 0; i < 4; i++) {
            if (!noiseLabels[i]) continue;
            char tmp[64];
            bool badF = (std::isnan(noiseValsF[i]) || std::isinf(noiseValsF[i]));
            snprintf(tmp, sizeof(tmp), "%s%s: %.4f  ", badF ? "*" : "", noiseLabels[i], noiseValsF[i]);
            strcat(firstPartF, tmp);
        }
        for (int i = 4; i < 8; i++) {
            if (!noiseLabels[i]) continue;
            char tmp[64];
            bool badF = (std::isnan(noiseValsF[i]) || std::isinf(noiseValsF[i]));
            snprintf(tmp, sizeof(tmp), "%s%s: %.4f  ", badF ? "*" : "", noiseLabels[i], noiseValsF[i]);
            strcat(secondPartF, tmp);
        }
    }

    // ===================== 构建 27 行显示文本 =====================
    static char ln[27][2048];
    sprintf(ln[0], "Minecraft NoiseFarlands Reference [MFSCelebrate/BiliBiliMobile]");
    sprintf(ln[1], "%.2f fps", fps);
    ln[2][0] = '\0';
    sprintf(ln[3], "--- Local Server Position ---");
    if (showBig) {
    snprintf(ln[4], sizeof(ln[4]),
        "XYZ(BigWorldCoordinate): %s / %s / %s",
        bigPXStr.c_str(), bigPYStr.c_str(), bigPZStr.c_str());
    } else {
    snprintf(ln[4], sizeof(ln[4]),
        "XYZ: %.3f / %.5f / %.3f",
        px, py, pz);
    }
    
    if (showBig) {
        snprintf(ln[5], sizeof(ln[5]), "X(World/BigWorldCoordinate): %s", pxoBigStr.c_str());
        snprintf(ln[6], sizeof(ln[6]), "Y(World/BigWorldCoordinate): %s", pyoBigStr.c_str());
        snprintf(ln[7], sizeof(ln[7]), "Z(World/BigWorldCoordinate): %s", pzoBigStr.c_str());
    } else {
        sprintf(ln[5], "X(World): %.15f", pxo);
        sprintf(ln[6], "Y(World): %.15f", pyo);
        sprintf(ln[7], "Z(World): %.15f", pzo);
    }

    if (!sOffX.empty()) {
        snprintf(ln[8], sizeof(ln[8]),
            "Offset: %s / %s / %s (Scale: %s / %s / %s)",
            sOffX.c_str(), sOffY.c_str(), sOffZ.c_str(), sSclX.c_str(), sSclY.c_str(), sSclZ.c_str());
    } else {
        snprintf(ln[8], sizeof(ln[8]),
            "Offsets: %.2f / %.2f / %.2f (Scales: %.3f / %.3f / %.3f)",
            terrainOffsetX, terrainOffsetY, terrainOffsetZ,
            worldScaleX, worldScaleY, worldScaleZ);
    }

    ln[9][0] = '\0';
    sprintf(ln[10], "--- World Generator ---");

    // 🛡️ 64Bit Farlands：两种生成器都显示
    sprintf(ln[11], "64Bit Farlands: %s", fringeEnabled ? "True" : "False");

    // 🛡️ ln[12]：普通世界 Sea Level，末地 Generator 类型
    if (isEnd) {
        sprintf(ln[12], "LocalGenerator: TheEndLevelSource (.cpp)");
    } else {
        sprintf(ln[12], "Sea Level: %d", seaLevel);
    }

    snprintf(ln[13], sizeof(ln[13]), "Terrain Noise: %s", firstPartD);
    snprintf(ln[14], sizeof(ln[14]), "Surface Noise: %s", secondPartD);
    ln[15][0] = '\0';
    snprintf(ln[16], sizeof(ln[16]), "Terrain Noise(Float): %s", firstPartF);
    snprintf(ln[17], sizeof(ln[17]), "Surface Noise(Float): %s", secondPartF);
    ln[18][0] = '\0';
    ln[19][0] = '\0';

    sprintf(ln[20], "--- Other Information ---");
    sprintf(ln[21], "Block: %lld %lld %lld   Chunk: %lld %lld",
        (long long)bx, (long long)by, (long long)bz,
        (long long)cx, (long long)cz);
    sprintf(ln[22], "Facing: %s (%s)  (%.1f / %.1f)  Biome: %s",
        facing, axis, p->yRot, p->xRot, biomeName);
    sprintf(ln[23], "Day %ld  Time: %ld  Seed: %ld", day, dayTime, seed);
    ln[24][0] = '\0';

    bool stripeRepairOn = minecraft->options.getBooleanValue(OPTIONS_STRIPE_REPAIR);
    if (!stripeRepairOn) {
        snprintf(ln[25], sizeof(ln[25]),
            "Warning: Entity Rendering Has Become Invalid!!! (Enable Stripe Repair)");
    } else {
        ln[25][0] = '\0';
    }
    ln[26][0] = '\0';

    // ===================== 渲染 =====================
    const int N = 27;
    const float LH  = (float)Font::DefaultLineHeight;
    const float MGN = 2.0f;
    const float PAD = 2.0f;
    Font* font = minecraft->font;

    glPushMatrix();
    glScalef(debugScale, debugScale, 1.0f);

    // 背景框
    for (int i = 0; i < N; i++) {
        if (ln[i][0] == '\0') continue;
        float w  = (float)font->width(ln[i]);
        float x0 = MGN - PAD;
        float y0 = MGN + i * LH - 1.0f;
        float x1 = MGN + w + PAD;
        float y1 = MGN + (i + 1) * LH - 1.0f;
        fill(x0, y0, x1, y1, 0x90000000);
    }

    // 文本 + 颜色
    Tesselator& t = Tesselator::instance;
    t.beginOverride();
    for (int i = 0; i < N; i++) {
        if (ln[i][0] == '\0') continue;
        float y = MGN + i * LH;
        int col = 0xffffffff;

if (i == 0)
    col = 0xffFFFF55;
else if (i == 11)
    col = fringeEnabled ? 0xff00ff00 : 0xffff0000;
else if (i == 16 || i == 17)
    col = 0xFFFF8080;
else if (i == 25)
    col = 0xffff0000;

        font->draw(ln[i], MGN, y, col);
    }
    t.endOverrideAndDraw();

    // ========== 精度丢失显示（替换原 ln[18] 位置） ==========
    {
        double maxCoord = std::max(std::abs(pxo), std::abs(pzo));
        double doubleStep; float floatStep;
        Mth::computePrecisionLoss(maxCoord, doubleStep, floatStep);
        float yPos = MGN + 18 * LH;

        // BigCoord 步长：定点数最小精度 = 1 / FIXED_SCALE = 1e-12
        double bigStep = 1.0e-12;
		
        char bufDouble[64], bufFloat[64], bufBig[64];
        snprintf(bufDouble, sizeof(bufDouble), "%.16f", doubleStep);
        snprintf(bufFloat, sizeof(bufFloat), "%.9f", floatStep);
        snprintf(bufBig, sizeof(bufBig), "%.12f", bigStep);

        auto trimZeros = [](char* str) {
            if (!str || *str == '\0') return;
            char* dot = strchr(str, '.');
            if (!dot) return;
            char* p = str + strlen(str) - 1;
            while (p > dot && *p == '0') *p-- = '\0';
            if (*p == '.') *p = '\0';
        };
        trimZeros(bufDouble);
        trimZeros(bufFloat);
        trimZeros(bufBig);

        const char* labelText = "Current Precision: ";
        const char* middleText = " (Float: ";
        const char* bigText   = ") (BigWorldCoordinate: ";
        const char* endText   = ")";

        float totalWidth = font->width(labelText)
                         + font->width(bufDouble)
                         + font->width(middleText)
                         + font->width(bufFloat)
                         + font->width(bigText)
                         + font->width(bufBig)
                         + font->width(endText);

        fill(MGN - PAD, yPos - 1.0f, MGN + totalWidth + PAD, yPos + LH - 1.0f, 0x90000000);

        font->draw(labelText, MGN, yPos, 0xFFFFFFFF);

        float xCursor = MGN + font->width(labelText);
        int colD = Mth::getPrecisionColor(doubleStep);
        font->draw(bufDouble, xCursor, yPos, colD);
        xCursor += font->width(bufDouble);

        font->draw(middleText, xCursor, yPos, 0xFFFFFFFF);
        xCursor += font->width(middleText);

        int colF = Mth::getPrecisionColor(floatStep);
        font->draw(bufFloat, xCursor, yPos, colF);
        xCursor += font->width(bufFloat);

        font->draw(bigText, xCursor, yPos, 0xFFFFFFFF);
        xCursor += font->width(bigText);

        int colBig = 0xFF88FF88; // BigCoord 步长恒定 1e-12，永远绿色
        font->draw(bufBig, xCursor, yPos, colBig);
        xCursor += font->width(bufBig);

        font->draw(endText, xCursor, yPos, 0xFFFFFFFF);
	}

    glPopMatrix();
}

void Gui::renderPlayerList(Font* font, int screenWidth, int screenHeight) {
	// only show when in game, no other screen
	// if (!minecraft->level) return;

	// only show the overlay while connected to a multiplayer server
	Level* level = minecraft->level;
	if (!level) return;
	if (!level->isClientSide) return;

	std::vector<std::string> playerNames;
	playerNames.reserve(level->players.size());

	for (Player* player : level->players) {
		if (!player) continue;
		playerNames.push_back(player->name);
	}

	// is this check needed? if there are no players, the box won't render at all since height will be 0, 
	// but maybe we want to skip rendering entirely in that case
	// if (playerNames.empty())
	// 	return;

	std::sort(playerNames.begin(), playerNames.end());

	float maxNameWidth = 0.0f;
	// find the longest name so we can size the box accordingly
	for (const std::string& name : playerNames) {
		float nameWidth = font->width(name);
		if (nameWidth > maxNameWidth)
			maxNameWidth = nameWidth;
	}

	// player count title
	std::ostringstream titleStream;
	titleStream << "Players (" << playerNames.size() << ")";
	std::string titleText = titleStream.str();
	float titleWidth = font->width(titleText);

	if (titleWidth > maxNameWidth)
		maxNameWidth = titleWidth;

	const float padding = 4.0f;
	const float lineHeight = (float)Font::DefaultLineHeight;

	const float boxWidth = maxNameWidth + padding * 2;
	const float boxHeight = (playerNames.size() + 1) * lineHeight + padding * 2;

	const float boxLeft = (screenWidth - boxWidth) / 2.0f;
	const float boxTop = 10.0f;
	const float boxRight = boxLeft + boxWidth;
	const float boxBottom = boxTop + boxHeight;

	fill(boxLeft, boxTop, boxRight, boxBottom, 0x90000000);

	float titleX = (screenWidth - titleWidth) / 2.0f;
	float titleY = boxTop + padding;

	// scale the text down slightly
	// i think the gl scaling is the best for this
	// oh my god this looks really bad OH GOD
	//const float textScale = 0.8f;
	//const float invTextScale = 1.0f / textScale;
	//glPushMatrix2();
	//glScalef2(textScale, textScale, 1);

	// draw title
	//font->draw(titleText, titleX * invTextScale, titleY * invTextScale, 0xFFFFFFFF);
	font->draw(titleText, titleX, titleY, 0xFFFFFFFF);

	// draw player names
	// we should add ping icons here eventually, but for now just show names
	float currentY = boxTop + padding + lineHeight;
	for (const std::string& name : playerNames) {
		font->draw(name, (boxLeft + padding), currentY, 0xFFDDDDDD);
		currentY += lineHeight;
	}
	//glPopMatrix2();
}

void Gui::renderSleepAnimation( const int screenWidth, const int screenHeight ) {
	int timer = minecraft->player->getSleepTimer();
	float amount = (float) timer / (float) Player::SLEEP_DURATION;
	if (amount > 1) {
		// waking up
		amount = 1.0f - ((float) (timer - Player::SLEEP_DURATION) / (float) Player::WAKE_UP_DURATION);
	}

	int color = (int) (220.0f * amount) << 24 | (0x101020);
	fill(0, 0, screenWidth, screenHeight, color);
}

void Gui::renderOnSelectItemNameText( const int screenWidth, Font* font, int ySlot ) {
	if(itemNameOverlayTime < 1.0f) {
		ItemInstance* item = minecraft->player->inventory->getSelected();
		if(item != NULL) {
			float x = float(screenWidth / 2 - font->width(item->getName()) / 2);
			float y = float(ySlot - 22);
			int alpha = 255;
			if(itemNameOverlayTime > 0.75) {
				float time = 0.25f - (itemNameOverlayTime - 0.75f);
				float percentage = cubeSmoothStep(time *  4, 0.0f, 1.0f);
				alpha = int(percentage * 255);
			}
			if(alpha != 0)
				font->drawShadow(item->getName(), x, y, 0x00ffffff + (alpha << 24));
		}
	}
}

// helper structure used by drawColoredString
struct ColorSegment {
    std::string text;
    uint32_t color;
};

// parse [tag] and [/tag] markers; tags may contain a color name (gold, green, etc.)
static void parseColorTags(const std::string& in, std::vector<ColorSegment>& out) {
    uint32_t curColor = 0xffffff;
    size_t pos = 0;
    while (pos < in.size()) {
        size_t open = in.find('[', pos);
        if (open == std::string::npos) {
            out.push_back({in.substr(pos), curColor});
            break;
        }
        if (open > pos) {
            out.push_back({in.substr(pos, open - pos), curColor});
        }
        size_t close = in.find(']', open);
        if (close == std::string::npos) {
            out.push_back({in.substr(open), curColor});
            break;
        }
        std::string tag = in.substr(open + 1, close - open - 1);
        if (!tag.empty() && tag[0] == '/') {
            curColor = 0xffffff;
        } else {
            std::string lower;
            lower.resize(tag.size());
            std::transform(tag.begin(), tag.end(), lower.begin(), ::tolower);
            if (lower.find("gold") != std::string::npos) curColor = 0xffd700;
            else if (lower.find("green") != std::string::npos) curColor = 0x00ff00;
            else if (lower.find("yellow") != std::string::npos) curColor = 0xffff00;
            else if (lower.find("red") != std::string::npos) curColor = 0xff0000;
            else if (lower.find("blue") != std::string::npos) curColor = 0x0000ff;
        }
        pos = close + 1;
    }
}

void Gui::drawColoredString(Font* font, const std::string& text, float x, float y, int alpha) {
    std::vector<ColorSegment> segs;
    parseColorTags(text, segs);
    float cx = x;
    for (auto &s : segs) {
        int color = s.color + (alpha << 24);
        font->drawShadow(s.text, cx, y, color);
        cx += font->width(s.text);
    }
}

float Gui::getColoredWidth(Font* font, const std::string& text) {
    std::vector<ColorSegment> segs;
    parseColorTags(text, segs);
    float w = 0;
    for (auto &s : segs) {
        w += font->width(s.text);
    }
    return w;
}

void Gui::renderChatMessages( const int screenHeight, unsigned int max, bool isChatting, Font* font ) {
	//        if (minecraft.screen instanceof ChatScreen) {
	//            max = 20;
	//            isChatting = true;
	//        }
	//
	//        glEnable(GL_BLEND);
	//        glBlendFunc2(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	//        glDisable(GL_ALPHA_TEST);
	//
	//        glPushMatrix2();
	//        glTranslatef2(0, screenHeight - 48, 0);
	//        // glScalef2(1.0f / ssc.scale, 1.0f / ssc.scale, 1);
	glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	int baseY = screenHeight - 48;
	int start = chatScrollOffset;
	if (start < 0) start = 0;
	for (unsigned int i = 0; i < max; i++) {
		unsigned int msgIdx = (unsigned int)start + i;
		if (msgIdx >= guiMessages.size())
			break;

		GuiMessage& message = guiMessages.at(msgIdx);
		if (message.ticks < 20 * 10 || isChatting) {
			float t = message.ticks / (20 * 10.0f);
			t = 1 - t;
			t = t * 10;
			if (t < 0) t = 0;
			if (t > 1) t = 1;
			t = t * t;
			int alpha = (int) (255 * t);
			if (isChatting) alpha = 255;

			if (alpha > 0) {
				const float x = 2;
				const float y = (float)(baseY - i * 9);
				std::string msg = message.message;
				this->fill(x, y - 1, x + MAX_MESSAGE_WIDTH, y + 8, (alpha / 2) << 24);
				glEnable(GL_BLEND);

				// special-case join/leave announcements
			int baseColor = 0xffffff;
			if (msg.find(" joined the game") != std::string::npos ||
				msg.find(" left the game") != std::string::npos) {
				baseColor = 0xffff00; // yellow
			}
			// replace previous logic; allow full colour tags now
			Gui::drawColoredString(font, msg, x, y, alpha);
			}
		}
	}
}

void Gui::renderToolBar( float a, int ySlot, const int screenWidth ) {
	glColor4f2(1, 1, 1, .5);
	minecraft->textures->loadAndBindTexture("gui/gui.png");

	Inventory* inventory = minecraft->player->inventory;

	int xBase, yBase;
	getSlotPos(0, xBase, yBase);
	const float baseItemX = (float)xBase + 3;
	const int slotsWidth = 20 * getNumSlots();
	// Left + right side of the selection bar
	blit(xBase, yBase, 0, 0, slotsWidth, 22);
	blit(xBase + slotsWidth, yBase, 180, 0, 2, 22);

	if (_currentDropSlot >= 0 && inventory->getItem(_currentDropSlot)) {
		int x = xBase + 3 +  _currentDropSlot * 20;
		int color = 0x8000ff00;
		int yy = (int)(17.0f * (_currentDropTicks + a) / DropTicks);

		if (_currentDropTicks >= 3) {
			glColor4f2(0, 1, 0, 0.5f);
		}
		fill(x, ySlot+16-yy, x+16, ySlot+16, color);
	}
	blit(xBase-1 + 20*inventory->selected, yBase - 1, 0, 22, 24, 22);
	glColor4f2(1, 1, 1, 1);

	// Flash a slot background
	if (_flashSlotId >= 0) {
		const float since = getTimeS() - _flashSlotStartTime;
		if (since > 0.2f) _flashSlotId = -1;
		else {
			int x = screenWidth / 2 - getNumSlots() * 10 + _flashSlotId * 20 + 2;
			int color = 0xffffff + (((int)(/*0x80 * since +*/ 0x51 - 0x50 * Mth::cos(10 * 6.28f * since))) << 24);
			//LOGI("Color: %.8x\n", color);
			fill(x, ySlot, x+16, ySlot+16, color);
		}
	}
	glColor4f2(1, 1, 1, 1);

	//static Stopwatch w;
	//w.start();

	Tesselator& t = Tesselator::instance;
	t.beginOverride();

	float x = baseItemX;

	int slots = getNumSlots() - _openInventorySlot;

	for (int i = 0; i < slots; i++) {
		renderSlot(i, (int)x, ySlot, a);
		x += 20;
	}
	_inventoryNeedsUpdate = false;


	if (_openInventorySlot) {
		blit(screenWidth / 2 + 10 * getNumSlots() - 20 + 4, ySlot + 6, 242, 252, 14, 4, 14, 4);
	}

	minecraft->textures->loadAndBindTexture("gui/gui_blocks.png");
	t.endOverrideAndDraw();

	// Render damaged items (@todo: investigate if it's faster by drawing in same batch)
	glDisable2(GL_DEPTH_TEST);
	glDisable2(GL_TEXTURE_2D);
	t.beginOverride();
	x = baseItemX;
	for (int i = 0; i < slots; i++) {
		ItemRenderer::renderGuiItemDecorations(minecraft->player->inventory->getItem(i), x, (float)ySlot);
		x += 20;
	}
	t.endOverrideAndDraw();
	glEnable(GL_DEPTH_TEST);
	glEnable(GL_TEXTURE_2D);

	//w.stop();
	//w.printEvery(100, "gui-slots");

	// Draw count
	//Tesselator& t = Tesselator::instance;
	glPushMatrix2();
	glScalef2(InvGuiScale + InvGuiScale, InvGuiScale + InvGuiScale, 1);
	const float k = 0.5f * GuiScale;

	t.beginOverride();
	if (minecraft->gameMode->isSurvivalType()) {
		x = baseItemX;
		for (int i = 0; i < slots; i++) {
			ItemInstance* item = minecraft->player->inventory->getItem(i);
			if (item && item->count >= 0)
				renderSlotText(item, k*x, k*ySlot + 1, true, true);
			x += 20;
		}
	}
	minecraft->textures->loadAndBindTexture("font/default8.png");
	t.endOverrideAndDraw();

	glPopMatrix2();
}

