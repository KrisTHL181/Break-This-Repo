package net.minecraft.client.gui.screens.worldselection;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.function.ToIntFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class OptimizeWorldScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ToIntFunction<ResourceKey<Level>> DIMENSION_COLORS = Util.make(new Reference2IntOpenHashMap(), p_308238_ -> {
      p_308238_.put(Level.OVERWORLD, -13408734);
      p_308238_.put(Level.NETHER, -10075085);
      p_308238_.put(Level.END, -8943531);
      p_308238_.defaultReturnValue(-2236963);
   });
   private final BooleanConsumer callback;
   private final WorldUpgrader upgrader;

   public static @Nullable OptimizeWorldScreen create(
      Minecraft p_101316_, BooleanConsumer p_101317_, DataFixer p_101318_, LevelStorageSource.LevelStorageAccess p_101319_, boolean p_101320_
   ) {
      try {
         WorldOpenFlows worldopenflows = p_101316_.createWorldOpenFlows();
         PackRepository packrepository = ServerPacksSource.createPackRepository(p_101319_);

         try (WorldStem worldstem = worldopenflows.loadWorldStem(p_101319_.getDataTag(), false, packrepository)) {
            WorldData worlddata = worldstem.worldData();
            RegistryAccess.Frozen registryaccess$frozen = worldstem.registries().compositeAccess();
            p_101319_.saveDataTag(registryaccess$frozen, worlddata);
            return new OptimizeWorldScreen(p_101317_, p_101318_, p_101319_, worlddata, p_101320_, registryaccess$frozen);
         }
      } catch (Exception exception) {
         LOGGER.warn("Failed to load datapacks, can't optimize world", exception);
         return null;
      }
   }

   private OptimizeWorldScreen(
      BooleanConsumer p_251295_,
      DataFixer p_250489_,
      LevelStorageSource.LevelStorageAccess p_248781_,
      WorldData p_394973_,
      boolean p_250358_,
      RegistryAccess p_327796_
   ) {
      super(Component.translatable("optimizeWorld.title", p_394973_.getLevelSettings().levelName()));
      this.callback = p_251295_;
      this.upgrader = new WorldUpgrader(p_248781_, p_250489_, p_394973_, p_327796_, p_250358_, false);
   }

   @Override
   protected void init() {
      super.init();
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, p_101322_ -> {
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
   public void onClose() {
      this.callback.accept(false);
   }

   @Override
   public void removed() {
      this.upgrader.cancel();
      this.upgrader.close();
   }

   @Override
   public void render(GuiGraphics p_281829_, int p_101312_, int p_101313_, float p_101314_) {
      super.render(p_281829_, p_101312_, p_101313_, p_101314_);
      p_281829_.drawCenteredString(this.font, this.title, this.width / 2, 20, -1);
      int i = this.width / 2 - 150;
      int j = this.width / 2 + 150;
      int k = this.height / 4 + 100;
      int l = k + 10;
      p_281829_.drawCenteredString(this.font, this.upgrader.getStatus(), this.width / 2, k - 9 - 2, -6250336);
      if (this.upgrader.getTotalChunks() > 0) {
         p_281829_.fill(i - 1, k - 1, j + 1, l + 1, -16777216);
         p_281829_.drawString(this.font, Component.translatable("optimizeWorld.info.converted", this.upgrader.getConverted()), i, 40, -6250336);
         p_281829_.drawString(this.font, Component.translatable("optimizeWorld.info.skipped", this.upgrader.getSkipped()), i, 52, -6250336);
         p_281829_.drawString(this.font, Component.translatable("optimizeWorld.info.total", this.upgrader.getTotalChunks()), i, 64, -6250336);
         int i1 = 0;

         for (ResourceKey<Level> resourcekey : this.upgrader.levels()) {
            int j1 = Mth.floor(this.upgrader.dimensionProgress(resourcekey) * (j - i));
            p_281829_.fill(i + i1, k, i + i1 + j1, l, DIMENSION_COLORS.applyAsInt(resourcekey));
            i1 += j1;
         }

         int k1 = this.upgrader.getConverted() + this.upgrader.getSkipped();
         Component component = Component.translatable("optimizeWorld.progress.counter", k1, this.upgrader.getTotalChunks());
         Component component1 = Component.translatable("optimizeWorld.progress.percentage", Mth.floor(this.upgrader.getProgress() * 100.0F));
         p_281829_.drawCenteredString(this.font, component, this.width / 2, k + 18 + 2, -6250336);
         p_281829_.drawCenteredString(this.font, component1, this.width / 2, k + (l - k) / 2 - 4, -6250336);
      }
   }
}
