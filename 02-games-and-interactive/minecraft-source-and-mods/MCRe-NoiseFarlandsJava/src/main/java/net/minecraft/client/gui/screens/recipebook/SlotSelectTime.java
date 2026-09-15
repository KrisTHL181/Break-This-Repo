package net.minecraft.client.gui.screens.recipebook;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface SlotSelectTime {
    int currentIndex();
}