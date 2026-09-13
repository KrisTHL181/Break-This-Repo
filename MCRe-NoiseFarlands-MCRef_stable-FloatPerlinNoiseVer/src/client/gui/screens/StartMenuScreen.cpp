#include "StartMenuScreen.h"
#include "UsernameScreen.h"
#include "SelectWorldScreen.h"
#include "ProgressScreen.h"
#include "JoinGameScreen.h"
#include "OptionsScreen.h"
#include "PauseScreen.h"
#include "PrerenderTilesScreen.h" // test button
#include "../components/ImageButton.h"

#include "../../../util/Mth.h"

#include "../Font.h"
#include "../components/ScrolledSelectionList.h"

#include "../../Minecraft.h"
#include "../../renderer/Tesselator.h"
#include "../../../AppPlatform.h"
#include "../../../LicenseCodes.h"
#include "SimpleChooseLevelScreen.h"
#include "../../renderer/Textures.h"
#include "../../../SharedConstants.h"

// Some kind of default settings, might be overridden in ::init
StartMenuScreen::StartMenuScreen()
:	bHost(    2, 0, 0, 160, 24, "Start Game"),
	bJoin(    3, 0, 0, 160, 24, "Join Game"),
	bOptions( 4, 0, 0, 160, 24, "Options"),
	bQuit(    5, "")
{
}

StartMenuScreen::~StartMenuScreen()
{
}

void StartMenuScreen::init()
{
	m_titleBackgroundTexture = minecraft->textures->loadTexture("gui/TitleBG.png");
	bJoin.active = bHost.active = bOptions.active = true;

	if (minecraft->options.getStringValue(OPTIONS_USERNAME).empty()) {
		return; // tick() will redirect to UsernameScreen
	}

	buttons.push_back(&bHost);
	buttons.push_back(&bJoin);
	//buttons.push_back(&bTest);

	tabButtons.push_back(&bHost);
	tabButtons.push_back(&bJoin);

	#ifndef RPI
		buttons.push_back(&bOptions);
		tabButtons.push_back(&bOptions);
	#endif

    // add quit button (top right X icon) – match OptionsScreen style
    {
        ImageDef def;
        def.name = "gui/touchgui.png";
        def.width = 34;
        def.height = 26;
        def.setSrc(IntRectangle(150, 0, (int)def.width, (int)def.height));
        bQuit.setImageDef(def, true);
        bQuit.scaleWhenPressed = false;
        buttons.push_back(&bQuit);
        // don't include in tab navigation
    }

	copyright = "\xffMojang AB";//. Do not distribute!";

	// always show base version string, suffix was previously added for Android builds
	std::string versionString = Common::getGameVersionString();

	std::string _username = minecraft->options.getStringValue(OPTIONS_USERNAME);
	if (_username.empty()) _username = "unknown";

	username = "Username: " + _username;

	#ifdef DEMO_MODE
	#ifdef __APPLE__
		version = versionString + " (Lite)";
	#else
		version = versionString + " (Demo)";
	#endif
	#else
		#ifdef RPI
			version = "v0.1.1 alpha";//(MCPE " + versionString + " compatible)";
		#else
			version = "v0.6.2 alpha";  // 随便写
		#endif
	#endif
}

void StartMenuScreen::setupPositions() {
	int yBase = height / 2;

	bHost.y =	 yBase;
	bJoin.y =	 bHost.y + 24 + 4;
	bOptions.y = bJoin.y + 24 + 4;

	// Center buttons
	bHost.x = (width - bHost.width) / 2;
	bJoin.x = (width - bJoin.width) / 2;
	bOptions.x = (width - bOptions.width) / 2;

    // position quit icon at top-right (use image-defined size)
    bQuit.x = width - bQuit.width;
    bQuit.y = 0;
}

void StartMenuScreen::tick() {
}

void StartMenuScreen::buttonClicked(Button* button) {

	if (button->id == bHost.id)
	{
        #if defined(DEMO_MODE) || defined(APPLE_DEMO_PROMOTION)
			minecraft->setScreen( new SimpleChooseLevelScreen("_DemoLevel") );
		#else
			minecraft->screenChooser.setScreen(SCREEN_SELECTWORLD);
		#endif
	}
	if (button->id == bJoin.id)
	{
		minecraft->locateMultiplayer();
		minecraft->screenChooser.setScreen(SCREEN_JOINGAME);
	}
	if (button->id == bOptions.id)
	{
		minecraft->setScreen(new OptionsScreen());
	}
	if (button == &bQuit)
	{
		minecraft->quit();
	}
}

