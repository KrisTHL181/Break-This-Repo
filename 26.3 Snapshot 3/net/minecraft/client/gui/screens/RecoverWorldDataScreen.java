package net.minecraft.client.gui.screens;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.CommonLinks;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RecoverWorldDataScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SCREEN_SIDE_MARGIN = 25;
   private static final Component TITLE = Component.translatable("recover_world.title").withStyle(ChatFormatting.BOLD);
   private static final Component BUGTRACKER_BUTTON = Component.translatable("recover_world.bug_tracker");
   private static final Component RESTORE_BUTTON = Component.translatable("recover_world.restore");
   private static final Component NO_FALLBACK_TOOLTIP = Component.translatable("recover_world.no_fallback");
   private static final Component DONE_TITLE = Component.translatable("recover_world.done.title");
   private static final Component DONE_SUCCESS = Component.translatable("recover_world.done.success");
   private static final Component DONE_FAILED = Component.translatable("recover_world.done.failed");
   private static final Component NO_ISSUES = Component.translatable("recover_world.issue.none").withStyle(ChatFormatting.GREEN);
   private static final Component MISSING_FILE = Component.translatable("recover_world.issue.missing_file").withStyle(ChatFormatting.RED);
   private final BooleanConsumer callback;
   private final LinearLayout layout = LinearLayout.vertical().spacing(8);
   private final Component message;
   private final MultiLineTextWidget messageWidget;
   private final MultiLineTextWidget issuesWidget;
   private final LevelStorageSource.LevelStorageAccess storageAccess;

   public RecoverWorldDataScreen(final Minecraft minecraft, final BooleanConsumer callback, final LevelStorageSource.LevelStorageAccess storageAccess) {
      super(TITLE);
      this.callback = callback;
      this.message = Component.translatable("recover_world.message", Component.literal(storageAccess.getLevelId()).withStyle(ChatFormatting.GRAY));
      this.messageWidget = new MultiLineTextWidget(this.message, minecraft.font);
      this.storageAccess = storageAccess;
      Exception levelDatIssues = this.collectIssue(storageAccess, false);
      Exception levelDatOldIssues = this.collectIssue(storageAccess, true);
      Component issues = Component.empty()
         .append(this.buildInfo(storageAccess, false, levelDatIssues))
         .append("\n")
         .append(this.buildInfo(storageAccess, true, levelDatOldIssues));
      this.issuesWidget = new MultiLineTextWidget(issues, minecraft.font);
      boolean canRecover = levelDatIssues != null && levelDatOldIssues == null;
      this.layout.defaultCellSetting().alignHorizontallyCenter();
      this.layout.addChild(new StringWidget(this.title, minecraft.font));
      this.layout.addChild(this.messageWidget.setCentered(true));
      this.layout.addChild(this.issuesWidget);
      LinearLayout buttonGrid = LinearLayout.horizontal().spacing(5);
      buttonGrid.addChild(Button.builder(BUGTRACKER_BUTTON, ConfirmLinkScreen.confirmLink(this, CommonLinks.SNAPSHOT_BUGS_FEEDBACK)).size(120, 20).build());
      buttonGrid.addChild(
            Button.builder(RESTORE_BUTTON, button -> this.attemptRestore(minecraft))
               .size(120, 20)
               .tooltip(canRecover ? null : Tooltip.create(NO_FALLBACK_TOOLTIP))
               .build()
         )
         .active = canRecover;
      this.layout.addChild(buttonGrid);
      this.layout.addChild(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).size(120, 20).build());
      this.layout.visitWidgets(this::addRenderableWidget);
   }

   private void attemptRestore(final Minecraft minecraft) {
      Exception current = this.collectIssue(this.storageAccess, false);
      Exception old = this.collectIssue(this.storageAccess, true);
      if (current != null && old == null) {
         minecraft.setScreenAndShow(new GenericMessageScreen(Component.translatable("recover_world.restoring")));
         EditWorldScreen.makeBackupAndShowToast(this.storageAccess).thenAcceptAsync(var2x -> {
            if (this.storageAccess.restoreLevelDataFromOld()) {
               minecraft.gui.setScreen(new ConfirmScreen(this.callback, DONE_TITLE, DONE_SUCCESS, CommonComponents.GUI_CONTINUE, CommonComponents.GUI_BACK));
            } else {
               minecraft.gui.setScreen(new AlertScreen(() -> this.callback.accept(false), DONE_TITLE, DONE_FAILED));
            }
         }, minecraft);
      } else {
         LOGGER.error(
            "Failed to recover world, files not as expected. level.dat: {}, level.dat_old: {}",
            current != null ? current.getMessage() : "no issues",
            old != null ? old.getMessage() : "no issues"
         );
         minecraft.gui.setScreen(new AlertScreen(() -> this.callback.accept(false), DONE_TITLE, DONE_FAILED));
      }
   }

   private Component buildInfo(final LevelStorageSource.LevelStorageAccess access, final boolean fallback, final @Nullable Exception exception) {
      if (fallback && exception instanceof FileNotFoundException) {
         return Component.empty();
      }

      MutableComponent component = Component.empty();
      Instant timeStamp = access.getFileModificationTime(fallback);
      MutableComponent time = timeStamp != null
         ? Component.literal(WorldSelectionList.DATE_FORMAT.format(ZonedDateTime.ofInstant(timeStamp, ZoneId.systemDefault())))
         : Component.translatable("recover_world.state_entry.unknown");
      component.append(Component.translatable("recover_world.state_entry", time.withStyle(ChatFormatting.GRAY)));
      if (exception == null) {
         component.append(NO_ISSUES);
      } else if (exception instanceof FileNotFoundException) {
         component.append(MISSING_FILE);
      } else if (exception instanceof ReportedNbtException) {
         component.append(Component.literal(exception.getCause().toString()).withStyle(ChatFormatting.RED));
      } else {
         component.append(Component.literal(exception.toString()).withStyle(ChatFormatting.RED));
      }

      return component;
   }

   private @Nullable Exception collectIssue(final LevelStorageSource.LevelStorageAccess access, final boolean useFallback) {
      try {
         access.collectIssues(useFallback);
         return null;
      } catch (IOException | NbtException | ReportedNbtException e) {
         return e;
      }
   }

   @Override
   protected void init() {
      super.init();
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.issuesWidget.setMaxWidth(this.width - 50);
      this.messageWidget.setMaxWidth(this.width - 50);
      this.layout.arrangeElements();
      FrameLayout.centerInRectangle(this.layout, this.getRectangle());
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), this.message);
   }

   @Override
   public void onClose() {
      this.callback.accept(false);
   }
}
