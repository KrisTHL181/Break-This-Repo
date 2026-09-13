package com.mojang.realmsclient.gui.screens.configuration;

import com.google.common.collect.ImmutableList;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsLabel;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsSlotOptionsScreen extends RealmsScreen {
   private static final int DEFAULT_DIFFICULTY = 2;
   public static final List<Difficulty> DIFFICULTIES = ImmutableList.of(Difficulty.PEACEFUL, Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD);
   private static final int DEFAULT_GAME_MODE = 0;
   public static final List<GameType> GAME_MODES = ImmutableList.of(GameType.SURVIVAL, GameType.CREATIVE, GameType.ADVENTURE);
   private static final Component NAME_LABEL = Component.translatable("mco.configure.world.edit.slot.name");
   static final Component SPAWN_PROTECTION_TEXT = Component.translatable("mco.configure.world.spawnProtection");
   private EditBox nameEdit;
   protected final RealmsConfigureWorldScreen parentScreen;
   private int column1X;
   private int columnWidth;
   private final RealmsSlot slot;
   private final RealmsServer.WorldType worldType;
   private Difficulty difficulty;
   private GameType gameMode;
   private final String defaultSlotName;
   private String worldName;
   int spawnProtection;
   private boolean forceGameMode;
   RealmsSlotOptionsScreen.SettingsSlider spawnProtectionButton;

   public RealmsSlotOptionsScreen(RealmsConfigureWorldScreen p_406063_, RealmsSlot p_407904_, RealmsServer.WorldType p_405898_, int p_409133_) {
      super(Component.translatable("mco.configure.world.buttons.options"));
      this.parentScreen = p_406063_;
      this.slot = p_407904_;
      this.worldType = p_405898_;
      this.difficulty = findByIndex(DIFFICULTIES, p_407904_.options.difficulty, 2);
      this.gameMode = findByIndex(GAME_MODES, p_407904_.options.gameMode, 0);
      this.defaultSlotName = p_407904_.options.getDefaultSlotName(p_409133_);
      this.setWorldName(p_407904_.options.getSlotName(p_409133_));
      if (p_405898_ == RealmsServer.WorldType.NORMAL) {
         this.spawnProtection = p_407904_.options.spawnProtection;
         this.forceGameMode = p_407904_.options.forceGameMode;
      } else {
         this.spawnProtection = 0;
         this.forceGameMode = false;
      }
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parentScreen);
   }

   private static <T> T findByIndex(List<T> p_409406_, int p_410563_, int p_407035_) {
      try {
         return p_409406_.get(p_410563_);
      } catch (IndexOutOfBoundsException indexoutofboundsexception) {
         return p_409406_.get(p_407035_);
      }
   }

   private static <T> int findIndex(List<T> p_408574_, T p_410728_, int p_407120_) {
      int i = p_408574_.indexOf(p_410728_);
      return i == -1 ? p_407120_ : i;
   }

   @Override
   public void init() {
      this.columnWidth = 170;
      this.column1X = this.width / 2 - this.columnWidth;
      int i = this.width / 2 + 10;
      if (this.worldType != RealmsServer.WorldType.NORMAL) {
         Component component;
         if (this.worldType == RealmsServer.WorldType.ADVENTUREMAP) {
            component = Component.translatable("mco.configure.world.edit.subscreen.adventuremap");
         } else if (this.worldType == RealmsServer.WorldType.INSPIRATION) {
            component = Component.translatable("mco.configure.world.edit.subscreen.inspiration");
         } else {
            component = Component.translatable("mco.configure.world.edit.subscreen.experience");
         }

         this.addLabel(new RealmsLabel(component, this.width / 2, 26, -65536));
      }

      this.nameEdit = this.addWidget(
         new EditBox(this.minecraft.font, this.column1X, row(1), this.columnWidth, 20, null, Component.translatable("mco.configure.world.edit.slot.name"))
      );
      this.nameEdit.setValue(this.worldName);
      this.nameEdit.setResponder(this::setWorldName);
      CycleButton<Difficulty> cyclebutton2 = this.addRenderableWidget(
         CycleButton.builder(Difficulty::getDisplayName, this.difficulty)
            .withValues(DIFFICULTIES)
            .create(i, row(1), this.columnWidth, 20, Component.translatable("options.difficulty"), (p_420615_, p_420616_) -> this.difficulty = p_420616_)
      );
      CycleButton<GameType> cyclebutton = this.addRenderableWidget(
         CycleButton.builder(GameType::getShortDisplayName, this.gameMode)
            .withValues(GAME_MODES)
            .create(
               this.column1X, row(3), this.columnWidth, 20, Component.translatable("selectWorld.gameMode"), (p_409637_, p_408624_) -> this.gameMode = p_408624_
            )
      );
      CycleButton<Boolean> cyclebutton1 = this.addRenderableWidget(
         CycleButton.onOffBuilder(this.forceGameMode)
            .create(
               i,
               row(3),
               this.columnWidth,
               20,
               Component.translatable("mco.configure.world.forceGameMode"),
               (p_407915_, p_409942_) -> this.forceGameMode = p_409942_
            )
      );
      this.spawnProtectionButton = this.addRenderableWidget(
         new RealmsSlotOptionsScreen.SettingsSlider(this.column1X, row(5), this.columnWidth, this.spawnProtection, 0.0F, 16.0F)
      );
      if (this.worldType != RealmsServer.WorldType.NORMAL) {
         this.spawnProtectionButton.active = false;
         cyclebutton1.active = false;
      }

      if (this.slot.isHardcore()) {
         cyclebutton2.active = false;
         cyclebutton.active = false;
         cyclebutton1.active = false;
      }

      this.addRenderableWidget(
         Button.builder(Component.translatable("mco.configure.world.buttons.done"), p_407224_ -> this.saveSettings())
            .bounds(this.column1X, row(13), this.columnWidth, 20)
            .build()
      );
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, p_406277_ -> this.onClose()).bounds(i, row(13), this.columnWidth, 20).build());
   }

   private CycleButton.OnValueChange<Boolean> confirmDangerousOption(Component p_410363_, Consumer<Boolean> p_406359_) {
      return (p_408603_, p_407022_) -> {
         if (p_407022_) {
            p_406359_.accept(true);
         } else {
            this.minecraft.setScreen(RealmsPopups.warningPopupScreen(this, p_410363_, p_407700_ -> {
               p_406359_.accept(false);
               p_407700_.onClose();
            }));
         }
      };
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(this.getTitle(), this.createLabelNarration());
   }

   @Override
   public void render(GuiGraphics p_406931_, int p_409237_, int p_405986_, float p_408960_) {
      super.render(p_406931_, p_409237_, p_405986_, p_408960_);
      p_406931_.drawCenteredString(this.font, this.title, this.width / 2, 17, -1);
      p_406931_.drawString(this.font, NAME_LABEL, this.column1X + this.columnWidth / 2 - this.font.width(NAME_LABEL) / 2, row(0) - 5, -1);
      this.nameEdit.render(p_406931_, p_409237_, p_405986_, p_408960_);
   }

   private void setWorldName(String p_406125_) {
      if (p_406125_.equals(this.defaultSlotName)) {
         this.worldName = "";
      } else {
         this.worldName = p_406125_;
      }
   }

   private void saveSettings() {
      int i = findIndex(DIFFICULTIES, this.difficulty, 2);
      int j = findIndex(GAME_MODES, this.gameMode, 0);
      if (this.worldType != RealmsServer.WorldType.ADVENTUREMAP
         && this.worldType != RealmsServer.WorldType.EXPERIENCE
         && this.worldType != RealmsServer.WorldType.INSPIRATION) {
         this.parentScreen
            .saveSlotSettings(
               new RealmsSlot(
                  this.slot.slotId,
                  new RealmsWorldOptions(
                     this.spawnProtection, i, j, this.forceGameMode, this.worldName, this.slot.options.version, this.slot.options.compatibility
                  ),
                  this.slot.settings
               )
            );
      } else {
         this.parentScreen
            .saveSlotSettings(
               new RealmsSlot(
                  this.slot.slotId,
                  new RealmsWorldOptions(
                     this.slot.options.spawnProtection,
                     i,
                     j,
                     this.slot.options.forceGameMode,
                     this.worldName,
                     this.slot.options.version,
                     this.slot.options.compatibility
                  ),
                  this.slot.settings
               )
            );
      }
   }

   @OnlyIn(Dist.CLIENT)
   class SettingsSlider extends AbstractSliderButton {
      private final double minValue;
      private final double maxValue;

      public SettingsSlider(final int p_407158_, final int p_408790_, final int p_410422_, final int p_410411_, final float p_410070_, final float p_409871_) {
         super(p_407158_, p_408790_, p_410422_, 20, CommonComponents.EMPTY, 0.0);
         this.minValue = p_410070_;
         this.maxValue = p_409871_;
         this.value = (Mth.clamp(p_410411_, p_410070_, p_409871_) - p_410070_) / (p_409871_ - p_410070_);
         this.updateMessage();
      }

      @Override
      public void applyValue() {
         if (RealmsSlotOptionsScreen.this.spawnProtectionButton.active) {
            RealmsSlotOptionsScreen.this.spawnProtection = (int)Mth.lerp(Mth.clamp(this.value, 0.0, 1.0), this.minValue, this.maxValue);
         }
      }

      @Override
      protected void updateMessage() {
         this.setMessage(
            CommonComponents.optionNameValue(
               RealmsSlotOptionsScreen.SPAWN_PROTECTION_TEXT,
               RealmsSlotOptionsScreen.this.spawnProtection == 0
                  ? CommonComponents.OPTION_OFF
                  : Component.literal(String.valueOf(RealmsSlotOptionsScreen.this.spawnProtection))
            )
         );
      }
   }
}
