package net.minecraft.client.gui.screens.worldselection;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.function.ToIntFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class OptimizeWorldScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ToIntFunction<ResourceKey<Level>> DIMENSION_COLORS = Util.make(new Reference2IntOpenHashMap(), map -> {
      map.put(Level.OVERWORLD, -13408734);
      map.put(Level.NETHER, -10075085);
      map.put(Level.END, -8943531);
      map.defaultReturnValue(-2236963);
   });
   private final BooleanConsumer callback;
   private final WorldUpgrader upgrader;

   public static @Nullable OptimizeWorldScreen create(
      final Minecraft minecraft,
      final BooleanConsumer callback,
      final DataFixer dataFixer,
      final LevelStorageSource.LevelStorageAccess levelSourceAccess,
      final boolean eraseCache
   ) {
      try {
         WorldOpenFlows worldOpenFlows = minecraft.createWorldOpenFlows();
         PackRepository packRepository = ServerPacksSource.createPackRepository(levelSourceAccess);
         Dynamic<?> unfixedDataTag = levelSourceAccess.getUnfixedDataTagWithFallback();
         int dataVersion = NbtUtils.getDataVersion(unfixedDataTag);
         if (DataFixers.getFileFixer().requiresFileFixing(dataVersion)) {
            throw new IllegalStateException("Can't optimize world before file fixing; shouldn't be able to get here");
         }

         Dynamic<?> dataTag = DataFixTypes.LEVEL.updateToCurrentVersion(DataFixers.getDataFixer(), unfixedDataTag, dataVersion);

         try (WorldStem worldStem = worldOpenFlows.loadWorldStem(levelSourceAccess, dataTag, false, packRepository)) {
            WorldData worldData = worldStem.worldDataAndGenSettings().data();
            RegistryAccess.Frozen registryAccess = worldStem.registries().compositeAccess();
            levelSourceAccess.saveDataTag(worldData);
            return new OptimizeWorldScreen(callback, dataFixer, levelSourceAccess, worldData, eraseCache, registryAccess);
         }
      } catch (Exception e) {
         LOGGER.warn("Failed to load datapacks, can't optimize world", e);
         return null;
      }
   }

   private OptimizeWorldScreen(
      final BooleanConsumer callback,
      final DataFixer dataFixer,
      final LevelStorageSource.LevelStorageAccess levelSource,
      final WorldData worldData,
      final boolean eraseCache,
      final RegistryAccess registryAccess
   ) {
      super(Component.translatable("optimizeWorld.title", worldData.getLevelSettings().levelName()));
      this.callback = callback;
      this.upgrader = new WorldUpgrader(levelSource, dataFixer, registryAccess, eraseCache, false);
   }

   @Override
   protected void init() {
      super.init();
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> {
         this.upgrader.cancel();
         this.callback.accept(false);
      }).bounds(this.width / 2 - 100, this.height / 4 + 150, 200, 20).build());
   }

   @Override
   public void tick() {
      if (this.upgrader.isFinished()) {
         this.callback.accept(true);
      }
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public void onClose() {
      this.callback.accept(false);
   }

   @Override
   public void removed() {
      this.upgrader.cancel();
      this.upgrader.close();
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractRenderState(graphics, mouseX, mouseY, a);
      graphics.centeredText(this.font, this.title, this.width / 2, 20, -1);
      int x0 = this.width / 2 - 150;
      int x1 = this.width / 2 + 150;
      int y0 = this.height / 4 + 100;
      int y1 = y0 + 10;
      graphics.centeredText(this.font, this.upgrader.getStatus(), this.width / 2, y0 - 9 - 2, -6250336);
      if (this.upgrader.getTotalChunks() > 0) {
         graphics.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, -16777216);
         graphics.text(this.font, Component.translatable("optimizeWorld.info.converted", this.upgrader.getConverted()), x0, 40, -6250336);
         graphics.text(this.font, Component.translatable("optimizeWorld.info.skipped", this.upgrader.getSkipped()), x0, 40 + 9 + 3, -6250336);
         graphics.text(this.font, Component.translatable("optimizeWorld.info.total", this.upgrader.getTotalChunks()), x0, 40 + (9 + 3) * 2, -6250336);
         int progress = 0;

         for (ResourceKey<Level> dimension : this.upgrader.levels()) {
            int length = Mth.floor(this.upgrader.dimensionProgress(dimension) * (x1 - x0));
            graphics.fill(x0 + progress, y0, x0 + progress + length, y1, DIMENSION_COLORS.applyAsInt(dimension));
            progress += length;
         }

         int totalProgress = this.upgrader.getConverted() + this.upgrader.getSkipped();
         Component countStr = Component.translatable("optimizeWorld.progress.counter", totalProgress, this.upgrader.getTotalChunks());
         Component progressStr = Component.translatable("optimizeWorld.progress.percentage", Mth.floor(this.upgrader.getTotalProgress() * 100.0F));
         graphics.centeredText(this.font, countStr, this.width / 2, y0 + 2 * 9 + 2, -6250336);
         graphics.centeredText(this.font, progressStr, this.width / 2, y0 + (y1 - y0) / 2 - 9 / 2, -6250336);
      }
   }
}
