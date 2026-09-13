package com.mojang.realmsclient.gui.screens.configuration;

import com.google.common.collect.Lists;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.gui.RealmsWorldSlotButton;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.gui.screens.RealmsResetWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsSelectWorldTemplateScreen;
import com.mojang.realmsclient.util.task.SwitchMinigameTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public class RealmsWorldsTab extends GridLayoutTab implements RealmsConfigurationTab {
   static final Component TITLE = Component.translatable("mco.configure.worlds.title");
   private final RealmsConfigureWorldScreen configurationScreen;
   private final Minecraft minecraft;
   private RealmsServer serverData;
   private final Button optionsButton;
   private final Button backupButton;
   private final Button resetWorldButton;
   private final List<RealmsWorldSlotButton> slotButtonList = Lists.newArrayList();

   public RealmsWorldsTab(final RealmsConfigureWorldScreen configurationScreen, final Minecraft minecraft, final RealmsServer serverData) {
      super(TITLE);
      this.configurationScreen = configurationScreen;
      this.minecraft = minecraft;
      this.serverData = serverData;
      GridLayout.RowHelper helper = this.layout.spacing(20).createRowHelper(1);
      GridLayout.RowHelper slots = new GridLayout().spacing(16).createRowHelper(4);
      this.slotButtonList.clear();

      for (int i = 1; i < 5; i++) {
         this.slotButtonList.add(slots.addChild(this.createSlotButton(i), LayoutSettings.defaults().alignVerticallyBottom()));
      }

      helper.addChild(slots.getGrid());
      GridLayout.RowHelper buttons = new GridLayout().spacing(8).createRowHelper(1);
      this.optionsButton = buttons.addChild(
         Button.builder(
               Component.translatable("mco.configure.world.buttons.options"),
               var3 -> minecraft.gui
                  .setScreen(
                     new RealmsSlotOptionsScreen(
                        configurationScreen, serverData.slots.get(serverData.activeSlot).copy(), serverData.worldType, serverData.activeSlot
                     )
                  )
            )
            .bounds(0, 0, 150, 20)
            .build()
      );
      this.backupButton = buttons.addChild(
         Button.builder(
               Component.translatable("mco.configure.world.backup"),
               var3 -> minecraft.gui.setScreen(new RealmsBackupScreen(configurationScreen, serverData.copy(), serverData.activeSlot))
            )
            .bounds(0, 0, 150, 20)
            .build()
      );
      this.resetWorldButton = buttons.addChild(Button.builder(Component.empty(), button -> this.resetButtonPressed()).bounds(0, 0, 150, 20).build());
      helper.addChild(buttons.getGrid(), LayoutSettings.defaults().alignHorizontallyCenter());
      this.backupButton.active = true;
      this.updateData(serverData);
   }

   private void resetButtonPressed() {
      if (this.isMinigame()) {
         this.minecraft
            .gui
            .setScreen(
               new RealmsSelectWorldTemplateScreen(
                  Component.translatable("mco.template.title.minigame"), this::templateSelectionCallback, RealmsServer.WorldType.MINIGAME, null
               )
            );
      } else {
         this.minecraft
            .gui
            .setScreen(
               RealmsResetWorldScreen.forResetSlot(
                  this.configurationScreen,
                  this.serverData.copy(),
                  () -> this.minecraft.execute(() -> this.minecraft.gui.setScreen(this.configurationScreen.getNewScreen()))
               )
            );
      }
   }

   private void templateSelectionCallback(final @Nullable WorldTemplate worldTemplate) {
      if (worldTemplate != null && WorldTemplate.WorldTemplateType.MINIGAME == worldTemplate.type()) {
         this.configurationScreen.stateChanged();
         RealmsConfigureWorldScreen newScreen = this.configurationScreen.getNewScreen();
         this.minecraft.gui.setScreen(new RealmsLongRunningMcoTaskScreen(newScreen, new SwitchMinigameTask(this.serverData.id, worldTemplate, newScreen)));
      } else {
         this.minecraft.gui.setScreen(this.configurationScreen);
      }
   }

   private boolean isMinigame() {
      return this.serverData.isMinigameActive();
   }

   @Override
   public void onSelected(final RealmsServer serverData) {
      this.updateData(serverData);
   }

   @Override
   public void updateData(final RealmsServer serverData) {
      this.serverData = serverData;
      this.optionsButton.active = !serverData.expired && !this.isMinigame();
      this.resetWorldButton.active = !serverData.expired;
      if (this.isMinigame()) {
         this.resetWorldButton.setMessage(Component.translatable("mco.configure.world.buttons.switchminigame"));
      } else {
         boolean emptySlot = serverData.slots.containsKey(serverData.activeSlot) && serverData.slots.get(serverData.activeSlot).options.empty;
         if (emptySlot) {
            this.resetWorldButton.setMessage(Component.translatable("mco.configure.world.buttons.newworld"));
         } else {
            this.resetWorldButton.setMessage(Component.translatable("mco.configure.world.buttons.resetworld"));
         }
      }

      this.backupButton.active = !this.isMinigame();

      for (RealmsWorldSlotButton realmsWorldSlotButton : this.slotButtonList) {
         RealmsWorldSlotButton.State state = realmsWorldSlotButton.setServerData(serverData);
         if (state.activeSlot) {
            realmsWorldSlotButton.setSize(80, 80);
         } else {
            realmsWorldSlotButton.setSize(50, 50);
         }
      }

      this.slotButtonList.get(serverData.activeSlot - 1).updateSlotState(serverData);
   }

   private RealmsWorldSlotButton createSlotButton(final int i) {
      return new RealmsWorldSlotButton(this.configurationScreen, 0, 0, 80, 80, i, this.serverData, button -> {
         RealmsWorldSlotButton.State state = ((RealmsWorldSlotButton)button).getState();
         switch (state.action) {
            case SWITCH_SLOT:
               if (state.minigame) {
                  this.switchToMinigame();
               } else if (state.empty) {
                  this.switchToEmptySlot(i, this.serverData);
               } else {
                  this.switchToFullSlot(i, this.serverData);
               }
            case NOTHING:
               return;
            default:
               throw new IllegalStateException("Unknown action " + state.action);
         }
      });
   }

   private void switchToMinigame() {
      RealmsSelectWorldTemplateScreen screen = new RealmsSelectWorldTemplateScreen(
         Component.translatable("mco.template.title.minigame"),
         this::templateSelectionCallback,
         RealmsServer.WorldType.MINIGAME,
         null,
         List.of(
            Component.translatable("mco.minigame.world.info.line1").withColor(-4539718),
            Component.translatable("mco.minigame.world.info.line2").withColor(-4539718)
         )
      );
      this.minecraft.gui.setScreen(screen);
   }

   private void switchToFullSlot(final int selectedSlot, final RealmsServer serverData) {
      this.minecraft
         .gui
         .setScreen(
            RealmsPopups.infoPopupScreen(
               this.configurationScreen,
               Component.translatable("mco.configure.world.slot.switch.question.line1"),
               popup -> {
                  RealmsConfigureWorldScreen newScreen = this.configurationScreen.getNewScreen();
                  this.configurationScreen.stateChanged();
                  this.minecraft
                     .gui
                     .setScreen(
                        new RealmsLongRunningMcoTaskScreen(
                           newScreen,
                           new SwitchSlotTask(serverData.id, selectedSlot, () -> this.minecraft.execute(() -> this.minecraft.gui.setScreen(newScreen)))
                        )
                     );
               }
            )
         );
   }

   private void switchToEmptySlot(final int selectedSlot, final RealmsServer serverData) {
      this.minecraft
         .gui
         .setScreen(
            RealmsPopups.infoPopupScreen(
               this.configurationScreen,
               Component.translatable("mco.configure.world.slot.switch.question.line1"),
               var3 -> {
                  this.configurationScreen.stateChanged();
                  RealmsResetWorldScreen resetWorldScreen = RealmsResetWorldScreen.forEmptySlot(
                     this.configurationScreen,
                     selectedSlot,
                     serverData,
                     () -> this.minecraft.execute(() -> this.minecraft.gui.setScreen(this.configurationScreen.getNewScreen()))
                  );
                  this.minecraft.gui.setScreen(resetWorldScreen);
               }
            )
         );
   }
}
