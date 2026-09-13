package net.minecraft.client.renderer.rendertype;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderTypes {
   static final BiFunction<Identifier, Boolean, RenderType> OUTLINE = Util.memoize(
      (p_456679_, p_456063_) -> RenderType.create(
         "outline",
         RenderSetup.builder(p_456063_ ? RenderPipelines.OUTLINE_CULL : RenderPipelines.OUTLINE_NO_CULL)
            .withTexture("Sampler0", p_456679_)
            .setOutputTarget(OutputTarget.OUTLINE_TARGET)
            .setOutline(RenderSetup.OutlineProperty.IS_OUTLINE)
            .createRenderSetup()
      )
   );
   public static final Supplier<GpuSampler> MOVING_BLOCK_SAMPLER = () -> RenderSystem.getSamplerCache()
      .getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.NEAREST, true);
   private static final RenderType SOLID_MOVING_BLOCK = RenderType.create(
      "solid_moving_block",
      RenderSetup.builder(RenderPipelines.SOLID_BLOCK)
         .useLightmap()
         .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS, MOVING_BLOCK_SAMPLER)
         .affectsCrumbling()
         .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
         .createRenderSetup()
   );
   private static final RenderType CUTOUT_MOVING_BLOCK = RenderType.create(
      "cutout_moving_block",
      RenderSetup.builder(RenderPipelines.CUTOUT_BLOCK)
         .useLightmap()
         .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS, MOVING_BLOCK_SAMPLER)
         .affectsCrumbling()
         .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
         .createRenderSetup()
   );
   private static final RenderType TRANSLUCENT_MOVING_BLOCK = RenderType.create(
      "translucent_moving_block",
      RenderSetup.builder(RenderPipelines.TRANSLUCENT_MOVING_BLOCK)
         .useLightmap()
         .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS, MOVING_BLOCK_SAMPLER)
         .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
         .sortOnUpload()
         .bufferSize(786432)
         .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
         .createRenderSetup()
   );
   private static final Function<Identifier, RenderType> ARMOR_CUTOUT_NO_CULL = Util.memoize(
      p_452267_ -> {
         RenderSetup rendersetup = RenderSetup.builder(RenderPipelines.ARMOR_CUTOUT_NO_CULL)
            .withTexture("Sampler0", p_452267_)
            .useLightmap()
            .useOverlay()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("armor_cutout_no_cull", rendersetup);
      }
   );
   private static final Function<Identifier, RenderType> ARMOR_TRANSLUCENT = Util.memoize(
      p_457036_ -> {
         RenderSetup rendersetup = RenderSetup.builder(RenderPipelines.ARMOR_TRANSLUCENT)
            .withTexture("Sampler0", p_457036_)
            .useLightmap()
            .useOverlay()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .affectsCrumbling()
            .sortOnUpload()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("armor_translucent", rendersetup);
      }
   );
   private static final Function<Identifier, RenderType> ENTITY_SOLID = Util.memoize(
      p_454553_ -> {
         RenderSetup rendersetup = RenderSetup.builder(RenderPipelines.ENTITY_SOLID)
            .withTexture("Sampler0", p_454553_)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("entity_solid", rendersetup);
      }
   );
   private static final Function<Identifier, RenderType> ENTITY_SOLID_Z_OFFSET_FORWARD = Util.memoize(
      p_456530_ -> {
         RenderSetup rendersetup = RenderSetup.builder(RenderPipelines.ENTITY_SOLID_Z_OFFSET_FORWARD)
            .withTexture("Sampler0", p_456530_)
            .useLightmap()
            .useOverlay()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING_FORWARD)
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("entity_solid_z_offset_forward", rendersetup);
      }
   );
   private static final Function<Identifier, RenderType> ENTITY_CUTOUT = Util.memoize(
      p_460713_ -> {
         RenderSetup rendersetup = RenderSetup.builder(RenderPipelines.ENTITY_CUTOUT)
            .withTexture("Sampler0", p_460713_)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("entity_cutout", rendersetup);
      }
   );
   private static final BiFunction<Identifier, Boolean, RenderType> ENTITY_CUTOUT_NO_CULL = Util.memoize(
      (p_454264_, p_457114_) -> {
         RenderSetup rendersetup = RenderSetup.builder(RenderPipelines.ENTITY_CUTOUT_NO_CULL)
            .withTexture("Sampler0", p_454264_)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .setOutline(p_457114_ ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE : RenderSetup.OutlineProperty.NONE)
            .createRenderSetup();
         return RenderType.create("entity_cutout_no_cull", rendersetup);
      }
   );
   private static final BiFunction<Identifier, Boolean, RenderType> ENTITY_CUTOUT_NO_CULL_Z_OFFSET = Util.memoize(
      (p_456019_, p_453688_) -> {
         RenderSetup rendersetup = RenderSetup.builder(RenderPipelines.ENTITY_CUTOUT_NO_CULL_Z_OFFSET)
            .withTexture("Sampler0", p_456019_)
            .useLightmap()
            .useOverlay()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .affectsCrumbling()
            .setOutline(p_453688_ ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE : RenderSetup.OutlineProperty.NONE)
            .createRenderSetup();
         return RenderType.create("entity_cutout_no_cull_z_offset", rendersetup);
      }
   );
   private static final Function<Identifier, RenderType> ITEM_ENTITY_TRANSLUCENT_CULL = Util.memoize(
      p_456607_ -> {
         RenderSetup rendersetup = RenderSetup.builder(RenderPipelines.ITEM_ENTITY_TRANSLUCENT_CULL)
            .withTexture("Sampler0", p_456607_)
            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .sortOnUpload()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("item_entity_translucent_cull", rendersetup);
      }
   );
   private static final BiFunction<Identifier, Boolean, RenderType> ENTITY_TRANSLUCENT = Util.memoize(
      (p_456910_, p_459434_) -> {
         RenderSetup rendersetup = RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT)
            .withTexture("Sampler0", p_456910_)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .sortOnUpload()
            .setOutline(p_459434_ ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE : RenderSetup.OutlineProperty.NONE)
            .createRenderSetup();
         return RenderType.create("entity_translucent", rendersetup);
      }
   );
   private static final BiFunction<Identifier, Boolean, RenderType> ENTITY_TRANSLUCENT_EMISSIVE = Util.memoize(
      (p_457062_, p_460213_) -> {
         RenderSetup rendersetup = RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE)
            .withTexture("Sampler0", p_457062_)
            .useOverlay()
            .affectsCrumbling()
            .sortOnUpload()
            .setOutline(p_460213_ ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE : RenderSetup.OutlineProperty.NONE)
            .createRenderSetup();
         return RenderType.create("entity_translucent_emissive", rendersetup);
      }
   );
   private static final Function<Identifier, RenderType> ENTITY_SMOOTH_CUTOUT = Util.memoize(
      p_452997_ -> {
         RenderSetup rendersetup = RenderSetup.builder(RenderPipelines.ENTITY_SMOOTH_CUTOUT)
            .withTexture("Sampler0", p_452997_)
            .useLightmap()
            .useOverlay()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("entity_smooth_cutout", rendersetup);
      }
   );
   private static final BiFunction<Identifier, Boolean, RenderType> BEACON_BEAM = Util.memoize(
      (p_457442_, p_460634_) -> {
         RenderSetup rendersetup = RenderSetup.builder(p_460634_ ? RenderPipelines.BEACON_BEAM_TRANSLUCENT : RenderPipelines.BEACON_BEAM_OPAQUE)
            .withTexture("Sampler0", p_457442_)
            .sortOnUpload()
            .createRenderSetup();
         return RenderType.create("beacon_beam", rendersetup);
      }
   );
   private static final Function<Identifier, RenderType> ENTITY_DECAL = Util.memoize(
      p_456695_ -> {
         RenderSetup rendersetup = RenderSetup.builder(RenderPipelines.ENTITY_DECAL)
            .withTexture("Sampler0", p_456695_)
            .useLightmap()
            .useOverlay()
            .createRenderSetup();
         return RenderType.create("entity_decal", rendersetup);
      }
   );
   private static final Function<Identifier, RenderType> ENTITY_NO_OUTLINE = Util.memoize(
      p_453608_ -> {
         RenderSetup rendersetup = RenderSetup.builder(RenderPipelines.ENTITY_NO_OUTLINE)
            .withTexture("Sampler0", p_453608_)
            .useLightmap()
            .useOverlay()
            .sortOnUpload()
            .createRenderSetup();
         return RenderType.create("entity_no_outline", rendersetup);
      }
   );
   private static final Function<Identifier, RenderType> ENTITY_SHADOW = Util.memoize(
      p_459916_ -> {
         RenderSetup rendersetup = RenderSetup.builder(RenderPipelines.ENTITY_SHADOW)
            .withTexture("Sampler0", p_459916_)
            .useLightmap()
            .useOverlay()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .createRenderSetup();
         return RenderType.create("entity_shadow", rendersetup);
      }
   );
   private static final Function<Identifier, RenderType> DRAGON_EXPLOSION_ALPHA = Util.memoize(
      p_451937_ -> {
         RenderSetup rendersetup = RenderSetup.builder(RenderPipelines.DRAGON_EXPLOSION_ALPHA)
            .withTexture("Sampler0", p_451937_)
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("entity_alpha", rendersetup);
      }
   );
   private static final Function<Identifier, RenderType> EYES = Util.memoize(
      p_450418_ -> RenderType.create("eyes", RenderSetup.builder(RenderPipelines.EYES).withTexture("Sampler0", p_450418_).sortOnUpload().createRenderSetup())
   );
   private static final RenderType LEASH = RenderType.create("leash", RenderSetup.builder(RenderPipelines.LEASH).useLightmap().createRenderSetup());
   private static final RenderType WATER_MASK = RenderType.create("water_mask", RenderSetup.builder(RenderPipelines.WATER_MASK).createRenderSetup());
   private static final RenderType ARMOR_ENTITY_GLINT = RenderType.create(
      "armor_entity_glint",
      RenderSetup.builder(RenderPipelines.GLINT)
         .withTexture("Sampler0", ItemRenderer.ENCHANTED_GLINT_ARMOR)
         .setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
         .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
         .createRenderSetup()
   );
   private static final RenderType GLINT_TRANSLUCENT = RenderType.create(
      "glint_translucent",
      RenderSetup.builder(RenderPipelines.GLINT)
         .withTexture("Sampler0", ItemRenderer.ENCHANTED_GLINT_ITEM)
         .setTextureTransform(TextureTransform.GLINT_TEXTURING)
         .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
         .createRenderSetup()
   );
   private static final RenderType GLINT = RenderType.create(
      "glint",
      RenderSetup.builder(RenderPipelines.GLINT)
         .withTexture("Sampler0", ItemRenderer.ENCHANTED_GLINT_ITEM)
         .setTextureTransform(TextureTransform.GLINT_TEXTURING)
         .createRenderSetup()
   );
   private static final RenderType ENTITY_GLINT = RenderType.create(
      "entity_glint",
      RenderSetup.builder(RenderPipelines.GLINT)
         .withTexture("Sampler0", ItemRenderer.ENCHANTED_GLINT_ITEM)
         .setTextureTransform(TextureTransform.ENTITY_GLINT_TEXTURING)
         .createRenderSetup()
   );
   private static final Function<Identifier, RenderType> CRUMBLING = Util.memoize(
      p_450448_ -> RenderType.create(
         "crumbling", RenderSetup.builder(RenderPipelines.CRUMBLING).withTexture("Sampler0", p_450448_).sortOnUpload().createRenderSetup()
      )
   );
   private static final Function<Identifier, RenderType> TEXT = Util.memoize(
      p_450546_ -> RenderType.create(
         "text", RenderSetup.builder(RenderPipelines.TEXT).withTexture("Sampler0", p_450546_).useLightmap().bufferSize(786432).createRenderSetup()
      )
   );
   private static final RenderType TEXT_BACKGROUND = RenderType.create(
      "text_background", RenderSetup.builder(RenderPipelines.TEXT_BACKGROUND).useLightmap().sortOnUpload().createRenderSetup()
   );
   private static final Function<Identifier, RenderType> TEXT_INTENSITY = Util.memoize(
      p_456402_ -> RenderType.create(
         "text_intensity",
         RenderSetup.builder(RenderPipelines.TEXT_INTENSITY).withTexture("Sampler0", p_456402_).useLightmap().bufferSize(786432).createRenderSetup()
      )
   );
   private static final Function<Identifier, RenderType> TEXT_POLYGON_OFFSET = Util.memoize(
      p_455387_ -> RenderType.create(
         "text_polygon_offset",
         RenderSetup.builder(RenderPipelines.TEXT_POLYGON_OFFSET).withTexture("Sampler0", p_455387_).useLightmap().sortOnUpload().createRenderSetup()
      )
   );
   private static final Function<Identifier, RenderType> TEXT_INTENSITY_POLYGON_OFFSET = Util.memoize(
      p_457768_ -> RenderType.create(
         "text_intensity_polygon_offset",
         RenderSetup.builder(RenderPipelines.TEXT_INTENSITY).withTexture("Sampler0", p_457768_).useLightmap().sortOnUpload().createRenderSetup()
      )
   );
   private static final Function<Identifier, RenderType> TEXT_SEE_THROUGH = Util.memoize(
      p_450751_ -> RenderType.create(
         "text_see_through", RenderSetup.builder(RenderPipelines.TEXT_SEE_THROUGH).withTexture("Sampler0", p_450751_).useLightmap().createRenderSetup()
      )
   );
   private static final RenderType TEXT_BACKGROUND_SEE_THROUGH = RenderType.create(
      "text_background_see_through", RenderSetup.builder(RenderPipelines.TEXT_BACKGROUND_SEE_THROUGH).useLightmap().sortOnUpload().createRenderSetup()
   );
   private static final Function<Identifier, RenderType> TEXT_INTENSITY_SEE_THROUGH = Util.memoize(
      p_457934_ -> RenderType.create(
         "text_intensity_see_through",
         RenderSetup.builder(RenderPipelines.TEXT_INTENSITY_SEE_THROUGH).withTexture("Sampler0", p_457934_).useLightmap().sortOnUpload().createRenderSetup()
      )
   );
   private static final RenderType LIGHTNING = RenderType.create(
      "lightning", RenderSetup.builder(RenderPipelines.LIGHTNING).setOutputTarget(OutputTarget.WEATHER_TARGET).sortOnUpload().createRenderSetup()
   );
   private static final RenderType DRAGON_RAYS = RenderType.create("dragon_rays", RenderSetup.builder(RenderPipelines.DRAGON_RAYS).createRenderSetup());
   private static final RenderType DRAGON_RAYS_DEPTH = RenderType.create(
      "dragon_rays_depth", RenderSetup.builder(RenderPipelines.DRAGON_RAYS_DEPTH).createRenderSetup()
   );
   private static final RenderType TRIPWIRE_MOVING_BLOCk = RenderType.create(
      "tripwire_moving_block",
      RenderSetup.builder(RenderPipelines.TRIPWIRE_BLOCK)
         .useLightmap()
         .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS, MOVING_BLOCK_SAMPLER)
         .setOutputTarget(OutputTarget.WEATHER_TARGET)
         .affectsCrumbling()
         .sortOnUpload()
         .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
         .createRenderSetup()
   );
   private static final RenderType END_PORTAL = RenderType.create(
      "end_portal",
      RenderSetup.builder(RenderPipelines.END_PORTAL)
         .withTexture("Sampler0", AbstractEndPortalRenderer.END_SKY_LOCATION)
         .withTexture("Sampler1", AbstractEndPortalRenderer.END_PORTAL_LOCATION)
         .createRenderSetup()
   );
   private static final RenderType END_GATEWAY = RenderType.create(
      "end_gateway",
      RenderSetup.builder(RenderPipelines.END_GATEWAY)
         .withTexture("Sampler0", AbstractEndPortalRenderer.END_SKY_LOCATION)
         .withTexture("Sampler1", AbstractEndPortalRenderer.END_PORTAL_LOCATION)
         .createRenderSetup()
   );
   public static final RenderType LINES = RenderType.create(
      "lines",
      RenderSetup.builder(RenderPipelines.LINES)
         .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
         .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
         .createRenderSetup()
   );
   public static final RenderType LINES_TRANSLUCENT = RenderType.create(
      "lines_translucent",
      RenderSetup.builder(RenderPipelines.LINES_TRANSLUCENT)
         .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
         .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
         .createRenderSetup()
   );
   public static final RenderType SECONDARY_BLOCK_OUTLINE = RenderType.create(
      "secondary_block_outline",
      RenderSetup.builder(RenderPipelines.SECONDARY_BLOCK_OUTLINE)
         .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
         .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
         .createRenderSetup()
   );
   private static final RenderType DEBUG_FILLED_BOX = RenderType.create(
      "debug_filled_box",
      RenderSetup.builder(RenderPipelines.DEBUG_FILLED_BOX).sortOnUpload().setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).createRenderSetup()
   );
   private static final RenderType DEBUG_POINT = RenderType.create("debug_point", RenderSetup.builder(RenderPipelines.DEBUG_POINTS).createRenderSetup());
   private static final RenderType DEBUG_QUADS = RenderType.create(
      "debug_quads", RenderSetup.builder(RenderPipelines.DEBUG_QUADS).sortOnUpload().createRenderSetup()
   );
   private static final RenderType DEBUG_TRIANGLE_FAN = RenderType.create(
      "debug_triangle_fan", RenderSetup.builder(RenderPipelines.DEBUG_TRIANGLE_FAN).sortOnUpload().createRenderSetup()
   );
   private static final Function<Identifier, RenderType> WEATHER_DEPTH_WRITE = createWeather(RenderPipelines.WEATHER_DEPTH_WRITE);
   private static final Function<Identifier, RenderType> WEATHER_NO_DEPTH_WRITE = createWeather(RenderPipelines.WEATHER_NO_DEPTH_WRITE);
   private static final Function<Identifier, RenderType> BLOCK_SCREEN_EFFECT = Util.memoize(
      p_451189_ -> RenderType.create(
         "block_screen_effect", RenderSetup.builder(RenderPipelines.BLOCK_SCREEN_EFFECT).withTexture("Sampler0", p_451189_).createRenderSetup()
      )
   );
   private static final Function<Identifier, RenderType> FIRE_SCREEN_EFFECT = Util.memoize(
      p_452202_ -> RenderType.create(
         "fire_screen_effect", RenderSetup.builder(RenderPipelines.FIRE_SCREEN_EFFECT).withTexture("Sampler0", p_452202_).createRenderSetup()
      )
   );

   public static RenderType solidMovingBlock() {
      return SOLID_MOVING_BLOCK;
   }

   public static RenderType cutoutMovingBlock() {
      return CUTOUT_MOVING_BLOCK;
   }

   public static RenderType translucentMovingBlock() {
      return TRANSLUCENT_MOVING_BLOCK;
   }

   public static RenderType armorCutoutNoCull(Identifier p_451066_) {
      return ARMOR_CUTOUT_NO_CULL.apply(p_451066_);
   }

   public static RenderType createArmorDecalCutoutNoCull(Identifier p_456320_) {
      RenderSetup rendersetup = RenderSetup.builder(RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL)
         .withTexture("Sampler0", p_456320_)
         .useLightmap()
         .useOverlay()
         .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
         .affectsCrumbling()
         .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
         .createRenderSetup();
      return RenderType.create("armor_decal_cutout_no_cull", rendersetup);
   }

   public static RenderType armorTranslucent(Identifier p_456826_) {
      return ARMOR_TRANSLUCENT.apply(p_456826_);
   }

   public static RenderType entitySolid(Identifier p_459492_) {
      return ENTITY_SOLID.apply(p_459492_);
   }

   public static RenderType entitySolidZOffsetForward(Identifier p_456827_) {
      return ENTITY_SOLID_Z_OFFSET_FORWARD.apply(p_456827_);
   }

   public static RenderType entityCutout(Identifier p_455921_) {
      return ENTITY_CUTOUT.apply(p_455921_);
   }

   public static RenderType entityCutoutNoCull(Identifier p_455269_, boolean p_452134_) {
      return ENTITY_CUTOUT_NO_CULL.apply(p_455269_, p_452134_);
   }

   public static RenderType entityCutoutNoCull(Identifier p_455118_) {
      return entityCutoutNoCull(p_455118_, true);
   }

   public static RenderType entityCutoutNoCullZOffset(Identifier p_457116_, boolean p_459459_) {
      return ENTITY_CUTOUT_NO_CULL_Z_OFFSET.apply(p_457116_, p_459459_);
   }

   public static RenderType entityCutoutNoCullZOffset(Identifier p_457132_) {
      return entityCutoutNoCullZOffset(p_457132_, true);
   }

   public static RenderType itemEntityTranslucentCull(Identifier p_452518_) {
      return ITEM_ENTITY_TRANSLUCENT_CULL.apply(p_452518_);
   }

   public static RenderType entityTranslucent(Identifier p_460959_, boolean p_458207_) {
      return ENTITY_TRANSLUCENT.apply(p_460959_, p_458207_);
   }

   public static RenderType entityTranslucent(Identifier p_460575_) {
      return entityTranslucent(p_460575_, true);
   }

   public static RenderType entityTranslucentEmissive(Identifier p_460609_, boolean p_453862_) {
      return ENTITY_TRANSLUCENT_EMISSIVE.apply(p_460609_, p_453862_);
   }

   public static RenderType entityTranslucentEmissive(Identifier p_460314_) {
      return entityTranslucentEmissive(p_460314_, true);
   }

   public static RenderType entitySmoothCutout(Identifier p_456718_) {
      return ENTITY_SMOOTH_CUTOUT.apply(p_456718_);
   }

   public static RenderType beaconBeam(Identifier p_459004_, boolean p_457551_) {
      return BEACON_BEAM.apply(p_459004_, p_457551_);
   }

   public static RenderType entityDecal(Identifier p_451479_) {
      return ENTITY_DECAL.apply(p_451479_);
   }

   public static RenderType entityNoOutline(Identifier p_452511_) {
      return ENTITY_NO_OUTLINE.apply(p_452511_);
   }

   public static RenderType entityShadow(Identifier p_455302_) {
      return ENTITY_SHADOW.apply(p_455302_);
   }

   public static RenderType dragonExplosionAlpha(Identifier p_461074_) {
      return DRAGON_EXPLOSION_ALPHA.apply(p_461074_);
   }

   public static RenderType eyes(Identifier p_451607_) {
      return EYES.apply(p_451607_);
   }

   public static RenderType breezeEyes(Identifier p_460073_) {
      return ENTITY_TRANSLUCENT_EMISSIVE.apply(p_460073_, false);
   }

   public static RenderType breezeWind(Identifier p_451678_, float p_451181_, float p_450604_) {
      return RenderType.create(
         "breeze_wind",
         RenderSetup.builder(RenderPipelines.BREEZE_WIND)
            .withTexture("Sampler0", p_451678_)
            .setTextureTransform(new TextureTransform.OffsetTextureTransform(p_451181_, p_450604_))
            .useLightmap()
            .sortOnUpload()
            .createRenderSetup()
      );
   }

   public static RenderType energySwirl(Identifier p_454362_, float p_457157_, float p_454885_) {
      return RenderType.create(
         "energy_swirl",
         RenderSetup.builder(RenderPipelines.ENERGY_SWIRL)
            .withTexture("Sampler0", p_454362_)
            .setTextureTransform(new TextureTransform.OffsetTextureTransform(p_457157_, p_454885_))
            .useLightmap()
            .useOverlay()
            .sortOnUpload()
            .createRenderSetup()
      );
   }

   public static RenderType leash() {
      return LEASH;
   }

   public static RenderType waterMask() {
      return WATER_MASK;
   }

   public static RenderType outline(Identifier p_450306_) {
      return OUTLINE.apply(p_450306_, false);
   }

   public static RenderType armorEntityGlint() {
      return ARMOR_ENTITY_GLINT;
   }

   public static RenderType glintTranslucent() {
      return GLINT_TRANSLUCENT;
   }

   public static RenderType glint() {
      return GLINT;
   }

   public static RenderType entityGlint() {
      return ENTITY_GLINT;
   }

   public static RenderType crumbling(Identifier p_457416_) {
      return CRUMBLING.apply(p_457416_);
   }

   public static RenderType text(Identifier p_456274_) {
      return TEXT.apply(p_456274_);
   }

   public static RenderType textBackground() {
      return TEXT_BACKGROUND;
   }

   public static RenderType textIntensity(Identifier p_460070_) {
      return TEXT_INTENSITY.apply(p_460070_);
   }

   public static RenderType textPolygonOffset(Identifier p_450888_) {
      return TEXT_POLYGON_OFFSET.apply(p_450888_);
   }

   public static RenderType textIntensityPolygonOffset(Identifier p_457059_) {
      return TEXT_INTENSITY_POLYGON_OFFSET.apply(p_457059_);
   }

   public static RenderType textSeeThrough(Identifier p_452096_) {
      return TEXT_SEE_THROUGH.apply(p_452096_);
   }

   public static RenderType textBackgroundSeeThrough() {
      return TEXT_BACKGROUND_SEE_THROUGH;
   }

   public static RenderType textIntensitySeeThrough(Identifier p_453978_) {
      return TEXT_INTENSITY_SEE_THROUGH.apply(p_453978_);
   }

   public static RenderType lightning() {
      return LIGHTNING;
   }

   public static RenderType dragonRays() {
      return DRAGON_RAYS;
   }

   public static RenderType dragonRaysDepth() {
      return DRAGON_RAYS_DEPTH;
   }

   public static RenderType tripwireMovingBlock() {
      return TRIPWIRE_MOVING_BLOCk;
   }

   public static RenderType endPortal() {
      return END_PORTAL;
   }

   public static RenderType endGateway() {
      return END_GATEWAY;
   }

   public static RenderType lines() {
      return LINES;
   }

   public static RenderType linesTranslucent() {
      return LINES_TRANSLUCENT;
   }

   public static RenderType secondaryBlockOutline() {
      return SECONDARY_BLOCK_OUTLINE;
   }

   public static RenderType debugFilledBox() {
      return DEBUG_FILLED_BOX;
   }

   public static RenderType debugPoint() {
      return DEBUG_POINT;
   }

   public static RenderType debugQuads() {
      return DEBUG_QUADS;
   }

   public static RenderType debugTriangleFan() {
      return DEBUG_TRIANGLE_FAN;
   }

   private static Function<Identifier, RenderType> createWeather(RenderPipeline p_455942_) {
      return Util.memoize(
         p_454705_ -> RenderType.create(
            "weather",
            RenderSetup.builder(p_455942_).withTexture("Sampler0", p_454705_).setOutputTarget(OutputTarget.WEATHER_TARGET).useLightmap().createRenderSetup()
         )
      );
   }

   public static RenderType weather(Identifier p_460413_, boolean p_460850_) {
      return (p_460850_ ? WEATHER_DEPTH_WRITE : WEATHER_NO_DEPTH_WRITE).apply(p_460413_);
   }

   public static RenderType blockScreenEffect(Identifier p_454996_) {
      return BLOCK_SCREEN_EFFECT.apply(p_454996_);
   }

   public static RenderType fireScreenEffect(Identifier p_452501_) {
      return FIRE_SCREEN_EFFECT.apply(p_452501_);
   }
}
