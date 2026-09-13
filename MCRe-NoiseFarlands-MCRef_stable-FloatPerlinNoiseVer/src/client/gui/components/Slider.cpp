#include "Slider.h"
#include "../../Minecraft.h"
#include "../../renderer/Textures.h"
#include "../Screen.h"
#include "../../../locale/I18n.h"
#include "../../../util/Mth.h"
#include <algorithm>
#include <assert.h>

Slider::Slider(OptionId optId) : m_mouseDownOnElement(false), m_optId(optId), m_numSteps(0) {}

void Slider::render( Minecraft* minecraft, int xm, int ym ) {
	int xSliderStart = x + 5;
	int xSliderEnd = x + width - 5;
	int ySliderStart = y + 6;
	int ySliderEnd = y + 9;
	int handleSizeX = 9;
	int handleSizeY = 15;
	int barWidth = xSliderEnd - xSliderStart;
	//fill(x, y + 8, x + (int)(width * percentage), y + height, 0xffff00ff);
	fill(xSliderStart, ySliderStart, xSliderEnd, ySliderEnd, 0xff606060);

	if (m_numSteps > 2) {
		int stepDistance = barWidth / (m_numSteps-1);
		for(int a = 0; a < m_numSteps; ++a) {
			int renderSliderStepPosX = xSliderStart + a * stepDistance + 1;
			fill(renderSliderStepPosX - 1, ySliderStart - 2, renderSliderStepPosX + 1, ySliderEnd + 2, 0xff606060);
		}
	}

	minecraft->textures->loadAndBindTexture("gui/touchgui.png");
	blit(xSliderStart + (int)(m_percentage * barWidth) - handleSizeX / 2, y, 226, 126, handleSizeX, handleSizeY, handleSizeX, handleSizeY);
}

void Slider::mouseClicked( Minecraft* minecraft, int x, int y, int buttonNum ) {
	if(pointInside(x, y)) {
		m_mouseDownOnElement = true;
	}
}

void Slider::mouseReleased( Minecraft* minecraft, int x, int y, int buttonNum ) {
	m_mouseDownOnElement = false;
} 

void Slider::tick(Minecraft* minecraft) {
	if(minecraft->screen != NULL) {
		int xm = Mouse::getX();
		int ym = Mouse::getY();
		
		minecraft->screen->toGUICoordinate(xm, ym);

		if(m_mouseDownOnElement) {
			m_percentage = float(xm - x) / float(width);
			m_percentage = Mth::clamp(m_percentage, 0.0f, 1.0f);
		}
	}
}

SliderFloat::SliderFloat(Minecraft* minecraft, OptionId option) 
: Slider(option), m_option(dynamic_cast<OptionFloat*>(minecraft->options.getOpt(option)))
{
	m_percentage = Mth::clamp((m_option->get() - m_option->getMin()) / (m_option->getMax() - m_option->getMin()), 0.f, 1.f);
}

SliderInt::SliderInt(Minecraft* minecraft, OptionId option) 
: Slider(option), m_option(dynamic_cast<OptionInt*>(minecraft->options.getOpt(option)))
{
	m_numSteps = m_option->getMax() - m_option->getMin() + 1;
	m_percentage = float(m_option->get() - m_option->getMin()) / (m_numSteps-1);
}

void SliderInt::render( Minecraft* minecraft, int xm, int ym ) {
	Slider::render(minecraft, xm, ym);
}

void SliderInt::mouseReleased( Minecraft* minecraft, int x, int y, int buttonNum ) {
	Slider::mouseReleased(minecraft, x, y, buttonNum);

	if (pointInside(x, y)) {
		int curStep = int(m_percentage * (m_numSteps-1) + 0.5f);
		curStep = Mth::clamp(curStep + m_option->getMin(), m_option->getMin(), m_option->getMax());
		m_percentage = float(curStep - m_option->getMin()) / (m_numSteps-1);

		minecraft->options.set(m_optId, curStep);
	}
}

void SliderFloat::mouseReleased( Minecraft* minecraft, int x, int y, int buttonNum ) {
	Slider::mouseReleased(minecraft, x, y, buttonNum);

	if (pointInside(x, y)) {
		minecraft->options.set(m_optId, m_percentage * (m_option->getMax() - m_option->getMin()) + m_option->getMin());
	}
}