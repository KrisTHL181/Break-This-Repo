#include "SimpleChooseLevelScreen.h"
#include "ProgressScreen.h"
#include "ScreenChooser.h"
#include "../components/Button.h"
#include "../components/ImageButton.h"
#include "../../Minecraft.h"
#include "../../../world/level/LevelSettings.h"
#include "../../../platform/time.h"
#include "../../../platform/input/Keyboard.h"
#include "../../../platform/log.h"
#include "../../../client/Options.h"

SimpleChooseLevelScreen::SimpleChooseLevelScreen(const std::string& levelName)
:   bHeader(0),
    bGamemode(0),
    bCheats(0),
    bNoiseMode(0),
    bBack(0),
    bCreate(0),
    levelName(levelName),
    hasChosen(false),
    gamemode(GameType::Survival),
    cheatsEnabled(false),
    noiseMode(-1),                     // 初始化为 -1，表示未读取
    tLevelName(0, "World name"),
    tSeed(1, "World seed")
{
}

SimpleChooseLevelScreen::~SimpleChooseLevelScreen()
{
    if (bHeader) delete bHeader;
    delete bGamemode;
    delete bCheats;
    delete bNoiseMode;
    delete bBack;
    delete bCreate;
}

void SimpleChooseLevelScreen::init()
{
    // make sure the base class loads the existing level list
    ChooseLevelScreen::init();

    tLevelName.text = "New world";

    // header + close button
    bHeader = new Touch::THeader(0, "Create World");
    bBack = new ImageButton(2, "");
    {
        ImageDef def;
        def.name = "gui/touchgui.png";
        def.width = 34;
        def.height = 26;
        def.setSrc(IntRectangle(150, 0, (int)def.width, (int)def.height));
        bBack->setImageDef(def, true);
    }
    if (/* minecraft->useTouchscreen() */ true) {
        bGamemode = new Touch::TButton(1, "Survival mode");
        bCheats  = new Touch::TButton(4, "Cheats: Off");
        bNoiseMode = new Touch::TButton(5, "Noise mode: 32-bit");   // 临时文字，马上会根据实际选项更新
        bCreate  = new Touch::TButton(3, "Create");
    } else {
        bGamemode = new Button(1, "Survival mode");
        bCheats  = new Button(4, "Cheats: Off");
        bNoiseMode = new Button(5, "Noise mode: 32-bit");
        bCreate  = new Button(3, "Create");
    }

    // --- 🆕 从全局选项读取当前噪声模式并同步界面 ---
    Options& opts = minecraft->options;
    bool is64 = opts.getBooleanValue(OPTIONS_SIXTYFOUR_FARLANDS);
    bool isDouble = opts.getBooleanValue(OPTIONS_DOUBLE_FARLANDS);
	bool isTheEnd = opts.getBooleanValue(OPTIONS_END_GENERATOR);
    if (isDouble) {
        noiseMode = 2;
        bNoiseMode->msg = "Noise mode: Double";
    } else if (is64) {
        noiseMode = 1;
        bNoiseMode->msg = "Noise mode: 64-bit";
    } else if (isTheEnd) {
        noiseMode = 3;
        bNoiseMode->msg = "Noise mode: The End";
    } else {
        noiseMode = 0;
        bNoiseMode->msg = "Noise mode: 32-bit";
    }
    // ---------------------------------------------

    buttons.push_back(bHeader);
    buttons.push_back(bBack);
    buttons.push_back(bGamemode);
    buttons.push_back(bCheats);
    buttons.push_back(bNoiseMode);
    buttons.push_back(bCreate);

    tabButtons.push_back(bGamemode);
    tabButtons.push_back(bCheats);
    tabButtons.push_back(bNoiseMode);
    tabButtons.push_back(bBack);
    tabButtons.push_back(bCreate);

    textBoxes.push_back(&tLevelName);
    textBoxes.push_back(&tSeed);
}

