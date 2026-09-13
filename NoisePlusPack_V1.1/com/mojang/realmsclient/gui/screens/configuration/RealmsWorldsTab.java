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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
class RealmsWorldsTab extends GridLayoutTab implements RealmsConfigurationTab {
   static final Component TITLE = Component.translatable("mco.configure.worlds.title");
   private final RealmsConfigureWorldScreen configurationScreen;
   private final Minecraft minecraft;
   private RealmsServer serverData;
   private final Button optionsButton;
   private final Button backupButton;
   private final Button resetWorldButton;
   private final List<RealmsWorldSlotButton> slotButtonList = Lists.newArrayList();

   RealmsWorldsTab(RealmsConfigureWorldScreen p_410270_, Minecraft p_407127_, RealmsServer p_408890_) {
      super(TITLE);
      this.configurationScreen = p_410270_;
      this.minecraft = p_407127_;
      this.serverData = p_408890_;
      GridLayout.RowHelper gridlayout$rowhelper = this.layout.spacing(20).createRowHelper(1);
      GridLayout.RowHelper gridlayout$rowhelper1 = new GridLayout().spacing(16).createRowHelper(4);
      this.slotButtonList.clear();

      for (int i = 1; i < 5; i++) {
         this.slotButtonList.add(gridlayout$rowhelper1.addChild(this.createSlotButton(i), LayoutSettings.defaults().alignVerticallyBottom()));
      }

      gridlayout$rowhelper.addChild(gridlayout$rowhelper1.getGrid());
      GridLayout.RowHelper gridlayout$rowhelper2 = new GridLayout().spacing(8).createRowHelper(1);
      this.optionsButton = gridlayout$rowhelper2.addChild(
         Button.builder(
               Component.translatable("mco.configure.world.buttons.options"),
               p_447769_ -> p_407127_.setScreen(
                  new RealmsSlotOptionsScreen(p_410270_, p_408890_.slots.get(p_408890_.activeSlot).copy(), p_408890_.worldType, p_408890_.activeSlot)
               )
            )
            .bounds(0, 0, 150, 20)
            .build()
      );
      this.backupButton = gridlayout$rowhelper2.addChild(
         Button.builder(
               Component.translatable("mco.configure.world.backup"),
               p_447765_ -> p_407127_.setScreen(new RealmsBackupScreen(p_410270_, p_408890_.copy(), p_408890_.activeSlot))
            )
            .bounds(0, 0, 150, 20)
            .build()
      );
      this.resetWorldButton = gridlayout$rowhelper2.addChild(
         Button.builder(Component.empty(), p_410267_ -> this.resetButtonPressed()).bounds(0, 0, 150, 20).build()
      );
      gridlayout$rowhelper.addChild(gridlayout$rowhelper2.getGrid(), LayoutSettings.defaults().alignHorizontallyCenter());
      this.backupButton.active = true;
      this.updateData(p_408890_);
   }

   private void resetButtonPressed() {
      if (this.isMinigame()) {
         this.minecraft
            .setScreen(
               new RealmsSelectWorldTemplateScreen(
                  Component.translatable("mco.template.title.minigame"), this::templateSelectionCallback, RealmsServer.WorldType.MINIGAME, null
               )
            );
      } else {
         this.minecraft
            .setScreen(
               RealmsResetWorldScreen.forResetSlot(
                  this.configurationScreen,
                  this.serverData.copy(),
                  () -> this.minecraft.execute(() -> this.minecraft.setScreen(this.configurationScreen.getNewScreen()))
               )
            );
      }
   }

   private void templateSelectionCallback(@Nullable WorldTemplate p_409426_) {
      if (p_409426_ != null && WorldTemplate.WorldTemplateType.MINIGAME == p_409426_.type()) {
         this.configurationScreen.stateChanged();
         RealmsConfigureWorldScreen realmsconfigureworldscreen = this.configurationScreen.getNewScreen();
         this.minecraft
            .setScreen(
               new RealmsLongRunningMcoTaskScreen(realmsconfigureworldscreen, new SwitchMinigameTask(this.serverData.id, p_409426_, realmsconfigureworldscreen))
            );
      } else {
         this.minecraft.setScreen(this.configurationScreen);
      }
   }

   private boolean isMinigame() {
      return this.serverData.isMinigameActive();
   }

   @Override
   public void onSelected(RealmsServer p_408580_) {
      this.updateData(p_408580_);
   }

