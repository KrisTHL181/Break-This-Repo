#ifndef NET_MINECRAFT_CLIENT_GUI_COMPONENTS__Slider_H__
#define NET_MINECRAFT_CLIENT_GUI_COMPONENTS__Slider_H__

#include "GuiElement.h"
#include "../../../client/Options.h"
#include <client/Option.h>

class Slider : public GuiElement {
	typedef GuiElement super;
public:
	virtual void render( Minecraft* minecraft, int xm, int ym );
	virtual void mouseClicked( Minecraft* minecraft, int x, int y, int buttonNum );
	virtual void mouseReleased( Minecraft* minecraft, int x, int y, int buttonNum );
	virtual void tick(Minecraft* minecraft);

protected:
	Slider(OptionId optId);

	OptionId m_optId;

	bool m_mouseDownOnElement;
	float m_percentage;
	int m_numSteps;
};

class SliderFloat : public Slider {
public:
	SliderFloat(Minecraft* minecraft, OptionId option);

	virtual void mouseReleased( Minecraft* minecraft, int x, int y, int buttonNum ) override;	

protected:
	OptionFloat* m_option;
};


class SliderInt : public Slider {
public:
	SliderInt(Minecraft* minecraft, OptionId option);

	virtual void render( Minecraft* minecraft, int xm, int ym ) override;
	virtual void mouseReleased( Minecraft* minecraft, int x, int y, int buttonNum ) override;

protected:
	OptionInt* m_option;
};

#endif /*NET_MINECRAFT_CLIENT_GUI_COMPONENTS__Slider_H__*/
