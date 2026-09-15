package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.realmsclient.dto.RealmsServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface RealmsConfigurationTab {
   void updateData(RealmsServer var1);

   default void onSelected(RealmsServer p_405812_) {
   }

   default void onDeselected(RealmsServer p_406570_) {
   }
}
