package com.mojang.realmsclient.gui.screens.configuration;

import com.google.common.collect.ImmutableList;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import java.util.List;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;

public class RealmsSlotOptionsScreen extends RealmsScreen {
   private static final int DEFAULT_DIFFICULTY = 2;
   public static final List<Difficulty> DIFFICULTIES = ImmutableList.of(Difficulty.PEACEFUL, Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD);
   private static final int DEFAULT_GAME_MODE = 0;
   public static final List<GameType> GAME_MODES = ImmutableList.of(GameType.SURVIVAL, GameType.CREATIVE, GameType.ADVENTURE);
   private static final Component TITLE = Component.translatable("mco.configure.world.buttons.options");
   private static final Component WORLD_NAME_EDIT_LABEL = Component.translatable("mco.configure.world.edit.slot.name");
   private static final Component SPAWN_PROTECTION_TEXT = Component.translatable("mco.configure.world.spawnProtection");
   private static final Component GAME_MODE_BUTTON = Component.translatable("selectWorld.gameMode");
   private static final Component DIFFICULTY_BUTTON = Component.translatable("options.difficulty");
   private static final Component FORCE_GAME_MODE_BUTTON = Component.translatable("mco.configure.world.forceGameMode");
   private static final int SPACING = 8;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final RealmsConfigureWorldScreen parentScreen;
   private final RealmsSlot slot;
   private final RealmsServer.WorldType worldType;
   private final String defaultSlotName;
   private int spawnProtection;
   private boolean forceGameMode;
   private Difficulty difficulty;
   private GameType gameMode;
   private String worldName;
   private @Nullable StringWidget warningHeader;
   private RealmsSlotOptionsScreen.@Nullable SettingsSlider spawnProtectionButton;

   public RealmsSlotOptionsScreen(
      final RealmsConfigureWorldScreen configureWorldScreen, final RealmsSlot slot, final RealmsServer.WorldType worldType, final int activeSlot
   ) {
      super(TITLE);
      this.parentScreen = configureWorldScreen;
      this.slot = slot;
      this.worldType = worldType;
      this.difficulty = findByIndex(DIFFICULTIES, slot.options.difficulty, 2);
      this.gameMode = findByIndex(GAME_MODES, slot.options.gameMode, 0);
      this.defaultSlotName = slot.options.getDefaultSlotName(activeSlot);
      this.setWorldName(slot.options.getSlotName(activeSlot));
      if (worldType == RealmsServer.WorldType.NORMAL) {
         this.spawnProtection = slot.options.spawnProtection;
         this.forceGameMode = slot.options.forceGameMode;
      } else {
         this.spawnProtection = 0;
         this.forceGameMode = false;
      }
   }

   @Override
   public void init() {
      LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(8));
      header.defaultCellSetting().alignHorizontallyCenter();
      header.addChild(new StringWidget(TITLE, this.minecraft.font));

      Component warning = switch (this.worldType) {
         case ADVENTUREMAP -> Component.translatable("mco.configure.world.edit.subscreen.adventuremap").withColor(-65536);
         case INSPIRATION -> Component.translatable("mco.configure.world.edit.subscreen.inspiration").withColor(-65536);
         case EXPERIENCE -> Component.translatable("mco.configure.world.edit.subscreen.experience").withColor(-65536);
         default -> null;
      };
      if (warning != null) {
         this.layout.setHeaderHeight(41 + 9 + 8);
         this.warningHeader = header.addChild(new StringWidget(warning, this.font));
      }

