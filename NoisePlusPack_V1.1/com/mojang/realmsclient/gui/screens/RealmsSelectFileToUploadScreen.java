package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.task.RealmCreationTask;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsSelectFileToUploadScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Component TITLE = Component.translatable("mco.upload.select.world.title");
   private static final Component UNABLE_TO_LOAD_WORLD = Component.translatable("selectWorld.unable_to_load");
   private final @Nullable RealmCreationTask realmCreationTask;
   private final RealmsResetWorldScreen lastScreen;
   private final long realmId;
   private final int slotId;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 49, 33);
   protected @Nullable EditBox searchBox;
   private @Nullable WorldSelectionList list;
   private @Nullable Button uploadButton;

   public RealmsSelectFileToUploadScreen(@Nullable RealmCreationTask p_334261_, long p_89498_, int p_89499_, RealmsResetWorldScreen p_89500_) {
      super(TITLE);
      this.realmCreationTask = p_334261_;
      this.lastScreen = p_89500_;
      this.realmId = p_89498_;
      this.slotId = p_89499_;
   }

   @Override
   public void init() {
      LinearLayout linearlayout = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
      linearlayout.defaultCellSetting().alignHorizontallyCenter();
      linearlayout.addChild(new StringWidget(this.title, this.font));
      this.searchBox = linearlayout.addChild(
         new EditBox(this.font, this.width / 2 - 100, 22, 200, 20, this.searchBox, Component.translatable("selectWorld.search"))
      );
      this.searchBox.setResponder(p_420605_ -> {
         if (this.list != null) {
            this.list.updateFilter(p_420605_);
         }
      });

      try {
         this.list = this.layout
            .addToContents(
               new WorldSelectionList.Builder(this.minecraft, this)
                  .width(this.width)
                  .height(this.layout.getContentHeight())
                  .filter(this.searchBox.getValue())
                  .oldList(this.list)
                  .uploadWorld()
                  .onEntrySelect(this::updateButtonState)
                  .onEntryInteract(this::upload)
                  .build()
            );
      } catch (Exception exception) {
         LOGGER.error("Couldn't load level list", exception);
         return;
      }

      LinearLayout linearlayout1 = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      linearlayout1.defaultCellSetting().alignHorizontallyCenter();
      this.uploadButton = linearlayout1.addChild(
         Button.builder(Component.translatable("mco.upload.button.name"), p_420606_ -> this.list.getSelectedOpt().ifPresent(this::upload)).build()
      );
      linearlayout1.addChild(Button.builder(CommonComponents.GUI_BACK, p_420607_ -> this.onClose()).build());
      this.updateButtonState(null);
      this.layout.visitWidgets(p_420604_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_420604_);
      });
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      if (this.list != null) {
         this.list.updateSize(this.width, this.layout);
      }

      this.layout.arrangeElements();
   }

   @Override
   protected void setInitialFocus() {
      this.setInitialFocus(this.searchBox);
   }

   private void updateButtonState(@Nullable LevelSummary p_423402_) {
      if (this.list != null && this.uploadButton != null) {
         this.uploadButton.active = this.list.getSelected() != null;
      }
   }

   private void upload(WorldSelectionList.WorldListEntry p_427938_) {
      this.minecraft.setScreen(new RealmsUploadScreen(this.realmCreationTask, this.realmId, this.slotId, this.lastScreen, p_427938_.getLevelSummary()));
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(this.getTitle(), this.createLabelNarration());
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }
}