void SimpleChooseLevelScreen::setupPositions()
{
    int buttonHeight = bBack->height;

    // position back button in upper-right
    bBack->x = width - bBack->width;
    bBack->y = 0;

    // header occupies remaining top bar
    if (bHeader) {
        bHeader->x = 0;
        bHeader->y = 0;
        bHeader->width = width - bBack->width;
        bHeader->height = buttonHeight;
    }

    // layout the form elements below the header
    int centerX = width / 2;
    const int padding = 5;

    tLevelName.width = tSeed.width = 200;
    tLevelName.x = centerX - tLevelName.width / 2;
    tLevelName.y = buttonHeight + 20;

    tSeed.x = tLevelName.x;
    tSeed.y = tLevelName.y + 30;

    const int buttonWidth = 120;
    const int buttonSpacing = 10;
    const int totalButtonWidth = buttonWidth * 3 + buttonSpacing * 2;   // 三个按钮

    bGamemode->width = buttonWidth;
    bCheats->width = buttonWidth;
    bNoiseMode->width = buttonWidth;

    bGamemode->x = centerX - totalButtonWidth / 2;
    bCheats->x = bGamemode->x + buttonWidth + buttonSpacing;
    bNoiseMode->x = bCheats->x + buttonWidth + buttonSpacing;

    // compute vertical centre for buttons in remaining space
    {
        int bottomPad = 20;
        int availTop = buttonHeight + 20 + 30 + 10; // just below seed
        int availBottom = height - bottomPad - bCreate->height - 10; // leave some gap before create
        int availHeight = availBottom - availTop;
        if (availHeight < 0) availHeight = 0;
        int y = availTop + (availHeight - bGamemode->height) / 2;
        bGamemode->y = y;
        bCheats->y = y;
        bNoiseMode->y = y;
    }

    bCreate->width = 100;
    bCreate->x = centerX - bCreate->width / 2;
    int bottomPadding = 20;
    bCreate->y = height - bottomPadding - bCreate->height;
}

void SimpleChooseLevelScreen::tick()
{
    // let any textboxes handle their own blinking/input
    for (auto* tb : textBoxes)
        tb->tick(minecraft);
}

void SimpleChooseLevelScreen::render( int xm, int ym, float a )
{
    renderDirtBackground(0);
    glEnable2(GL_BLEND);

    const char* modeDesc = NULL;
    if (gamemode == GameType::Survival) {
        modeDesc = "Mobs, health and gather resources";
    } else if (gamemode == GameType::Creative) {
        modeDesc = "Unlimited resources and flying";
    }
    if (modeDesc) {
        drawCenteredString(minecraft->font, modeDesc, width / 2, bGamemode->y + bGamemode->height + 4, 0xffcccccc);
    }

    // 噪声模式描述
    const char* noiseDesc = NULL;
    switch (noiseMode) {
    case 0: noiseDesc = "32-bit int (Original)"; break;
    case 1: noiseDesc = "64-bit int (64-bit Generation)"; break;
    case 2: noiseDesc = "Double (Imitate Java Edition)"; break;
    case 3: noiseDesc = "End stone islands in the void"; break;  // 🔧 新增
	}
    if (noiseDesc) {
       drawCenteredString(minecraft->font, noiseDesc, width / 2, bNoiseMode->y + bNoiseMode->height + 20, 0xffcccccc);
	}

    drawString(minecraft->font, "World name:", tLevelName.x, tLevelName.y - Font::DefaultLineHeight - 2, 0xffcccccc);
    drawString(minecraft->font, "World seed:", tSeed.x, tSeed.y - Font::DefaultLineHeight - 2, 0xffcccccc);

    Screen::render(xm, ym, a);
    glDisable2(GL_BLEND);
}