bool StartMenuScreen::isInGameScreen() { return false; }

void StartMenuScreen::render(int xm, int ym, float a)
{
    // === 绘制自定义背景 ===
    if (Textures::isTextureIdValid(m_titleBackgroundTexture)) {
        minecraft->textures->bind(m_titleBackgroundTexture);
        glColor4f(1, 1, 1, 1);
        Tesselator& t = Tesselator::instance;
        t.begin();
        t.vertexUV(0,            (float)height, 0, 0, 1);
        t.vertexUV((float)width, (float)height, 0, 1, 1);
        t.vertexUV((float)width, 0,             0, 1, 0);
        t.vertexUV(0,            0,             0, 0, 0);
        t.draw();
    } else {
        Screen::renderBackground();   // 没图就回退默认
    }

    // 后面原有的标题、按钮、版权信息渲染保持不变
    // ..
	// Show current username in the top-left corner
	drawString(font, username, 2, 2, 0xff000000);

#if defined(RPI)
	TextureId id = minecraft->textures->loadTexture("gui/pi_title.png");
#else
	TextureId id = minecraft->textures->loadTexture("gui/title.png");
#endif
	const TextureData* data = minecraft->textures->getTemporaryTextureData(id);

	if (data) {
		minecraft->textures->bind(id);

		const float x = (float)width / 2;
		const float y = height/16;
		//const float scale = Mth::Min(
		const float wh = Mth::Min((float)width/2.0f, (float)data->w / 2);
		const float scale = 2.0f * wh / (float)data->w;
		const float h = scale * (float)data->h;

		// Render title text
		Tesselator& t = Tesselator::instance;
		glColor4f2(1, 1, 1, 1);
		t.begin();
		t.vertexUV(x-wh, y+h, blitOffset, 0, 1);
		t.vertexUV(x+wh, y+h, blitOffset, 1, 1);
		t.vertexUV(x+wh, y+0, blitOffset, 1, 0);
		t.vertexUV(x-wh, y+0, blitOffset, 0, 0);
		t.draw();
	}

#if defined(RPI)
	if (Textures::isTextureIdValid(minecraft->textures->loadAndBindTexture("gui/logo/raknet_high_72.png")))
		blit(0, height - 12, 0, 0, 43, 12, 256, 72+72);
#endif

	drawString(font, version, width - font->width(version) - 2, height - 10, 0xff000000);//0x666666);
	drawString(font, copyright, 2, height - 20, 0xff000000);
	glEnable2(GL_BLEND);
	glBlendFunc2(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	glColor4f2(1, 1, 1, 1);
	if (Textures::isTextureIdValid(minecraft->textures->loadAndBindTexture("gui/logo/github.png")))
		blit(2, height - 10, 0, 0, 8, 8, 256, 256);
	{
			std::string txt = "Github or BiliBiliMobile/MFSCelebrate_";
			float wtxt = font->width(txt);
			Gui::drawColoredString(font, txt, 12, height - 10, 255);
			// underline link
			float y0 = height - 10 + font->lineHeight - 1;
			this->fill(12, (int)y0, 12 + (int)wtxt, (int)(y0 + 1), 0xff000000);
    }

	
	Screen::render(xm, ym, a);
}

void StartMenuScreen::mouseClicked(int x, int y, int buttonNum) {
	const int logoX = 2;
	const int logoW = 8 + 2 + font->width("Github or BiliBiliMobile/MFSCelebrate_");
	const int logoY = height - 10;
	const int logoH = 10;
	if (x >= logoX && x <= logoX + logoW && y >= logoY && y <= logoY + logoH)
		minecraft->platform()->openURL("https://github.com/MFSCelebrate/MCReference_NoiseFarlands/");
	else
		Screen::mouseClicked(x, y, buttonNum);
}

bool StartMenuScreen::handleBackEvent( bool isDown ) {
	minecraft->quit();
	return true;
}
