#ifndef NET_MINECRAFT_CLIENT_GUI_SCREENS_TOUCH__TouchIngameBlockSelectionScreen_H__
#define NET_MINECRAFT_CLIENT_GUI_SCREENS_TOUCH__TouchIngameBlockSelectionScreen_H__

#include "../../Screen.h"
#include "../../components/InventoryPane.h"
#include "../../components/Button.h"
#include "../../components/ScrollingPane.h"
#include "../../components/ItemPane.h"
#include "../../TweenData.h"
#include "../../../player/input/touchscreen/TouchAreaModel.h"
#include "../../../../AppPlatform.h"

namespace Touch {

class IngameBlockSelectionScreen :	public Screen,
									public IInventoryPaneCallback
{
	typedef Screen super;

public:
	IngameBlockSelectionScreen();
	virtual ~IngameBlockSelectionScreen();

	virtual void init() override;
	virtual void setupPositions() override;
	virtual void removed() override;

	void tick() override;
	void render(int xm, int ym, float a) override;

	bool hasClippingArea(IntRectangle& out) override;

	// IInventoryPaneCallback
	bool addItem(const InventoryPane* pane, int itemId) override;
	bool isAllowed(int slot) override;
	std::vector<const ItemInstance*> getItems(const InventoryPane* forPane) override;

	void buttonClicked(Button* button) override;
protected:
	virtual void mouseClicked(int x, int y, int buttonNum) override;
	virtual void mouseReleased(int x, int y, int buttonNum) override;

	// also support wheel scrolling
	virtual void mouseWheel(int dx, int dy, int xm, int ym) override;
private:
	void renderDemoOverlay();

	//int getLinearSlotId(int x, int y);
	int getSlotPosX(int slotX);
	int getSlotPosY(int slotY);
	int getSlotHeight();

private:
	int selectedItem;
	bool  _pendingClose;
	InventoryPane* _blockList;

	THeader bHeader;
	ImageButton bDone;
	TButton bCraft;
	TButton bArmor;
	TButton bMenu;

	IntRectangle clippingArea;

	int InventoryRows;
	int InventorySize;
	int InventoryColumns;
};

}

#endif /*NET_MINECRAFT_CLIENT_GUI_SCREENS_TOUCH__TouchIngameBlockSelectionScreen_H__*/
