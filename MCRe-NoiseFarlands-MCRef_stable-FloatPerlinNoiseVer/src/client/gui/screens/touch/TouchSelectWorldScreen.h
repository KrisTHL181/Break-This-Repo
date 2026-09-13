#ifndef NET_MINECRAFT_CLIENT_GUI_SCREENS_TOUCH__TouchSelectWorldScreen_H__
#define NET_MINECRAFT_CLIENT_GUI_SCREENS_TOUCH__TouchSelectWorldScreen_H__

#include "../ConfirmScreen.h"
#include "../../Screen.h"
#include "../../TweenData.h"
#include "../../components/ImageButton.h"
#include "../../components/Button.h"
#include "../../components/RolledSelectionListH.h"
#include "../../../Minecraft.h"
#include "../../../../world/level/storage/LevelStorageSource.h"


namespace Touch {

class SelectWorldScreen;

//
// Scrolling World selection list
//
class TouchWorldSelectionList : public RolledSelectionListH
{
public:
	TouchWorldSelectionList(Minecraft* _minecraft, int _width, int _height);
	virtual void tick();
	void stepLeft();
	void stepRight();

	void commit();
protected:
	virtual int getNumberOfItems();
	virtual void selectItem(int item, bool doubleClick);
	virtual bool isSelectedItem(int item);

	virtual void renderBackground() {}
	virtual void renderItem(int i, int x, int y, int h, Tesselator& t);
	virtual float getPos(float alpha);
	virtual void touched() { mode = 0; }
	virtual bool capXPosition();

	virtual void selectStart(int item, int localX, int localY);
	virtual void selectCancel();
private:
	TweenData td;
	void tweenInited();

	int selectedItem;
	bool _newWorldSelected; // Is the PLUS button pressed?
	int _height;
	LevelSummaryList levels;
	std::vector<StringVector> _descriptions;
	StringVector _imageNames;
	
	bool hasPickedLevel;
	LevelSummary pickedLevel;
	int pickedIndex;

	int stoppedTick;
	int currentTick;
	float accRatio;
	int mode;
	
	friend class SelectWorldScreen;
};

//
// Delete World screen
//
class TouchDeleteWorldScreen: public ConfirmScreen
{
public:
	TouchDeleteWorldScreen(const LevelSummary& levelId);
protected:
	virtual void postResult(bool isOk);
private:
	LevelSummary _level;
};


//
// Select world screen
//
class SelectWorldScreen: public Screen
{
public:
	SelectWorldScreen();
	virtual ~SelectWorldScreen();

	virtual void init() override;
	virtual void setupPositions() override;

	virtual void tick() override;
	virtual void render(int xm, int ym, float a) override;

	virtual bool isIndexValid(int index);
	virtual bool handleBackEvent(bool isDown) override;
	virtual void buttonClicked(Button* button) override;
	virtual void keyPressed(int eventKey) override;

	// support for mouse wheel when desktop code uses touch variant
	virtual void mouseWheel(int dx, int dy, int xm, int ym) override;

	bool isInGameScreen() override;
private:
	void loadLevelSource();
	std::string getUniqueLevelName(const std::string& level);

	ImageButton bDelete;
	TButton bCreate;
	THeader bHeader;
	TButton bBack;
	Button bWorldView;
	TouchWorldSelectionList* worldsList;
	LevelSummaryList levels;

	bool _mouseHasBeenUp;
	bool _hasStartedLevel;
	//LevelStorageSource* levels;
};
};

#endif /*NET_MINECRAFT_CLIENT_GUI_SCREENS_TOUCH__TouchSelectWorldScreen_H__*/