// mouse clicks should also manage textbox focus explicitly
void SimpleChooseLevelScreen::mouseClicked(int x, int y, int buttonNum)
{
    if (buttonNum == MouseAction::ACTION_LEFT) {
        // determine if the click landed on either textbox or its label above
        int lvlTop = tLevelName.y - (Font::DefaultLineHeight + 4);
        int lvlBottom = tLevelName.y + tLevelName.height;
        int lvlLeft = tLevelName.x;
        int lvlRight = tLevelName.x + tLevelName.width;
        bool clickedLevel = x >= lvlLeft && x < lvlRight && y >= lvlTop && y < lvlBottom;

        int seedTop = tSeed.y - (Font::DefaultLineHeight + 4);
        int seedBottom = tSeed.y + tSeed.height;
        int seedLeft = tSeed.x;
        int seedRight = tSeed.x + tSeed.width;
        bool clickedSeed  = x >= seedLeft && x < seedRight && y >= seedTop && y < seedBottom;

        if (clickedLevel) {
            LOGI("SimpleChooseLevelScreen: level textbox clicked (%d,%d)\n", x, y);
            tLevelName.setFocus(minecraft);
            tSeed.loseFocus(minecraft);
        } else if (clickedSeed) {
            LOGI("SimpleChooseLevelScreen: seed textbox clicked (%d,%d)\n", x, y);
            tSeed.setFocus(minecraft);
            tLevelName.loseFocus(minecraft);
        } else {
            // click outside both fields -> blur both
            tLevelName.loseFocus(minecraft);
            tSeed.loseFocus(minecraft);
        }
    }

    // allow normal button and textbox handling too
    Screen::mouseClicked(x, y, buttonNum);
}

void SimpleChooseLevelScreen::buttonClicked( Button* button )
{
    if (hasChosen)
        return;

    if (button == bGamemode) {
        gamemode ^= 1;
        bGamemode->msg = (gamemode == GameType::Survival) ? "Survival mode" : "Creative mode";
        return;
    }

    if (button == bCheats) {
        cheatsEnabled = !cheatsEnabled;
        bCheats->msg = cheatsEnabled ? "Cheats: On" : "Cheats: Off";
        return;
    }

    if (button == bNoiseMode) {
    noiseMode = (noiseMode + 1) % 4;  // 🔧 3 → 4
    switch (noiseMode) {
        case 0: bNoiseMode->msg = "Noise mode: 32-bit";  break;
        case 1: bNoiseMode->msg = "Noise mode: 64-bit";  break;
        case 2: bNoiseMode->msg = "Noise mode: Double";  break;
        case 3: bNoiseMode->msg = "Noise mode: The End"; break;  // 🔧 新增
    }
    return;
	}
    if (button == bCreate && !tLevelName.text.empty()) {
    int seed = getEpochTimeS();
    if (!tSeed.text.empty()) {
        std::string seedString = Util::stringTrim(tSeed.text);
        int tmpSeed;
        if (sscanf(seedString.c_str(), "%d", &tmpSeed) > 0) {
            seed = tmpSeed;
        } else {
            seed = Util::hashCode(seedString);
        }
    }
    std::string levelId = getUniqueLevelName(tLevelName.text);

    // 🔧 useEndGenerator 通过 LevelSettings 传入
    LevelSettings settings(seed, gamemode, cheatsEnabled, (noiseMode == 3));

    // 根据选择的噪声模式设置全局选项（32/64/Double，不影响 The End）
    Options& opts = minecraft->options;
    opts.set(OPTIONS_SIXTYFOUR_FARLANDS, (noiseMode == 1));
    opts.set(OPTIONS_DOUBLE_FARLANDS,  (noiseMode == 2));
	opts.set(OPTIONS_END_GENERATOR, (noiseMode == 3));

    minecraft->selectLevel(levelId, levelId, settings);
    minecraft->hostMultiplayer();
    minecraft->setScreen(new ProgressScreen());
    hasChosen = true;
    return;
	}

    if (button == bBack) {
        minecraft->screenChooser.setScreen(SCREEN_STARTMENU);
    }
}

void SimpleChooseLevelScreen::keyPressed(int eventKey) {
    // ESC 键：只有两个输入框都未聚焦时才允许退出
    if (eventKey == Keyboard::KEY_ESCAPE) {
        if (tLevelName.focused || tSeed.focused) {
            tLevelName.loseFocus(minecraft);
            tSeed.loseFocus(minecraft);
        } else {
            minecraft->screenChooser.setScreen(SCREEN_STARTMENU);
        }
        return;
    }
    // 其他键正常交给父类（会分发给 textBoxes）
    Screen::keyPressed(eventKey);
}

bool SimpleChooseLevelScreen::handleBackEvent(bool isDown) {
	if (!isDown)
		minecraft->screenChooser.setScreen(SCREEN_STARTMENU);
	return true; 
}
