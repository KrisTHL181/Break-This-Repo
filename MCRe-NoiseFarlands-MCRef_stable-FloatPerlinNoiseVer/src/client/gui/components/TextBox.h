#ifndef NET_MINECRAFT_CLIENT_GUI_COMPONENTS__TextBox_H__
#define NET_MINECRAFT_CLIENT_GUI_COMPONENTS__TextBox_H__

//package net.minecraft.client.gui;

#include <string>
#include "GuiElement.h"
#include "../../Options.h"
#include "../../../platform/input/Mouse.h"
#include "../../../platform/input/Keyboard.h"

class Font;
class Minecraft;

class TextBox: public GuiElement
{
public:
	TextBox(int id, const std::string& msg);
	TextBox(int id, int x, int y, const std::string& msg);
	TextBox(int id, int x, int y, int w, int h, const std::string& msg);

	virtual void mouseClicked(Minecraft* minecraft, int x, int y, int buttonNum);

	virtual void setFocus(Minecraft* minecraft);
	virtual bool loseFocus(Minecraft* minecraft);

	virtual void render(Minecraft* minecraft, int xm, int ym);

	virtual void keyPressed(Minecraft* minecraft, int key);
	virtual void charPressed(Minecraft* minecraft, char c);
	virtual void tick(Minecraft* minecraft);

	
public:
	std::string hint;
	std::string text;
    bool _justTypedDigit;   // 记录刚输入了数字，用于过滤误映射的退格
	int id;

	int blinkTicks;

	bool focused;
	bool blink;
};

#endif /*NET_MINECRAFT_CLIENT_GUI_COMPONENTS__TextBox_H__*/
