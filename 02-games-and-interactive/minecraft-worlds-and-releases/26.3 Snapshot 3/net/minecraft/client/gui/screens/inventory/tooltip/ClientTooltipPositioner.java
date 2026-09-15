package net.minecraft.client.gui.screens.inventory.tooltip;

import org.joml.Vector2ic;

public interface ClientTooltipPositioner {
   Vector2ic positionTooltip(int screenWidth, int screenHeight, int x, int y, int tooltipWidth, int tooltipHeight);
}
