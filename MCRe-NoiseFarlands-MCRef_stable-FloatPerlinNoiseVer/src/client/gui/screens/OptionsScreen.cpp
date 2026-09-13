#include "OptionsScreen.h"
#include "StartMenuScreen.h"
#include "UsernameScreen.h"
#include "DialogDefinitions.h"
#include "../../Minecraft.h"
#include "../../../AppPlatform.h"
#include "CreditsScreen.h"
#include "../components/ImageButton.h"
#include "../components/OptionsGroup.h"
#include "../components/TextOption.h"
#include "../components/OptionsItem.h"
#include "../Gui.h"
#include "platform/input/Keyboard.h"
#include "../../../client/renderer/Tesselator.h"   // <--- 新增，避免 Tesselator 不完整类型
#include <cmath>
#include <algorithm>

OptionsScreen::OptionsScreen()
	: btnClose(NULL), bHeader(NULL), btnCredits(NULL), selectedCategory(0),
	  scrollOffset(0.0f), maxScrollOffset(0.0f), scrollVelocity(0.0f),
	  lastMouseY(0.0f), isDragging(false), isScrollbarVisible(false),
m_backgroundTexture(Textures::InvalidId)   // <--- 加上这行
{ }

OptionsScreen::~OptionsScreen() {
	if (btnClose) delete btnClose;
	if (bHeader) delete bHeader;
	if (btnCredits) delete btnCredits;
	for (auto btn : categoryButtons) delete btn;
	for (auto pane : optionPanes) delete pane;
}

void OptionsScreen::init() {
	m_backgroundTexture = minecraft->textures->loadTexture("gui/setting_background.png");
	bHeader = new Touch::THeader(0, "Options");
	btnClose = new ImageButton(1, "");
	ImageDef def;
	def.name = "gui/touchgui.png";
	def.width = 34;
	def.height = 26;
	def.setSrc(IntRectangle(150, 0, (int)def.width, (int)def.height));
	btnClose->setImageDef(def, true);

	categoryButtons.push_back(new Touch::TButton(2, "General"));
	categoryButtons.push_back(new Touch::TButton(3, "Game"));
	categoryButtons.push_back(new Touch::TButton(4, "Controls"));
	categoryButtons.push_back(new Touch::TButton(5, "Graphics"));
	categoryButtons.push_back(new Touch::TButton(6, "Tweaks"));
	categoryButtons.push_back(new Touch::TButton(7, "World"));

	btnCredits = new Touch::TButton(11, "Credits");

	buttons.push_back(bHeader);
	buttons.push_back(btnClose);
	buttons.push_back(btnCredits);
	for (auto btn : categoryButtons) {
		buttons.push_back(btn);
		tabButtons.push_back(btn);
	}

	generateOptionScreens();
	selectCategory(0);
}

void OptionsScreen::setupPositions() {
	int buttonHeight = btnClose->height;
	btnClose->x = width - btnClose->width;
	btnClose->y = 0;

	int offsetNum = 1;
	for (auto btn : categoryButtons) {
		btn->x = 0;
		btn->y = offsetNum * buttonHeight;
		btn->selected = false;
		offsetNum++;
	}

	bHeader->x = 0;
	bHeader->y = 0;
	bHeader->width = width - btnClose->width;
	bHeader->height = btnClose->height;

	if (btnCredits) {
		btnCredits->x = width - btnCredits->width;
		btnCredits->y = height - btnCredits->height;
	}

	for (auto pane : optionPanes) {
		if (!categoryButtons.empty() && categoryButtons[0]) {
			pane->x = categoryButtons[0]->width;
			pane->y = bHeader->height;
			pane->width = width - categoryButtons[0]->width;
			pane->setupPositions();
		}
	}
	updateMaxScrollOffset();
	applyScrollLimits();
}