   @Override
   public void updateData(RealmsServer p_408768_) {
      this.serverData = p_408768_;
      this.optionsButton.active = !p_408768_.expired && !this.isMinigame();
      this.resetWorldButton.active = !p_408768_.expired;
      if (this.isMinigame()) {
         this.resetWorldButton.setMessage(Component.translatable("mco.configure.world.buttons.switchminigame"));
      } else {
         boolean flag = p_408768_.slots.containsKey(p_408768_.activeSlot) && p_408768_.slots.get(p_408768_.activeSlot).options.empty;
         if (flag) {
            this.resetWorldButton.setMessage(Component.translatable("mco.configure.world.buttons.newworld"));
         } else {
            this.resetWorldButton.setMessage(Component.translatable("mco.configure.world.buttons.resetworld"));
         }
      }

      this.backupButton.active = !this.isMinigame();

      for (RealmsWorldSlotButton realmsworldslotbutton : this.slotButtonList) {
         RealmsWorldSlotButton.State realmsworldslotbutton$state = realmsworldslotbutton.setServerData(p_408768_);
         if (realmsworldslotbutton$state.activeSlot) {
            realmsworldslotbutton.setSize(80, 80);
         } else {
            realmsworldslotbutton.setSize(50, 50);
         }
      }
   }

   private RealmsWorldSlotButton createSlotButton(int p_406233_) {
      return new RealmsWorldSlotButton(0, 0, 80, 80, p_406233_, this.serverData, p_406823_ -> {
         RealmsWorldSlotButton.State realmsworldslotbutton$state = ((RealmsWorldSlotButton)p_406823_).getState();
         switch (realmsworldslotbutton$state.action) {
            case SWITCH_SLOT:
               if (realmsworldslotbutton$state.minigame) {
                  this.switchToMinigame();
               } else if (realmsworldslotbutton$state.empty) {
                  this.switchToEmptySlot(p_406233_, this.serverData);
               } else {
                  this.switchToFullSlot(p_406233_, this.serverData);
               }
            case NOTHING:
               return;
            default:
               throw new IllegalStateException("Unknown action " + realmsworldslotbutton$state.action);
         }
      });
   }

   private void switchToMinigame() {
      RealmsSelectWorldTemplateScreen realmsselectworldtemplatescreen = new RealmsSelectWorldTemplateScreen(
         Component.translatable("mco.template.title.minigame"),
         this::templateSelectionCallback,
         RealmsServer.WorldType.MINIGAME,
         null,
         List.of(
            Component.translatable("mco.minigame.world.info.line1").withColor(-4539718),
            Component.translatable("mco.minigame.world.info.line2").withColor(-4539718)
         )
      );
      this.minecraft.setScreen(realmsselectworldtemplatescreen);
   }

   private void switchToFullSlot(int p_409281_, RealmsServer p_406821_) {
      this.minecraft
         .setScreen(
            RealmsPopups.infoPopupScreen(
               this.configurationScreen,
               Component.translatable("mco.configure.world.slot.switch.question.line1"),
               p_409812_ -> {
                  RealmsConfigureWorldScreen realmsconfigureworldscreen = this.configurationScreen.getNewScreen();
                  this.configurationScreen.stateChanged();
                  this.minecraft
                     .setScreen(
                        new RealmsLongRunningMcoTaskScreen(
                           realmsconfigureworldscreen,
                           new SwitchSlotTask(p_406821_.id, p_409281_, () -> this.minecraft.execute(() -> this.minecraft.setScreen(realmsconfigureworldscreen)))
                        )
                     );
               }
            )
         );
   }

   private void switchToEmptySlot(int p_406010_, RealmsServer p_407740_) {
      this.minecraft
         .setScreen(
            RealmsPopups.infoPopupScreen(
               this.configurationScreen,
               Component.translatable("mco.configure.world.slot.switch.question.line1"),
               p_406450_ -> {
                  this.configurationScreen.stateChanged();
                  RealmsResetWorldScreen realmsresetworldscreen = RealmsResetWorldScreen.forEmptySlot(
                     this.configurationScreen,
                     p_406010_,
                     p_407740_,
                     () -> this.minecraft.execute(() -> this.minecraft.setScreen(this.configurationScreen.getNewScreen()))
                  );
                  this.minecraft.setScreen(realmsresetworldscreen);
               }
            )
         );
   }
}
