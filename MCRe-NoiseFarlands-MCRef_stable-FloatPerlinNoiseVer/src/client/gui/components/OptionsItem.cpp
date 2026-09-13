#include "OptionsItem.h"
#include "../../Minecraft.h"
#include "../../../locale/I18n.h"
#include "../../../util/Mth.h"
OptionsItem::OptionsItem( OptionId optionId, std::string label, GuiElement* element )
: GuiElementContainer(false, true, 0, 0, 24, 12),
  m_optionId(optionId),
  m_label(label) {
	  addChild(element);
}

void OptionsItem::setupPositions() {
	int currentHeight = 0;
	for(std::vector<GuiElement*>::iterator it = children.begin(); it != children.end(); ++it) {
		(*it)->x = x + width - (*it)->width - 15;
		(*it)->y = y + currentHeight;
		currentHeight += (*it)->height;
	}
	height = currentHeight;
}

void OptionsItem::render( Minecraft* minecraft, int xm, int ym ) {
	int yOffset = (height - 8) / 2;
	std::string text = m_label;
	if (m_optionId == OPTIONS_GUI_SCALE) {
		int value = minecraft->options.getIntValue(OPTIONS_GUI_SCALE);
		std::string scaleText;
		switch (value) {
		case 0: scaleText = I18n::get("options.guiScale.auto"); break;
		case 1: scaleText = I18n::get("options.guiScale.small"); break;
		case 2: scaleText = I18n::get("options.guiScale.medium"); break;
		case 3: scaleText = I18n::get("options.guiScale.large"); break;
		case 4: scaleText = I18n::get("options.guiScale.larger"); break;
		case 5: scaleText = I18n::get("options.guiScale.largest"); break;
		default: scaleText = I18n::get("options.guiScale.auto"); break;
		}
		text += ": " + scaleText;
	}

	minecraft->font->draw(text, (float)x, (float)y + yOffset, 0x909090, false);
	super::render(minecraft, xm, ym);
}