void OptionsScreen::render(int xm, int ym, float a)
{
    // === 绘制自定义背景（或回退默认） ===
    if (Textures::isTextureIdValid(m_backgroundTexture)) {
        minecraft->textures->bind(m_backgroundTexture);
        glColor4f(1, 1, 1, 1);
        Tesselator& t = Tesselator::instance;
        t.begin();
        t.vertexUV(0,            (float)height, 0, 0, 1);
        t.vertexUV((float)width, (float)height, 0, 1, 1);
        t.vertexUV((float)width, 0,             0, 1, 0);
        t.vertexUV(0,            0,             0, 0, 0);
        t.draw();
    } else {
        Screen::renderBackground();   // 图片载入失败就用原版背景
    }

    // ---- 后面所有滚动区域和控件渲染保持不变 ----
    // ... 原来 render 函数里的代码 ...

	// 临时隐藏 textBoxes，避免基类重复渲染
	std::vector<TextBox*> savedTextBoxes;
	savedTextBoxes.swap(textBoxes);

	if (currentOptionsGroup) {
		float scale = Gui::GuiScale;
		int logicX = currentOptionsGroup->x;
		int logicY = currentOptionsGroup->y;
		int logicW = currentOptionsGroup->width;
		int bottomPadding = (btnCredits ? btnCredits->height + 5 : 0);
		int logicH = height - logicY - bottomPadding;
		if (logicH < 0) logicH = 0;

		int scissorX = (int)(logicX * scale);
		int scissorY = (int)(minecraft->height - ((logicY + logicH) * scale));
		int scissorW = (int)(logicW * scale);
		int scissorH = (int)(logicH * scale);

		GLboolean wasEnabled = glIsEnabled(GL_SCISSOR_TEST);
		GLint oldBox[4];
		glGetIntegerv(GL_SCISSOR_BOX, oldBox);

		glEnable(GL_SCISSOR_TEST);
		glScissor(scissorX, scissorY, scissorW, scissorH);

		glPushMatrix();
		glTranslatef(0.0f, -scrollOffset, 0.0f);

		// 禁用内部裁剪，防止 TextBox 文字消失
		glDisable(GL_SCISSOR_TEST);

		int xmm = (int)(xm * width / (float)minecraft->width);
		int ymm = (int)(ym * height / (float)minecraft->height) - 1 + (int)scrollOffset;
		currentOptionsGroup->render(minecraft, xmm, ymm);

		glEnable(GL_SCISSOR_TEST);
		glPopMatrix();

		if (wasEnabled) {
			glEnable(GL_SCISSOR_TEST);
			glScissor(oldBox[0], oldBox[1], oldBox[2], oldBox[3]);
		} else {
			glDisable(GL_SCISSOR_TEST);
		}
	}

	// 渲染其他 UI（此时 textBoxes 为空，不会重复绘制输入框）
	super::render(xm, ym, a);

	// 恢复 textBoxes，保证焦点管理和保存功能正常
	savedTextBoxes.swap(textBoxes);
}

void OptionsScreen::removed() { }

void OptionsScreen::buttonClicked(Button* button) {
	if (button == btnClose) {
		lostFocus();                 // 保存输入框
		minecraft->options.save();
		if (minecraft->screen)
			minecraft->setScreen(NULL);
		else
			minecraft->screenChooser.setScreen(SCREEN_STARTMENU);
	} else if (button->id >= categoryButtons[0]->id && button->id <= categoryButtons.back()->id) {
		int idx = button->id - categoryButtons[0]->id;
		selectCategory(idx);
	} else if (button == btnCredits) {
		minecraft->setScreen(new CreditsScreen());
	}
}

void OptionsScreen::selectCategory(int index) {
	for (size_t i = 0; i < categoryButtons.size(); ++i)
		categoryButtons[i]->selected = (int)i == index;
	if (index < (int)optionPanes.size()) {
		currentOptionsGroup = optionPanes[index];
		scrollOffset = 0.0f;
		scrollVelocity = 0.0f;
		updateMaxScrollOffset();

		// 🆕 重建 textBoxes 列表：只包含当前面板中的 TextOption
		textBoxes.clear();
		if (currentOptionsGroup) {
			for (GuiElement* child : currentOptionsGroup->getChildren()) {
				if (OptionsItem* item = dynamic_cast<OptionsItem*>(child)) {
					for (GuiElement* grandChild : item->getChildren()) {
						if (TextOption* tb = dynamic_cast<TextOption*>(grandChild)) {
							textBoxes.push_back(tb);
						}
					}
				}
			}
		}
	}
}

