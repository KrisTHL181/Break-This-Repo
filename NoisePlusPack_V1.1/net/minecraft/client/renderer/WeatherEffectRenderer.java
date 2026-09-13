package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.WeatherRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WeatherEffectRenderer {
   private static final float RAIN_PARTICLES_PER_BLOCK = 0.225F;
   private static final int RAIN_RADIUS = 10;
   private static final Identifier RAIN_LOCATION = Identifier.withDefaultNamespace("textures/environment/rain.png");
   private static final Identifier SNOW_LOCATION = Identifier.withDefaultNamespace("textures/environment/snow.png");
   private static final int RAIN_TABLE_SIZE = 32;
   private static final int HALF_RAIN_TABLE_SIZE = 16;
   private int rainSoundTime;
   private final float[] columnSizeX = new float[1024];
   private final float[] columnSizeZ = new float[1024];

   public WeatherEffectRenderer() {
      for (int i = 0; i < 32; i++) {
         for (int j = 0; j < 32; j++) {
            float f = j - 16;
            float f1 = i - 16;
            float f2 = Mth.length(f, f1);
            this.columnSizeX[i * 32 + j] = -f1 / f2;
            this.columnSizeZ[i * 32 + j] = f / f2;
         }
      }
   }

   public void extractRenderState(Level p_430760_, int p_428982_, float p_431567_, Vec3 p_427801_, WeatherRenderState p_426425_) {
      p_426425_.intensity = p_430760_.getRainLevel(p_431567_);
      if (!(p_426425_.intensity <= 0.0F)) {
         p_426425_.radius = Minecraft.getInstance().options.weatherRadius().get();
         int i = Mth.floor(p_427801_.x);
         int j = Mth.floor(p_427801_.y);
         int k = Mth.floor(p_427801_.z);
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
         RandomSource randomsource = RandomSource.create();

         for (int l = k - p_426425_.radius; l <= k + p_426425_.radius; l++) {
            for (int i1 = i - p_426425_.radius; i1 <= i + p_426425_.radius; i1++) {
               int j1 = p_430760_.getHeight(Heightmap.Types.MOTION_BLOCKING, i1, l);
               int k1 = Math.max(j - p_426425_.radius, j1);
               int l1 = Math.max(j + p_426425_.radius, j1);
               if (l1 - k1 != 0) {
                  Biome.Precipitation biome$precipitation = this.getPrecipitationAt(p_430760_, blockpos$mutableblockpos.set(i1, j, l));
                  if (biome$precipitation != Biome.Precipitation.NONE) {
                     int i2 = i1 * i1 * 3121 + i1 * 45238971 ^ l * l * 418711 + l * 13761;
                     randomsource.setSeed(i2);
                     int j2 = Math.max(j, j1);
                     int k2 = LevelRenderer.getLightColor(p_430760_, blockpos$mutableblockpos.set(i1, j2, l));
                     if (biome$precipitation == Biome.Precipitation.RAIN) {
                        p_426425_.rainColumns.add(this.createRainColumnInstance(randomsource, p_428982_, i1, k1, l1, l, k2, p_431567_));
                     } else if (biome$precipitation == Biome.Precipitation.SNOW) {
                        p_426425_.snowColumns.add(this.createSnowColumnInstance(randomsource, p_428982_, i1, k1, l1, l, k2, p_431567_));
                     }
                  }
               }
            }
         }
      }
   }

   public void render(MultiBufferSource p_375954_, Vec3 p_368504_, WeatherRenderState p_428296_) {
      if (!p_428296_.rainColumns.isEmpty()) {
         RenderType rendertype = RenderTypes.weather(RAIN_LOCATION, Minecraft.useShaderTransparency());
         this.renderInstances(p_375954_.getBuffer(rendertype), p_428296_.rainColumns, p_368504_, 1.0F, p_428296_.radius, p_428296_.intensity);
      }

      if (!p_428296_.snowColumns.isEmpty()) {
         RenderType rendertype1 = RenderTypes.weather(SNOW_LOCATION, Minecraft.useShaderTransparency());
         this.renderInstances(p_375954_.getBuffer(rendertype1), p_428296_.snowColumns, p_368504_, 0.8F, p_428296_.radius, p_428296_.intensity);
      }
   }

   private WeatherEffectRenderer.ColumnInstance createRainColumnInstance(
      RandomSource p_369207_, int p_369418_, int p_368589_, int p_364560_, int p_362596_, int p_368571_, int p_362548_, float p_362995_
   ) {
      int i = p_369418_ & 131071;
      int j = p_368589_ * p_368589_ * 3121 + p_368589_ * 45238971 + p_368571_ * p_368571_ * 418711 + p_368571_ * 13761 & 0xFF;
      float f = 3.0F + p_369207_.nextFloat();
      float f1 = -(i + j + p_362995_) / 32.0F * f;
      float f2 = f1 % 32.0F;
      return new WeatherEffectRenderer.ColumnInstance(p_368589_, p_368571_, p_364560_, p_362596_, 0.0F, f2, p_362548_);
   }

   private WeatherEffectRenderer.ColumnInstance createSnowColumnInstance(
      RandomSource p_362287_, int p_363885_, int p_367897_, int p_362095_, int p_364648_, int p_366422_, int p_369864_, float p_367820_
   ) {
      float f = p_363885_ + p_367820_;
      float f1 = (float)(p_362287_.nextDouble() + f * 0.01F * (float)p_362287_.nextGaussian());
      float f2 = (float)(p_362287_.nextDouble() + f * (float)p_362287_.nextGaussian() * 0.001F);
      float f3 = -((p_363885_ & 511) + p_367820_) / 512.0F;
      int i = LightTexture.pack((LightTexture.block(p_369864_) * 3 + 15) / 4, (LightTexture.sky(p_369864_) * 3 + 15) / 4);
      return new WeatherEffectRenderer.ColumnInstance(p_367897_, p_366422_, p_362095_, p_364648_, f1, f3 + f2, i);
   }

   private void renderInstances(
      VertexConsumer p_377303_, List<WeatherEffectRenderer.ColumnInstance> p_364835_, Vec3 p_367411_, float p_360961_, int p_369839_, float p_363459_
   ) {
      float f = p_369839_ * p_369839_;

      for (WeatherEffectRenderer.ColumnInstance weathereffectrenderer$columninstance : p_364835_) {
         float f1 = (float)(weathereffectrenderer$columninstance.x + 0.5 - p_367411_.x);
         float f2 = (float)(weathereffectrenderer$columninstance.z + 0.5 - p_367411_.z);
         float f3 = (float)Mth.lengthSquared(f1, f2);
         float f4 = Mth.lerp(Math.min(f3 / f, 1.0F), p_360961_, 0.5F) * p_363459_;
         int i = ARGB.white(f4);
         int j = (weathereffectrenderer$columninstance.z - Mth.floor(p_367411_.z) + 16) * 32
            + weathereffectrenderer$columninstance.x
            - Mth.floor(p_367411_.x)
            + 16;
         float f5 = this.columnSizeX[j] / 2.0F;
         float f6 = this.columnSizeZ[j] / 2.0F;
         float f7 = f1 - f5;
         float f8 = f1 + f5;
         float f9 = (float)(weathereffectrenderer$columninstance.topY - p_367411_.y);
         float f10 = (float)(weathereffectrenderer$columninstance.bottomY - p_367411_.y);
         float f11 = f2 - f6;
         float f12 = f2 + f6;
         float f13 = weathereffectrenderer$columninstance.uOffset + 0.0F;
         float f14 = weathereffectrenderer$columninstance.uOffset + 1.0F;
         float f15 = weathereffectrenderer$columninstance.bottomY * 0.25F + weathereffectrenderer$columninstance.vOffset;
         float f16 = weathereffectrenderer$columninstance.topY * 0.25F + weathereffectrenderer$columninstance.vOffset;
         p_377303_.addVertex(f7, f9, f11).setUv(f13, f15).setColor(i).setLight(weathereffectrenderer$columninstance.lightCoords);
         p_377303_.addVertex(f8, f9, f12).setUv(f14, f15).setColor(i).setLight(weathereffectrenderer$columninstance.lightCoords);
         p_377303_.addVertex(f8, f10, f12).setUv(f14, f16).setColor(i).setLight(weathereffectrenderer$columninstance.lightCoords);
         p_377303_.addVertex(f7, f10, f11).setUv(f13, f16).setColor(i).setLight(weathereffectrenderer$columninstance.lightCoords);
      }
   }

   public void tickRainParticles(ClientLevel p_365121_, Camera p_364267_, int p_360728_, ParticleStatus p_367686_, int p_455035_) {
      float f = p_365121_.getRainLevel(1.0F);
      if (!(f <= 0.0F)) {
         RandomSource randomsource = RandomSource.create(p_360728_ * 312987231L);
         BlockPos blockpos = BlockPos.containing(p_364267_.position());
         BlockPos blockpos1 = null;
         int i = 2 * p_455035_ + 1;
         int j = i * i;
         int k = (int)(0.225F * j * f * f) / (p_367686_ == ParticleStatus.DECREASED ? 2 : 1);

         for (int l = 0; l < k; l++) {
            int i1 = randomsource.nextInt(i) - p_455035_;
            int j1 = randomsource.nextInt(i) - p_455035_;
            BlockPos blockpos2 = p_365121_.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos.offset(i1, 0, j1));
            if (blockpos2.getY() > p_365121_.getMinY()
               && blockpos2.getY() <= blockpos.getY() + 10
               && blockpos2.getY() >= blockpos.getY() - 10
               && this.getPrecipitationAt(p_365121_, blockpos2) == Biome.Precipitation.RAIN) {
               blockpos1 = blockpos2.below();
               if (p_367686_ == ParticleStatus.MINIMAL) {
                  break;
               }

               double d0 = randomsource.nextDouble();
               double d1 = randomsource.nextDouble();
               BlockState blockstate = p_365121_.getBlockState(blockpos1);
               FluidState fluidstate = p_365121_.getFluidState(blockpos1);
               VoxelShape voxelshape = blockstate.getCollisionShape(p_365121_, blockpos1);
               double d2 = voxelshape.max(Direction.Axis.Y, d0, d1);
               double d3 = fluidstate.getHeight(p_365121_, blockpos1);
               double d4 = Math.max(d2, d3);
               ParticleOptions particleoptions = !fluidstate.is(FluidTags.LAVA)
                     && !blockstate.is(Blocks.MAGMA_BLOCK)
                     && !CampfireBlock.isLitCampfire(blockstate)
                  ? ParticleTypes.RAIN
                  : ParticleTypes.SMOKE;
               p_365121_.addParticle(particleoptions, blockpos1.getX() + d0, blockpos1.getY() + d4, blockpos1.getZ() + d1, 0.0, 0.0, 0.0);
            }
         }

         if (blockpos1 != null && randomsource.nextInt(3) < this.rainSoundTime++) {
            this.rainSoundTime = 0;
            if (blockpos1.getY() > blockpos.getY() + 1
               && p_365121_.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos).getY() > Mth.floor(blockpos.getY())) {
               p_365121_.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, 0.1F, 0.5F, false);
            } else {
               p_365121_.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.2F, 1.0F, false);
            }
         }
      }
   }

   private Biome.Precipitation getPrecipitationAt(Level p_360760_, BlockPos p_361577_) {
      if (!p_360760_.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(p_361577_.getX()), SectionPos.blockToSectionCoord(p_361577_.getZ()))) {
         return Biome.Precipitation.NONE;
      }

      Biome biome = p_360760_.getBiome(p_361577_).value();
      return biome.getPrecipitationAt(p_361577_, p_360760_.getSeaLevel());
   }

   @OnlyIn(Dist.CLIENT)
   public record ColumnInstance(int x, int z, int bottomY, int topY, float uOffset, float vOffset, int lightCoords) {
   }
}
