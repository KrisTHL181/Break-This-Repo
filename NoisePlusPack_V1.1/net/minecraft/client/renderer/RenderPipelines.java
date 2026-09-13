package net.minecraft.client.renderer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderPipelines {
   private static final Map<Identifier, RenderPipeline> PIPELINES_BY_LOCATION = new HashMap<>();
   private static final RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET = RenderPipeline.builder()
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .buildSnippet();
   private static final RenderPipeline.Snippet FOG_SNIPPET = RenderPipeline.builder().withUniform("Fog", UniformType.UNIFORM_BUFFER).buildSnippet();
   private static final RenderPipeline.Snippet GLOBALS_SNIPPET = RenderPipeline.builder().withUniform("Globals", UniformType.UNIFORM_BUFFER).buildSnippet();
   private static final RenderPipeline.Snippet MATRICES_FOG_SNIPPET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET, FOG_SNIPPET).buildSnippet();
   private static final RenderPipeline.Snippet MATRICES_FOG_LIGHT_DIR_SNIPPET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET, FOG_SNIPPET)
      .withUniform("Lighting", UniformType.UNIFORM_BUFFER)
      .buildSnippet();
   private static final RenderPipeline.Snippet GENERIC_BLOCKS_SNIPPET = RenderPipeline.builder(FOG_SNIPPET)
      .withSampler("Sampler0")
      .withSampler("Sampler2")
      .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS)
      .buildSnippet();
   private static final RenderPipeline.Snippet TERRAIN_SNIPPET = RenderPipeline.builder(GENERIC_BLOCKS_SNIPPET)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("ChunkSection", UniformType.UNIFORM_BUFFER)
      .withVertexShader("core/terrain")
      .withFragmentShader("core/terrain")
      .buildSnippet();
   private static final RenderPipeline.Snippet BLOCK_SNIPPET = RenderPipeline.builder(GENERIC_BLOCKS_SNIPPET, MATRICES_PROJECTION_SNIPPET)
      .withVertexShader("core/block")
      .withFragmentShader("core/block")
      .buildSnippet();
   private static final RenderPipeline.Snippet ENTITY_SNIPPET = RenderPipeline.builder(MATRICES_FOG_LIGHT_DIR_SNIPPET)
      .withVertexShader("core/entity")
      .withFragmentShader("core/entity")
      .withSampler("Sampler0")
      .withSampler("Sampler2")
      .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
      .buildSnippet();
   private static final RenderPipeline.Snippet ENTITY_EMISSIVE_SNIPPET = RenderPipeline.builder(MATRICES_FOG_LIGHT_DIR_SNIPPET)
      .withVertexShader("core/entity")
      .withFragmentShader("core/entity")
      .withSampler("Sampler0")
      .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
      .withShaderDefine("EMISSIVE")
      .buildSnippet();
   private static final RenderPipeline.Snippet BEACON_BEAM_SNIPPET = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
      .withVertexShader("core/rendertype_beacon_beam")
      .withFragmentShader("core/rendertype_beacon_beam")
      .withSampler("Sampler0")
      .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS)
      .buildSnippet();
   private static final RenderPipeline.Snippet TEXT_SNIPPET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
      .buildSnippet();
   private static final RenderPipeline.Snippet END_PORTAL_SNIPPET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET, FOG_SNIPPET, GLOBALS_SNIPPET)
      .withVertexShader("core/rendertype_end_portal")
      .withFragmentShader("core/rendertype_end_portal")
      .withSampler("Sampler0")
      .withSampler("Sampler1")
      .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
      .buildSnippet();
   private static final RenderPipeline.Snippet CLOUDS_SNIPPET = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
      .withVertexShader("core/rendertype_clouds")
      .withFragmentShader("core/rendertype_clouds")
      .withBlend(BlendFunction.TRANSLUCENT)
      .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.QUADS)
      .withUniform("CloudInfo", UniformType.UNIFORM_BUFFER)
      .withUniform("CloudFaces", UniformType.TEXEL_BUFFER, TextureFormat.RED8I)
      .buildSnippet();
   private static final RenderPipeline.Snippet LINES_SNIPPET = RenderPipeline.builder(MATRICES_FOG_SNIPPET, GLOBALS_SNIPPET)
      .withVertexShader("core/rendertype_lines")
      .withFragmentShader("core/rendertype_lines")
      .withBlend(BlendFunction.TRANSLUCENT)
      .withCull(false)
      .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.Mode.LINES)
      .buildSnippet();
   private static final RenderPipeline.Snippet DEBUG_FILLED_SNIPPET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
      .withVertexShader("core/position_color")
      .withFragmentShader("core/position_color")
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthWrite(false)
      .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
      .buildSnippet();
   private static final RenderPipeline.Snippet PARTICLE_SNIPPET = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
      .withVertexShader("core/particle")
      .withFragmentShader("core/particle")
      .withSampler("Sampler0")
      .withSampler("Sampler2")
      .withVertexFormat(DefaultVertexFormat.PARTICLE, VertexFormat.Mode.QUADS)
      .buildSnippet();
   private static final RenderPipeline.Snippet WEATHER_SNIPPET = RenderPipeline.builder(PARTICLE_SNIPPET)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withCull(false)
      .buildSnippet();
   private static final RenderPipeline.Snippet GUI_SNIPPET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
      .withVertexShader("core/gui")
      .withFragmentShader("core/gui")
      .withBlend(BlendFunction.TRANSLUCENT)
      .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .buildSnippet();
   private static final RenderPipeline.Snippet GUI_TEXTURED_SNIPPET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
      .withVertexShader("core/position_tex_color")
      .withFragmentShader("core/position_tex_color")
      .withSampler("Sampler0")
      .withBlend(BlendFunction.TRANSLUCENT)
      .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .buildSnippet();
   private static final RenderPipeline.Snippet GUI_TEXT_SNIPPET = RenderPipeline.builder(TEXT_SNIPPET)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .buildSnippet();
   private static final RenderPipeline.Snippet OUTLINE_SNIPPET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
      .withVertexShader("core/rendertype_outline")
      .withFragmentShader("core/rendertype_outline")
      .withSampler("Sampler0")
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
      .buildSnippet();
   public static final RenderPipeline.Snippet POST_PROCESSING_SNIPPET = RenderPipeline.builder()
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
      .buildSnippet();
   public static final RenderPipeline SOLID_BLOCK = register(RenderPipeline.builder(BLOCK_SNIPPET).withLocation("pipeline/solid_block").build());
   public static final RenderPipeline SOLID_TERRAIN = register(RenderPipeline.builder(TERRAIN_SNIPPET).withLocation("pipeline/solid_terrain").build());
   public static final RenderPipeline WIREFRAME = register(
      RenderPipeline.builder(TERRAIN_SNIPPET).withLocation("pipeline/wireframe").withPolygonMode(PolygonMode.WIREFRAME).build()
   );
   public static final RenderPipeline CUTOUT_BLOCK = register(
      RenderPipeline.builder(BLOCK_SNIPPET).withLocation("pipeline/cutout_block").withShaderDefine("ALPHA_CUTOUT", 0.5F).build()
   );
   public static final RenderPipeline CUTOUT_TERRAIN = register(
      RenderPipeline.builder(TERRAIN_SNIPPET).withLocation("pipeline/cutout_terrain").withShaderDefine("ALPHA_CUTOUT", 0.5F).build()
   );
   public static final RenderPipeline TRANSLUCENT_TERRAIN = register(
      RenderPipeline.builder(TERRAIN_SNIPPET)
         .withLocation("pipeline/translucent_terrain")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withShaderDefine("ALPHA_CUTOUT", 0.01F)
         .build()
   );
   public static final RenderPipeline TRIPWIRE_BLOCK = register(
      RenderPipeline.builder(BLOCK_SNIPPET)
         .withLocation("pipeline/tripwire_block")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withBlend(BlendFunction.TRANSLUCENT)
         .build()
   );
   public static final RenderPipeline TRIPWIRE_TERRAIN = register(
      RenderPipeline.builder(TERRAIN_SNIPPET)
         .withLocation("pipeline/tripwire_terrain")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withBlend(BlendFunction.TRANSLUCENT)
         .build()
   );
   public static final RenderPipeline TRANSLUCENT_MOVING_BLOCK = register(
      RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
         .withLocation("pipeline/translucent_moving_block")
         .withVertexShader("core/rendertype_translucent_moving_block")
         .withFragmentShader("core/rendertype_translucent_moving_block")
         .withSampler("Sampler0")
         .withSampler("Sampler2")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS)
         .build()
   );
   public static final RenderPipeline ARMOR_CUTOUT_NO_CULL = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/armor_cutout_no_cull")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("NO_OVERLAY")
         .withShaderDefine("PER_FACE_LIGHTING")
         .withCull(false)
         .build()
   );
   public static final RenderPipeline ARMOR_DECAL_CUTOUT_NO_CULL = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/armor_decal_cutout_no_cull")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("NO_OVERLAY")
         .withShaderDefine("PER_FACE_LIGHTING")
         .withCull(false)
         .withDepthTestFunction(DepthTestFunction.EQUAL_DEPTH_TEST)
         .build()
   );
   public static final RenderPipeline ARMOR_TRANSLUCENT = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/armor_translucent")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("NO_OVERLAY")
         .withShaderDefine("PER_FACE_LIGHTING")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withCull(false)
         .build()
   );
   public static final RenderPipeline ENTITY_SOLID = register(
      RenderPipeline.builder(ENTITY_SNIPPET).withLocation("pipeline/entity_solid").withSampler("Sampler1").build()
   );
   public static final RenderPipeline ENTITY_SOLID_Z_OFFSET_FORWARD = register(
      RenderPipeline.builder(ENTITY_SNIPPET).withLocation("pipeline/entity_solid_offset_forward").withSampler("Sampler1").build()
   );
   public static final RenderPipeline ENTITY_CUTOUT = register(
      RenderPipeline.builder(ENTITY_SNIPPET).withLocation("pipeline/entity_cutout").withShaderDefine("ALPHA_CUTOUT", 0.1F).withSampler("Sampler1").build()
   );
   public static final RenderPipeline ENTITY_CUTOUT_NO_CULL = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/entity_cutout_no_cull")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("PER_FACE_LIGHTING")
         .withSampler("Sampler1")
         .withCull(false)
         .build()
   );
   public static final RenderPipeline ENTITY_CUTOUT_NO_CULL_Z_OFFSET = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/entity_cutout_no_cull_z_offset")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("PER_FACE_LIGHTING")
         .withSampler("Sampler1")
         .withCull(false)
         .build()
   );
   public static final RenderPipeline ENTITY_TRANSLUCENT = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/entity_translucent")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("PER_FACE_LIGHTING")
         .withSampler("Sampler1")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withCull(false)
         .build()
   );
   public static final RenderPipeline ENTITY_TRANSLUCENT_EMISSIVE = register(
      RenderPipeline.builder(ENTITY_EMISSIVE_SNIPPET)
         .withLocation("pipeline/entity_translucent_emissive")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("PER_FACE_LIGHTING")
         .withSampler("Sampler1")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withCull(false)
         .withDepthWrite(false)
         .build()
   );
   public static final RenderPipeline ENTITY_SMOOTH_CUTOUT = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/entity_smooth_cutout")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withSampler("Sampler1")
         .withCull(false)
         .build()
   );
   public static final RenderPipeline ENTITY_NO_OUTLINE = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/entity_no_outline")
         .withShaderDefine("NO_OVERLAY")
         .withShaderDefine("PER_FACE_LIGHTING")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withCull(false)
         .withDepthWrite(false)
         .build()
   );
   public static final RenderPipeline BREEZE_WIND = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/breeze_wind")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("APPLY_TEXTURE_MATRIX")
         .withShaderDefine("NO_OVERLAY")
         .withShaderDefine("NO_CARDINAL_LIGHTING")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withCull(false)
         .build()
   );
   public static final RenderPipeline ENERGY_SWIRL = register(
      RenderPipeline.builder(MATRICES_FOG_SNIPPET)
         .withLocation("pipeline/energy_swirl")
         .withVertexShader("core/entity")
         .withFragmentShader("core/entity")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("EMISSIVE")
         .withShaderDefine("NO_OVERLAY")
         .withShaderDefine("NO_CARDINAL_LIGHTING")
         .withShaderDefine("APPLY_TEXTURE_MATRIX")
         .withSampler("Sampler0")
         .withBlend(BlendFunction.ADDITIVE)
         .withCull(false)
         .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
         .build()
   );
   public static final RenderPipeline EYES = register(
      RenderPipeline.builder(MATRICES_FOG_SNIPPET)
         .withLocation("pipeline/eyes")
         .withVertexShader("core/entity")
         .withFragmentShader("core/entity")
         .withShaderDefine("EMISSIVE")
         .withShaderDefine("NO_OVERLAY")
         .withShaderDefine("NO_CARDINAL_LIGHTING")
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthWrite(false)
         .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
         .build()
   );
   public static final RenderPipeline ENTITY_DECAL = register(
      RenderPipeline.builder(MATRICES_FOG_LIGHT_DIR_SNIPPET)
         .withLocation("pipeline/entity_decal")
         .withVertexShader("core/rendertype_entity_decal")
         .withFragmentShader("core/rendertype_entity_decal")
         .withSampler("Sampler0")
         .withSampler("Sampler1")
         .withSampler("Sampler2")
         .withDepthTestFunction(DepthTestFunction.EQUAL_DEPTH_TEST)
         .withCull(false)
         .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
         .build()
   );
   public static final RenderPipeline ENTITY_SHADOW = register(
      RenderPipeline.builder(MATRICES_FOG_SNIPPET)
         .withLocation("pipeline/entity_shadow")
         .withVertexShader("core/rendertype_entity_shadow")
         .withFragmentShader("core/rendertype_entity_shadow")
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthWrite(false)
         .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
         .build()
   );
   public static final RenderPipeline ITEM_ENTITY_TRANSLUCENT_CULL = register(
      RenderPipeline.builder(MATRICES_FOG_LIGHT_DIR_SNIPPET)
         .withLocation("pipeline/item_entity_translucent_cull")
         .withVertexShader("core/rendertype_item_entity_translucent_cull")
         .withFragmentShader("core/rendertype_item_entity_translucent_cull")
         .withSampler("Sampler0")
         .withSampler("Sampler2")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
         .build()
   );
   public static final RenderPipeline BEACON_BEAM_OPAQUE = register(
      RenderPipeline.builder(BEACON_BEAM_SNIPPET).withLocation("pipeline/beacon_beam_opaque").build()
   );
   public static final RenderPipeline BEACON_BEAM_TRANSLUCENT = register(
      RenderPipeline.builder(BEACON_BEAM_SNIPPET)
         .withLocation("pipeline/beacon_beam_translucent")
         .withDepthWrite(false)
         .withBlend(BlendFunction.TRANSLUCENT)
         .build()
   );
   public static final RenderPipeline DRAGON_EXPLOSION_ALPHA = register(
      RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
         .withLocation("pipeline/dragon_explosion_alpha")
         .withVertexShader("core/rendertype_entity_alpha")
         .withFragmentShader("core/rendertype_entity_alpha")
         .withSampler("Sampler0")
         .withCull(false)
         .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
         .build()
   );
   public static final RenderPipeline LEASH = register(
      RenderPipeline.builder(MATRICES_FOG_SNIPPET)
         .withLocation("pipeline/leash")
         .withVertexShader("core/rendertype_leash")
         .withFragmentShader("core/rendertype_leash")
         .withSampler("Sampler2")
         .withCull(false)
         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_LIGHTMAP, VertexFormat.Mode.TRIANGLE_STRIP)
         .build()
   );
   public static final RenderPipeline WATER_MASK = register(
      RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
         .withLocation("pipeline/water_mask")
         .withVertexShader("core/rendertype_water_mask")
         .withFragmentShader("core/rendertype_water_mask")
         .withColorWrite(false)
         .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
         .build()
   );
   public static final RenderPipeline GLINT = register(
      RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET, FOG_SNIPPET, GLOBALS_SNIPPET)
         .withLocation("pipeline/glint")
         .withVertexShader("core/glint")
         .withFragmentShader("core/glint")
         .withSampler("Sampler0")
         .withDepthWrite(false)
         .withCull(false)
         .withDepthTestFunction(DepthTestFunction.EQUAL_DEPTH_TEST)
         .withBlend(BlendFunction.GLINT)
         .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
         .build()
   );
   public static final RenderPipeline CRUMBLING = register(
      RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
         .withLocation("pipeline/crumbling")
         .withVertexShader("core/rendertype_crumbling")
         .withFragmentShader("core/rendertype_crumbling")
         .withSampler("Sampler0")
         .withBlend(new BlendFunction(SourceFactor.DST_COLOR, DestFactor.SRC_COLOR, SourceFactor.ONE, DestFactor.ZERO))
         .withDepthWrite(false)
         .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS)
         .withDepthBias(-1.0F, -10.0F)
         .build()
   );
   public static final RenderPipeline TEXT = register(
      RenderPipeline.builder(TEXT_SNIPPET, FOG_SNIPPET)
         .withLocation("pipeline/text")
         .withVertexShader("core/rendertype_text")
         .withFragmentShader("core/rendertype_text")
         .withSampler("Sampler0")
         .withSampler("Sampler2")
         .build()
   );
   public static final RenderPipeline GUI_TEXT = register(
      RenderPipeline.builder(GUI_TEXT_SNIPPET, FOG_SNIPPET)
         .withLocation("pipeline/gui_text")
         .withVertexShader("core/rendertype_text")
         .withFragmentShader("core/rendertype_text")
         .withSampler("Sampler0")
         .withSampler("Sampler2")
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .build()
   );
   public static final RenderPipeline TEXT_BACKGROUND = register(
      RenderPipeline.builder(TEXT_SNIPPET, FOG_SNIPPET)
         .withLocation("pipeline/text_background")
         .withVertexShader("core/rendertype_text_background")
         .withFragmentShader("core/rendertype_text_background")
         .withSampler("Sampler2")
         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_LIGHTMAP, VertexFormat.Mode.QUADS)
         .build()
   );
   public static final RenderPipeline TEXT_INTENSITY = register(
      RenderPipeline.builder(TEXT_SNIPPET, FOG_SNIPPET)
         .withLocation("pipeline/text_intensity")
         .withVertexShader("core/rendertype_text_intensity")
         .withFragmentShader("core/rendertype_text_intensity")
         .withSampler("Sampler0")
         .withSampler("Sampler2")
         .withDepthBias(-1.0F, -10.0F)
         .build()
   );
   public static final RenderPipeline GUI_TEXT_INTENSITY = register(
      RenderPipeline.builder(GUI_TEXT_SNIPPET, FOG_SNIPPET)
         .withLocation("pipeline/gui_text_intensity")
         .withVertexShader("core/rendertype_text_intensity")
         .withFragmentShader("core/rendertype_text_intensity")
         .withSampler("Sampler0")
         .withSampler("Sampler2")
         .build()
   );
   public static final RenderPipeline TEXT_POLYGON_OFFSET = register(
      RenderPipeline.builder(TEXT_SNIPPET, FOG_SNIPPET)
         .withLocation("pipeline/text_polygon_offset")
         .withVertexShader("core/rendertype_text")
         .withFragmentShader("core/rendertype_text")
         .withSampler("Sampler0")
         .withSampler("Sampler2")
         .withDepthBias(-1.0F, -10.0F)
         .build()
   );
   public static final RenderPipeline TEXT_SEE_THROUGH = register(
      RenderPipeline.builder(TEXT_SNIPPET)
         .withLocation("pipeline/text_see_through")
         .withVertexShader("core/rendertype_text_see_through")
         .withFragmentShader("core/rendertype_text_see_through")
         .withSampler("Sampler0")
         .withDepthWrite(false)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .build()
   );
   public static final RenderPipeline TEXT_BACKGROUND_SEE_THROUGH = register(
      RenderPipeline.builder(TEXT_SNIPPET)
         .withLocation("pipeline/text_background_see_through")
         .withVertexShader("core/rendertype_text_background_see_through")
         .withFragmentShader("core/rendertype_text_background_see_through")
         .withDepthWrite(false)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_LIGHTMAP, VertexFormat.Mode.QUADS)
         .build()
   );
   public static final RenderPipeline TEXT_INTENSITY_SEE_THROUGH = register(
      RenderPipeline.builder(TEXT_SNIPPET)
         .withLocation("pipeline/text_intensity_see_through")
         .withVertexShader("core/rendertype_text_intensity_see_through")
         .withFragmentShader("core/rendertype_text_intensity_see_through")
         .withSampler("Sampler0")
         .withDepthWrite(false)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .build()
   );
   public static final RenderPipeline LIGHTNING = register(
      RenderPipeline.builder(MATRICES_FOG_SNIPPET)
         .withLocation("pipeline/lightning")
         .withVertexShader("core/rendertype_lightning")
         .withFragmentShader("core/rendertype_lightning")
         .withBlend(BlendFunction.LIGHTNING)
         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
         .build()
   );
   public static final RenderPipeline DRAGON_RAYS = register(
      RenderPipeline.builder(MATRICES_FOG_SNIPPET)
         .withLocation("pipeline/dragon_rays")
         .withVertexShader("core/rendertype_lightning")
         .withFragmentShader("core/rendertype_lightning")
         .withDepthWrite(false)
         .withBlend(BlendFunction.LIGHTNING)
         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
         .build()
   );
   public static final RenderPipeline DRAGON_RAYS_DEPTH = register(
      RenderPipeline.builder(MATRICES_FOG_SNIPPET)
         .withLocation("pipeline/dragon_rays_depth")
         .withVertexShader("core/position")
         .withFragmentShader("core/position")
         .withColorWrite(false)
         .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.TRIANGLES)
         .build()
   );
   public static final RenderPipeline END_PORTAL = register(
      RenderPipeline.builder(END_PORTAL_SNIPPET).withLocation("pipeline/end_portal").withShaderDefine("PORTAL_LAYERS", 15).build()
   );
   public static final RenderPipeline END_GATEWAY = register(
      RenderPipeline.builder(END_PORTAL_SNIPPET).withLocation("pipeline/end_gateway").withShaderDefine("PORTAL_LAYERS", 16).build()
   );
   public static final RenderPipeline FLAT_CLOUDS = register(
      RenderPipeline.builder(CLOUDS_SNIPPET).withLocation("pipeline/flat_clouds").withCull(false).build()
   );
   public static final RenderPipeline CLOUDS = register(RenderPipeline.builder(CLOUDS_SNIPPET).withLocation("pipeline/clouds").build());
   public static final RenderPipeline LINES = register(RenderPipeline.builder(LINES_SNIPPET).withLocation("pipeline/lines").build());
   public static final RenderPipeline LINES_TRANSLUCENT = register(
      RenderPipeline.builder(LINES_SNIPPET).withDepthWrite(false).withLocation("pipeline/lines_translucent").build()
   );
   public static final RenderPipeline SECONDARY_BLOCK_OUTLINE = register(
      RenderPipeline.builder(LINES_SNIPPET).withLocation("pipeline/secondary_block_outline").withBlend(BlendFunction.TRANSLUCENT).withDepthWrite(false).build()
   );
   public static final RenderPipeline DEBUG_POINTS = register(
      RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
         .withLocation("pipeline/debug_points")
         .withVertexShader("core/debug_point")
         .withFragmentShader("core/position_color")
         .withCull(false)
         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_LINE_WIDTH, VertexFormat.Mode.POINTS)
         .build()
   );
   public static final RenderPipeline DEBUG_FILLED_BOX = register(
      RenderPipeline.builder(DEBUG_FILLED_SNIPPET).withLocation("pipeline/debug_filled_box").build()
   );
   public static final RenderPipeline DEBUG_QUADS = register(
      RenderPipeline.builder(DEBUG_FILLED_SNIPPET).withLocation("pipeline/debug_quads").withCull(false).build()
   );
   public static final RenderPipeline DEBUG_TRIANGLE_FAN = register(
      RenderPipeline.builder(DEBUG_FILLED_SNIPPET)
         .withLocation("pipeline/debug_triangle_fan")
         .withCull(false)
         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_FAN)
         .build()
   );
   public static final RenderPipeline WORLD_BORDER = register(
      RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
         .withLocation("pipeline/world_border")
         .withVertexShader("core/rendertype_world_border")
         .withFragmentShader("core/rendertype_world_border")
         .withSampler("Sampler0")
         .withBlend(BlendFunction.OVERLAY)
         .withCull(false)
         .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
         .withDepthBias(-3.0F, -3.0F)
         .build()
   );
   public static final RenderPipeline OPAQUE_PARTICLE = register(RenderPipeline.builder(PARTICLE_SNIPPET).withLocation("pipeline/opaque_particle").build());
   public static final RenderPipeline TRANSLUCENT_PARTICLE = register(
      RenderPipeline.builder(PARTICLE_SNIPPET).withLocation("pipeline/translucent_particle").withBlend(BlendFunction.TRANSLUCENT).build()
   );
   public static final RenderPipeline WEATHER_DEPTH_WRITE = register(
      RenderPipeline.builder(WEATHER_SNIPPET).withLocation("pipeline/weather_depth_write").build()
   );
   public static final RenderPipeline WEATHER_NO_DEPTH_WRITE = register(
      RenderPipeline.builder(WEATHER_SNIPPET).withLocation("pipeline/weather_no_depth_write").withDepthWrite(false).build()
   );
   public static final RenderPipeline SKY = register(
      RenderPipeline.builder(MATRICES_FOG_SNIPPET)
         .withLocation("pipeline/sky")
         .withVertexShader("core/sky")
         .withFragmentShader("core/sky")
         .withDepthWrite(false)
         .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.TRIANGLE_FAN)
         .build()
   );
   public static final RenderPipeline END_SKY = register(
      RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
         .withLocation("pipeline/end_sky")
         .withVertexShader("core/position_tex_color")
         .withFragmentShader("core/position_tex_color")
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthWrite(false)
         .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
         .build()
   );
   public static final RenderPipeline SUNRISE_SUNSET = register(
      RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
         .withLocation("pipeline/sunrise_sunset")
         .withVertexShader("core/position_color")
         .withFragmentShader("core/position_color")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthWrite(false)
         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_FAN)
         .build()
   );
   public static final RenderPipeline STARS = register(
      RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
         .withLocation("pipeline/stars")
         .withVertexShader("core/stars")
         .withFragmentShader("core/stars")
         .withBlend(BlendFunction.OVERLAY)
         .withDepthWrite(false)
         .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
         .build()
   );
   public static final RenderPipeline CELESTIAL = register(
      RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
         .withLocation("pipeline/celestial")
         .withVertexShader("core/position_tex")
         .withFragmentShader("core/position_tex")
         .withSampler("Sampler0")
         .withBlend(BlendFunction.OVERLAY)
         .withDepthWrite(false)
         .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
         .build()
   );
   public static final RenderPipeline GUI = register(RenderPipeline.builder(GUI_SNIPPET).withLocation("pipeline/gui").build());
   public static final RenderPipeline GUI_INVERT = register(
      RenderPipeline.builder(GUI_SNIPPET).withLocation("pipeline/gui_invert").withBlend(BlendFunction.INVERT).build()
   );
   public static final RenderPipeline GUI_TEXT_HIGHLIGHT = register(
      RenderPipeline.builder(GUI_SNIPPET).withLocation("pipeline/gui_text_highlight").withBlend(BlendFunction.ADDITIVE).build()
   );
   public static final RenderPipeline GUI_TEXTURED = register(RenderPipeline.builder(GUI_TEXTURED_SNIPPET).withLocation("pipeline/gui_textured").build());
   public static final RenderPipeline GUI_TEXTURED_PREMULTIPLIED_ALPHA = register(
      RenderPipeline.builder(GUI_TEXTURED_SNIPPET)
         .withLocation("pipeline/gui_textured_premultiplied_alpha")
         .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
         .build()
   );
   public static final RenderPipeline BLOCK_SCREEN_EFFECT = register(
      RenderPipeline.builder(GUI_TEXTURED_SNIPPET)
         .withLocation("pipeline/block_screen_effect")
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .build()
   );
   public static final RenderPipeline FIRE_SCREEN_EFFECT = register(
      RenderPipeline.builder(GUI_TEXTURED_SNIPPET)
         .withLocation("pipeline/fire_screen_effect")
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .build()
   );
   public static final RenderPipeline GUI_OPAQUE_TEXTURED_BACKGROUND = register(
      RenderPipeline.builder(GUI_TEXTURED_SNIPPET).withLocation("pipeline/gui_opaque_textured_background").withoutBlend().build()
   );
   public static final RenderPipeline GUI_NAUSEA_OVERLAY = register(
      RenderPipeline.builder(GUI_TEXTURED_SNIPPET).withLocation("pipeline/gui_nausea_overlay").withBlend(BlendFunction.ADDITIVE).build()
   );
   public static final RenderPipeline VIGNETTE = register(
      RenderPipeline.builder(GUI_TEXTURED_SNIPPET)
         .withLocation("pipeline/vignette")
         .withBlend(new BlendFunction(SourceFactor.ZERO, DestFactor.ONE_MINUS_SRC_COLOR))
         .build()
   );
   public static final RenderPipeline CROSSHAIR = register(
      RenderPipeline.builder(GUI_TEXTURED_SNIPPET).withLocation("pipeline/crosshair").withBlend(BlendFunction.INVERT).build()
   );
   public static final RenderPipeline MOJANG_LOGO = register(
      RenderPipeline.builder(GUI_TEXTURED_SNIPPET)
         .withLocation("pipeline/mojang_logo")
         .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE))
         .build()
   );
   public static final RenderPipeline ENTITY_OUTLINE_BLIT = register(
      RenderPipeline.builder()
         .withLocation("pipeline/entity_outline_blit")
         .withVertexShader("core/screenquad")
         .withFragmentShader("core/blit_screen")
         .withSampler("InSampler")
         .withBlend(BlendFunction.ENTITY_OUTLINE_BLIT)
         .withDepthWrite(false)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withColorWrite(true, false)
         .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
         .build()
   );
   public static final RenderPipeline TRACY_BLIT = register(
      RenderPipeline.builder()
         .withLocation("pipeline/tracy_blit")
         .withVertexShader("core/screenquad")
         .withFragmentShader("core/blit_screen")
         .withSampler("InSampler")
         .withDepthWrite(false)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
         .build()
   );
   public static final RenderPipeline PANORAMA = register(
      RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
         .withLocation("pipeline/panorama")
         .withVertexShader("core/panorama")
         .withFragmentShader("core/panorama")
         .withSampler("Sampler0")
         .withDepthWrite(false)
         .withColorWrite(true, false)
         .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
         .build()
   );
   public static final RenderPipeline OUTLINE_CULL = register(RenderPipeline.builder(OUTLINE_SNIPPET).withLocation("pipeline/outline_cull").build());
   public static final RenderPipeline OUTLINE_NO_CULL = register(
      RenderPipeline.builder(OUTLINE_SNIPPET).withLocation("pipeline/outline_no_cull").withCull(false).build()
   );
   public static final RenderPipeline LIGHTMAP = register(
      RenderPipeline.builder()
         .withLocation("pipeline/lightmap")
         .withVertexShader("core/screenquad")
         .withFragmentShader("core/lightmap")
         .withUniform("LightmapInfo", UniformType.UNIFORM_BUFFER)
         .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
         .withDepthWrite(false)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .build()
   );
   public static final RenderPipeline.Snippet ANIMATE_SPRITE_SNIPPET = RenderPipeline.builder()
      .withVertexShader("core/animate_sprite")
      .withUniform("SpriteAnimationInfo", UniformType.UNIFORM_BUFFER)
      .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
      .withDepthWrite(false)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .buildSnippet();
   public static final RenderPipeline ANIMATE_SPRITE_BLIT = register(
      RenderPipeline.builder(ANIMATE_SPRITE_SNIPPET)
         .withFragmentShader("core/animate_sprite_blit")
         .withLocation("pipeline/animate_sprite_blit")
         .withSampler("Sprite")
         .build()
   );
   public static final RenderPipeline ANIMATE_SPRITE_INTERPOLATE = register(
      RenderPipeline.builder(ANIMATE_SPRITE_SNIPPET)
         .withFragmentShader("core/animate_sprite_interpolate")
         .withLocation("pipeline/animate_sprite_interpolate")
         .withSampler("CurrentSprite")
         .withSampler("NextSprite")
         .build()
   );

   private static RenderPipeline register(RenderPipeline p_396877_) {
      PIPELINES_BY_LOCATION.put(p_396877_.getLocation(), p_396877_);
      return p_396877_;
   }

   public static List<RenderPipeline> getStaticPipelines() {
      return PIPELINES_BY_LOCATION.values().stream().toList();
   }
}
