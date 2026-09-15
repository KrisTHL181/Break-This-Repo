package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2ic;

@OnlyIn(Dist.CLIENT)
public interface ClientTooltipPositioner {
   Vector2ic positionTooltip(int var1, int var2, int var3, int var4, int var5, int var6);
}
