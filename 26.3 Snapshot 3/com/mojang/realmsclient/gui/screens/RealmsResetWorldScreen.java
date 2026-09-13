package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.client.worldupload.RealmsCreateWorldFlow;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.dto.WorldTemplatePaginatedList;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.task.LongRunningTask;
import com.mojang.realmsclient.util.task.RealmCreationTask;
import com.mojang.realmsclient.util.task.ResettingTemplateWorldTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RealmsResetWorldScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component CREATE_REALM_TITLE = Component.translatable("mco.selectServer.create");
   private static final Component CREATE_REALM_SUBTITLE = Component.translatable("mco.selectServer.create.subtitle").withColor(-6250336);
   private static final Component CREATE_WORLD_TITLE = Component.translatable("mco.configure.world.switch.slot");
   private static final Component CREATE_WORLD_SUBTITLE = Component.translatable("mco.configure.world.switch.slot.subtitle").withColor(-6250336);
   private static final Component GENERATE_NEW_WORLD = Component.translatable("mco.reset.world.generate");
   private static final Component RESET_WORLD_TITLE = Component.translatable("mco.reset.world.title");
   private static final Component RESET_WORLD_SUBTITLE = Component.translatable("mco.reset.world.warning").withColor(-65536);
   public static final Component CREATE_WORLD_RESET_TASK_TITLE = Component.translatable("mco.create.world.reset.title");
   private static final Component RESET_WORLD_RESET_TASK_TITLE = Component.translatable("mco.reset.world.resetting.screen.title");
   private static final Component WORLD_TEMPLATES_TITLE = Component.translatable("mco.reset.world.template");
   private static final Component ADVENTURES_TITLE = Component.translatable("mco.reset.world.adventure");
   private static final Component EXPERIENCES_TITLE = Component.translatable("mco.reset.world.experience");
   private static final Component INSPIRATION_TITLE = Component.translatable("mco.reset.world.inspiration");
   private final Screen lastScreen;
   private final RealmsServer serverData;
   private final Component subtitle;
   private final Component resetTaskTitle;
   private static final Identifier UPLOAD_LOCATION = Identifier.withDefaultNamespace("textures/gui/realms/upload.png");
   private static final Identifier ADVENTURE_MAP_LOCATION = Identifier.withDefaultNamespace("textures/gui/realms/adventure.png");
   private static final Identifier SURVIVAL_SPAWN_LOCATION = Identifier.withDefaultNamespace("textures/gui/realms/survival_spawn.png");
   private static final Identifier NEW_WORLD_LOCATION = Identifier.withDefaultNamespace("textures/gui/realms/new_world.png");
   private static final Identifier EXPERIENCE_LOCATION = Identifier.withDefaultNamespace("textures/gui/realms/experience.png");
   private static final Identifier INSPIRATION_LOCATION = Identifier.withDefaultNamespace("textures/gui/realms/inspiration.png");
   private WorldTemplatePaginatedList templates;
   private WorldTemplatePaginatedList adventuremaps;
   private WorldTemplatePaginatedList experiences;
   private WorldTemplatePaginatedList inspirations;
   public final int slot;
   private final @Nullable RealmCreationTask realmCreationTask;
   private final Runnable resetWorldRunnable;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

   private RealmsResetWorldScreen(
      final Screen lastScreen,
      final RealmsServer serverData,
      final int slot,
      final Component title,
      final Component subtitle,
      final Component resetTaskTitle,
      final Runnable resetWorldRunnable
   ) {
      this(lastScreen, serverData, slot, title, subtitle, resetTaskTitle, null, resetWorldRunnable);
   }

   public RealmsResetWorldScreen(
      final Screen lastScreen,
      final RealmsServer serverData,
      final int slot,
      final Component title,
      final Component subtitle,
      final Component resetTaskTitle,
      final @Nullable RealmCreationTask realmCreationTask,
      final Runnable resetWorldRunnable
   ) {
      super(title);
      this.lastScreen = lastScreen;
      this.serverData = serverData;
      this.slot = slot;
      this.subtitle = subtitle;
      this.resetTaskTitle = resetTaskTitle;
      this.realmCreationTask = realmCreationTask;
      this.resetWorldRunnable = resetWorldRunnable;
   }

   public static RealmsResetWorldScreen forNewRealm(
      final Screen lastScreen, final RealmsServer serverData, final RealmCreationTask realmCreationTask, final Runnable resetWorldRunnable
   ) {
      return new RealmsResetWorldScreen(
         lastScreen,
         serverData,
         serverData.activeSlot,
         CREATE_REALM_TITLE,
         CREATE_REALM_SUBTITLE,
         CREATE_WORLD_RESET_TASK_TITLE,
         realmCreationTask,
         resetWorldRunnable
      );
   }

   public static RealmsResetWorldScreen forEmptySlot(final Screen lastScreen, final int slot, final RealmsServer serverData, final Runnable resetWorldRunnable) {
      return new RealmsResetWorldScreen(
         lastScreen, serverData, slot, CREATE_WORLD_TITLE, CREATE_WORLD_SUBTITLE, CREATE_WORLD_RESET_TASK_TITLE, resetWorldRunnable
      );
   }

   public static RealmsResetWorldScreen forResetSlot(final Screen lastScreen, final RealmsServer serverData, final Runnable resetWorldRunnable) {
      return new RealmsResetWorldScreen(
         lastScreen, serverData, serverData.activeSlot, RESET_WORLD_TITLE, RESET_WORLD_SUBTITLE, RESET_WORLD_RESET_TASK_TITLE, resetWorldRunnable
      );
   }

   @Override
   public void init() {
      LinearLayout header = this.layout.addToHeader(LinearLayout.vertical());
      header.defaultCellSetting().padding(9 / 3);
      header.addChild(new StringWidget(this.title, this.font), LayoutSettings::alignHorizontallyCenter);
      header.addChild(new StringWidget(this.subtitle, this.font), LayoutSettings::alignHorizontallyCenter);
      (new Thread("Realms-reset-world-fetcher") {
         @Override
         public void run() {
            RealmsClient client = RealmsClient.getOrCreate();

            try {
               WorldTemplatePaginatedList templates = client.fetchWorldTemplates(1, 10, RealmsServer.WorldType.NORMAL);
               WorldTemplatePaginatedList adventuremaps = client.fetchWorldTemplates(1, 10, RealmsServer.WorldType.ADVENTUREMAP);
               WorldTemplatePaginatedList experiences = client.fetchWorldTemplates(1, 10, RealmsServer.WorldType.EXPERIENCE);
               WorldTemplatePaginatedList inspirations = client.fetchWorldTemplates(1, 10, RealmsServer.WorldType.INSPIRATION);
               RealmsResetWorldScreen.this.minecraft.execute(() -> {
                  RealmsResetWorldScreen.this.templates = templates;
                  RealmsResetWorldScreen.this.adventuremaps = adventuremaps;
                  RealmsResetWorldScreen.this.experiences = experiences;
                  RealmsResetWorldScreen.this.inspirations = inspirations;
               });
            } catch (RealmsServiceException e) {
               RealmsResetWorldScreen.LOGGER.error("Couldn't fetch templates in reset world", e);
            }
         }
      }).start();
      GridLayout grid = this.layout.addToContents(new GridLayout());
      GridLayout.RowHelper helper = grid.createRowHelper(3);
      helper.defaultCellSetting().paddingHorizontal(16);
      helper.addChild(
         new RealmsResetWorldScreen.FrameButton(
            this.minecraft.font,
            GENERATE_NEW_WORLD,
            NEW_WORLD_LOCATION,
            button -> RealmsCreateWorldFlow.createWorld(this.minecraft, this.lastScreen, this, this.slot, this.serverData, this.realmCreationTask)
         )
      );
      helper.addChild(
         new RealmsResetWorldScreen.FrameButton(
            this.minecraft.font,
            RealmsSelectFileToUploadScreen.TITLE,
            UPLOAD_LOCATION,
            var1x -> this.minecraft.gui.setScreen(new RealmsSelectFileToUploadScreen(this.realmCreationTask, this.serverData.id, this.slot, this))
         )
      );
      helper.addChild(
         new RealmsResetWorldScreen.FrameButton(
            this.minecraft.font,
            WORLD_TEMPLATES_TITLE,
            SURVIVAL_SPAWN_LOCATION,
            var1x -> this.minecraft
               .gui
               .setScreen(
                  new RealmsSelectWorldTemplateScreen(WORLD_TEMPLATES_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.NORMAL, this.templates)
               )
         )
      );
      helper.addChild(SpacerElement.height(16), 3);
      helper.addChild(
         new RealmsResetWorldScreen.FrameButton(
            this.minecraft.font,
            ADVENTURES_TITLE,
            ADVENTURE_MAP_LOCATION,
            var1x -> this.minecraft
               .gui
               .setScreen(
                  new RealmsSelectWorldTemplateScreen(
                     ADVENTURES_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.ADVENTUREMAP, this.adventuremaps
                  )
               )
         )
      );
      helper.addChild(
         new RealmsResetWorldScreen.FrameButton(
            this.minecraft.font,
            EXPERIENCES_TITLE,
            EXPERIENCE_LOCATION,
            var1x -> this.minecraft
               .gui
               .setScreen(
                  new RealmsSelectWorldTemplateScreen(EXPERIENCES_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.EXPERIENCE, this.experiences)
               )
         )
      );
      helper.addChild(
         new RealmsResetWorldScreen.FrameButton(
            this.minecraft.font,
            INSPIRATION_TITLE,
            INSPIRATION_LOCATION,
            var1x -> this.minecraft
               .gui
               .setScreen(
                  new RealmsSelectWorldTemplateScreen(INSPIRATION_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.INSPIRATION, this.inspirations)
               )
         )
      );
      this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).build());
      this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(this.getTitle(), this.subtitle);
   }

   @Override
   public void onClose() {
      this.minecraft.gui.setScreen(this.lastScreen);
   }

   private void templateSelectionCallback(final @Nullable WorldTemplate template) {
      this.minecraft.gui.setScreen(this);
      if (template != null) {
         this.runResetTasks(new ResettingTemplateWorldTask(template, this.serverData.id, this.resetTaskTitle, this.resetWorldRunnable));
      }

      RealmsMainScreen.refreshServerList();
   }

   private void runResetTasks(final LongRunningTask resetTask) {
      List<LongRunningTask> tasks = new ArrayList<>();
      if (this.realmCreationTask != null) {
         tasks.add(this.realmCreationTask);
      }

      if (this.slot != this.serverData.activeSlot) {
         tasks.add(new SwitchSlotTask(this.serverData.id, this.slot, () -> {}));
      }

      tasks.add(resetTask);
      this.minecraft.gui.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, tasks.toArray(new LongRunningTask[0])));
   }

   private class FrameButton extends Button {
      private static final Identifier SLOT_FRAME_SPRITE = Identifier.withDefaultNamespace("widget/slot_frame");
      private static final int FRAME_SIZE = 60;
      private static final int FRAME_WIDTH = 2;
      private static final int IMAGE_SIZE = 56;
      private final Identifier image;

      private FrameButton(final Font font, final Component text, final Identifier image, final Button.OnPress onPress) {
         super(0, 0, 60, 60 + 9, text, onPress, DEFAULT_NARRATION);
         this.image = image;
      }

      @Override
      public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
         boolean hoveredOrFocused = this.isHoveredOrFocused();
         int color = -1;
         if (hoveredOrFocused) {
            color = ARGB.colorFromFloat(1.0F, 0.56F, 0.56F, 0.56F);
         }

         int x = this.getX();
         int y = this.getY();
         graphics.blit(RenderPipelines.GUI_TEXTURED, this.image, x + 2, y + 2, 0.0F, 0.0F, 56, 56, 56, 56, 56, 56, color);
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_SPRITE, x, y, 60, 60, color);
         int textColor = hoveredOrFocused ? -6250336 : -1;
         graphics.centeredText(RealmsResetWorldScreen.this.font, this.getMessage(), x + 28, y - 14, textColor);
      }
   }
}
