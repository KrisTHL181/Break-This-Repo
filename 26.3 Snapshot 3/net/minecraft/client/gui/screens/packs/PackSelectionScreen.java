package net.minecraft.client.gui.screens.packs;

import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackMetadataResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackDetector;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.Util;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class PackSelectionScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component AVAILABLE_TITLE = Component.translatable("pack.available.title");
   private static final Component SELECTED_TITLE = Component.translatable("pack.selected.title");
   private static final Component OPEN_PACK_FOLDER_TITLE = Component.translatable("pack.openFolder");
   private static final Component SEARCH = Component.translatable("gui.packSelection.search").withStyle(EditBox.SEARCH_HINT_STYLE);
   private static final int LIST_WIDTH = 200;
   private static final int HEADER_ELEMENT_SPACING = 4;
   private static final int SEARCH_BOX_HEIGHT = 15;
   private static final Component DRAG_AND_DROP = Component.translatable("pack.dropInfo").withStyle(ChatFormatting.GRAY);
   private static final Component DIRECTORY_BUTTON_TOOLTIP = Component.translatable("pack.folderInfo");
   private static final int RELOAD_COOLDOWN = 20;
   private static final Identifier DEFAULT_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final PackSelectionModel model;
   private PackSelectionScreen.@Nullable Watcher watcher;
   private long ticksToReload;
   private @Nullable TransferableSelectionList availablePackList;
   private @Nullable TransferableSelectionList selectedPackList;
   private @Nullable EditBox search;
   private final Path packDir;
   private @Nullable Button doneButton;
   private final Map<String, Identifier> packIcons = Maps.newHashMap();

   public PackSelectionScreen(final PackRepository repository, final Consumer<PackRepository> output, final Path packDir, final Component title) {
      super(title);
      this.model = new PackSelectionModel(this::populateLists, this::getPackIcon, repository, output);
      this.packDir = packDir;
      this.watcher = PackSelectionScreen.Watcher.create(packDir);
   }

   @Override
   public void onClose() {
      this.model.commit();
      this.closeWatcher();
   }

   private void closeWatcher() {
      if (this.watcher != null) {
         try {
            this.watcher.close();
            this.watcher = null;
         } catch (Exception var2) {
         }
      }
   }

   @Override
   protected void init() {
      this.layout.setHeaderHeight(4 + 9 + 4 + 9 + 4 + 15 + 4);
      LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
      header.defaultCellSetting().alignHorizontallyCenter();
      header.addChild(new StringWidget(this.getTitle(), this.font));
      header.addChild(new StringWidget(DRAG_AND_DROP, this.font));
      this.search = header.addChild(new EditBox(this.font, 0, 0, 200, 15, Component.empty()));
      this.search.setHint(SEARCH);
      this.search.setResponder(this::updateFilteredEntries);
      this.availablePackList = this.layout.addToContents(new TransferableSelectionList(this.minecraft, this, 200, this.height - 66, AVAILABLE_TITLE));
      this.selectedPackList = this.layout.addToContents(new TransferableSelectionList(this.minecraft, this, 200, this.height - 66, SELECTED_TITLE));
      LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      footer.addChild(
         Button.builder(OPEN_PACK_FOLDER_TITLE, button -> Util.getPlatform().openPath(this.packDir)).tooltip(Tooltip.create(DIRECTORY_BUTTON_TOOLTIP)).build()
      );
      this.doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).build());
      this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
      this.repositionElements();
      this.reload();
   }

   @Override
   protected void setInitialFocus() {
      if (this.search != null) {
         this.setInitialFocus(this.search);
      } else {
         super.setInitialFocus();
      }
   }

   private void updateFilteredEntries(final String value) {
      this.updateFilteredEntries(value, null);
   }

   private void updateFilteredEntries(final String value, final PackSelectionModel.@Nullable EntryBase transferredEntry) {
      this.filterEntries(value, this.model.getSelected(), this.selectedPackList, transferredEntry);
      this.filterEntries(value, this.model.getUnselected(), this.availablePackList, transferredEntry);
   }

   private void filterEntries(
      final String value,
      final Stream<PackSelectionModel.Entry> oldEntries,
      final @Nullable TransferableSelectionList listToUpdate,
      final PackSelectionModel.@Nullable EntryBase transferredEntry
   ) {
      if (listToUpdate != null) {
         String lowerCaseValue = value.toLowerCase(Locale.ROOT);
         Stream<PackSelectionModel.Entry> filteredEntries = oldEntries.filter(
            packEntry -> value.isBlank()
               || packEntry.getId().toLowerCase(Locale.ROOT).contains(lowerCaseValue)
               || packEntry.getTitle().getString().toLowerCase(Locale.ROOT).contains(lowerCaseValue)
               || packEntry.getDescription().getString().toLowerCase(Locale.ROOT).contains(lowerCaseValue)
         );
         listToUpdate.updateList(filteredEntries, transferredEntry);
      }
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      if (this.availablePackList != null) {
         this.availablePackList.updateSizeAndPosition(200, this.layout.getContentHeight(), this.width / 2 - 15 - 200, this.layout.getHeaderHeight());
      }

      if (this.selectedPackList != null) {
         this.selectedPackList.updateSizeAndPosition(200, this.layout.getContentHeight(), this.width / 2 + 15, this.layout.getHeaderHeight());
      }
   }

   @Override
   public void tick() {
      if (this.watcher != null) {
         try {
            if (this.watcher.pollForChanges()) {
               this.ticksToReload = 20L;
            }
         } catch (IOException e) {
            LOGGER.warn("Failed to poll for directory {} changes, stopping", this.packDir);
            this.closeWatcher();
         }
      }

      if (this.ticksToReload > 0L && --this.ticksToReload == 0L) {
         this.reload();
      }
   }

   private void populateLists(final PackSelectionModel.@Nullable EntryBase transferredEntry) {
      if (this.selectedPackList != null) {
         this.selectedPackList.updateList(this.model.getSelected(), transferredEntry);
      }

      if (this.availablePackList != null) {
         this.availablePackList.updateList(this.model.getUnselected(), transferredEntry);
      }

      if (this.search != null) {
         this.updateFilteredEntries(this.search.getValue(), transferredEntry);
      }

      if (this.doneButton != null) {
         this.doneButton.active = !this.selectedPackList.children().isEmpty();
      }
   }

   private void reload() {
      this.model.findNewPacks();
      this.populateLists(null);
      this.ticksToReload = 0L;
      this.packIcons.clear();
   }

   protected static void copyPacks(final Minecraft minecraft, final List<Path> files, final Path targetDir) {
      MutableBoolean showErrorToast = new MutableBoolean();
      files.forEach(pack -> {
         try (Stream<Path> contents = Files.walk(pack)) {
            contents.forEach(path -> {
               try {
                  Util.copyBetweenDirs(pack.getParent(), targetDir, path);
               } catch (IOException e) {
                  LOGGER.warn("Failed to copy datapack file  from {} to {}", new Object[]{path, targetDir, ex});
                  showErrorToast.setTrue();
               }
            });
         } catch (IOException e) {
            LOGGER.warn("Failed to copy datapack file from {} to {}", pack, targetDir);
            showErrorToast.setTrue();
         }
      });
      if (showErrorToast.isTrue()) {
         SystemToast.onPackCopyFailure(minecraft, targetDir.toString());
      }
   }

   @Override
   public void onFilesDrop(final List<Path> files) {
      String names = extractPackNames(files).collect(Collectors.joining(", "));
      this.minecraft
         .gui
         .setScreen(
            new ConfirmScreen(
               result -> {
                  if (result) {
                     List<Path> packCandidates = new ArrayList<>(files.size());
                     Set<Path> leftoverPacks = new HashSet<>(files);
                     PackDetector<Path> packDetector = new PackDetector<Path>(this.minecraft.directoryValidator()) {
                        protected Path createZipPack(final Path content) {
                           return content;
                        }

                        protected Path createDirectoryPack(final Path content) {
                           return content;
                        }
                     };
                     List<ForbiddenSymlinkInfo> issues = new ArrayList<>();

                     for (Path path : files) {
                        try {
                           Path candidate = packDetector.detectPackResources(path, issues);
                           if (candidate == null) {
                              LOGGER.warn("Path {} does not seem like pack", path);
                           } else {
                              packCandidates.add(candidate);
                              leftoverPacks.remove(candidate);
                           }
                        } catch (IOException e) {
                           LOGGER.warn("Failed to check {} for packs", path, e);
                        }
                     }

                     if (!issues.isEmpty()) {
                        this.minecraft.gui.setScreen(NoticeWithLinkScreen.createPackSymlinkWarningScreen(() -> this.minecraft.gui.setScreen(this)));
                        return;
                     }

                     if (!packCandidates.isEmpty()) {
                        copyPacks(this.minecraft, packCandidates, this.packDir);
                        this.reload();
                     }

                     if (!leftoverPacks.isEmpty()) {
                        String leftoverNames = extractPackNames(leftoverPacks).collect(Collectors.joining(", "));
                        this.minecraft
                           .gui
                           .setScreen(
                              new AlertScreen(
                                 () -> this.minecraft.gui.setScreen(this),
                                 Component.translatable("pack.dropRejected.title"),
                                 Component.translatable("pack.dropRejected.message", leftoverNames)
                              )
                           );
                        return;
                     }
                  }

                  this.minecraft.gui.setScreen(this);
               },
               Component.translatable("pack.dropConfirm"),
               Component.literal(names)
            )
         );
   }

   private static Stream<String> extractPackNames(final Collection<Path> files) {
      return files.stream().map(Path::getFileName).map(Path::toString);
   }

   private Identifier loadPackIcon(final TextureManager textureManager, final Pack pack) {
      try (PackMetadataResources packResources = pack.openMetadata()) {
         IoSupplier<InputStream> resource = packResources.getRootResource("pack.png");
         if (resource == null) {
            return DEFAULT_ICON;
         }

         String id = pack.getId();
         Identifier location = Identifier.withDefaultNamespace(
            "pack/" + Util.sanitizeName(id, Identifier::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(id) + "/icon"
         );

         try (InputStream stream = resource.get()) {
            NativeImage iconImage = NativeImage.read(stream);
            textureManager.register(location, new DynamicTexture(location::toString, iconImage));
            return location;
         }
      } catch (Exception e) {
         LOGGER.warn("Failed to load icon from pack {}", pack.getId(), e);
         return DEFAULT_ICON;
      }
   }

   private Identifier getPackIcon(final Pack pack) {
      return this.packIcons.computeIfAbsent(pack.getId(), s -> this.loadPackIcon(this.minecraft.getTextureManager(), pack));
   }

   private static class Watcher implements AutoCloseable {
      private final WatchService watcher;
      private final Path packPath;

      public Watcher(final Path packPath) throws IOException {
         this.packPath = packPath;
         this.watcher = packPath.getFileSystem().newWatchService();

         try {
            this.watchDir(packPath);

            try (DirectoryStream<Path> paths = Files.newDirectoryStream(packPath)) {
               for (Path path : paths) {
                  if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                     this.watchDir(path);
                  }
               }
            }
         } catch (Exception e) {
            this.watcher.close();
            throw e;
         }
      }

      public static PackSelectionScreen.@Nullable Watcher create(final Path packDir) {
         try {
            return new PackSelectionScreen.Watcher(packDir);
         } catch (IOException e) {
            PackSelectionScreen.LOGGER.warn("Failed to initialize pack directory {} monitoring", packDir, e);
            return null;
         }
      }

      private void watchDir(final Path packPath) throws IOException {
         packPath.register(this.watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
      }

      public boolean pollForChanges() throws IOException {
         boolean hasChanges = false;

         WatchKey key;
         while ((key = this.watcher.poll()) != null) {
            for (WatchEvent<?> watchEvent : key.pollEvents()) {
               hasChanges = true;
               if (key.watchable() == this.packPath && watchEvent.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                  Path newPath = this.packPath.resolve((Path)watchEvent.context());
                  if (Files.isDirectory(newPath, LinkOption.NOFOLLOW_LINKS)) {
                     this.watchDir(newPath);
                  }
               }
            }

            key.reset();
         }

         return hasChanges;
      }

      @Override
      public void close() throws IOException {
         this.watcher.close();
      }
   }
}
