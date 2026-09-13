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
import net.minecraft.client.gui.GuiGraphics;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class WorldSelectionList extends ObjectSelectionList<WorldSelectionList.Entry> {
   public static final DateTimeFormatter DATE_FORMAT = Util.localizedDateFormatter(FormatStyle.SHORT);
   static final Identifier ERROR_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("world_list/error_highlighted");
   static final Identifier ERROR_SPRITE = Identifier.withDefaultNamespace("world_list/error");
   static final Identifier MARKED_JOIN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("world_list/marked_join_highlighted");
   static final Identifier MARKED_JOIN_SPRITE = Identifier.withDefaultNamespace("world_list/marked_join");
   static final Identifier WARNING_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("world_list/warning_highlighted");
   static final Identifier WARNING_SPRITE = Identifier.withDefaultNamespace("world_list/warning");
   static final Identifier JOIN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("world_list/join_highlighted");
   static final Identifier JOIN_SPRITE = Identifier.withDefaultNamespace("world_list/join");
   static final Logger LOGGER = LogUtils.getLogger();
   static final Component FROM_NEWER_TOOLTIP_1 = Component.translatable("selectWorld.tooltip.fromNewerVersion1").withStyle(ChatFormatting.RED);
   static final Component FROM_NEWER_TOOLTIP_2 = Component.translatable("selectWorld.tooltip.fromNewerVersion2").withStyle(ChatFormatting.RED);
   static final Component SNAPSHOT_TOOLTIP_1 = Component.translatable("selectWorld.tooltip.snapshot1").withStyle(ChatFormatting.GOLD);
   static final Component SNAPSHOT_TOOLTIP_2 = Component.translatable("selectWorld.tooltip.snapshot2").withStyle(ChatFormatting.GOLD);
   static final Component WORLD_LOCKED_TOOLTIP = Component.translatable("selectWorld.locked").withStyle(ChatFormatting.RED);
   static final Component WORLD_REQUIRES_CONVERSION = Component.translatable("selectWorld.conversion.tooltip").withStyle(ChatFormatting.RED);
   static final Component INCOMPATIBLE_VERSION_TOOLTIP = Component.translatable("selectWorld.incompatible.tooltip").withStyle(ChatFormatting.RED);
   static final Component WORLD_EXPERIMENTAL = Component.translatable("selectWorld.experimental");
   private final Screen screen;
   private CompletableFuture<List<LevelSummary>> pendingLevels;
   private @Nullable List<LevelSummary> currentlyDisplayedLevels;
   private final WorldSelectionList.LoadingHeader loadingHeader;
   final WorldSelectionList.EntryType entryType;
   private String filter;
   private boolean hasPolled;
   private final @Nullable Consumer<LevelSummary> onEntrySelect;
   final @Nullable Consumer<WorldSelectionList.WorldListEntry> onEntryInteract;

   WorldSelectionList(
      Screen p_422996_,
      Minecraft p_239541_,
      int p_239542_,
      int p_239543_,
      String p_239547_,
      @Nullable WorldSelectionList p_239548_,
      @Nullable Consumer<LevelSummary> p_431625_,
      @Nullable Consumer<WorldSelectionList.WorldListEntry> p_430990_,
      WorldSelectionList.EntryType p_424584_
   ) {
      super(p_239541_, p_239542_, p_239543_, 0, 36);
      this.screen = p_422996_;
      this.loadingHeader = new WorldSelectionList.LoadingHeader(p_239541_);
      this.filter = p_239547_;
      this.onEntrySelect = p_431625_;
      this.onEntryInteract = p_430990_;
      this.entryType = p_424584_;
      if (p_239548_ != null) {
         this.pendingLevels = p_239548_.pendingLevels;
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
         List<LevelSummary> list = this.pendingLevels.getNow(null);
         if (this.entryType == WorldSelectionList.EntryType.UPLOAD_WORLD) {
            if (list == null || this.hasPolled) {
               return null;
            }

            this.hasPolled = true;
            list = list.stream().filter(LevelSummary::canUpload).toList();
         }

         return list;
      } catch (CancellationException | CompletionException completionexception) {
         return null;
      }
   }

   public void reloadWorldList() {
      this.pendingLevels = this.loadLevels();
   }

   @Override
   public void renderWidget(GuiGraphics p_310403_, int p_312182_, int p_312151_, float p_311062_) {
      List<LevelSummary> list = this.pollLevelsIgnoreErrors();
      if (list != this.currentlyDisplayedLevels) {
         this.handleNewLevels(list);
      }

      super.renderWidget(p_310403_, p_312182_, p_312151_, p_311062_);
   }

   private void handleNewLevels(@Nullable List<LevelSummary> p_239665_) {
      if (p_239665_ != null) {
         if (p_239665_.isEmpty()) {
            switch (this.entryType) {
               case SINGLEPLAYER:
                  CreateWorldScreen.openFresh(this.minecraft, () -> this.minecraft.setScreen(null));
                  break;
               case UPLOAD_WORLD:
                  this.clearEntries();
                  this.addEntry(new WorldSelectionList.NoWorldsEntry(Component.translatable("mco.upload.select.world.none"), this.screen.getFont()));
            }
         } else {
            this.fillLevels(this.filter, p_239665_);
            this.currentlyDisplayedLevels = p_239665_;
         }
      }
   }

   public void updateFilter(String p_239901_) {
      if (this.currentlyDisplayedLevels != null && !p_239901_.equals(this.filter)) {
         this.fillLevels(p_239901_, this.currentlyDisplayedLevels);
      }

      this.filter = p_239901_;
   }

   private CompletableFuture<List<LevelSummary>> loadLevels() {
      LevelStorageSource.LevelCandidates levelstoragesource$levelcandidates;
      try {
         levelstoragesource$levelcandidates = this.minecraft.getLevelSource().findLevelCandidates();
      } catch (LevelStorageException levelstorageexception) {
         LOGGER.error("Couldn't load level list", levelstorageexception);
         this.handleLevelLoadFailure(levelstorageexception.getMessageComponent());
         return CompletableFuture.completedFuture(List.of());
      }

      return this.minecraft.getLevelSource().loadLevelSummaries(levelstoragesource$levelcandidates).exceptionally(p_233202_ -> {
         this.minecraft.delayCrash(CrashReport.forThrowable(p_233202_, "Couldn't load level list"));
         return List.of();
      });
   }

   private void fillLevels(String p_233199_, List<LevelSummary> p_233200_) {
      List<WorldSelectionList.Entry> list = new ArrayList<>();
      Optional<WorldSelectionList.WorldListEntry> optional = this.getSelectedOpt();
      WorldSelectionList.WorldListEntry worldselectionlist$worldlistentry = null;

      for (LevelSummary levelsummary : p_233200_.stream().filter(p_420802_ -> this.filterAccepts(p_233199_.toLowerCase(Locale.ROOT), p_420802_)).toList()) {
         WorldSelectionList.WorldListEntry worldselectionlist$worldlistentry1 = new WorldSelectionList.WorldListEntry(this, levelsummary);
         if (optional.isPresent() && optional.get().getLevelSummary().getLevelId().equals(worldselectionlist$worldlistentry1.getLevelSummary().getLevelId())) {
            worldselectionlist$worldlistentry = worldselectionlist$worldlistentry1;
         }

         list.add(worldselectionlist$worldlistentry1);
      }

      this.removeEntries(this.children().stream().filter(p_420805_ -> !list.contains(p_420805_)).toList());
      list.forEach(p_420803_ -> {
         if (!this.children().contains(p_420803_)) {
            this.addEntry(p_420803_);
         }
      });
      this.setSelected(worldselectionlist$worldlistentry);
      this.notifyListUpdated();
   }

   private boolean filterAccepts(String p_233196_, LevelSummary p_233197_) {
      return p_233197_.getLevelName().toLowerCase(Locale.ROOT).contains(p_233196_) || p_233197_.getLevelId().toLowerCase(Locale.ROOT).contains(p_233196_);
   }

   private void notifyListUpdated() {
      this.refreshScrollAmount();
      this.screen.triggerImmediateNarration(true);
   }

   private void handleLevelLoadFailure(Component p_233212_) {
      this.minecraft.setScreen(new ErrorScreen(Component.translatable("selectWorld.unable_to_load"), p_233212_));
   }

   @Override
   public int getRowWidth() {
      return 270;
   }

   public void setSelected(WorldSelectionList.@Nullable Entry p_233190_) {
      super.setSelected(p_233190_);
      if (this.onEntrySelect != null) {
         this.onEntrySelect
            .accept(p_233190_ instanceof WorldSelectionList.WorldListEntry worldselectionlist$worldlistentry ? worldselectionlist$worldlistentry.summary : null);
      }
   }

   public Optional<WorldSelectionList.WorldListEntry> getSelectedOpt() {
      WorldSelectionList.Entry worldselectionlist$entry = this.getSelected();
      return worldselectionlist$entry instanceof WorldSelectionList.WorldListEntry worldselectionlist$worldlistentry
         ? Optional.of(worldselectionlist$worldlistentry)
         : Optional.empty();
   }

   public void returnToScreen() {
      this.reloadWorldList();
      this.minecraft.setScreen(this.screen);
   }

   public Screen getScreen() {
      return this.screen;
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput p_313204_) {
      if (this.children().contains(this.loadingHeader)) {
         this.loadingHeader.updateNarration(p_313204_);
      } else {
         super.updateWidgetNarration(p_313204_);
      }
   }

   @OnlyIn(Dist.CLIENT)
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

      public Builder(Minecraft p_430056_, Screen p_431029_) {
         this.minecraft = p_430056_;
         this.screen = p_431029_;
      }

      public WorldSelectionList.Builder width(int p_431275_) {
         this.width = p_431275_;
         return this;
      }

      public WorldSelectionList.Builder height(int p_423630_) {
         this.height = p_423630_;
         return this;
      }

      public WorldSelectionList.Builder filter(String p_426893_) {
         this.filter = p_426893_;
         return this;
      }

      public WorldSelectionList.Builder oldList(@Nullable WorldSelectionList p_429975_) {
         this.oldList = p_429975_;
         return this;
      }

      public WorldSelectionList.Builder onEntrySelect(Consumer<LevelSummary> p_422872_) {
         this.onEntrySelect = p_422872_;
         return this;
      }

      public WorldSelectionList.Builder onEntryInteract(Consumer<WorldSelectionList.WorldListEntry> p_431314_) {
         this.onEntryInteract = p_431314_;
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

   @OnlyIn(Dist.CLIENT)
   public abstract static class Entry extends ObjectSelectionList.Entry<WorldSelectionList.Entry> implements AutoCloseable {
      @Override
      public void close() {
      }

      public @Nullable LevelSummary getLevelSummary() {
         return null;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public enum EntryType {
      SINGLEPLAYER,
      UPLOAD_WORLD;
   }

   @OnlyIn(Dist.CLIENT)
   public static class LoadingHeader extends WorldSelectionList.Entry {
      private static final Component LOADING_LABEL = Component.translatable("selectWorld.loading_list");
      private final Minecraft minecraft;

      public LoadingHeader(Minecraft p_233222_) {
         this.minecraft = p_233222_;
      }

      @Override
      public void renderContent(GuiGraphics p_423994_, int p_426960_, int p_429558_, boolean p_428179_, float p_431514_) {
         int i = (this.minecraft.screen.width - this.minecraft.font.width(LOADING_LABEL)) / 2;
         int j = this.getContentY() + (this.getContentHeight() - 9) / 2;
         p_423994_.drawString(this.minecraft.font, LOADING_LABEL, i, j, -1);
         String s = LoadingDotsText.get(Util.getMillis());
         int k = (this.minecraft.screen.width - this.minecraft.font.width(s)) / 2;
         int l = j + 9;
         p_423994_.drawString(this.minecraft.font, s, k, l, -8355712);
      }

      @Override
      public Component getNarration() {
         return LOADING_LABEL;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static final class NoWorldsEntry extends WorldSelectionList.Entry {
      private final StringWidget stringWidget;

      public NoWorldsEntry(Component p_423669_, Font p_429287_) {
         this.stringWidget = new StringWidget(p_423669_, p_429287_);
      }

      @Override
      public Component getNarration() {
         return this.stringWidget.getMessage();
      }

      @Override
      public void renderContent(GuiGraphics p_430175_, int p_428503_, int p_424551_, boolean p_425538_, float p_429078_) {
         this.stringWidget
            .setPosition(this.getContentXMiddle() - this.stringWidget.getWidth() / 2, this.getContentYMiddle() - this.stringWidget.getHeight() / 2);
         this.stringWidget.render(p_430175_, p_428503_, p_424551_, p_429078_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public final class WorldListEntry extends WorldSelectionList.Entry implements SelectableEntry {
      private static final int ICON_SIZE = 32;
      private final WorldSelectionList list;
      private final Minecraft minecraft;
      private final Screen screen;
      final LevelSummary summary;
      private final FaviconTexture icon;
      private final StringWidget worldNameText;
      private final StringWidget idAndLastPlayedText;
      private final StringWidget infoText;
      private @Nullable Path iconFile;

      public WorldListEntry(final WorldSelectionList p_454623_, final LevelSummary p_101703_) {
         this.list = p_454623_;
         this.minecraft = p_454623_.minecraft;
         this.screen = p_454623_.getScreen();
         this.summary = p_101703_;
         this.icon = FaviconTexture.forWorld(this.minecraft.getTextureManager(), p_101703_.getLevelId());
         this.iconFile = p_101703_.getIcon();
         int i = p_454623_.getRowWidth() - this.getTextX() - 2;
         Component component = Component.literal(p_101703_.getLevelName());
         this.worldNameText = new StringWidget(component, this.minecraft.font);
         this.worldNameText.setMaxWidth(i);
         if (this.minecraft.font.width(component) > i) {
            this.worldNameText.setTooltip(Tooltip.create(component));
         }

         String s = p_101703_.getLevelId();
         long j = p_101703_.getLastPlayed();
         if (j != -1L) {
            ZonedDateTime zoneddatetime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(j), ZoneId.systemDefault());
            s = s + " (" + WorldSelectionList.DATE_FORMAT.format(zoneddatetime) + ")";
         }

         Component component2 = Component.literal(s).withColor(-8355712);
         this.idAndLastPlayedText = new StringWidget(component2, this.minecraft.font);
         this.idAndLastPlayedText.setMaxWidth(i);
         if (this.minecraft.font.width(s) > i) {
            this.idAndLastPlayedText.setTooltip(Tooltip.create(component2));
         }

         Component component1 = ComponentUtils.mergeStyles(p_101703_.getInfo(), Style.EMPTY.withColor(-8355712));
         this.infoText = new StringWidget(component1, this.minecraft.font);
         this.infoText.setMaxWidth(i);
         if (this.minecraft.font.width(component1) > i) {
            this.infoText.setTooltip(Tooltip.create(component1));
         }

         this.validateIconFile();
         this.loadIcon();
      }

      private void validateIconFile() {
         if (this.iconFile != null) {
            try {
               BasicFileAttributes basicfileattributes = Files.readAttributes(this.iconFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
               if (basicfileattributes.isSymbolicLink()) {
                  List<ForbiddenSymlinkInfo> list = this.minecraft.directoryValidator().validateSymlink(this.iconFile);
                  if (!list.isEmpty()) {
                     WorldSelectionList.LOGGER.warn("{}", ContentValidationException.getMessage(this.iconFile, list));
                     this.iconFile = null;
                  } else {
                     basicfileattributes = Files.readAttributes(this.iconFile, BasicFileAttributes.class);
                  }
               }

               if (!basicfileattributes.isRegularFile()) {
                  this.iconFile = null;
               }
            } catch (NoSuchFileException nosuchfileexception) {
               this.iconFile = null;
            } catch (IOException ioexception) {
               WorldSelectionList.LOGGER.error("could not validate symlink", ioexception);
               this.iconFile = null;
            }
         }
      }

      @Override
      public Component getNarration() {
         Component component = Component.translatable(
            "narrator.select.world_info", this.summary.getLevelName(), Component.translationArg(new Date(this.summary.getLastPlayed())), this.summary.getInfo()
         );
         if (this.summary.isLocked()) {
            component = CommonComponents.joinForNarration(component, WorldSelectionList.WORLD_LOCKED_TOOLTIP);
         }

         if (this.summary.isExperimental()) {
            component = CommonComponents.joinForNarration(component, WorldSelectionList.WORLD_EXPERIMENTAL);
         }

         return Component.translatable("narrator.select", component);
      }

      @Override
      public void renderContent(GuiGraphics p_424437_, int p_425443_, int p_425024_, boolean p_430261_, float p_424775_) {
         int i = this.getTextX();
         this.worldNameText.setPosition(i, this.getContentY() + 1);
         this.worldNameText.render(p_424437_, p_425443_, p_425024_, p_424775_);
         this.idAndLastPlayedText.setPosition(i, this.getContentY() + 9 + 3);
         this.idAndLastPlayedText.render(p_424437_, p_425443_, p_425024_, p_424775_);
         this.infoText.setPosition(i, this.getContentY() + 9 + 9 + 3);
         this.infoText.render(p_424437_, p_425443_, p_425024_, p_424775_);
         p_424437_.blit(RenderPipelines.GUI_TEXTURED, this.icon.textureLocation(), this.getContentX(), this.getContentY(), 0.0F, 0.0F, 32, 32, 32, 32);
         if (this.list.entryType == WorldSelectionList.EntryType.SINGLEPLAYER && (this.minecraft.options.touchscreen().get() || p_430261_)) {
            p_424437_.fill(this.getContentX(), this.getContentY(), this.getContentX() + 32, this.getContentY() + 32, -1601138544);
            int j = p_425443_ - this.getContentX();
            int k = p_425024_ - this.getContentY();
            boolean flag = this.mouseOverIcon(j, k, 32);
            Identifier identifier = flag ? WorldSelectionList.JOIN_HIGHLIGHTED_SPRITE : WorldSelectionList.JOIN_SPRITE;
            Identifier identifier1 = flag ? WorldSelectionList.WARNING_HIGHLIGHTED_SPRITE : WorldSelectionList.WARNING_SPRITE;
            Identifier identifier2 = flag ? WorldSelectionList.ERROR_HIGHLIGHTED_SPRITE : WorldSelectionList.ERROR_SPRITE;
            Identifier identifier3 = flag ? WorldSelectionList.MARKED_JOIN_HIGHLIGHTED_SPRITE : WorldSelectionList.MARKED_JOIN_SPRITE;
            if (this.summary instanceof LevelSummary.SymlinkLevelSummary || this.summary instanceof LevelSummary.CorruptedLevelSummary) {
               p_424437_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier2, this.getContentX(), this.getContentY(), 32, 32);
               p_424437_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier3, this.getContentX(), this.getContentY(), 32, 32);
               return;
            }

            if (this.summary.isLocked()) {
               p_424437_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier2, this.getContentX(), this.getContentY(), 32, 32);
               if (flag) {
                  p_424437_.setTooltipForNextFrame(this.minecraft.font.split(WorldSelectionList.WORLD_LOCKED_TOOLTIP, 175), p_425443_, p_425024_);
               }
            } else if (this.summary.requiresManualConversion()) {
               p_424437_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier2, this.getContentX(), this.getContentY(), 32, 32);
               if (flag) {
                  p_424437_.setTooltipForNextFrame(this.minecraft.font.split(WorldSelectionList.WORLD_REQUIRES_CONVERSION, 175), p_425443_, p_425024_);
               }
            } else if (!this.summary.isCompatible()) {
               p_424437_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier2, this.getContentX(), this.getContentY(), 32, 32);
               if (flag) {
                  p_424437_.setTooltipForNextFrame(this.minecraft.font.split(WorldSelectionList.INCOMPATIBLE_VERSION_TOOLTIP, 175), p_425443_, p_425024_);
               }
            } else if (this.summary.shouldBackup()) {
               p_424437_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier3, this.getContentX(), this.getContentY(), 32, 32);
               if (this.summary.isDowngrade()) {
                  p_424437_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier2, this.getContentX(), this.getContentY(), 32, 32);
                  if (flag) {
                     p_424437_.setTooltipForNextFrame(
                        ImmutableList.of(
                           WorldSelectionList.FROM_NEWER_TOOLTIP_1.getVisualOrderText(), WorldSelectionList.FROM_NEWER_TOOLTIP_2.getVisualOrderText()
                        ),
                        p_425443_,
                        p_425024_
                     );
                  }
               } else if (!SharedConstants.getCurrentVersion().stable()) {
                  p_424437_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier1, this.getContentX(), this.getContentY(), 32, 32);
                  if (flag) {
                     p_424437_.setTooltipForNextFrame(
                        ImmutableList.of(WorldSelectionList.SNAPSHOT_TOOLTIP_1.getVisualOrderText(), WorldSelectionList.SNAPSHOT_TOOLTIP_2.getVisualOrderText()),
                        p_425443_,
                        p_425024_
                     );
                  }
               }

               if (flag) {
                  WorldSelectionList.this.handleCursor(p_424437_);
               }
            } else {
               p_424437_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getContentX(), this.getContentY(), 32, 32);
               if (flag) {
                  WorldSelectionList.this.handleCursor(p_424437_);
               }
            }
         }
      }

      private int getTextX() {
         return this.getContentX() + 32 + 3;
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent p_423032_, boolean p_423860_) {
         if (this.canInteract()) {
            int i = (int)p_423032_.x() - this.getContentX();
            int j = (int)p_423032_.y() - this.getContentY();
            if (p_423860_ || this.mouseOverIcon(i, j, 32) && this.list.entryType == WorldSelectionList.EntryType.SINGLEPLAYER) {
               this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
               Consumer<WorldSelectionList.WorldListEntry> consumer = this.list.onEntryInteract;
               if (consumer != null) {
                  consumer.accept(this);
                  return true;
               }
            }
         }

         return super.mouseClicked(p_423032_, p_423860_);
      }

      @Override
      public boolean keyPressed(KeyEvent p_428804_) {
         if (p_428804_.isSelection() && this.canInteract()) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            Consumer<WorldSelectionList.WorldListEntry> consumer = this.list.onEntryInteract;
            if (consumer != null) {
               consumer.accept(this);
               return true;
            }
         }

         return super.keyPressed(p_428804_);
      }

      public boolean canInteract() {
         return this.summary.primaryActionActive() || this.list.entryType == WorldSelectionList.EntryType.UPLOAD_WORLD;
      }

      public void joinWorld() {
         if (this.summary.primaryActionActive()) {
            if (this.summary instanceof LevelSummary.SymlinkLevelSummary) {
               this.minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.setScreen(this.screen)));
            } else {
               this.minecraft.createWorldOpenFlows().openWorld(this.summary.getLevelId(), this.list::returnToScreen);
            }
         }
      }

      public void deleteWorld() {
         this.minecraft
            .setScreen(
               new ConfirmScreen(
                  p_420806_ -> {
                     if (p_420806_) {
                        this.minecraft.setScreen(new ProgressScreen(true));
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
         LevelStorageSource levelstoragesource = this.minecraft.getLevelSource();
         String s = this.summary.getLevelId();

         try (LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = levelstoragesource.createAccess(s)) {
            levelstoragesource$levelstorageaccess.deleteLevel();
         } catch (IOException ioexception) {
            SystemToast.onWorldDeleteFailure(this.minecraft, s);
            WorldSelectionList.LOGGER.error("Failed to delete world {}", s, ioexception);
         }
      }

      public void editWorld() {
         this.queueLoadScreen();
         String s = this.summary.getLevelId();

         LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess;
         try {
            levelstoragesource$levelstorageaccess = this.minecraft.getLevelSource().validateAndCreateAccess(s);
         } catch (IOException ioexception1) {
            SystemToast.onWorldAccessFailure(this.minecraft, s);
            WorldSelectionList.LOGGER.error("Failed to access level {}", s, ioexception1);
            this.list.reloadWorldList();
            return;
         } catch (ContentValidationException contentvalidationexception) {
            WorldSelectionList.LOGGER.warn("{}", contentvalidationexception.getMessage());
            this.minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.setScreen(this.screen)));
            return;
         }

         EditWorldScreen editworldscreen;
         try {
            editworldscreen = EditWorldScreen.create(this.minecraft, levelstoragesource$levelstorageaccess, p_420812_ -> {
               levelstoragesource$levelstorageaccess.safeClose();
               this.list.returnToScreen();
            });
         } catch (NbtException | ReportedNbtException | IOException ioexception) {
            levelstoragesource$levelstorageaccess.safeClose();
            SystemToast.onWorldAccessFailure(this.minecraft, s);
            WorldSelectionList.LOGGER.error("Failed to load world data {}", s, ioexception);
            this.list.reloadWorldList();
            return;
         }

         this.minecraft.setScreen(editworldscreen);
      }

      public void recreateWorld() {
         this.queueLoadScreen();

         try (LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = this.minecraft
               .getLevelSource()
               .validateAndCreateAccess(this.summary.getLevelId())) {
            Pair<LevelSettings, WorldCreationContext> pair = this.minecraft.createWorldOpenFlows().recreateWorldData(levelstoragesource$levelstorageaccess);
            LevelSettings levelsettings = (LevelSettings)pair.getFirst();
            WorldCreationContext worldcreationcontext = (WorldCreationContext)pair.getSecond();
            Path path = CreateWorldScreen.createTempDataPackDirFromExistingWorld(
               levelstoragesource$levelstorageaccess.getLevelPath(LevelResource.DATAPACK_DIR), this.minecraft
            );
            worldcreationcontext.validate();
            if (worldcreationcontext.options().isOldCustomizedWorld()) {
               this.minecraft
                  .setScreen(
                     new ConfirmScreen(
                        p_420810_ -> this.minecraft
                           .setScreen(
                              p_420810_
                                 ? CreateWorldScreen.createFromExisting(this.minecraft, this.list::returnToScreen, levelsettings, worldcreationcontext, path)
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
                  .setScreen(CreateWorldScreen.createFromExisting(this.minecraft, this.list::returnToScreen, levelsettings, worldcreationcontext, path));
            }
         } catch (ContentValidationException contentvalidationexception) {
            WorldSelectionList.LOGGER.warn("{}", contentvalidationexception.getMessage());
            this.minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.setScreen(this.screen)));
         } catch (Exception exception) {
            WorldSelectionList.LOGGER.error("Unable to recreate world", exception);
            this.minecraft
               .setScreen(
                  new AlertScreen(
                     () -> this.minecraft.setScreen(this.screen),
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
         boolean flag = this.iconFile != null && Files.isRegularFile(this.iconFile);
         if (flag) {
            try (InputStream inputstream = Files.newInputStream(this.iconFile)) {
               this.icon.upload(NativeImage.read(inputstream));
            } catch (Throwable throwable) {
               WorldSelectionList.LOGGER.error("Invalid icon for world {}", this.summary.getLevelId(), throwable);
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
