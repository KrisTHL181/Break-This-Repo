package net.minecraft.client.gui.screens.options;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface HasDifficultyReaction {
    void onDifficultyChanged();
}