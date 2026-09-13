package net.minecraft.client.gui.screens;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class LevelLoadingScreen extends Screen {
   private static final Component DOWNLOADING_TERRAIN_TEXT = Component.translatable("multiplayer.downloadingTerrain");
   private static final Component READY_TO_PLAY_TEXT = Component.translatable("narrator.ready_to_play");
   private static final long NARRATION_DELAY_MS = 2000L;
   private static final int PROGRESS_BAR_WIDTH = 200;
   private LevelLoadTracker loadTracker;
   private float smoothedProgress;
   private long lastNarration = -1L;
   private LevelLoadingScreen.Reason reason;
   private @Nullable TextureAtlasSprite cachedNetherPortalSprite;
   private static final Object2IntMap<ChunkStatus> COLORS = Util.make(new Object2IntOpenHashMap(), p_280803_ -> {
      p_280803_.defaultReturnValue(0);
      p_280803_.put(ChunkStatus.EMPTY, 5526612);
      p_280803_.put(ChunkStatus.STRUCTURE_STARTS, 10066329);
      p_280803_.put(ChunkStatus.STRUCTURE_REFERENCES, 6250897);
      p_280803_.put(ChunkStatus.BIOMES, 8434258);
      p_280803_.put(ChunkStatus.NOISE, 13750737);
      p_280803_.put(ChunkStatus.SURFACE, 7497737);
      p_280803_.put(ChunkStatus.CARVERS, 3159410);
      p_280803_.put(ChunkStatus.FEATURES, 2213376);
      p_280803_.put(ChunkStatus.INITIALIZE_LIGHT, 13421772);
      p_280803_.put(ChunkStatus.LIGHT, 16769184);
      p_280803_.put(ChunkStatus.SPAWN, 15884384);
      p_280803_.put(ChunkStatus.FULL, 16777215);
   });

   public LevelLoadingScreen(LevelLoadTracker p_431526_, LevelLoadingScreen.Reason p_424258_) {
      super(GameNarrator.NO_TITLE);
      this.loadTracker = p_431526_;
      this.reason = p_424258_;
   }

   public void update(LevelLoadTracker p_426338_, LevelLoadingScreen.Reason p_423642_) {
      this.loadTracker = p_426338_;
      this.reason = p_423642_;
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   protected boolean shouldNarrateNavigation() {
      return false;
   }

   @Override
   protected void updateNarratedWidget(NarrationElementOutput p_169312_) {
      if (this.loadTracker.hasProgress()) {
         p_169312_.add(NarratedElementType.TITLE, Component.translatable("loading.progress", Mth.floor(this.loadTracker.serverProgress() * 100.0F)));
      }
   }

   @Override
   public void tick() {
      super.tick();
      this.smoothedProgress = this.smoothedProgress + (this.loadTracker.serverProgress() - this.smoothedProgress) * 0.2F;
      if (this.loadTracker.isLevelReady()) {
         this.onClose();
      }
   }

   @Override
   public void render(GuiGraphics p_283534_, int p_96146_, int p_96147_, float p_96148_) {
      super.render(p_283534_, p_96146_, p_96147_, p_96148_);
      long i = Util.getMillis();
      if (i - this.lastNarration > 2000L) {
         this.lastNarration = i;
         this.triggerImmediateNarration(true);
      }

      int j = this.width / 2;
      int k = this.height / 2;
      ChunkLoadStatusView chunkloadstatusview = this.loadTracker.statusView();
      int l;
      if (chunkloadstatusview != null) {
         int i1 = 2;
         renderChunks(p_283534_, j, k, 2, 0, chunkloadstatusview);
         l = k - chunkloadstatusview.radius() * 2 - 27;
      } else {
         l = k - 50;
      }

      p_283534_.drawCenteredString(this.font, DOWNLOADING_TERRAIN_TEXT, j, l, -1);
      if (this.loadTracker.hasProgress()) {
         this.drawProgressBar(p_283534_, j - 100, l + 9 + 3, 200, 2, this.smoothedProgress);
      }
   }

   private void drawProgressBar(GuiGraphics p_425263_, int p_428547_, int p_424879_, int p_425894_, int p_428297_, float p_428964_) {
      p_425263_.fill(p_428547_, p_424879_, p_428547_ + p_425894_, p_424879_ + p_428297_, -16777216);
      p_425263_.fill(p_428547_, p_424879_, p_428547_ + Math.round(p_428964_ * p_425894_), p_424879_ + p_428297_, -16711936);
   }

   public static void renderChunks(GuiGraphics p_283467_, int p_96152_, int p_96153_, int p_96154_, int p_96155_, ChunkLoadStatusView p_426174_) {
      int i = p_96154_ + p_96155_;
      int j = p_426174_.radius() * 2 + 1;
      int k = j * i - p_96155_;
      int l = p_96152_ - k / 2;
      int i1 = p_96153_ - k / 2;
      if (Minecraft.getInstance().debugEntries.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_CHUNKS_ON_SERVER)) {
         int j1 = i / 2 + 1;
         p_283467_.fill(p_96152_ - j1, p_96153_ - j1, p_96152_ + j1, p_96153_ + j1, -65536);
      }

      for (int j2 = 0; j2 < j; j2++) {
         for (int k1 = 0; k1 < j; k1++) {
            ChunkStatus chunkstatus = p_426174_.get(j2, k1);
            int l1 = l + j2 * i;
            int i2 = i1 + k1 * i;
            p_283467_.fill(l1, i2, l1 + p_96154_, i2 + p_96154_, ARGB.opaque(COLORS.getInt(chunkstatus)));
         }
      }
   }

   @Override
   public void renderBackground(GuiGraphics p_431240_, int p_425628_, int p_431072_, float p_431014_) {
      switch (this.reason) {
         case NETHER_PORTAL:
            p_431240_.blitSprite(
               RenderPipelines.GUI_OPAQUE_TEXTURED_BACKGROUND, this.getNetherPortalSprite(), 0, 0, p_431240_.guiWidth(), p_431240_.guiHeight()
            );
            break;
         case END_PORTAL:
            TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
            AbstractTexture abstracttexture = texturemanager.getTexture(AbstractEndPortalRenderer.END_SKY_LOCATION);
            AbstractTexture abstracttexture1 = texturemanager.getTexture(AbstractEndPortalRenderer.END_PORTAL_LOCATION);
            TextureSetup texturesetup = TextureSetup.doubleTexture(
               abstracttexture.getTextureView(), abstracttexture.getSampler(), abstracttexture1.getTextureView(), abstracttexture1.getSampler()
            );
            p_431240_.fill(RenderPipelines.END_PORTAL, texturesetup, 0, 0, this.width, this.height);
            break;
         case OTHER:
            this.renderPanorama(p_431240_, p_431014_);
            this.renderBlurredBackground(p_431240_);
            this.renderMenuBackground(p_431240_);
      }
   }

   private TextureAtlasSprite getNetherPortalSprite() {
      if (this.cachedNetherPortalSprite != null) {
         return this.cachedNetherPortalSprite;
      }

      this.cachedNetherPortalSprite = this.minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.NETHER_PORTAL.defaultBlockState());
      return this.cachedNetherPortalSprite;
   }

   @Override
   public void onClose() {
      this.minecraft.getNarrator().saySystemNow(READY_TO_PLAY_TEXT);
      super.onClose();
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   public enum Reason {
      NETHER_PORTAL,
      END_PORTAL,
      OTHER;
   }
}
