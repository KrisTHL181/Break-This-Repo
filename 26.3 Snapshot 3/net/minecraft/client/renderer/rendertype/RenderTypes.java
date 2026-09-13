package net.minecraft.client.renderer.rendertype;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public class RenderTypes {
   static final BiFunction<Identifier, Boolean, RenderType> OUTLINE = Util.memoize(
      (texture, cullState) -> RenderType.create(
         "outline",
         RenderSetup.builder(cullState ? RenderPipelines.OUTLINE_CULL : RenderPipelines.OUTLINE_NO_CULL)
            .withTexture("Sampler0", texture)
            .setOutline(RenderSetup.OutlineProperty.IS_OUTLINE)
            .createRenderSetup()
      )
   );
   private static final RenderType SOLID_MOVING_BLOCK = RenderType.create("solid_moving_block", createMovingBlockSetup(RenderPipelines.SOLID_BLOCK, false));
   private static final RenderType CUTOUT_MOVING_BLOCK = RenderType.create("cutout_moving_block", createMovingBlockSetup(RenderPipelines.CUTOUT_BLOCK, false));
   private static final RenderType TRANSLUCENT_MOVING_BLOCK = RenderType.create(
      "translucent_moving_block", createMovingBlockSetup(RenderPipelines.TRANSLUCENT_BLOCK, true)
   );
   private static final Function<Identifier, RenderType> ARMOR_CUTOUT_NO_CULL = Util.memoize(
      texture -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ARMOR_CUTOUT_NO_CULL)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("armor_cutout_no_cull", state);
      }
   );
   private static final Function<Identifier, RenderType> ARMOR_CUTOUT_NO_CULL_GLINT = Util.memoize(
      texture -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ARMOR_CUTOUT_NO_CULL_GLINT)
            .withTexture("Sampler0", texture)
            .withTexture("GlintSampler", ItemFeatureRenderer.ENCHANTED_GLINT_ARMOR)
            .setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
            .useLightmap()
            .useOverlay()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("armor_cutout_no_cull_glint", state);
      }
   );
   private static final Function<Identifier, RenderType> ARMOR_TRIM = Util.memoize(
      texture -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ARMOR_CUTOUT_NO_CULL)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("armor_trim", state);
      }
   );
   private static final Function<Identifier, RenderType> ARMOR_TRIM_DECAL = Util.memoize(
      texture -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("armor_trim_decal", state);
      }
   );
   private static final Function<Identifier, RenderType> WOLF_ARMOR_CRACKS = Util.memoize(
      texture -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.WOLF_ARMOR_CRACKS)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .affectsCrumbling()
            .sortOnUpload()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .withForcedSolidModelPhase()
            .createRenderSetup();
         return RenderType.create("wolf_armor_cracks", state);
      }
   );
   private static final Function<Identifier, RenderType> ENTITY_SOLID = Util.memoize(
      texture -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ENTITY_SOLID)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("entity_solid", state);
      }
   );
   private static final Function<Identifier, RenderType> ENTITY_SOLID_GLINT = Util.memoize(
      texture -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ENTITY_SOLID_GLINT)
            .withTexture("Sampler0", texture)
            .withTexture("GlintSampler", ItemFeatureRenderer.ENCHANTED_GLINT_ITEM)
            .setTextureTransform(TextureTransform.ENTITY_GLINT_TEXTURING)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("entity_solid_glint", state);
      }
   );
   private static final Function<Identifier, RenderType> ENTITY_SOLID_Z_OFFSET_FORWARD = Util.memoize(
      texture -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ENTITY_SOLID_Z_OFFSET_FORWARD)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING_FORWARD)
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("entity_solid_z_offset_forward", state);
      }
   );
   private static final Function<Identifier, RenderType> ENTITY_CUTOUT_CULL = Util.memoize(
      texture -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ENTITY_CUTOUT_CULL)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("entity_cutout_cull", state);
      }
   );
   private static final BiFunction<Identifier, Boolean, RenderType> ENTITY_CUTOUT = Util.memoize(
      (texture, affectsOutline) -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ENTITY_CUTOUT)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .setOutline(affectsOutline ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE : RenderSetup.OutlineProperty.NONE)
            .createRenderSetup();
         return RenderType.create("entity_cutout", state);
      }
   );
   private static final BiFunction<Identifier, Boolean, RenderType> ENTITY_CUTOUT_Z_OFFSET = Util.memoize(
      (texture, affectsOutline) -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ENTITY_CUTOUT_Z_OFFSET)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .affectsCrumbling()
            .setOutline(affectsOutline ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE : RenderSetup.OutlineProperty.NONE)
            .createRenderSetup();
         return RenderType.create("entity_cutout_z_offset", state);
      }
   );
   private static final BiFunction<Identifier, Identifier, RenderType> ENTITY_CUTOUT_DISSOLVE = Util.memoize(
      (texture, maskTexture) -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ENTITY_CUTOUT_DISSOLVE)
            .withTexture("Sampler0", texture)
            .withTexture("DissolveMaskSampler", maskTexture)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("entity_cutout_dissolve", state);
      }
   );
   private static final Function<Identifier, RenderType> ENTITY_TRANSLUCENT_CULL = Util.memoize(
      texture -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT_CULL)
            .setOitPipelines(RenderPipelines.OIT_ENTITY_CULL)
            .setOpaquePartsPipeline(RenderPipelines.ENTITY_TRANSLUCENT_CULL_OPAQUE_PARTS)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .sortOnUpload()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("entity_translucent_cull_item_target", state);
      }
   );
   private static final Function<Identifier, RenderType> ITEM_CUTOUT = Util.memoize(
      texture -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ITEM_CUTOUT)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("item_cutout", state);
      }
   );
   private static final Function<Identifier, RenderType> ITEM_CUTOUT_GLINT = Util.memoize(
      texture -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ITEM_CUTOUT_GLINT)
            .withTexture("Sampler0", texture)
            .withTexture("GlintSampler", ItemFeatureRenderer.ENCHANTED_GLINT_ITEM)
            .setTextureTransform(TextureTransform.GLINT_TEXTURING)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("item_cutout_glint", state);
      }
   );
   private static final Function<Identifier, RenderType> ITEM_CUTOUT_GLINT_SPECIAL = Util.memoize(
      texture -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ITEM_CUTOUT_GLINT_SPECIAL)
            .withTexture("Sampler0", texture)
            .withTexture("GlintSampler", ItemFeatureRenderer.ENCHANTED_GLINT_ITEM)
            .setTextureTransform(TextureTransform.GLINT_TEXTURING)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("item_cutout_glint_special", state);
      }
   );
   private static final Function<Identifier, RenderType> ITEM_TRANSLUCENT = Util.memoize(
      texture -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ITEM_TRANSLUCENT)
            .setOitPipelines(RenderPipelines.OIT_ITEM)
            .setOpaquePartsPipeline(RenderPipelines.ITEM_TRANSLUCENT_OPAQUE_PARTS)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .sortOnUpload()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("item_translucent", state);
      }
   );
   private static final Function<Identifier, RenderType> ITEM_TRANSLUCENT_GLINT = Util.memoize(
      texture -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ITEM_TRANSLUCENT_GLINT)
            .setOitPipelines(RenderPipelines.OIT_ITEM_GLINT)
            .setOpaquePartsPipeline(RenderPipelines.ITEM_TRANSLUCENT_GLINT_OPAQUE_PARTS)
            .withTexture("Sampler0", texture)
            .withTexture("GlintSampler", ItemFeatureRenderer.ENCHANTED_GLINT_ITEM)
            .setTextureTransform(TextureTransform.GLINT_TEXTURING)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .sortOnUpload()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("item_translucent_glint", state);
      }
   );
   private static final Function<Identifier, RenderType> ITEM_TRANSLUCENT_GLINT_SPECIAL = Util.memoize(
      texture -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ITEM_TRANSLUCENT_GLINT_SPECIAL)
            .setOitPipelines(RenderPipelines.OIT_ITEM_GLINT_SPECIAL)
            .setOpaquePartsPipeline(RenderPipelines.ITEM_TRANSLUCENT_GLINT_SPECIAL_OPAQUE_PARTS)
            .withTexture("Sampler0", texture)
            .withTexture("GlintSampler", ItemFeatureRenderer.ENCHANTED_GLINT_ITEM)
            .setTextureTransform(TextureTransform.GLINT_TEXTURING)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .sortOnUpload()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
         return RenderType.create("item_translucent_glint_special", state);
      }
   );
   private static final BiFunction<Identifier, Boolean, RenderType> ENTITY_TRANSLUCENT = Util.memoize(
      (texture, affectsOutline) -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT)
            .setOitPipelines(RenderPipelines.OIT_ENTITY)
            .setOpaquePartsPipeline(RenderPipelines.ENTITY_TRANSLUCENT_OPAQUE_PARTS)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .sortOnUpload()
            .setOutline(affectsOutline ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE : RenderSetup.OutlineProperty.NONE)
            .createRenderSetup();
         return RenderType.create("entity_translucent", state);
      }
   );
   private static final BiFunction<Identifier, Boolean, RenderType> ENTITY_TRANSLUCENT_EMISSIVE = Util.memoize(
      (texture, affectsOutline) -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE)
            .setOitPipelines(RenderPipelines.OIT_ENTITY_EMISSIVE)
            .setOpaquePartsPipeline(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE_OPAQUE_PARTS)
            .withTexture("Sampler0", texture)
            .useOverlay()
            .affectsCrumbling()
            .sortOnUpload()
            .setOutline(affectsOutline ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE : RenderSetup.OutlineProperty.NONE)
            .createRenderSetup();
         return RenderType.create("entity_translucent_emissive", state);
      }
   );
   private static final Function<Identifier, RenderType> END_CRYSTAL_BEAM = Util.memoize(
      texture -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.END_CRYSTAL_BEAM)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .setOutline(RenderSetup.OutlineProperty.NONE)
            .createRenderSetup();
         return RenderType.create("end_crystal_beam", state);
      }
   );
   private static final BiFunction<Identifier, Boolean, RenderType> BEACON_BEAM = Util.memoize(
      (texture, translucent) -> {
         RenderSetup.RenderSetupBuilder builder = RenderSetup.builder(
               translucent ? RenderPipelines.BEACON_BEAM_TRANSLUCENT : RenderPipelines.BEACON_BEAM_OPAQUE
            )
            .withTexture("Sampler0", texture)
            .sortOnUpload();
         if (translucent) {
            builder.setOitPipelines(RenderPipelines.OIT_BEACON_BEAM);
         }

         return RenderType.create("beacon_beam", builder.createRenderSetup());
      }
   );
   private static final Function<Identifier, RenderType> BANNER_PATTERN = Util.memoize(
      texture -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.BANNER_PATTERN)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .sortOnUpload()
            .withForcedSolidModelPhase()
            .createRenderSetup();
         return RenderType.create("banner_pattern", state);
      }
   );
   private static final Function<Identifier, RenderType> ENTITY_SHADOW = Util.memoize(
      texture -> {
         RenderSetup state = RenderSetup.builder(RenderPipelines.ENTITY_SHADOW)
            .setOitPipelines(RenderPipelines.OIT_ENTITY_SHADOW)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .createRenderSetup();
         return RenderType.create("entity_shadow", state);
      }
   );
   private static final Function<Identifier, RenderType> EYES = Util.memoize(
      texture -> RenderType.create(
         "eyes",
         RenderSetup.builder(RenderPipelines.EYES)
            .setOitPipelines(RenderPipelines.OIT_EYES)
            .setOpaquePartsPipeline(RenderPipelines.EYES_OPAQUE_PARTS)
            .withTexture("Sampler0", texture)
            .sortOnUpload()
            .createRenderSetup()
      )
   );
   private static final RenderType LEASH = RenderType.create("leash", RenderSetup.builder(RenderPipelines.LEASH).useLightmap().createRenderSetup());
   private static final RenderType WATER_MASK = RenderType.create("water_mask", RenderSetup.builder(RenderPipelines.WATER_MASK).createRenderSetup());
   private static final RenderType OIT_WATER_MASK = RenderType.create("oit_water_mask", RenderSetup.builder(RenderPipelines.OIT_WATER_MASK).createRenderSetup());
   private static final RenderType PATTERNED_SHIELD_GLINT = RenderType.create(
      "patterned_shield_glint",
      RenderSetup.builder(RenderPipelines.GLINT)
         .withTexture("Sampler0", ItemFeatureRenderer.ENCHANTED_GLINT_ITEM)
         .setTextureTransform(TextureTransform.ENTITY_GLINT_TEXTURING)
         .withForcedSolidModelPhase()
         .createRenderSetup()
   );
   private static final RenderType TRIMMED_ARMOR_GLINT = RenderType.create(
      "trimmed_armor_glint",
      RenderSetup.builder(RenderPipelines.GLINT)
         .withTexture("Sampler0", ItemFeatureRenderer.ENCHANTED_GLINT_ARMOR)
         .setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
         .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
         .withForcedSolidModelPhase()
         .createRenderSetup()
   );
   private static final Function<Identifier, RenderType> CRUMBLING = Util.memoize(
      texture -> RenderType.create(
         "crumbling",
         RenderSetup.builder(RenderPipelines.CRUMBLING)
            .setOitPipelines(RenderPipelines.OIT_CRUMBLING)
            .withTexture("Sampler0", texture)
            .sortOnUpload()
            .createRenderSetup()
      )
   );
   private static final Function<Identifier, RenderType> TEXT = Util.memoize(
      texture -> RenderType.create(
         "text",
         RenderSetup.builder(RenderPipelines.TEXT).setOitPipelines(RenderPipelines.OIT_TEXT).withTexture("Sampler0", texture).useLightmap().createRenderSetup()
      )
   );
   private static final RenderType TEXT_BACKGROUND = RenderType.create(
      "text_background",
      RenderSetup.builder(RenderPipelines.TEXT_BACKGROUND)
         .setOitPipelines(RenderPipelines.OIT_TEXT_BACKGROUND)
         .useLightmap()
         .sortOnUpload()
         .createRenderSetup()
   );
   private static final Function<Identifier, RenderType> TEXT_GRAYSCALE = Util.memoize(
      texture -> RenderType.create(
         "text_grayscale",
         RenderSetup.builder(RenderPipelines.TEXT_GRAYSCALE)
            .setOitPipelines(RenderPipelines.OIT_TEXT_GRAYSCALE)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .createRenderSetup()
      )
   );
   private static final Function<Identifier, RenderType> TEXT_POLYGON_OFFSET = Util.memoize(
      texture -> RenderType.create(
         "text_polygon_offset",
         RenderSetup.builder(RenderPipelines.TEXT_POLYGON_OFFSET)
            .setOitPipelines(RenderPipelines.OIT_TEXT_POLYGON_OFFSET)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .sortOnUpload()
            .createRenderSetup()
      )
   );
   private static final Function<Identifier, RenderType> TEXT_GRAYSCALE_POLYGON_OFFSET = Util.memoize(
      texture -> RenderType.create(
         "text_grayscale_polygon_offset",
         RenderSetup.builder(RenderPipelines.TEXT_GRAYSCALE_POLYGON_OFFSET)
            .setOitPipelines(RenderPipelines.OIT_TEXT_GRAYSCALE_POLYGON_OFFSET)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .sortOnUpload()
            .createRenderSetup()
      )
   );
   private static final Function<Identifier, RenderType> TEXT_SEE_THROUGH = Util.memoize(
      texture -> RenderType.create(
         "text_see_through",
         RenderSetup.builder(RenderPipelines.TEXT_SEE_THROUGH)
            .setOitPipelines(RenderPipelines.OIT_TEXT_SEE_THROUGH)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .createRenderSetup()
      )
   );
   private static final RenderType TEXT_BACKGROUND_SEE_THROUGH = RenderType.create(
      "text_background_see_through",
      RenderSetup.builder(RenderPipelines.TEXT_BACKGROUND_SEE_THROUGH)
         .setOitPipelines(RenderPipelines.OIT_TEXT_BACKGROUND_SEE_THROUGH)
         .useLightmap()
         .sortOnUpload()
         .createRenderSetup()
   );
   private static final Function<Identifier, RenderType> TEXT_GRAYSCALE_SEE_THROUGH = Util.memoize(
      texture -> RenderType.create(
         "text_grayscale_see_through",
         RenderSetup.builder(RenderPipelines.TEXT_GRAYSCALE_SEE_THROUGH)
            .setOitPipelines(RenderPipelines.OIT_TEXT_GRAYSCALE_SEE_THROUGH)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .sortOnUpload()
            .createRenderSetup()
      )
   );
   private static final RenderType LIGHTNING = RenderType.create(
      "lightning", RenderSetup.builder(RenderPipelines.LIGHTNING).setOitPipelines(RenderPipelines.OIT_LIGHTNING).sortOnUpload().createRenderSetup()
   );
   private static final RenderType DRAGON_RAYS = RenderType.create(
      "dragon_rays", RenderSetup.builder(RenderPipelines.DRAGON_RAYS).setOitPipelines(RenderPipelines.OIT_DRAGON_RAYS).createRenderSetup()
   );
   private static final RenderType END_PORTAL = RenderType.create(
      "end_portal",
      RenderSetup.builder(RenderPipelines.END_PORTAL)
         .withTexture("Sampler0", AbstractEndPortalRenderer.END_SKY_LOCATION)
         .withTexture("Sampler1", AbstractEndPortalRenderer.END_PORTAL_LOCATION)
         .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
         .createRenderSetup()
   );
   private static final RenderType END_GATEWAY = RenderType.create(
      "end_gateway",
      RenderSetup.builder(RenderPipelines.END_GATEWAY)
         .withTexture("Sampler0", AbstractEndPortalRenderer.END_SKY_LOCATION)
         .withTexture("Sampler1", AbstractEndPortalRenderer.END_PORTAL_LOCATION)
         .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
         .createRenderSetup()
   );
   public static final RenderType LINES = RenderType.create(
      "lines", RenderSetup.builder(RenderPipelines.LINES).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).createRenderSetup()
   );
   public static final RenderType LINES_TRANSLUCENT = RenderType.create(
      "lines_translucent",
      RenderSetup.builder(RenderPipelines.LINES_TRANSLUCENT)
         .setOitPipelines(RenderPipelines.OIT_LINES_TRANSLUCENT)
         .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
         .createRenderSetup()
   );
   public static final RenderType LINES_DEPTH_BIAS = RenderType.create(
      "lines_translucent_depth_bias",
      RenderSetup.builder(RenderPipelines.LINES_DEPTH_BIAS)
         .setOitPipelines(RenderPipelines.OIT_LINES_TRANSLUCENT)
         .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
         .createRenderSetup()
   );
   public static final RenderType SECONDARY_BLOCK_OUTLINE = RenderType.create(
      "secondary_block_outline",
      RenderSetup.builder(RenderPipelines.SECONDARY_BLOCK_OUTLINE)
         .setOitPipelines(RenderPipelines.OIT_SECONDARY_BLOCK_OUTLINE)
         .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
         .createRenderSetup()
   );
   private static final RenderType DEBUG_FILLED_BOX = RenderType.create(
      "debug_filled_box",
      RenderSetup.builder(RenderPipelines.DEBUG_FILLED_BOX)
         .setOitPipelines(RenderPipelines.OIT_DEBUG_FILLED_BOX)
         .sortOnUpload()
         .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
         .createRenderSetup()
   );
   private static final RenderType DEBUG_POINT = RenderType.create(
      "debug_point", RenderSetup.builder(RenderPipelines.DEBUG_POINTS).setOitPipelines(RenderPipelines.OIT_DEBUG_POINTS).createRenderSetup()
   );
   private static final RenderType DEBUG_QUADS = RenderType.create(
      "debug_quads", RenderSetup.builder(RenderPipelines.DEBUG_QUADS).setOitPipelines(RenderPipelines.OIT_DEBUG_QUADS).sortOnUpload().createRenderSetup()
   );
   private static final RenderType DEBUG_TRIANGLE_FAN = RenderType.create(
      "debug_triangle_fan", RenderSetup.builder(RenderPipelines.DEBUG_TRIANGLE_FAN).setOitPipelines(RenderPipelines.OIT_DEBUG_TRIANGLE_FAN).createRenderSetup()
   );
   private static final Function<Identifier, RenderType> BLOCK_SCREEN_EFFECT = Util.memoize(
      texture -> RenderType.create(
         "block_screen_effect", RenderSetup.builder(RenderPipelines.BLOCK_SCREEN_EFFECT).withTexture("Sampler0", texture).createRenderSetup()
      )
   );
   private static final Function<Identifier, RenderType> FIRE_SCREEN_EFFECT = Util.memoize(
      texture -> RenderType.create(
         "fire_screen_effect", RenderSetup.builder(RenderPipelines.FIRE_SCREEN_EFFECT).withTexture("Sampler0", texture).createRenderSetup()
      )
   );

   private static RenderSetup createMovingBlockSetup(final RenderPipeline pipeline, final boolean translucent) {
      RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(pipeline)
         .useLightmap()
         .withTexture(
            "Sampler0",
            TextureAtlas.LOCATION_BLOCKS,
            () -> RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.NEAREST, true)
         )
         .affectsCrumbling()
         .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE);
      if (translucent) {
         setup.sortOnUpload().setOitPipelines(RenderPipelines.OIT_TRANSLUCENT_BLOCK);
      }

      return setup.createRenderSetup();
   }

   public static RenderType solidMovingBlock() {
      return SOLID_MOVING_BLOCK;
   }

   public static RenderType cutoutMovingBlock() {
      return CUTOUT_MOVING_BLOCK;
   }

   public static RenderType translucentMovingBlock() {
      return TRANSLUCENT_MOVING_BLOCK;
   }

   public static RenderType armorCutoutNoCull(final Identifier texture) {
      return ARMOR_CUTOUT_NO_CULL.apply(texture);
   }

   public static RenderType armorCutoutNoCullGlint(final Identifier texture) {
      return ARMOR_CUTOUT_NO_CULL_GLINT.apply(texture);
   }

   public static RenderType armorTrim(final Identifier texture, final boolean decal) {
      return (decal ? ARMOR_TRIM_DECAL : ARMOR_TRIM).apply(texture);
   }

   public static RenderType wolfArmorCracks(final Identifier texture) {
      return WOLF_ARMOR_CRACKS.apply(texture);
   }

   public static RenderType entitySolid(final Identifier texture) {
      return ENTITY_SOLID.apply(texture);
   }

   public static RenderType entitySolidGlint(final Identifier texture) {
      return ENTITY_SOLID_GLINT.apply(texture);
   }

   public static RenderType entitySolidZOffsetForward(final Identifier texture) {
      return ENTITY_SOLID_Z_OFFSET_FORWARD.apply(texture);
   }

   public static RenderType entityCutoutCull(final Identifier texture) {
      return ENTITY_CUTOUT_CULL.apply(texture);
   }

   public static RenderType entityCutout(final Identifier texture, final boolean affectsOutline) {
      return ENTITY_CUTOUT.apply(texture, affectsOutline);
   }

   public static RenderType entityCutout(final Identifier texture) {
      return entityCutout(texture, true);
   }

   public static RenderType entityCutoutZOffset(final Identifier texture, final boolean affectsOutline) {
      return ENTITY_CUTOUT_Z_OFFSET.apply(texture, affectsOutline);
   }

   public static RenderType entityCutoutZOffset(final Identifier texture) {
      return entityCutoutZOffset(texture, true);
   }

   public static RenderType entityCutoutDissolve(final Identifier texture, final Identifier maskTexture) {
      return ENTITY_CUTOUT_DISSOLVE.apply(texture, maskTexture);
   }

   public static RenderType entityTranslucentCull(final Identifier texture) {
      return ENTITY_TRANSLUCENT_CULL.apply(texture);
   }

   public static RenderType itemCutout(final Identifier texture) {
      return ITEM_CUTOUT.apply(texture);
   }

   public static RenderType itemCutoutGlint(final Identifier texture) {
      return ITEM_CUTOUT_GLINT.apply(texture);
   }

   public static RenderType itemCutoutGlintSpecial(final Identifier texture) {
      return ITEM_CUTOUT_GLINT_SPECIAL.apply(texture);
   }

   public static RenderType itemTranslucent(final Identifier texture) {
      return ITEM_TRANSLUCENT.apply(texture);
   }

   public static RenderType itemTranslucentGlint(final Identifier texture) {
      return ITEM_TRANSLUCENT_GLINT.apply(texture);
   }

   public static RenderType itemTranslucentGlintSpecial(final Identifier texture) {
      return ITEM_TRANSLUCENT_GLINT_SPECIAL.apply(texture);
   }

   public static RenderType entityTranslucent(final Identifier texture, final boolean affectsOutline) {
      return ENTITY_TRANSLUCENT.apply(texture, affectsOutline);
   }

   public static RenderType entityTranslucent(final Identifier texture) {
      return entityTranslucent(texture, true);
   }

   public static RenderType entityTranslucentEmissive(final Identifier texture) {
      return ENTITY_TRANSLUCENT_EMISSIVE.apply(texture, true);
   }

   public static RenderType endCrystalBeam(final Identifier texture) {
      return END_CRYSTAL_BEAM.apply(texture);
   }

   public static RenderType beaconBeam(final Identifier texture, final boolean translucent) {
      return BEACON_BEAM.apply(texture, translucent);
   }

   public static RenderType bannerPattern(final Identifier texture) {
      return BANNER_PATTERN.apply(texture);
   }

   public static RenderType entityShadow(final Identifier texture) {
      return ENTITY_SHADOW.apply(texture);
   }

   public static RenderType eyes(final Identifier texture) {
      return EYES.apply(texture);
   }

   public static RenderType breezeEyes(final Identifier texture) {
      return ENTITY_TRANSLUCENT_EMISSIVE.apply(texture, false);
   }

   public static RenderType breezeWind(final Identifier texture, final float uOffset, final float vOffset) {
      return RenderType.create(
         "breeze_wind",
         RenderSetup.builder(RenderPipelines.BREEZE_WIND)
            .setOitPipelines(RenderPipelines.OIT_BREEZE_WIND)
            .setOpaquePartsPipeline(RenderPipelines.BREEZE_WIND_OPAQUE_PARTS)
            .withTexture("Sampler0", texture)
            .setTextureTransform(new TextureTransform.OffsetTextureTransform(uOffset, vOffset))
            .useLightmap()
            .sortOnUpload()
            .createRenderSetup()
      );
   }

   public static RenderType energySwirl(final Identifier texture, final float uOffset, final float vOffset) {
      return RenderType.create(
         "energy_swirl",
         RenderSetup.builder(RenderPipelines.ENERGY_SWIRL)
            .setOitPipelines(RenderPipelines.OIT_ENERGY_SWIRL)
            .withTexture("Sampler0", texture)
            .setTextureTransform(new TextureTransform.OffsetTextureTransform(uOffset, vOffset))
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
      return Minecraft.getInstance().gameRenderer.useImprovedTransparency() ? OIT_WATER_MASK : WATER_MASK;
   }

   public static RenderType outline(final Identifier texture) {
      return OUTLINE.apply(texture, false);
   }

   public static RenderType patternedShieldGlint() {
      return PATTERNED_SHIELD_GLINT;
   }

   public static RenderType trimmedArmorGlint() {
      return TRIMMED_ARMOR_GLINT;
   }

   public static RenderType crumbling(final Identifier texture) {
      return CRUMBLING.apply(texture);
   }

   public static RenderType text(final Identifier texture) {
      return TEXT.apply(texture);
   }

   public static RenderType textBackground() {
      return TEXT_BACKGROUND;
   }

   public static RenderType textGrayscale(final Identifier texture) {
      return TEXT_GRAYSCALE.apply(texture);
   }

   public static RenderType textPolygonOffset(final Identifier texture) {
      return TEXT_POLYGON_OFFSET.apply(texture);
   }

   public static RenderType textGrayscalePolygonOffset(final Identifier texture) {
      return TEXT_GRAYSCALE_POLYGON_OFFSET.apply(texture);
   }

   public static RenderType textSeeThrough(final Identifier texture) {
      return TEXT_SEE_THROUGH.apply(texture);
   }

   public static RenderType textBackgroundSeeThrough() {
      return TEXT_BACKGROUND_SEE_THROUGH;
   }

   public static RenderType textGrayscaleSeeThrough(final Identifier texture) {
      return TEXT_GRAYSCALE_SEE_THROUGH.apply(texture);
   }

   public static RenderType lightning() {
      return LIGHTNING;
   }

   public static RenderType dragonRays() {
      return DRAGON_RAYS;
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

   public static RenderType linesDepthBias() {
      return LINES_DEPTH_BIAS;
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

   public static RenderType blockScreenEffect(final Identifier texture) {
      return BLOCK_SCREEN_EFFECT.apply(texture);
   }

   public static RenderType fireScreenEffect(final Identifier texture) {
      return FIRE_SCREEN_EFFECT.apply(texture);
   }
}