void OptionsScreen::generateOptionScreens() {
	optionPanes.push_back(new OptionsGroup("options.group.general"));
	optionPanes.push_back(new OptionsGroup("options.group.game"));
	optionPanes.push_back(new OptionsGroup("options.group.controls"));
	optionPanes.push_back(new OptionsGroup("options.group.graphics"));
	optionPanes.push_back(new OptionsGroup("options.group.tweaks"));
	optionPanes.push_back(new OptionsGroup("options.group.world"));

	// General
	optionPanes[0]->addOptionItem(OPTIONS_USERNAME, minecraft)
		.addOptionItem(OPTIONS_SENSITIVITY, minecraft);
	// Game
	optionPanes[1]->addOptionItem(OPTIONS_DIFFICULTY, minecraft)
		.addOptionItem(OPTIONS_SERVER_VISIBLE, minecraft)
		.addOptionItem(OPTIONS_THIRD_PERSON_VIEW, minecraft)
		.addOptionItem(OPTIONS_GUI_SCALE, minecraft)
		.addOptionItem(OPTIONS_SENSITIVITY, minecraft)
		.addOptionItem(OPTIONS_MUSIC_VOLUME, minecraft)
		.addOptionItem(OPTIONS_SOUND_VOLUME, minecraft)
		.addOptionItem(OPTIONS_SMOOTH_CAMERA, minecraft)
		.addOptionItem(OPTIONS_DESTROY_VIBRATION, minecraft)
		.addOptionItem(OPTIONS_IS_LEFT_HANDED, minecraft);
	// Controls
	optionPanes[2]->addOptionItem(OPTIONS_INVERT_Y_MOUSE, minecraft)
		.addOptionItem(OPTIONS_USE_TOUCHSCREEN, minecraft)
		.addOptionItem(OPTIONS_AUTOJUMP, minecraft);
	for (int i = OPTIONS_KEY_FORWARD; i <= OPTIONS_KEY_USE; ++i)
		optionPanes[2]->addOptionItem((OptionId)i, minecraft);
	// Graphics
	// Graphics
optionPanes[3]->addOptionItem(OPTIONS_FANCY_GRAPHICS, minecraft)
    .addOptionItem(OPTIONS_LIMIT_FRAMERATE, minecraft)
    .addOptionItem(OPTIONS_VSYNC, minecraft)
    .addOptionItem(OPTIONS_RENDER_DEBUG, minecraft)
    .addOptionItem(OPTIONS_ANAGLYPH_3D, minecraft)
    .addOptionItem(OPTIONS_VIEW_BOBBING, minecraft)
    .addOptionItem(OPTIONS_AMBIENT_OCCLUSION, minecraft)
    .addOptionItem(OPTIONS_VIEW_DISTANCE, minecraft);   // 新加视距
	optionPanes[3]->addOptionItem(OPTIONS_DEBUG_SCREEN_SIZE, minecraft);
	// Tweaks
	optionPanes[4]->addOptionItem(OPTIONS_ALLOW_SPRINT, minecraft)
		.addOptionItem(OPTIONS_BAR_ON_TOP, minecraft)
		.addOptionItem(OPTIONS_RPI_CURSOR, minecraft);
	// World
	optionPanes[5]->addOptionItem(OPTIONS_WORLD_SCALE_X, minecraft)
		.addOptionItem(OPTIONS_WORLD_SCALE_Y, minecraft)    // 🆕 Y 缩放
		.addOptionItem(OPTIONS_WORLD_SCALE_Z, minecraft)
		.addOptionItem(OPTIONS_WORLD_OFFSET_X, minecraft)
	    .addOptionItem(OPTIONS_WORLD_OFFSET_Y, minecraft)   // 🆕 Y 偏移
		.addOptionItem(OPTIONS_WORLD_OFFSET_Z, minecraft)
		.addOptionItem(OPTIONS_POSTPONED_FRINGE, minecraft)
		.addOptionItem(OPTIONS_PROGRESSIVE_FARLANDS, minecraft)
		.addOptionItem(OPTIONS_SEA_LEVEL, minecraft)
		.addOptionItem(OPTIONS_STRIPE_REPAIR, minecraft)
		.addOptionItem(OPTIONS_TELEPORT, minecraft)
		.addOptionItem(OPTIONS_DISABLE_SKYGRID, minecraft)
	    .addOptionItem(OPTIONS_END_CIRCLES, minecraft);  // 🛡️ 新增

	// 收集所有 TextOption 到 textBoxes，以便基类 lostFocus 自动保存
}

