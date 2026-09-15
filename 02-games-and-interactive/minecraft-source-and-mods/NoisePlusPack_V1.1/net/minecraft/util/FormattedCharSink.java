package net.minecraft.util;

import net.minecraft.network.chat.Style;

@FunctionalInterface
public interface FormattedCharSink {
   boolean accept(int var1, Style var2, int var3);
}
