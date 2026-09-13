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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsResetWorldScreen extends RealmsScreen {
   static final Logger LOGGER = LogUtils.getLogger();
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
   WorldTemplatePaginatedList templates;
   WorldTemplatePaginatedList adventuremaps;
   WorldTemplatePaginatedList experiences;
   WorldTemplatePaginatedList inspirations;
   public final int slot;
   private final @Nullable RealmCreationTask realmCreationTask;
   private final Runnable resetWorldRunnable;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

   private RealmsResetWorldScreen(
      Screen p_310390_, RealmsServer p_311752_, int p_312610_, Component p_310665_, Component p_312883_, Component p_309644_, Runnable p_311131_
   ) {
      this(p_310390_, p_311752_, p_312610_, p_310665_, p_312883_, p_309644_, null, p_311131_);
   }

   public RealmsResetWorldScreen(
      Screen p_89329_,
      RealmsServer p_89330_,
      int p_299248_,
      Component p_299051_,
      Component p_300553_,
      Component p_298233_,
      @Nullable RealmCreationTask p_459068_,
      Runnable p_89331_
   ) {
      super(p_299051_);
      this.lastScreen = p_89329_;
      this.serverData = p_89330_;
      this.slot = p_299248_;
      this.subtitle = p_300553_;
      this.resetTaskTitle = p_298233_;
      this.realmCreationTask = p_459068_;
      this.resetWorldRunnable = p_89331_;
   }

   public static RealmsResetWorldScreen forNewRealm(Screen p_300926_, RealmsServer p_297918_, RealmCreationTask p_333820_, Runnable p_299910_) {
      return new RealmsResetWorldScreen(
         p_300926_, p_297918_, p_297918_.activeSlot, CREATE_REALM_TITLE, CREATE_REALM_SUBTITLE, CREATE_WORLD_RESET_TASK_TITLE, p_333820_, p_299910_
      );
   }

   public static RealmsResetWorldScreen forEmptySlot(Screen p_298871_, int p_300158_, RealmsServer p_300454_, Runnable p_298350_) {
      return new RealmsResetWorldScreen(p_298871_, p_300454_, p_300158_, CREATE_WORLD_TITLE, CREATE_WORLD_SUBTITLE, CREATE_WORLD_RESET_TASK_TITLE, p_298350_);
   }

   public static RealmsResetWorldScreen forResetSlot(Screen p_298755_, RealmsServer p_299132_, Runnable p_300942_) {
      return new RealmsResetWorldScreen(
         p_298755_, p_299132_, p_299132_.activeSlot, RESET_WORLD_TITLE, RESET_WORLD_SUBTITLE, RESET_WORLD_RESET_TASK_TITLE, p_300942_
      );
   }

   @Override
   public void init() {
      LinearLayout linearlayout = this.layout.addToHeader(LinearLayout.vertical());
      linearlayout.defaultCellSetting().padding(3);
      linearlayout.addChild(new StringWidget(this.title, this.font), LayoutSettings::alignHorizontallyCenter);
      linearlayout.addChild(new StringWidget(this.subtitle, this.font), LayoutSettings::alignHorizontallyCenter);
      (new Thread("Realms-reset-world-fetcher") {
         @Override
         public void run() {
            RealmsClient realmsclient = RealmsClient.getOrCreate();

            try {
               WorldTemplatePaginatedList worldtemplatepaginatedlist = realmsclient.fetchWorldTemplates(1, 10, RealmsServer.WorldType.NORMAL);
               WorldTemplatePaginatedList worldtemplatepaginatedlist1 = realmsclient.fetchWorldTemplates(1, 10, RealmsServer.WorldType.ADVENTUREMAP);
               WorldTemplatePaginatedList worldtemplatepaginatedlist2 = realmsclient.fetchWorldTemplates(1, 10, RealmsServer.WorldType.EXPERIENCE);
               WorldTemplatePaginatedList worldtemplatepaginatedlist3 = realmsclient.fetchWorldTemplates(1, 10, RealmsServer.WorldType.INSPIRATION);
               RealmsResetWorldScreen.this.minecraft.execute(() -> {
                  RealmsResetWorldScreen.this.templates = worldtemplatepaginatedlist;
                  RealmsResetWorldScreen.this.adventuremaps = worldtemplatepaginatedlist1;
                  RealmsResetWorldScreen.this.experiences = worldtemplatepaginatedlist2;
                  RealmsResetWorldScreen.this.inspirations = worldtemplatepaginatedlist3;
               });
            } catch (RealmsServiceException realmsserviceexception) {
               RealmsResetWorldScreen.LOGGER.error("Couldn't fetch templates in reset world", realmsserviceexception);
            }
         }
      }).start();
      GridLayout gridlayout = this.layout.addToContents(new GridLayout());
      GridLayout.RowHelper gridlayout$rowhelper = gridlayout.createRowHelper(3);
      gridlayout$rowhelper.defaultCellSetting().paddingHorizontal(16);
      gridlayout$rowhelper.addChild(
         new RealmsResetWorldScreen.FrameButton(
            this.minecraft.font,
            GENERATE_NEW_WORLD,
            NEW_WORLD_LOCATION,
            p_357566_ -> RealmsCreateWorldFlow.createWorld(this.minecraft, this.lastScreen, this, this.slot, this.serverData, this.realmCreationTask)
         )
      );
      gridlayout$rowhelper.addChild(
         new RealmsResetWorldScreen.FrameButton(
            this.minecraft.font,
            RealmsSelectFileToUploadScreen.TITLE,
            UPLOAD_LOCATION,
            p_325157_ -> this.minecraft.setScreen(new RealmsSelectFileToUploadScreen(this.realmCreationTask, this.serverData.id, this.slot, this))
         )
      );
      gridlayout$rowhelper.addChild(
         new RealmsResetWorldScreen.FrameButton(
            this.minecraft.font,
            WORLD_TEMPLATES_TITLE,
            SURVIVAL_SPAWN_LOCATION,
            p_296082_ -> this.minecraft
               .setScreen(
                  new RealmsSelectWorldTemplateScreen(WORLD_TEMPLATES_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.NORMAL, this.templates)
               )
         )
      );
      gridlayout$rowhelper.addChild(SpacerElement.height(16), 3);
      gridlayout$rowhelper.addChild(
         new RealmsResetWorldScreen.FrameButton(
            this.minecraft.font,
            ADVENTURES_TITLE,
            ADVENTURE_MAP_LOCATION,
            p_296077_ -> this.minecraft
               .setScreen(
                  new RealmsSelectWorldTemplateScreen(
                     ADVENTURES_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.ADVENTUREMAP, this.adventuremaps
                  )
               )
         )
      );
      gridlayout$rowhelper.addChild(
         new RealmsResetWorldScreen.FrameButton(
            this.minecraft.font,
            EXPERIENCES_TITLE,
            EXPERIENCE_LOCATION,
            p_296080_ -> this.minecraft
               .setScreen(
                  new RealmsSelectWorldTemplateScreen(EXPERIENCES_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.EXPERIENCE, this.experiences)
               )
         )
      );
      gridlayout$rowhelper.addChild(
         new RealmsResetWorldScreen.FrameButton(
            this.minecraft.font,
            INSPIRATION_TITLE,
            INSPIRATION_LOCATION,
            p_296083_ -> this.minecraft
               .setScreen(
                  new RealmsSelectWorldTemplateScreen(INSPIRATION_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.INSPIRATION, this.inspirations)
               )
         )
      );
      this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, p_296085_ -> this.onClose()).build());
      this.layout.visitWidgets(p_325156_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_325156_);
      });
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
      this.minecraft.setScreen(this.lastScreen);
   }

   private void templateSelectionCallback(@Nullable WorldTemplate p_167454_) {
      this.minecraft.setScreen(this);
      if (p_167454_ != null) {
         this.runResetTasks(new ResettingTemplateWorldTask(p_167454_, this.serverData.id, this.resetTaskTitle, this.resetWorldRunnable));
      }

      RealmsMainScreen.refreshServerList();
   }

   private void runResetTasks(LongRunningTask p_311937_) {
      List<LongRunningTask> list = new ArrayList<>();
      if (this.realmCreationTask != null) {
         list.add(this.realmCreationTask);
      }

      if (this.slot != this.serverData.activeSlot) {
         list.add(new SwitchSlotTask(this.serverData.id, this.slot, () -> {}));
      }

      list.add(p_311937_);
      this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, list.toArray(new LongRunningTask[0])));
   }

   @OnlyIn(Dist.CLIENT)
   class FrameButton extends Button {
      private static final Identifier SLOT_FRAME_SPRITE = Identifier.withDefaultNamespace("widget/slot_frame");
      private static final int FRAME_SIZE = 60;
      private static final int FRAME_WIDTH = 2;
      private static final int IMAGE_SIZE = 56;
      private final Identifier image;

      FrameButton(final Font p_328407_, final Component p_89441_, final Identifier p_460495_, final Button.OnPress p_89443_) {
         super(0, 0, 60, 69, p_89441_, p_89443_, DEFAULT_NARRATION);
         this.image = p_460495_;
      }

      @Override
      public void renderContents(GuiGraphics p_282595_, int p_282741_, int p_283560_, float p_281923_) {
         boolean flag = this.isHoveredOrFocused();
         int i = -1;
         if (flag) {
            i = ARGB.colorFromFloat(1.0F, 0.56F, 0.56F, 0.56F);
         }

         int j = this.getX();
         int k = this.getY();
         p_282595_.blit(RenderPipelines.GUI_TEXTURED, this.image, j + 2, k + 2, 0.0F, 0.0F, 56, 56, 56, 56, 56, 56, i);
         p_282595_.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_SPRITE, j, k, 60, 60, i);
         int l = flag ? -6250336 : -1;
         p_282595_.drawCenteredString(RealmsResetWorldScreen.this.font, this.getMessage(), j + 28, k - 14, l);
      }
   }
}