      GridLayout contentGrid = this.layout.addToContents(new GridLayout().spacing(8));
      contentGrid.defaultCellSetting().alignHorizontallyCenter();
      GridLayout.RowHelper rowHelper = contentGrid.createRowHelper(2);
      EditBox worldNameEdit = new EditBox(this.minecraft.font, 0, 0, 150, 20, null, WORLD_NAME_EDIT_LABEL);
      worldNameEdit.setValue(this.worldName);
      worldNameEdit.setResponder(this::setWorldName);
      rowHelper.addChild(CommonLayouts.labeledElement(this.font, worldNameEdit, WORLD_NAME_EDIT_LABEL), 2);
      CycleButton<Difficulty> difficultyCycleButton = rowHelper.addChild(
         CycleButton.builder(Difficulty::getDisplayName, this.difficulty)
            .withValues(DIFFICULTIES)
            .create(0, 0, 150, 20, DIFFICULTY_BUTTON, (var1x, value) -> this.difficulty = value)
      );
      CycleButton<GameType> gameTypeCycleButton = rowHelper.addChild(
         CycleButton.builder(GameType::getShortDisplayName, this.gameMode)
            .withValues(GAME_MODES)
            .create(0, 0, 150, 20, GAME_MODE_BUTTON, (var1x, value) -> this.gameMode = value)
      );
      CycleButton<Boolean> forceGameModeButton = rowHelper.addChild(
         CycleButton.onOffBuilder(this.forceGameMode).create(0, 0, 150, 20, FORCE_GAME_MODE_BUTTON, (var1x, value) -> this.forceGameMode = value)
      );
      this.spawnProtectionButton = rowHelper.addChild(new RealmsSlotOptionsScreen.SettingsSlider(0, 0, 150, this.spawnProtection, 0.0F, 16.0F));
      if (this.worldType != RealmsServer.WorldType.NORMAL) {
         this.spawnProtectionButton.active = false;
         forceGameModeButton.active = false;
      }

      if (this.slot.isHardcore()) {
         difficultyCycleButton.active = false;
         gameTypeCycleButton.active = false;
         forceGameModeButton.active = false;
      }

      LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      footer.addChild(Button.builder(CommonComponents.GUI_CONTINUE, var1x -> this.saveSettings()).build());
      footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, var1x -> this.onClose()).build());
      this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
   }

   @Override
   public void onClose() {
      this.minecraft.gui.setScreen(this.parentScreen);
   }

   private static <T> T findByIndex(final List<T> values, final int index, final int defaultIndex) {
      return index >= 0 && index < values.size() ? values.get(index) : values.get(defaultIndex);
   }

   private static <T> int findIndex(final List<T> values, final T value, final int defaultIndex) {
      int result = values.indexOf(value);
      return result == -1 ? defaultIndex : result;
   }

   @Override
   public Component getNarrationMessage() {
      return this.warningHeader == null ? super.getNarrationMessage() : CommonComponents.joinForNarration(this.getTitle(), this.warningHeader.getMessage());
   }

   private void setWorldName(final String value) {
      if (value.equals(this.defaultSlotName)) {
         this.worldName = "";
      } else {
         this.worldName = value;
      }
   }

   private void saveSettings() {
      int difficultyId = findIndex(DIFFICULTIES, this.difficulty, 2);
      int gameModeId = findIndex(GAME_MODES, this.gameMode, 0);
      if (this.worldType != RealmsServer.WorldType.ADVENTUREMAP
         && this.worldType != RealmsServer.WorldType.EXPERIENCE
         && this.worldType != RealmsServer.WorldType.INSPIRATION) {
         this.parentScreen
            .saveSlotSettings(
               new RealmsSlot(
                  this.slot.slotId,
                  new RealmsWorldOptions(
                     this.spawnProtection,
                     difficultyId,
                     gameModeId,
                     this.forceGameMode,
                     this.worldName,
                     this.slot.options.version,
                     this.slot.options.compatibility
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
                     difficultyId,
                     gameModeId,
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

   private class SettingsSlider extends AbstractSliderButton {
      private final double minValue;
      private final double maxValue;

      public SettingsSlider(final int x, final int y, final int width, final int currentValue, final float minValue, final float maxValue) {
         super(x, y, width, 20, CommonComponents.EMPTY, 0.0);
         this.minValue = minValue;
         this.maxValue = maxValue;
         this.value = (Mth.clamp(currentValue, minValue, maxValue) - minValue) / (maxValue - minValue);
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
