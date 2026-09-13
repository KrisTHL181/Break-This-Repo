#ifndef _MINECRAFT_INGAMEBLOCKSELECTIONSCREEN_H_
#define _MINECRAFT_INGAMEBLOCKSELECTIONSCREEN_H_

#include "../Screen.h"
#include "../../player/input/touchscreen/TouchAreaModel.h"
#include "../components/Button.h"

class IngameBlockSelectionScreen : public Screen
{
	typedef Screen super;
public:
	IngameBlockSelectionScreen();
	virtual ~IngameBlockSelectionScreen() {}

	virtual void init() override;
	virtual void removed() override;

	void render(int xm, int ym, float a) override;

protected:
	virtual void mouseClicked(int x, int y, int buttonNum) override;
	virtual void mouseReleased(int x, int y, int buttonNum) override;

	virtual void buttonClicked(Button* button) override;

	// wheel input for creative inventory scrolling
	virtual void mouseWheel(int dx, int dy, int xm, int ym) override;

	virtual void keyPressed(int eventKey) override;
private:
	void renderSlots();
	void renderSlot(int slot, int x, int y, float a);
	void renderDemoOverlay();

	int getSelectedSlot(int x, int y);
	void selectSlotAndClose();

	//int getLinearSlotId(int x, int y);
	int getSlotPosX(int slotX);
	int getSlotPosY(int slotY);

	int getSlotHeight();

	bool isAllowed(int slot);

private:
	int InventoryCols;
	int InventoryRows;
	int InventorySize;

	int selectedItem;
	bool _pendingQuit;

	Button bArmor;

	RectangleArea _area;
};

#endif
