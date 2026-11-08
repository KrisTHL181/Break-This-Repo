package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.realmsclient.dto.RealmsServer;

public interface RealmsConfigurationTab {
   void updateData(RealmsServer serverData);

   default void onSelected(final RealmsServer serverData) {
   }

   default void onDeselected(final RealmsServer serverData) {
   }
}
