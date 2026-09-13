package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.SelectableEntry;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ErrorScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class WorldSelectionList extends ObjectSelectionList<WorldSelectionList.Entry> {
   public static final DateTimeFormatter DATE_FORMAT = Util.localizedDateFormatter(FormatStyle.SHORT);
   private static final Identifier ERROR_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("world_list/error_highlighted");
   private static final Identifier ERROR_SPRITE = Identifier.withDefaultNamespace("world_list/error");
   private static final Identifier MARKED_JOIN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("world_list/marked_join_highlighted");
   private static final Identifier MARKED_JOIN_SPRITE = Identifier.withDefaultNamespace("world_list/marked_join");
   private static final Identifier WARNING_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("world_list/warning_highlighted");
   private static final Identifier WARNING_SPRITE = Identifier.withDefaultNamespace("world_list/warning");
   private static final Identifier JOIN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("world_list/join_highlighted");
   private static final Identifier JOIN_SPRITE = Identifier.withDefaultNamespace("world_list/join");
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component FROM_NEWER_TOOLTIP_1 = Component.translatable("selectWorld.tooltip.fromNewerVersion1").withStyle(ChatFormatting.RED);
   private static final Component FROM_NEWER_TOOLTIP_2 = Component.translatable("selectWorld.tooltip.fromNewerVersion2").withStyle(ChatFormatting.RED);
   private static final Component SNAPSHOT_TOOLTIP_1 = Component.translatable("selectWorld.tooltip.snapshot1").withStyle(ChatFormatting.GOLD);
   private static final Component SNAPSHOT_TOOLTIP_2 = Component.translatable("selectWorld.tooltip.snapshot2").withStyle(ChatFormatting.GOLD);
   private static final Component WORLD_LOCKED_TOOLTIP = Component.translatable("selectWorld.locked").withStyle(ChatFormatting.RED);
   private static final Component WORLD_REQUIRES_CONVERSION = Component.translatable("selectWorld.conversion.tooltip").withStyle(ChatFormatting.RED);
   private static final Component INCOMPATIBLE_VERSION_TOOLTIP = Component.translatable("selectWorld.incompatible.tooltip").withStyle(ChatFormatting.RED);
   private static final Component WORLD_EXPERIMENTAL = Component.translatable("selectWorld.experimental");
   private final Screen screen;
   private CompletableFuture<List<LevelSummary>> pendingLevels;
   private @Nullable List<LevelSummary> currentlyDisplayedLevels;
   private final WorldSelectionList.LoadingHeader loadingHeader;
   private final WorldSelectionList.EntryType entryType;
   private String filter;
   private boolean hasPolled;
   private final @Nullable Consumer<LevelSummary> onEntrySelect;
   private final @Nullable Consumer<WorldSelectionList.WorldListEntry> onEntryInteract;

   private WorldSelectionList(
      final Screen screen,
      final Minecraft minecraft,
      final int width,
      final int height,
      final String filter,
      final @Nullable WorldSelectionList oldList,
      final @Nullable Consumer<LevelSummary> onEntrySelect,
      final @Nullable Consumer<WorldSelectionList.WorldListEntry> onEntryInteract,
      final WorldSelectionList.EntryType entryType
   ) {
      super(minecraft, width, height, 0, 36);
      this.screen = screen;
      this.loadingHeader = new WorldSelectionList.LoadingHeader(minecraft);
      this.filter = filter;
      this.onEntrySelect = onEntrySelect;
      this.onEntryInteract = onEntryInteract;
      this.entryType = entryType;
      if (oldList != null) {
         this.pendingLevels = oldList.pendingLevels;
      } else {
         this.pendingLevels = this.loadLevels();
      }

      this.addEntry(this.loadingHeader);
      this.handleNewLevels(this.pollLevelsIgnoreErrors());
   }

   @Override
   protected void clearEntries() {
      this.children().forEach(WorldSelectionList.Entry::close);
      super.clearEntries();
   }

   private @Nullable List<LevelSummary> pollLevelsIgnoreErrors() {
      try {
         List<LevelSummary> completedLevels = this.pendingLevels.getNow(null);
         if (this.entryType == WorldSelectionList.EntryType.UPLOAD_WORLD) {
            if (completedLevels == null || this.hasPolled) {
               return null;
            }

            this.hasPolled = true;
            completedLevels = completedLevels.stream().filter(LevelSummary::canUpload).toList();
         }

         return completedLevels;
      } catch (CompletionException | CancellationException ignore) {
         return null;
      }
   }

   public void reloadWorldList() {
      this.pendingLevels = this.loadLevels();
   }

   @Override
   public void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      List<LevelSummary> newLevels = this.pollLevelsIgnoreErrors();
      if (newLevels != this.currentlyDisplayedLevels) {
         this.handleNewLevels(newLevels);
      }

      super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
   }

   private void handleNewLevels(final @Nullable List<LevelSummary> levels) {
      if (levels != null) {
         if (levels.isEmpty()) {
            switch (this.entryType) {
               case SINGLEPLAYER:
                  CreateWorldScreen.openFresh(this.minecraft, () -> this.minecraft.gui.setScreen(null));
                  break;
               case UPLOAD_WORLD:
                  this.clearEntries();
                  this.addEntry(new WorldSelectionList.NoWorldsEntry(Component.translatable("mco.upload.select.world.none"), this.screen.getFont()));
            }
         } else {
            this.fillLevels(this.filter, levels);
            this.currentlyDisplayedLevels = levels;
         }
      }
   }

   public void updateFilter(final String newFilter) {
      if (this.currentlyDisplayedLevels != null && !newFilter.equals(this.filter)) {
         this.fillLevels(newFilter, this.currentlyDisplayedLevels);
      }

      this.filter = newFilter;
   }

   private CompletableFuture<List<LevelSummary>> loadLevels() {
      LevelStorageSource.LevelCandidates levelCandidates;
      try {
         levelCandidates = this.minecraft.getLevelSource().findLevelCandidates();
      } catch (LevelStorageException e) {
         LOGGER.error("Couldn't load level list", e);
         this.handleLevelLoadFailure(e.getMessageComponent());
         return CompletableFuture.failedFuture(e);
      }

      return this.minecraft.getLevelSource().loadLevelSummaries(levelCandidates).exceptionally(throwable -> {
         this.minecraft.delayCrash(CrashReport.forThrowable(throwable, "Couldn't load level list"));
         return List.of();
      });
   }

   private void fillLevels(final String filter, final List<LevelSummary> levels) {
      List<WorldSelectionList.Entry> worldEntries = new ArrayList<>();
      Optional<WorldSelectionList.WorldListEntry> selectedOpt = this.getSelectedOpt();
      WorldSelectionList.WorldListEntry entryToSelect = null;

      for (LevelSummary level : levels.stream().filter(levelx -> this.filterAccepts(filter.toLowerCase(Locale.ROOT), levelx)).toList()) {
         WorldSelectionList.WorldListEntry worldListEntry = new WorldSelectionList.WorldListEntry(this, level);
         if (selectedOpt.isPresent() && selectedOpt.get().getLevelSummary().getLevelId().equals(worldListEntry.getLevelSummary().getLevelId())) {
            entryToSelect = worldListEntry;
         }

         worldEntries.add(worldListEntry);
      }

      this.removeEntries(this.children().stream().filter(entry -> !worldEntries.contains(entry)).toList());
      worldEntries.forEach(entry -> {
         if (!this.children().contains(entry)) {
            this.addEntry(entry);
         }
      });
      this.setSelected(entryToSelect);
      this.notifyListUpdated();
   }

   private boolean filterAccepts(final String filter, final LevelSummary level) {
      return level.getLevelName().toLowerCase(Locale.ROOT).contains(filter) || level.getLevelId().toLowerCase(Locale.ROOT).contains(filter);
   }

   private void notifyListUpdated() {
      this.refreshScrollAmount();
      this.screen.triggerImmediateNarration(true);
   }

   private void handleLevelLoadFailure(final Component message) {
      this.minecraft.gui.setScreen(new ErrorScreen(Component.translatable("selectWorld.unable_to_load"), message));
   }

   @Override
   public int getRowWidth() {
      return 270;
   }

   public void setSelected(final WorldSelectionList.@Nullable Entry selected) {
      super.setSelected(selected);
      if (this.onEntrySelect != null) {
         this.onEntrySelect.accept(selected instanceof WorldSelectionList.WorldListEntry entry ? entry.summary : null);
      }
   }

   public Optional<WorldSelectionList.WorldListEntry> getSelectedOpt() {
      WorldSelectionList.Entry selected = this.getSelected();
      return selected instanceof WorldSelectionList.WorldListEntry worldEntry ? Optional.of(worldEntry) : Optional.empty();
   }

   public void returnToScreen() {
      this.reloadWorldList();
      this.minecraft.gui.setScreen(this.screen);
   }

   public Screen getScreen() {
      return this.screen;
   }

   @Override
   public void updateWidgetNarration(final NarrationElementOutput output) {
      if (this.children().contains(this.loadingHeader)) {
         this.loadingHeader.updateNarration(output);
      } else {
         super.updateWidgetNarration(output);
      }
   }

   public static class Builder {
      private final Minecraft minecraft;
      private final Screen screen;
      private int width;
      private int height;
      private String filter = "";
      private WorldSelectionList.EntryType type = WorldSelectionList.EntryType.SINGLEPLAYER;
      private @Nullable WorldSelectionList oldList = null;
      private @Nullable Consumer<LevelSummary> onEntrySelect = null;
      private @Nullable Consumer<WorldSelectionList.WorldListEntry> onEntryInteract = null;

      public Builder(final Minecraft minecraft, final Screen screen) {
         this.minecraft = minecraft;
         this.screen = screen;
      }

      public WorldSelectionList.Builder width(final int width) {
         this.width = width;
         return this;
      }

      public WorldSelectionList.Builder height(final int height) {
         this.height = height;
         return this;
      }

      public WorldSelectionList.Builder filter(final String filter) {
         this.filter = filter;
         return this;
      }

      public WorldSelectionList.Builder oldList(final @Nullable WorldSelectionList oldList) {
         this.oldList = oldList;
         return this;
      }

      public WorldSelectionList.Builder onEntrySelect(final Consumer<LevelSummary> onEntrySelect) {
         this.onEntrySelect = onEntrySelect;
         return this;
      }

      public WorldSelectionList.Builder onEntryInteract(final Consumer<WorldSelectionList.WorldListEntry> onEntryInteract) {
         this.onEntryInteract = onEntryInteract;
         return this;
      }

      public WorldSelectionList.Builder uploadWorld() {
         this.type = WorldSelectionList.EntryType.UPLOAD_WORLD;
         return this;
      }

      public WorldSelectionList build() {
         return new WorldSelectionList(
            this.screen, this.minecraft, this.width, this.height, this.filter, this.oldList, this.onEntrySelect, this.onEntryInteract, this.type
         );
      }
   }

   public abstract static class Entry extends ObjectSelectionList.Entry<WorldSelectionList.Entry> implements AutoCloseable {
      @Override
      public void close() {
      }

      public @Nullable LevelSummary getLevelSummary() {
         return null;
      }
   }

   public enum EntryType {
      SINGLEPLAYER,
      UPLOAD_WORLD;
   }

   public static class LoadingHeader extends WorldSelectionList.Entry {
      private static final Component LOADING_LABEL = Component.translatable("selectWorld.loading_list");
      private final Minecraft minecraft;

      public LoadingHeader(final Minecraft minecraft) {
         this.minecraft = minecraft;
      }

      @Override
      public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
         int labelX = (this.minecraft.gui.screen().width - this.minecraft.font.width(LOADING_LABEL)) / 2;
         int labelY = this.getContentY() + (this.getContentHeight() - 9) / 2;
         graphics.text(this.minecraft.font, LOADING_LABEL, labelX, labelY, -1);
         String dots = LoadingDotsText.get(Util.getMillis());
         int dotsX = (this.minecraft.gui.screen().width - this.minecraft.font.width(dots)) / 2;
         int dotsY = labelY + 9;
         graphics.text(this.minecraft.font, dots, dotsX, dotsY, -8355712);
      }

      @Override
      public Component getNarration() {
         return LOADING_LABEL;
      }
   }

   public static final class NoWorldsEntry extends WorldSelectionList.Entry {
      private final StringWidget stringWidget;

      public NoWorldsEntry(final Component component, final Font font) {
         this.stringWidget = new StringWidget(component, font);
      }

      @Override
      public Component getNarration() {
         return this.stringWidget.getMessage();
      }

      @Override
      public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
         this.stringWidget
            .setPosition(this.getContentXMiddle() - this.stringWidget.getWidth() / 2, this.getContentYMiddle() - this.stringWidget.getHeight() / 2);
         this.stringWidget.extractRenderState(graphics, mouseX, mouseY, a);
      }
   }

   public final class WorldListEntry extends WorldSelectionList.Entry implements SelectableEntry {
      private static final int ICON_SIZE = 32;
      private final WorldSelectionList list;
      private final Minecraft minecraft;
      private final Screen screen;
      private final LevelSummary summary;
      private final FaviconTexture icon;
      private final StringWidget worldNameText;
      private final StringWidget idAndLastPlayedText;
      private final StringWidget infoText;
      private @Nullable Path iconFile;

      public WorldListEntry(final WorldSelectionList list, final LevelSummary summary) {
         this.list = list;
         this.minecraft = list.minecraft;
         this.screen = list.getScreen();
         this.summary = summary;
         this.icon = FaviconTexture.forWorld(this.minecraft.getTextureManager(), summary.getLevelId());
         this.iconFile = summary.getIcon();
         int maxTextWidth = list.getRowWidth() - this.getTextX() - 2;
         Component worldNameComponent = Component.literal(summary.getLevelName());
         this.worldNameText = new StringWidget(worldNameComponent, this.minecraft.font);
         this.worldNameText.setMaxWidth(maxTextWidth);
         if (this.minecraft.font.width(worldNameComponent) > maxTextWidth) {
            this.worldNameText.setTooltip(Tooltip.create(worldNameComponent));
         }

         String levelIdAndDate = summary.getLevelId();
         long lastPlayed = summary.getLastPlayed();
         if (lastPlayed != -1L) {
            ZonedDateTime lastPlayedTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastPlayed), ZoneId.systemDefault());
            levelIdAndDate = levelIdAndDate + " (" + WorldSelectionList.DATE_FORMAT.format(lastPlayedTime) + ")";
         }

         Component levelIdAndDateComponent = Component.literal(levelIdAndDate).withColor(-8355712);
         this.idAndLastPlayedText = new StringWidget(levelIdAndDateComponent, this.minecraft.font);
         this.idAndLastPlayedText.setMaxWidth(maxTextWidth);
         if (this.minecraft.font.width(levelIdAndDate) > maxTextWidth) {
            this.idAndLastPlayedText.setTooltip(Tooltip.create(levelIdAndDateComponent));
         }

         Component info = ComponentUtils.mergeStyles(summary.getInfo(), Style.EMPTY.withColor(-8355712));
         this.infoText = new StringWidget(info, this.minecraft.font);
         this.infoText.setMaxWidth(maxTextWidth);
         if (this.minecraft.font.width(info) > maxTextWidth) {
            this.infoText.setTooltip(Tooltip.create(info));
         }

         this.validateIconFile();
         this.loadIcon();
      }

      private void validateIconFile() {
         if (this.iconFile != null) {
            try {
               BasicFileAttributes attributes = Files.readAttributes(this.iconFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
               if (attributes.isSymbolicLink()) {
                  List<ForbiddenSymlinkInfo> issues = this.minecraft.directoryValidator().validateSymlink(this.iconFile);
                  if (!issues.isEmpty()) {
                     WorldSelectionList.LOGGER.warn("{}", ContentValidationException.getMessage(this.iconFile, issues));
                     this.iconFile = null;
                  } else {
                     attributes = Files.readAttributes(this.iconFile, BasicFileAttributes.class);
                  }
               }

               if (!attributes.isRegularFile()) {
                  this.iconFile = null;
               }
            } catch (NoSuchFileException e) {
               this.iconFile = null;
            } catch (IOException e) {
               WorldSelectionList.LOGGER.error("could not validate symlink", e);
               this.iconFile = null;
            }
         }
      }

      @Override
      public Component getNarration() {
         Component entryNarration = Component.translatable(
            "narrator.select.world_info", this.summary.getLevelName(), Component.translationArg(new Date(this.summary.getLastPlayed())), this.summary.getInfo()
         );
         if (this.summary.isLocked()) {
            entryNarration = CommonComponents.joinForNarration(entryNarration, WorldSelectionList.WORLD_LOCKED_TOOLTIP);
         }

         if (this.summary.isExperimental()) {
            entryNarration = CommonComponents.joinForNarration(entryNarration, WorldSelectionList.WORLD_EXPERIMENTAL);
         }

         return Component.translatable("narrator.select", entryNarration);
      }

      @Override
      public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
         int textX = this.getTextX();
         this.worldNameText.setPosition(textX, this.getContentY() + 1);
         this.worldNameText.extractRenderState(graphics, mouseX, mouseY, a);
         this.idAndLastPlayedText.setPosition(textX, this.getContentY() + 9 + 3);
         this.idAndLastPlayedText.extractRenderState(graphics, mouseX, mouseY, a);
         this.infoText.setPosition(textX, this.getContentY() + 9 + 9 + 3);
         this.infoText.extractRenderState(graphics, mouseX, mouseY, a);
         graphics.blit(RenderPipelines.GUI_TEXTURED, this.icon.textureLocation(), this.getContentX(), this.getContentY(), 0.0F, 0.0F, 32, 32, 32, 32);
         if (this.list.entryType == WorldSelectionList.EntryType.SINGLEPLAYER && hovered) {
            graphics.fill(this.getContentX(), this.getContentY(), this.getContentX() + 32, this.getContentY() + 32, -1601138544);
            int relX = mouseX - this.getContentX();
            int relY = mouseY - this.getContentY();
            boolean isOverIcon = this.mouseOverIcon(relX, relY, 32);
            Identifier joinSprite = isOverIcon ? WorldSelectionList.JOIN_HIGHLIGHTED_SPRITE : WorldSelectionList.JOIN_SPRITE;
            Identifier warningSprite = isOverIcon ? WorldSelectionList.WARNING_HIGHLIGHTED_SPRITE : WorldSelectionList.WARNING_SPRITE;
            Identifier errorSprite = isOverIcon ? WorldSelectionList.ERROR_HIGHLIGHTED_SPRITE : WorldSelectionList.ERROR_SPRITE;
            Identifier joinWithErrorSprite = isOverIcon ? WorldSelectionList.MARKED_JOIN_HIGHLIGHTED_SPRITE : WorldSelectionList.MARKED_JOIN_SPRITE;
            if (this.summary instanceof LevelSummary.SymlinkLevelSummary || this.summary instanceof LevelSummary.CorruptedLevelSummary) {
               graphics.blitSprite(RenderPipelines.GUI_TEXTURED, errorSprite, this.getContentX(), this.getContentY(), 32, 32);
               graphics.blitSprite(RenderPipelines.GUI_TEXTURED, joinWithErrorSprite, this.getContentX(), this.getContentY(), 32, 32);
               return;
            }

            if (this.summary.isLocked()) {
               graphics.blitSprite(RenderPipelines.GUI_TEXTURED, errorSprite, this.getContentX(), this.getContentY(), 32, 32);
               if (isOverIcon) {
                  graphics.setTooltipForNextFrame(this.minecraft.font.split(WorldSelectionList.WORLD_LOCKED_TOOLTIP, 175), mouseX, mouseY);
               }
            } else if (this.summary.requiresManualConversion()) {
               graphics.blitSprite(RenderPipelines.GUI_TEXTURED, errorSprite, this.getContentX(), this.getContentY(), 32, 32);
               if (isOverIcon) {
                  graphics.setTooltipForNextFrame(this.minecraft.font.split(WorldSelectionList.WORLD_REQUIRES_CONVERSION, 175), mouseX, mouseY);
               }
            } else if (!this.summary.isCompatible()) {
               graphics.blitSprite(RenderPipelines.GUI_TEXTURED, errorSprite, this.getContentX(), this.getContentY(), 32, 32);
               if (isOverIcon) {
                  graphics.setTooltipForNextFrame(this.minecraft.font.split(WorldSelectionList.INCOMPATIBLE_VERSION_TOOLTIP, 175), mouseX, mouseY);
               }
            } else if (this.summary.shouldBackup()) {
               graphics.blitSprite(RenderPipelines.GUI_TEXTURED, joinWithErrorSprite, this.getContentX(), this.getContentY(), 32, 32);
               if (this.summary.isDowngrade()) {
                  graphics.blitSprite(RenderPipelines.GUI_TEXTURED, errorSprite, this.getContentX(), this.getContentY(), 32, 32);
                  if (isOverIcon) {
                     graphics.setTooltipForNextFrame(
                        ImmutableList.of(
                           WorldSelectionList.FROM_NEWER_TOOLTIP_1.getVisualOrderText(), WorldSelectionList.FROM_NEWER_TOOLTIP_2.getVisualOrderText()
                        ),
                        mouseX,
                        mouseY
                     );
                  }
               } else if (!SharedConstants.getCurrentVersion().stable()) {
                  graphics.blitSprite(RenderPipelines.GUI_TEXTURED, warningSprite, this.getContentX(), this.getContentY(), 32, 32);
                  if (isOverIcon) {
                     graphics.setTooltipForNextFrame(
                        ImmutableList.of(WorldSelectionList.SNAPSHOT_TOOLTIP_1.getVisualOrderText(), WorldSelectionList.SNAPSHOT_TOOLTIP_2.getVisualOrderText()),
                        mouseX,
                        mouseY
                     );
                  }
               }

               if (isOverIcon) {
                  WorldSelectionList.this.handleCursor(graphics);
               }
            } else {
               graphics.blitSprite(RenderPipelines.GUI_TEXTURED, joinSprite, this.getContentX(), this.getContentY(), 32, 32);
               if (isOverIcon) {
                  WorldSelectionList.this.handleCursor(graphics);
               }
            }
         }
      }

      private int getTextX() {
         return this.getContentX() + 32 + 3;
      }

      @Override
      public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
         if (this.canInteract()) {
            int relX = (int)event.x() - this.getContentX();
            int relY = (int)event.y() - this.getContentY();
            if (doubleClick || this.mouseOverIcon(relX, relY, 32) && this.list.entryType == WorldSelectionList.EntryType.SINGLEPLAYER) {
               this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
               Consumer<WorldSelectionList.WorldListEntry> onEntryInteract = this.list.onEntryInteract;
               if (onEntryInteract != null) {
                  onEntryInteract.accept(this);
                  return true;
               }
            }
         }

         return super.mouseClicked(event, doubleClick);
      }

      @Override
      public boolean keyPressed(final KeyEvent event) {
         if (event.isSelection() && this.canInteract()) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            Consumer<WorldSelectionList.WorldListEntry> onEntryInteract = this.list.onEntryInteract;
            if (onEntryInteract != null) {
               onEntryInteract.accept(this);
               return true;
            }
         }

         return super.keyPressed(event);
      }

      public boolean canInteract() {
         return this.summary.primaryActionActive() || this.list.entryType == WorldSelectionList.EntryType.UPLOAD_WORLD;
      }

      public void joinWorld() {
         if (this.summary.primaryActionActive()) {
            if (this.summary instanceof LevelSummary.SymlinkLevelSummary) {
               this.minecraft.gui.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.gui.setScreen(this.screen)));
            } else {
               this.minecraft.createWorldOpenFlows().openWorld(this.summary.getLevelId(), this.list::returnToScreen);
            }
         }
      }

      public void deleteWorld() {
         this.minecraft
            .gui
            .setScreen(
               new ConfirmScreen(
                  result -> {
                     if (result) {
                        this.minecraft.gui.setScreen(new ProgressScreen(true));
                        this.doDeleteWorld();
                     }

                     this.list.returnToScreen();
                  },
                  Component.translatable("selectWorld.deleteQuestion"),
                  Component.translatable("selectWorld.deleteWarning", this.summary.getLevelName()),
                  Component.translatable("selectWorld.deleteButton"),
                  CommonComponents.GUI_CANCEL
               )
            );
      }

      public void doDeleteWorld() {
         LevelStorageSource levelSource = this.minecraft.getLevelSource();
         String levelId = this.summary.getLevelId();

         try (LevelStorageSource.LevelStorageAccess access = levelSource.createAccess(levelId)) {
            access.deleteLevel();
         } catch (IOException e) {
            SystemToast.onWorldDeleteFailure(this.minecraft, levelId);
            WorldSelectionList.LOGGER.error("Failed to delete world {}", levelId, e);
         }
      }

      public void editWorld() {
         this.queueLoadScreen();
         String levelId = this.summary.getLevelId();

         LevelStorageSource.LevelStorageAccess access;
         try {
            access = this.minecraft.getLevelSource().validateAndCreateAccess(levelId);
         } catch (IOException e) {
            SystemToast.onWorldAccessFailure(this.minecraft, levelId);
            WorldSelectionList.LOGGER.error("Failed to access level {}", levelId, e);
            this.list.reloadWorldList();
            return;
         } catch (ContentValidationException e) {
            WorldSelectionList.LOGGER.warn("{}", e.getMessage());
            this.minecraft.gui.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.gui.setScreen(this.screen)));
            return;
         }

         EditWorldScreen editScreen;
         try {
            editScreen = EditWorldScreen.create(this.minecraft, access, result -> {
               access.safeClose();
               this.list.returnToScreen();
            });
         } catch (IOException | NbtException | ReportedNbtException e) {
            access.safeClose();
            SystemToast.onWorldAccessFailure(this.minecraft, levelId);
            WorldSelectionList.LOGGER.error("Failed to load world data {}", levelId, e);
            this.list.reloadWorldList();
            return;
         }

         this.minecraft.gui.setScreen(editScreen);
      }

      public void recreateWorld() {
         this.queueLoadScreen();

         try (LevelStorageSource.LevelStorageAccess access = this.minecraft.getLevelSource().validateAndCreateAccess(this.summary.getLevelId())) {
            Pair<LevelSettings, WorldCreationContext> recreatedSettings = this.minecraft.createWorldOpenFlows().recreateWorldData(access);
            LevelSettings levelSettings = (LevelSettings)recreatedSettings.getFirst();
            WorldCreationContext creationContext = (WorldCreationContext)recreatedSettings.getSecond();
            Path dataPackDir = CreateWorldScreen.createTempDataPackDirFromExistingWorld(access.getLevelPath(LevelResource.DATAPACK_DIR), this.minecraft);
            creationContext.validate();
            if (creationContext.options().isOldCustomizedWorld()) {
               this.minecraft
                  .gui
                  .setScreen(
                     new ConfirmScreen(
                        result -> this.minecraft
                           .gui
                           .setScreen(
                              result
                                 ? CreateWorldScreen.createFromExisting(this.minecraft, this.list::returnToScreen, levelSettings, creationContext, dataPackDir)
                                 : this.screen
                           ),
                        Component.translatable("selectWorld.recreate.customized.title"),
                        Component.translatable("selectWorld.recreate.customized.text"),
                        CommonComponents.GUI_PROCEED,
                        CommonComponents.GUI_CANCEL
                     )
                  );
            } else {
               this.minecraft
                  .gui
                  .setScreen(CreateWorldScreen.createFromExisting(this.minecraft, this.list::returnToScreen, levelSettings, creationContext, dataPackDir));
            }
         } catch (ContentValidationException e) {
            WorldSelectionList.LOGGER.warn("{}", e.getMessage());
            this.minecraft.gui.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.gui.setScreen(this.screen)));
         } catch (Exception e) {
            WorldSelectionList.LOGGER.error("Unable to recreate world", e);
            this.minecraft
               .gui
               .setScreen(
                  new AlertScreen(
                     () -> this.minecraft.gui.setScreen(this.screen),
                     Component.translatable("selectWorld.recreate.error.title"),
                     Component.translatable("selectWorld.recreate.error.text")
                  )
               );
         }
      }

      private void queueLoadScreen() {
         this.minecraft.setScreenAndShow(new GenericMessageScreen(Component.translatable("selectWorld.data_read")));
      }

      private void loadIcon() {
         boolean shouldHaveIcon = this.iconFile != null && Files.isRegularFile(this.iconFile);
         if (shouldHaveIcon) {
            try (InputStream stream = Files.newInputStream(this.iconFile)) {
               this.icon.upload(NativeImage.read(stream));
            } catch (Throwable t) {
               WorldSelectionList.LOGGER.error("Invalid icon for world {}", this.summary.getLevelId(), t);
               this.iconFile = null;
            }
         } else {
            this.icon.clear();
         }
      }

      @Override
      public void close() {
         if (!this.icon.isClosed()) {
            this.icon.close();
         }
      }

      public String getLevelName() {
         return this.summary.getLevelName();
      }

      @Override
      public LevelSummary getLevelSummary() {
         return this.summary;
      }
   }
}
