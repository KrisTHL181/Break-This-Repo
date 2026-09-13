package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.realmsclient.dto.RealmsServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface RealmsConfigurationTab {
    void updateData(RealmsServer serverData);

    default void onSelected(final RealmsServer serverData) {
    }

    default void onDeselected(final RealmsServer serverData) {
    }
}