
#include "../Screen.h"
#include "../components/Button.h"
#include "../../Minecraft.h"
#include "client/gui/components/ImageButton.h"
#include "client/gui/components/TextBox.h"

class JoinByIPScreen: public Screen
{
public:
	JoinByIPScreen();
	virtual ~JoinByIPScreen();

	void init();
	void setupPositions();

	virtual void tick();
    void render(int xm, int ym, float a);

	virtual void keyPressed(int eventKey);
	void buttonClicked(Button* button);
    virtual bool handleBackEvent(bool isDown);
private:
    TextBox tIP;
    Touch::THeader bHeader;
	Touch::TButton bJoin;
	ImageButton bBack;
};