void OptionsScreen::mouseClicked(int x, int y, int buttonNum) {
	if (currentOptionsGroup && isPointInScrollArea(x, y)) {
		transformMouseForScroll(x, y);
		currentOptionsGroup->mouseClicked(minecraft, x, y, buttonNum);
		if (buttonNum == MouseAction::ACTION_LEFT) {
			isDragging = true;
			lastMouseY = (float)y;
			scrollVelocity = 0.0f;
		}
	} else {
		super::mouseClicked(x, y, buttonNum);
	}
}

void OptionsScreen::mouseReleased(int x, int y, int buttonNum) {
	if (currentOptionsGroup && isDragging) {
		transformMouseForScroll(x, y);
		currentOptionsGroup->mouseReleased(minecraft, x, y, buttonNum);
		isDragging = false;
	} else if (currentOptionsGroup && isPointInScrollArea(x, y)) {
		transformMouseForScroll(x, y);
		currentOptionsGroup->mouseReleased(minecraft, x, y, buttonNum);
	} else {
		super::mouseReleased(x, y, buttonNum);
	}
}

void OptionsScreen::mouseWheel(int dx, int dy, int xm, int ym) {
	if (currentOptionsGroup && isPointInScrollArea(xm, ym)) {
		scrollOffset -= dy * 20.0f;
		applyScrollLimits();
		scrollVelocity = 0.0f;
	} else {
		super::mouseWheel(dx, dy, xm, ym);
	}
}

void OptionsScreen::keyPressed(int eventKey) {
    if (currentOptionsGroup)
        currentOptionsGroup->keyPressed(minecraft, eventKey);
    if (eventKey == Keyboard::KEY_ESCAPE) {
        lostFocus();
        minecraft->options.save();
        minecraft->setScreen(NULL);
    }
    // 不再调用 super::keyPressed(eventKey)，避免二次分发
}

void OptionsScreen::charPressed(char inputChar) {
    if (currentOptionsGroup)
        currentOptionsGroup->charPressed(minecraft, inputChar);
    // 不再调用 super::charPressed(inputChar);
    // 因为 currentOptionsGroup 已经将事件分发给所有子控件
}

void OptionsScreen::tick() {
	if (currentOptionsGroup)
		currentOptionsGroup->tick(minecraft);

	if (!isDragging && fabs(scrollVelocity) > 0.01f) {
		scrollOffset += scrollVelocity;
		applyScrollLimits();
		scrollVelocity *= 0.92f;
		if (fabs(scrollVelocity) < 0.01f) scrollVelocity = 0.0f;
	}

	if (isDragging) {
		int mx = Mouse::getX();
		int my = Mouse::getY();
		minecraft->screen->toGUICoordinate(mx, my);
		if (isPointInScrollArea(mx, my)) {
			float delta = lastMouseY - my;
			scrollOffset += delta;
			applyScrollLimits();
			scrollVelocity = delta * 0.5f;
			lastMouseY = (float)my;
		}
	}
	super::tick();
}

void OptionsScreen::lostFocus() {
	super::lostFocus();   // 会遍历 textBoxes 调用 loseFocus，保存内容
}

// ========== 辅助函数 ==========
void OptionsScreen::updateMaxScrollOffset() {
	if (!currentOptionsGroup) {
		maxScrollOffset = 0.0f;
		return;
	}
	float contentH = (float)currentOptionsGroup->height;
	float viewportH = (float)(height - currentOptionsGroup->y - (btnCredits ? btnCredits->height + 5 : 0));
	maxScrollOffset = std::max(0.0f, contentH - viewportH);
}

void OptionsScreen::applyScrollLimits() {
	scrollOffset = Mth::clamp(scrollOffset, 0.0f, maxScrollOffset);
	if (scrollOffset <= 0.0f || scrollOffset >= maxScrollOffset)
		scrollVelocity = 0.0f;
}

float OptionsScreen::getContentHeight() const {
	return currentOptionsGroup ? (float)currentOptionsGroup->height : 0.0f;
}

bool OptionsScreen::isPointInScrollArea(int x, int y) const {
	if (!currentOptionsGroup) return false;
	int bottomPad = (btnCredits ? btnCredits->height + 5 : 0);
	int areaBottom = height - bottomPad;
	return (x >= currentOptionsGroup->x && x < currentOptionsGroup->x + currentOptionsGroup->width &&
	        y >= currentOptionsGroup->y && y < areaBottom);
}

void OptionsScreen::transformMouseForScroll(int& x, int& y) const {
	y += (int)scrollOffset;
}
