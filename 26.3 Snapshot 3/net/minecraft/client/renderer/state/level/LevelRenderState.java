package net.minecraft.client.renderer.state.level;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.jspecify.annotations.Nullable;

public class LevelRenderState {
   public CameraRenderState cameraRenderState = new CameraRenderState();
   public final List<SectionUpdateRenderState> sectionUpdateRenderStates = new ArrayList<>();
   public final List<EntityRenderState> entityRenderStates = new ArrayList<>();
   public final List<BlockEntityRenderState> blockEntityRenderStates = new ArrayList<>();
   public @Nullable BlockOutlineRenderState blockOutlineRenderState;
   public final List<BlockBreakingRenderState> blockBreakingRenderStates = new ArrayList<>();
   public final WeatherRenderState weatherRenderState = new WeatherRenderState();
   public final WorldBorderRenderState worldBorderRenderState = new WorldBorderRenderState();
   public final SkyRenderState skyRenderState = new SkyRenderState();
   public final ParticlesRenderState particlesRenderState = new ParticlesRenderState();
   public long gameTime;
   public int lastEntityRenderStateCount;
   public int cloudColor;
   public float cloudHeight;
   public boolean render3dCrosshair;
   public boolean renderWireframeTerrain;
   public @Nullable Runnable playerCompiledSectionCallback;
   public ChunkLoadingRenderState chunkLoadingRenderState = new ChunkLoadingRenderState();
   public boolean shouldResetChunkLayerSampler;
   public boolean shouldShowEntityOutlines;
   public boolean shouldResetSkyRenderer;

   public void reset() {
      this.sectionUpdateRenderStates.clear();
      this.entityRenderStates.clear();
      this.blockEntityRenderStates.clear();
      this.blockBreakingRenderStates.clear();
      this.blockOutlineRenderState = null;
      this.weatherRenderState.reset();
      this.worldBorderRenderState.reset();
      this.skyRenderState.reset();
      this.particlesRenderState.reset();
      this.chunkLoadingRenderState.reset();
      this.gameTime = 0L;
   }
}
