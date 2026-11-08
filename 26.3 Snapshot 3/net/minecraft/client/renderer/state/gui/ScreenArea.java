package net.minecraft.client.renderer.state.gui;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jspecify.annotations.Nullable;

public interface ScreenArea {
   @Nullable ScreenRectangle bounds();
}
