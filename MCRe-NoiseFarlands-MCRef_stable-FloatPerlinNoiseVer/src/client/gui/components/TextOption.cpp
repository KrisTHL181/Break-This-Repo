#include "TextOption.h"
#include <client/Minecraft.h>

TextOption::TextOption(Minecraft* minecraft, OptionId optId) 
    : TextBox((int)optId, minecraft->options.getOpt(optId)->getStringId()) 
{
    text = minecraft->options.getStringValue(optId);
}

bool TextOption::loseFocus(Minecraft* minecraft) {
    if (TextBox::loseFocus(minecraft)) {
        minecraft->options.set((OptionId)id, text);
        return true;
    }

    return false;
}