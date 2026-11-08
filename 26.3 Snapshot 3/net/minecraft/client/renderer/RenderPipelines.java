package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PolygonMode;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.resources.Identifier;

public class RenderPipelines {
   private static final Map<Identifier, RenderPipeline> PIPELINES_BY_LOCATION = new HashMap<>();
   private static final float ALPHA_CUTOUT_THRESHOLD_DEFAULT = 0.1F;
   private static final float ALPHA_CUTOUT_THRESHOLD_CUTOUT_TERRAIN = 0.5F;
   private static final RenderPipeline.Snippet GLOBALS_SNIPPET = RenderPipeline.builder().withBindGroupLayout(BindGroupLayouts.GLOBALS).buildSnippet();
   private static final RenderPipeline.Snippet MATRICES_FOG_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withBindGroupLayout(BindGroupLayouts.FOG)
      .buildSnippet();
   private static final RenderPipeline.Snippet MATRICES_FOG_LIGHT_DIR_SNIPPET = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.LIGHTING)
      .buildSnippet();
   public static final RenderPipeline.Snippet OIT_OPAQUE_PARTS_THRESHOLD_SNIPPET = RenderPipeline.builder()
      .withShaderDefine("OIT_OPAQUE_PARTS_THRESHOLD", 0.995F)
      .buildSnippet();
   private static final RenderPipeline.Snippet OIT_SNIPPET = RenderPipeline.builder(OIT_OPAQUE_PARTS_THRESHOLD_SNIPPET, GLOBALS_SNIPPET)
      .withShaderDefine("OIT")
      .withShaderDefine("WAVELET_RANK", 2)
      .withShaderDefine("COEFF_COUNT", LevelRenderer.OIT_COEFFICIENT_COUNT)
      .withShaderDefine("COEFF_ATTACHMENT_COUNT", LevelRenderer.OIT_TRANSMITTANCE_TARGET_COUNT)
      .buildSnippet();
   public static final RenderPipeline.Snippet OIT_DEPTH_BOUNDS_SNIPPET = RenderPipeline.builder(OIT_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.GLOBALS)
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.MAX), GpuFormat.RGBA32_FLOAT, 7))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withShaderDefine("OIT_ALPHA_ONLY")
      .withShaderDefine("OIT_DEPTH_BOUNDS")
      .buildSnippet();
   public static final RenderPipeline.Snippet OIT_TRANSMITTANCE_SNIPPET = RenderPipeline.builder(OIT_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.GLOBALS)
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.DEPTH_BOUNDS_SAMPLER)
      .withShaderDefine("OIT_ALPHA_ONLY")
      .withShaderDefine("OIT_TRANSMITTANCE")
      .withColorTargetStates(
         0, LevelRenderer.OIT_TRANSMITTANCE_TARGET_COUNT - 1, () -> new ColorTargetState(Optional.of(BlendFunction.ADDITIVE), GpuFormat.RGBA16_FLOAT, 15)
      )
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .buildSnippet();
   public static final RenderPipeline.Snippet OIT_ACCUMULATE_SNIPPET = RenderPipeline.builder(OIT_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.OIT_COEFFS_DEPTH_BOUNDS_SAMPLER)
      .withShaderDefine("OIT_ACCUMULATE")
      .withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.ADDITIVE), GpuFormat.RGBA16_FLOAT, 15))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .buildSnippet();
   private static final RenderPipeline.Snippet GENERIC_BLOCKS_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.FOG)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withVertexBinding(0, DefaultVertexFormat.BLOCK)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withDepthStencilState(DepthStencilState.DEFAULT)
      .buildSnippet();
   private static final RenderPipeline.Snippet LIT_BLOCKS_SNIPPET = RenderPipeline.builder(GENERIC_BLOCKS_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
      .buildSnippet();
   private static final RenderPipeline.Snippet TERRAIN_SNIPPET = RenderPipeline.builder(LIT_BLOCKS_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.CHUNK_SECTION)
      .withVertexShader("core/terrain")
      .withFragmentShader("core/terrain")
      .buildSnippet();
   private static final RenderPipeline.Snippet BLOCK_SNIPPET = RenderPipeline.builder(LIT_BLOCKS_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withVertexShader("core/block")
      .withFragmentShader("core/block")
      .buildSnippet();
   private static final RenderPipeline.Snippet WATER_MASK_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withVertexShader("core/rendertype_water_mask")
      .withFragmentShader("core/rendertype_water_mask")
      .withVertexBinding(0, DefaultVertexFormat.POSITION)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withDepthStencilState(DepthStencilState.DEFAULT)
      .buildSnippet();
   private static final RenderPipeline.Snippet LIGHTNING_SNIPPET = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
      .withVertexShader("core/rendertype_lightning")
      .withFragmentShader("core/rendertype_lightning")
      .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withDepthStencilState(DepthStencilState.DEFAULT)
      .buildSnippet();
   private static final RenderPipeline.Snippet DRAGON_RAYS_SNIPPET = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
      .withVertexShader("core/rendertype_lightning")
      .withFragmentShader("core/rendertype_lightning")
      .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withDepthStencilState(DepthStencilState.DEFAULT)
      .buildSnippet();
   private static final RenderPipeline.Snippet ENTITY_NO_LIGHTMAP_SNIPPET = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
      .withVertexShader("core/entity")
      .withFragmentShader("core/entity")
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withVertexBinding(0, DefaultVertexFormat.ENTITY)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withDepthStencilState(DepthStencilState.DEFAULT)
      .buildSnippet();
   private static final RenderPipeline.Snippet ENTITY_SNIPPET = RenderPipeline.builder(ENTITY_NO_LIGHTMAP_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.LIGHTING)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
      .buildSnippet();
   private static final RenderPipeline.Snippet OIT_ENTITY_SNIPPET = RenderPipeline.builder()
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withBindGroupLayout(BindGroupLayouts.FOG)
      .withBindGroupLayout(BindGroupLayouts.LIGHTING)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withVertexShader("core/entity")
      .withFragmentShader("core/entity")
      .withVertexBinding(0, DefaultVertexFormat.ENTITY)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withShaderDefine("ALPHA_CUTOUT", 0.1F)
      .buildSnippet();
   private static final RenderPipeline.Snippet ENTITY_EMISSIVE_SNIPPET = RenderPipeline.builder(MATRICES_FOG_LIGHT_DIR_SNIPPET)
      .withVertexShader("core/entity")
      .withFragmentShader("core/entity")
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withVertexBinding(0, DefaultVertexFormat.ENTITY)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withShaderDefine("EMISSIVE")
      .withDepthStencilState(DepthStencilState.DEFAULT)
      .buildSnippet();
   private static final RenderPipeline.Snippet EYES_SNIPPET = RenderPipeline.builder()
      .withShaderDefine("EMISSIVE")
      .withShaderDefine("NO_OVERLAY")
      .withShaderDefine("NO_CARDINAL_LIGHTING")
      .buildSnippet();
   private static final RenderPipeline.Snippet BEACON_BEAM_SNIPPET = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
      .withVertexShader("core/rendertype_beacon_beam")
      .withFragmentShader("core/rendertype_beacon_beam")
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withVertexBinding(0, DefaultVertexFormat.BLOCK)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withDepthStencilState(DepthStencilState.DEFAULT)
      .buildSnippet();
   private static final RenderPipeline.Snippet ITEM_SNIPPET = RenderPipeline.builder(MATRICES_FOG_LIGHT_DIR_SNIPPET)
      .withVertexShader("core/item")
      .withFragmentShader("core/item")
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER1_SAMPLER2)
      .withVertexBinding(0, DefaultVertexFormat.ENTITY)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withDepthStencilState(DepthStencilState.DEFAULT)
      .buildSnippet();
   private static final RenderPipeline.Snippet OIT_ITEM_SNIPPET = RenderPipeline.builder()
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withBindGroupLayout(BindGroupLayouts.FOG)
      .withBindGroupLayout(BindGroupLayouts.LIGHTING)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withVertexShader("core/item")
      .withFragmentShader("core/item")
      .withVertexBinding(0, DefaultVertexFormat.ENTITY)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withShaderDefine("ALPHA_CUTOUT", 0.1F)
      .buildSnippet();
   private static final RenderPipeline.Snippet TEXT_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withVertexShader("core/text")
      .withFragmentShader("core/text")
      .withDepthStencilState(DepthStencilState.DEFAULT)
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .buildSnippet();
   private static final RenderPipeline.Snippet TEXT_BACKGROUND_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withVertexShader("core/text_background")
      .withFragmentShader("core/text_background")
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .buildSnippet();
   private static final RenderPipeline.Snippet WORLD_TEXT_SNIPPET = RenderPipeline.builder(TEXT_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.FOG)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .buildSnippet();
   private static final RenderPipeline.Snippet END_PORTAL_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withBindGroupLayout(BindGroupLayouts.FOG)
      .withVertexShader("core/rendertype_end_portal")
      .withFragmentShader("core/rendertype_end_portal")
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER1)
      .withVertexBinding(0, DefaultVertexFormat.POSITION)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withColorTargetState(ColorTargetState.DEFAULT)
      .withDepthStencilState(DepthStencilState.DEFAULT)
      .buildSnippet();
   private static final RenderPipeline.Snippet CLOUDS_SNIPPET = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
      .withVertexShader("core/clouds")
      .withFragmentShader("core/clouds")
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withBindGroupLayout(BindGroupLayouts.CLOUD_INFO)
      .withDepthStencilState(DepthStencilState.DEFAULT)
      .buildSnippet();
   private static final RenderPipeline.Snippet OIT_CLOUDS_SNIPPET = RenderPipeline.builder()
      .withVertexShader("core/clouds")
      .withFragmentShader("core/clouds")
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withBindGroupLayout(BindGroupLayouts.FOG)
      .withBindGroupLayout(BindGroupLayouts.CLOUD_INFO)
      .buildSnippet();
   private static final RenderPipeline.Snippet LINES_SNIPPET = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
      .withVertexShader("core/rendertype_lines")
      .withFragmentShader("core/rendertype_lines")
      .withCull(false)
      .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
      .withPrimitiveTopology(PrimitiveTopology.LINES)
      .withDepthStencilState(DepthStencilState.DEFAULT)
      .buildSnippet();
   private static final RenderPipeline.Snippet OIT_LINES_SNIPPET = RenderPipeline.builder()
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withBindGroupLayout(BindGroupLayouts.FOG)
      .withVertexShader("core/rendertype_lines")
      .withFragmentShader("core/rendertype_lines")
      .withCull(false)
      .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
      .withPrimitiveTopology(PrimitiveTopology.LINES)
      .buildSnippet();
   private static final RenderPipeline.Snippet DEBUG_FILLED_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withVertexShader("core/position_color")
      .withFragmentShader("core/position_color")
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withCull(false)
      .buildSnippet();
   private static final RenderPipeline.Snippet OIT_DEBUG_FILLED_SNIPPET = RenderPipeline.builder()
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withVertexShader("core/position_color")
      .withFragmentShader("core/position_color")
      .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withCull(false)
      .buildSnippet();
   private static final RenderPipeline.Snippet DEBUG_POINTS_SNIPPET = RenderPipeline.builder()
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withVertexShader("core/debug_point")
      .withFragmentShader("core/position_color")
      .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_LINE_WIDTH)
      .withPrimitiveTopology(PrimitiveTopology.POINTS)
      .withCull(false)
      .buildSnippet();
   private static final RenderPipeline.Snippet PARTICLE_SNIPPET = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
      .withVertexShader("core/particle")
      .withFragmentShader("core/particle")
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
      .withVertexBinding(0, DefaultVertexFormat.PARTICLE)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withDepthStencilState(DepthStencilState.DEFAULT)
      .buildSnippet();
   private static final RenderPipeline.Snippet OIT_PARTICLE_SNIPPET = RenderPipeline.builder()
      .withVertexShader("core/particle")
      .withFragmentShader("core/particle")
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withBindGroupLayout(BindGroupLayouts.FOG)
      .withVertexBinding(0, DefaultVertexFormat.PARTICLE)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .buildSnippet();
   private static final RenderPipeline.Snippet WEATHER_SNIPPET = RenderPipeline.builder(PARTICLE_SNIPPET)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withCull(false)
      .buildSnippet();
   private static final RenderPipeline.Snippet CRUMBLING_SNIPPET = RenderPipeline.builder()
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withBindGroupLayout(BindGroupLayouts.FOG)
      .withVertexShader("core/rendertype_crumbling")
      .withFragmentShader("core/rendertype_crumbling")
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withVertexBinding(0, DefaultVertexFormat.BLOCK)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false, 1.0F, 10.0F))
      .buildSnippet();
   private static final RenderPipeline.Snippet GUI_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withVertexShader("core/gui")
      .withFragmentShader("core/gui")
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .buildSnippet();
   private static final RenderPipeline.Snippet GUI_TEXTURED_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withVertexShader("core/position_tex_color")
      .withFragmentShader("core/position_tex_color")
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .buildSnippet();
   private static final RenderPipeline.Snippet GUI_TEXT_SNIPPET = RenderPipeline.builder(TEXT_SNIPPET)
      .withShaderDefine("IS_GUI")
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(Optional.empty())
      .buildSnippet();
   private static final RenderPipeline.Snippet OUTLINE_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withVertexShader("core/rendertype_outline")
      .withFragmentShader("core/rendertype_outline")
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withColorTargetState(ColorTargetState.DEFAULT)
      .buildSnippet();
   public static final RenderPipeline.Snippet POST_PROCESSING_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .buildSnippet();
   public static final RenderPipeline SOLID_BLOCK = register(
      RenderPipeline.builder(BLOCK_SNIPPET).withLocation("pipeline/solid_block").withColorTargetState(ColorTargetState.DEFAULT).build()
   );
   public static final RenderPipeline SOLID_TERRAIN = register(
      RenderPipeline.builder(TERRAIN_SNIPPET).withLocation("pipeline/solid_terrain").withColorTargetState(ColorTargetState.DEFAULT).build()
   );
   public static final RenderPipeline WIREFRAME = register(
      RenderPipeline.builder(TERRAIN_SNIPPET)
         .withLocation("pipeline/wireframe")
         .withPolygonMode(PolygonMode.WIREFRAME)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline CUTOUT_BLOCK = register(
      RenderPipeline.builder(BLOCK_SNIPPET)
         .withLocation("pipeline/cutout_block")
         .withShaderDefine("ALPHA_CUTOUT", 0.5F)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline CUTOUT_TERRAIN = register(
      RenderPipeline.builder(TERRAIN_SNIPPET)
         .withLocation("pipeline/cutout_terrain")
         .withShaderDefine("ALPHA_CUTOUT", 0.5F)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline TRANSLUCENT_TERRAIN = register(
      RenderPipeline.builder(TERRAIN_SNIPPET)
         .withLocation("pipeline/translucent_terrain")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .build()
   );
   public static final OitPipelineSet OIT_TERRAIN = register(
      OitPipelineSet.builder(
            "terrain",
            RenderPipeline.builder(GENERIC_BLOCKS_SNIPPET)
               .withVertexShader("core/terrain")
               .withFragmentShader("core/terrain")
               .withBindGroupLayout(BindGroupLayouts.CHUNK_SECTION)
               .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         )
         .withAccumulateModifier(accumulate -> accumulate.withBindGroupLayout(BindGroupLayouts.SAMPLER2))
         .build()
   );
   public static final RenderPipeline TRANSLUCENT_BLOCK = register(
      RenderPipeline.builder(BLOCK_SNIPPET)
         .withLocation("pipeline/translucent_block")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withDepthStencilState(DepthStencilState.DEFAULT)
         .build()
   );
   public static final OitPipelineSet OIT_TRANSLUCENT_BLOCK = register(
      OitPipelineSet.builder(
            "translucent_block",
            RenderPipeline.builder(LIT_BLOCKS_SNIPPET)
               .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
               .withVertexShader("core/block")
               .withFragmentShader("core/block")
               .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         )
         .build()
   );
   public static final RenderPipeline ARMOR_CUTOUT_NO_CULL = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/armor_cutout_no_cull")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("NO_OVERLAY")
         .withShaderDefine("PER_FACE_LIGHTING")
         .withCull(false)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline ARMOR_DECAL_CUTOUT_NO_CULL = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/armor_decal_cutout_no_cull")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("NO_OVERLAY")
         .withShaderDefine("PER_FACE_LIGHTING")
         .withCull(false)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .withDepthStencilState(new DepthStencilState(CompareOp.EQUAL, false))
         .build()
   );
   public static final RenderPipeline WOLF_ARMOR_CRACKS = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/armor_translucent")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("NO_OVERLAY")
         .withShaderDefine("PER_FACE_LIGHTING")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline ENTITY_SOLID = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/entity_solid")
         .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline ENTITY_SOLID_Z_OFFSET_FORWARD = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/entity_solid_offset_forward")
         .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline ENTITY_CUTOUT_CULL = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/entity_cutout_cull")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline ENTITY_CUTOUT = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/entity_cutout")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("PER_FACE_LIGHTING")
         .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
         .withCull(false)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline ENTITY_CUTOUT_Z_OFFSET = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/entity_cutout_z_offset")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("PER_FACE_LIGHTING")
         .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
         .withCull(false)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline ENTITY_CUTOUT_DISSOLVE = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/entity_cutout_dissolve")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("PER_FACE_LIGHTING")
         .withShaderDefine("DISSOLVE")
         .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
         .withBindGroupLayout(BindGroupLayouts.DISSOLVE_MASK_SAMPLER)
         .withCull(false)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline ENTITY_TRANSLUCENT = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/entity_translucent")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("PER_FACE_LIGHTING")
         .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline ENTITY_TRANSLUCENT_OPAQUE_PARTS = register(
      RenderPipeline.builder(ENTITY_SNIPPET, OIT_OPAQUE_PARTS_THRESHOLD_SNIPPET)
         .withLocation("pipeline/entity_translucent_opaque_parts")
         .withShaderDefine("PER_FACE_LIGHTING")
         .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .withCull(false)
         .build()
   );
   public static final OitPipelineSet OIT_ENTITY = register(
      OitPipelineSet.builder("entity", RenderPipeline.builder(OIT_ENTITY_SNIPPET).withCull(false))
         .withAccumulateModifier(
            accumulate -> accumulate.withShaderDefine("PER_FACE_LIGHTING")
               .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
               .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
         )
         .build()
   );
   public static final RenderPipeline ENTITY_TRANSLUCENT_EMISSIVE = register(
      RenderPipeline.builder(ENTITY_EMISSIVE_SNIPPET)
         .withLocation("pipeline/entity_translucent_emissive")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("PER_FACE_LIGHTING")
         .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
         .build()
   );
   public static final RenderPipeline ENTITY_TRANSLUCENT_EMISSIVE_OPAQUE_PARTS = register(
      RenderPipeline.builder(ENTITY_EMISSIVE_SNIPPET, OIT_OPAQUE_PARTS_THRESHOLD_SNIPPET)
         .withLocation("pipeline/entity_translucent_emissive_opaque_parts")
         .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .withCull(false)
         .build()
   );
   public static final OitPipelineSet OIT_ENTITY_EMISSIVE = register(
      OitPipelineSet.builder("entity_emissive", RenderPipeline.builder(OIT_ENTITY_SNIPPET).withCull(false))
         .withAccumulateModifier(
            accumulate -> accumulate.withShaderDefine("PER_FACE_LIGHTING").withShaderDefine("EMISSIVE").withBindGroupLayout(BindGroupLayouts.SAMPLER1)
         )
         .build()
   );
   public static final RenderPipeline ENTITY_TRANSLUCENT_CULL = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/entity_translucent_cull")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .build()
   );
   public static final RenderPipeline ENTITY_TRANSLUCENT_CULL_OPAQUE_PARTS = register(
      RenderPipeline.builder(ENTITY_SNIPPET, OIT_OPAQUE_PARTS_THRESHOLD_SNIPPET)
         .withLocation("pipeline/entity_translucent_cull_opaque_parts")
         .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final OitPipelineSet OIT_ENTITY_CULL = register(
      OitPipelineSet.builder("entity_cull", RenderPipeline.builder(OIT_ENTITY_SNIPPET))
         .withAccumulateModifier(accumulate -> accumulate.withBindGroupLayout(BindGroupLayouts.SAMPLER1).withBindGroupLayout(BindGroupLayouts.SAMPLER2))
         .build()
   );
   public static final RenderPipeline END_CRYSTAL_BEAM = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/end_crystal_beam")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("NO_OVERLAY")
         .withCull(false)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline BANNER_PATTERN = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/banner_pattern")
         .withShaderDefine("NO_OVERLAY")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
         .build()
   );
   public static final RenderPipeline BREEZE_WIND = register(
      RenderPipeline.builder(ENTITY_SNIPPET)
         .withLocation("pipeline/breeze_wind")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("APPLY_TEXTURE_MATRIX")
         .withShaderDefine("NO_OVERLAY")
         .withShaderDefine("NO_CARDINAL_LIGHTING")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline BREEZE_WIND_OPAQUE_PARTS = register(
      RenderPipeline.builder(ENTITY_SNIPPET, OIT_OPAQUE_PARTS_THRESHOLD_SNIPPET)
         .withLocation("pipeline/breeze_wind_opaque_parts")
         .withShaderDefine("APPLY_TEXTURE_MATRIX")
         .withShaderDefine("NO_OVERLAY")
         .withShaderDefine("NO_CARDINAL_LIGHTING")
         .withColorTargetState(ColorTargetState.DEFAULT)
         .withCull(false)
         .build()
   );
   public static final OitPipelineSet OIT_BREEZE_WIND = register(
      OitPipelineSet.builder(
            "breeze_wind",
            RenderPipeline.builder(OIT_ENTITY_SNIPPET)
               .withShaderDefine("APPLY_TEXTURE_MATRIX")
               .withShaderDefine("NO_OVERLAY")
               .withShaderDefine("NO_CARDINAL_LIGHTING")
               .withCull(false)
         )
         .withAccumulateModifier(accumulate -> accumulate.withBindGroupLayout(BindGroupLayouts.SAMPLER2))
         .build()
   );
   private static final RenderPipeline.Snippet ENERGY_SWIRL_SNIPPET = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
      .withVertexShader("core/entity")
      .withFragmentShader("core/entity")
      .withShaderDefine("ALPHA_CUTOUT", 0.1F)
      .withShaderDefine("EMISSIVE")
      .withShaderDefine("NO_OVERLAY")
      .withShaderDefine("NO_CARDINAL_LIGHTING")
      .withShaderDefine("APPLY_TEXTURE_MATRIX")
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withCull(false)
      .withVertexBinding(0, DefaultVertexFormat.ENTITY)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .buildSnippet();
   public static final RenderPipeline ENERGY_SWIRL = register(
      RenderPipeline.builder(ENERGY_SWIRL_SNIPPET)
         .withLocation("pipeline/energy_swirl")
         .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
         .withDepthStencilState(DepthStencilState.DEFAULT)
         .build()
   );
   public static final OitPipelineSet OIT_ENERGY_SWIRL = register(
      OitPipelineSet.builder("energy_swirl", RenderPipeline.builder(ENERGY_SWIRL_SNIPPET).withShaderDefine("OIT_ADDITIVE")).build()
   );
   public static final RenderPipeline EYES = register(
      RenderPipeline.builder(ENTITY_NO_LIGHTMAP_SNIPPET, EYES_SNIPPET)
         .withLocation("pipeline/eyes")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
         .build()
   );
   public static final RenderPipeline EYES_OPAQUE_PARTS = register(
      RenderPipeline.builder(ENTITY_NO_LIGHTMAP_SNIPPET, EYES_SNIPPET, OIT_OPAQUE_PARTS_THRESHOLD_SNIPPET)
         .withLocation("pipeline/eyes_opaque_parts")
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final OitPipelineSet OIT_EYES = register(OitPipelineSet.builder("eyes", RenderPipeline.builder(OIT_ENTITY_SNIPPET, EYES_SNIPPET)).build());
   private static final RenderPipeline.Snippet ENTITY_SHADOW_SNIPPET = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
      .withVertexShader("core/rendertype_entity_shadow")
      .withFragmentShader("core/rendertype_entity_shadow")
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withVertexBinding(0, DefaultVertexFormat.ENTITY)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .buildSnippet();
   public static final RenderPipeline ENTITY_SHADOW = register(
      RenderPipeline.builder(ENTITY_SHADOW_SNIPPET)
         .withLocation("pipeline/entity_shadow")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .build()
   );
   public static final OitPipelineSet OIT_ENTITY_SHADOW = register(
      OitPipelineSet.builder("entity_shadow", RenderPipeline.builder(ENTITY_SHADOW_SNIPPET)).build()
   );
   public static final RenderPipeline.Snippet GLINT_SNIPPET = RenderPipeline.builder()
      .withShaderDefine("GLINT")
      .withBindGroupLayout(BindGroupLayouts.GLINT_SAMPLER)
      .buildSnippet();
   public static final RenderPipeline.Snippet GLINT_SPECIAL_SNIPPET = RenderPipeline.builder(GLINT_SNIPPET).withShaderDefine("GLINT_SPECIAL").buildSnippet();
   public static final RenderPipeline ARMOR_CUTOUT_NO_CULL_GLINT = register(
      RenderPipeline.builder(ENTITY_SNIPPET, GLINT_SNIPPET)
         .withLocation("pipeline/armor_cutout_no_cull_glint")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withShaderDefine("NO_OVERLAY")
         .withShaderDefine("PER_FACE_LIGHTING")
         .withCull(false)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline ENTITY_SOLID_GLINT = register(
      RenderPipeline.builder(ENTITY_SNIPPET, GLINT_SNIPPET)
         .withLocation("pipeline/entity_solid_glint")
         .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline ITEM_CUTOUT = register(
      RenderPipeline.builder(ITEM_SNIPPET)
         .withLocation("pipeline/item_cutout")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline ITEM_CUTOUT_GLINT = register(
      RenderPipeline.builder(ITEM_SNIPPET, GLINT_SNIPPET)
         .withLocation("pipeline/item_cutout")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline ITEM_CUTOUT_GLINT_SPECIAL = register(
      RenderPipeline.builder(ITEM_SNIPPET, GLINT_SPECIAL_SNIPPET)
         .withLocation("pipeline/item_cutout")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withVertexBinding(0, DefaultVertexFormat.ENTITY_GLINT_SPECIAL)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline ITEM_TRANSLUCENT = register(
      RenderPipeline.builder(ITEM_SNIPPET)
         .withLocation("pipeline/item_translucent")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .build()
   );
   public static final RenderPipeline ITEM_TRANSLUCENT_OPAQUE_PARTS = register(
      RenderPipeline.builder(ITEM_SNIPPET, OIT_OPAQUE_PARTS_THRESHOLD_SNIPPET)
         .withLocation("pipeline/item_translucent_opaque_parts")
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final OitPipelineSet OIT_ITEM = register(
      OitPipelineSet.builder("item", RenderPipeline.builder(OIT_ITEM_SNIPPET))
         .withAccumulateModifier(accumulate -> accumulate.withBindGroupLayout(BindGroupLayouts.SAMPLER1).withBindGroupLayout(BindGroupLayouts.SAMPLER2))
         .build()
   );
   public static final RenderPipeline ITEM_TRANSLUCENT_GLINT = register(
      RenderPipeline.builder(ITEM_SNIPPET, GLINT_SNIPPET)
         .withLocation("pipeline/item_translucent_glint")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .build()
   );
   public static final RenderPipeline ITEM_TRANSLUCENT_GLINT_SPECIAL = register(
      RenderPipeline.builder(ITEM_SNIPPET, GLINT_SPECIAL_SNIPPET)
         .withLocation("pipeline/item_translucent_glint")
         .withShaderDefine("ALPHA_CUTOUT", 0.1F)
         .withVertexBinding(0, DefaultVertexFormat.ENTITY_GLINT_SPECIAL)
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .build()
   );
   public static final RenderPipeline ITEM_TRANSLUCENT_GLINT_OPAQUE_PARTS = register(
      RenderPipeline.builder(ITEM_SNIPPET, GLINT_SNIPPET, OIT_OPAQUE_PARTS_THRESHOLD_SNIPPET)
         .withLocation("pipeline/item_translucent_glint_opaque_parts")
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline ITEM_TRANSLUCENT_GLINT_SPECIAL_OPAQUE_PARTS = register(
      RenderPipeline.builder(ITEM_SNIPPET, GLINT_SPECIAL_SNIPPET, OIT_OPAQUE_PARTS_THRESHOLD_SNIPPET)
         .withLocation("pipeline/item_translucent_glint_opaque_parts")
         .withVertexBinding(0, DefaultVertexFormat.ENTITY_GLINT_SPECIAL)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final OitPipelineSet OIT_ITEM_GLINT = register(
      OitPipelineSet.builder("item_glint", RenderPipeline.builder(OIT_ITEM_SNIPPET, GLINT_SNIPPET))
         .withAccumulateModifier(
            accumulate -> accumulate.withBindGroupLayout(BindGroupLayouts.GLOBALS)
               .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
               .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
         )
         .build()
   );
   public static final OitPipelineSet OIT_ITEM_GLINT_SPECIAL = register(
      OitPipelineSet.builder(
            "item_glint_special",
            RenderPipeline.builder(OIT_ITEM_SNIPPET, GLINT_SPECIAL_SNIPPET).withVertexBinding(0, DefaultVertexFormat.ENTITY_GLINT_SPECIAL)
         )
         .withAccumulateModifier(
            accumulate -> accumulate.withBindGroupLayout(BindGroupLayouts.GLOBALS)
               .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
               .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
         )
         .build()
   );
   public static final RenderPipeline BEACON_BEAM_OPAQUE = register(
      RenderPipeline.builder(BEACON_BEAM_SNIPPET).withLocation("pipeline/beacon_beam_opaque").withColorTargetState(ColorTargetState.DEFAULT).build()
   );
   public static final RenderPipeline BEACON_BEAM_TRANSLUCENT = register(
      RenderPipeline.builder(BEACON_BEAM_SNIPPET)
         .withLocation("pipeline/beacon_beam_translucent")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
         .build()
   );
   public static final OitPipelineSet OIT_BEACON_BEAM = register(OitPipelineSet.builder("beacon_beam", RenderPipeline.builder(BEACON_BEAM_SNIPPET)).build());
   public static final RenderPipeline LEASH = register(
      RenderPipeline.builder(MATRICES_FOG_SNIPPET)
         .withLocation("pipeline/leash")
         .withVertexShader("core/rendertype_leash")
         .withFragmentShader("core/rendertype_leash")
         .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
         .withCull(false)
         .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_LIGHTMAP)
         .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .withDepthStencilState(DepthStencilState.DEFAULT)
         .build()
   );
   public static final RenderPipeline WATER_MASK = register(
      RenderPipeline.builder(WATER_MASK_SNIPPET)
         .withLocation("pipeline/water_mask")
         .withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT), GpuFormat.RGBA8_UNORM, 0))
         .build()
   );
   public static final RenderPipeline OIT_WATER_MASK = register(RenderPipeline.builder(WATER_MASK_SNIPPET).withLocation("pipeline/oit_water_mask").build());
   public static final RenderPipeline GLINT = register(
      RenderPipeline.builder(GLOBALS_SNIPPET)
         .withBindGroupLayout(BindGroupLayouts.PROJECTION)
         .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
         .withBindGroupLayout(BindGroupLayouts.FOG)
         .withLocation("pipeline/glint")
         .withVertexShader("core/glint")
         .withFragmentShader("core/glint")
         .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
         .withCull(false)
         .withColorTargetState(new ColorTargetState(BlendFunction.GLINT))
         .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
         .withPrimitiveTopology(PrimitiveTopology.QUADS)
         .withDepthStencilState(new DepthStencilState(CompareOp.EQUAL, false))
         .build()
   );
   public static final RenderPipeline CRUMBLING = register(
      RenderPipeline.builder(CRUMBLING_SNIPPET, GLOBALS_SNIPPET)
         .withBindGroupLayout(BindGroupLayouts.PROJECTION)
         .withLocation("pipeline/crumbling")
         .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.DST_COLOR, BlendFactor.SRC_COLOR, BlendFactor.ONE, BlendFactor.ZERO)))
         .build()
   );
   public static final OitPipelineSet OIT_CRUMBLING = register(OitPipelineSet.builder("crumbling", RenderPipeline.builder(CRUMBLING_SNIPPET)).build());
   public static final RenderPipeline TEXT = register(
      RenderPipeline.builder(WORLD_TEXT_SNIPPET).withLocation("pipeline/text").withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).build()
   );
   public static final OitPipelineSet OIT_TEXT = register(OitPipelineSet.builder("text", RenderPipeline.builder(WORLD_TEXT_SNIPPET)).build());
   public static final RenderPipeline GUI_TEXT = register(RenderPipeline.builder(GUI_TEXT_SNIPPET).withLocation("pipeline/gui_text").build());
   public static final RenderPipeline TEXT_BACKGROUND = register(
      RenderPipeline.builder(TEXT_BACKGROUND_SNIPPET)
         .withBindGroupLayout(BindGroupLayouts.FOG)
         .withLocation("pipeline/text_background")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withDepthStencilState(DepthStencilState.DEFAULT)
         .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
         .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_LIGHTMAP)
         .build()
   );
   public static final OitPipelineSet OIT_TEXT_BACKGROUND = register(
      OitPipelineSet.builder(
            "text_background",
            RenderPipeline.builder(TEXT_BACKGROUND_SNIPPET)
               .withBindGroupLayout(BindGroupLayouts.FOG)
               .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
               .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_LIGHTMAP)
         )
         .build()
   );
   public static final RenderPipeline TEXT_GRAYSCALE = register(
      RenderPipeline.builder(WORLD_TEXT_SNIPPET)
         .withLocation("pipeline/text_grayscale")
         .withShaderDefine("IS_GRAYSCALE")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .build()
   );
   public static final OitPipelineSet OIT_TEXT_GRAYSCALE = register(
      OitPipelineSet.builder("text_grayscale", RenderPipeline.builder(WORLD_TEXT_SNIPPET).withShaderDefine("IS_GRAYSCALE")).build()
   );
   public static final RenderPipeline GUI_TEXT_GRAYSCALE = register(
      RenderPipeline.builder(GUI_TEXT_SNIPPET).withLocation("pipeline/gui_text_grayscale").withShaderDefine("IS_GRAYSCALE").build()
   );
   public static final RenderPipeline TEXT_POLYGON_OFFSET = register(
      RenderPipeline.builder(WORLD_TEXT_SNIPPET)
         .withLocation("pipeline/text_polygon_offset")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 1.0F, 10.0F))
         .build()
   );
   public static final OitPipelineSet OIT_TEXT_POLYGON_OFFSET = register(
      OitPipelineSet.builder("text_polygon_offset", RenderPipeline.builder(WORLD_TEXT_SNIPPET)).build()
   );
   public static final RenderPipeline TEXT_GRAYSCALE_POLYGON_OFFSET = register(
      RenderPipeline.builder(WORLD_TEXT_SNIPPET)
         .withLocation("pipeline/text_grayscale_polygon_offset")
         .withShaderDefine("IS_GRAYSCALE")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 1.0F, 10.0F))
         .build()
   );
   public static final OitPipelineSet OIT_TEXT_GRAYSCALE_POLYGON_OFFSET = register(
      OitPipelineSet.builder("text_grayscale_polygon_offset", RenderPipeline.builder(WORLD_TEXT_SNIPPET).withShaderDefine("IS_GRAYSCALE")).build()
   );
   public static final RenderPipeline TEXT_SEE_THROUGH = register(
      RenderPipeline.builder(TEXT_SNIPPET)
         .withLocation("pipeline/text_see_through")
         .withShaderDefine("IS_SEE_THROUGH")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withDepthStencilState(Optional.empty())
         .build()
   );
   public static final OitPipelineSet OIT_TEXT_SEE_THROUGH = register(
      OitPipelineSet.builder(
            "text_see_through", RenderPipeline.builder(TEXT_SNIPPET).withShaderDefine("IS_SEE_THROUGH").withShaderDefine("OIT_FORCE_ZERO_DEPTH")
         )
         .withoutDepthTest()
         .build()
   );
   public static final RenderPipeline TEXT_BACKGROUND_SEE_THROUGH = register(
      RenderPipeline.builder(TEXT_BACKGROUND_SNIPPET)
         .withLocation("pipeline/text_background_see_through")
         .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
         .withShaderDefine("IS_SEE_THROUGH")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withDepthStencilState(Optional.empty())
         .build()
   );
   public static final OitPipelineSet OIT_TEXT_BACKGROUND_SEE_THROUGH = register(
      OitPipelineSet.builder(
            "text_background_see_through",
            RenderPipeline.builder(TEXT_BACKGROUND_SNIPPET)
               .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
               .withShaderDefine("IS_SEE_THROUGH")
               .withShaderDefine("OIT_FORCE_ZERO_DEPTH")
         )
         .withoutDepthTest()
         .build()
   );
   public static final RenderPipeline TEXT_GRAYSCALE_SEE_THROUGH = register(
      RenderPipeline.builder(TEXT_SNIPPET)
         .withLocation("pipeline/text_grayscale_see_through")
         .withShaderDefine("IS_GRAYSCALE")
         .withShaderDefine("IS_SEE_THROUGH")
         .withColorTargetState(ColorTargetState.DEFAULT)
         .withDepthStencilState(Optional.empty())
         .build()
   );
   public static final OitPipelineSet OIT_TEXT_GRAYSCALE_SEE_THROUGH = register(
      OitPipelineSet.builder(
            "text_grayscale_see_through",
            RenderPipeline.builder(TEXT_SNIPPET).withShaderDefine("IS_GRAYSCALE").withShaderDefine("IS_SEE_THROUGH").withShaderDefine("OIT_FORCE_ZERO_DEPTH")
         )
         .withoutDepthTest()
         .build()
   );
   public static final RenderPipeline LIGHTNING = register(
      RenderPipeline.builder(LIGHTNING_SNIPPET).withLocation("pipeline/lightning").withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING)).build()
   );
   public static final OitPipelineSet OIT_LIGHTNING = register(
      OitPipelineSet.builder("lightning", RenderPipeline.builder(LIGHTNING_SNIPPET).withShaderDefine("OIT_ADDITIVE")).build()
   );
   public static final RenderPipeline DRAGON_RAYS = register(
      RenderPipeline.builder(DRAGON_RAYS_SNIPPET)
         .withLocation("pipeline/dragon_rays")
         .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
         .build()
   );
   public static final OitPipelineSet OIT_DRAGON_RAYS = register(
      OitPipelineSet.builder("dragon_rays", RenderPipeline.builder(DRAGON_RAYS_SNIPPET).withShaderDefine("OIT_ADDITIVE")).build()
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
   public static final OitPipelineSet OIT_CLOUDS = register(
      OitPipelineSet.builder("clouds", RenderPipeline.builder(OIT_CLOUDS_SNIPPET))
         .withDepthBoundsModifier(depthBounds -> depthBounds.withDepthStencilState(DepthStencilState.DEFAULT))
         .build()
   );
   public static final OitPipelineSet OIT_FLAT_CLOUDS = register(
      OitPipelineSet.builder("flat_clouds", RenderPipeline.builder(OIT_CLOUDS_SNIPPET).withCull(false)).build()
   );
   public static final RenderPipeline LINES = register(
      RenderPipeline.builder(LINES_SNIPPET).withLocation("pipeline/lines").withColorTargetState(ColorTargetState.DEFAULT).build()
   );
   public static final RenderPipeline LINES_TRANSLUCENT = register(
      RenderPipeline.builder(LINES_SNIPPET)
         .withLocation("pipeline/lines_translucent")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
         .build()
   );
   public static final OitPipelineSet OIT_LINES_TRANSLUCENT = register(
      OitPipelineSet.builder("lines_translucent", RenderPipeline.builder(OIT_LINES_SNIPPET)).build()
   );
   public static final RenderPipeline LINES_DEPTH_BIAS = register(
      RenderPipeline.builder(LINES_SNIPPET)
         .withLocation("pipeline/lines_depth_bias")
         .withColorTargetState(ColorTargetState.DEFAULT)
         .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 1.0F, 1.0F))
         .build()
   );
   public static final RenderPipeline SECONDARY_BLOCK_OUTLINE = register(
      RenderPipeline.builder(LINES_SNIPPET)
         .withLocation("pipeline/secondary_block_outline")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
         .build()
   );
   public static final OitPipelineSet OIT_SECONDARY_BLOCK_OUTLINE = register(
      OitPipelineSet.builder("secondary_block_outline", RenderPipeline.builder(OIT_LINES_SNIPPET)).build()
   );
   public static final RenderPipeline DEBUG_POINTS = register(
      RenderPipeline.builder(DEBUG_POINTS_SNIPPET)
         .withLocation("pipeline/debug_points")
         .withBindGroupLayout(BindGroupLayouts.GLOBALS)
         .withBindGroupLayout(BindGroupLayouts.PROJECTION)
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withDepthStencilState(DepthStencilState.DEFAULT)
         .build()
   );
   public static final OitPipelineSet OIT_DEBUG_POINTS = register(OitPipelineSet.builder("debug_points", RenderPipeline.builder(DEBUG_POINTS_SNIPPET)).build());
   public static final RenderPipeline DEBUG_FILLED_BOX = register(
      RenderPipeline.builder(DEBUG_FILLED_SNIPPET).withLocation("pipeline/debug_filled_box").build()
   );
   public static final OitPipelineSet OIT_DEBUG_FILLED_BOX = register(
      OitPipelineSet.builder("debug_filled_box", RenderPipeline.builder(OIT_DEBUG_FILLED_SNIPPET)).build()
   );
   public static final RenderPipeline DEBUG_QUADS = register(
      RenderPipeline.builder(DEBUG_FILLED_SNIPPET).withLocation("pipeline/debug_quads").withCull(false).build()
   );
   public static final OitPipelineSet OIT_DEBUG_QUADS = register(
      OitPipelineSet.builder("debug_quads", RenderPipeline.builder(OIT_DEBUG_FILLED_SNIPPET)).build()
   );
   public static final RenderPipeline DEBUG_TRIANGLE_FAN = register(
      RenderPipeline.builder(DEBUG_FILLED_SNIPPET)
         .withLocation("pipeline/debug_triangle_fan")
         .withCull(false)
         .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
         .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_FAN)
         .build()
   );
   public static final OitPipelineSet OIT_DEBUG_TRIANGLE_FAN = register(
      OitPipelineSet.builder(
            "debug_triangle_fan",
            RenderPipeline.builder(OIT_DEBUG_FILLED_SNIPPET)
               .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
               .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_FAN)
         )
         .build()
   );
   public static final RenderPipeline WORLD_BORDER = register(
      RenderPipeline.builder(GLOBALS_SNIPPET)
         .withBindGroupLayout(BindGroupLayouts.PROJECTION)
         .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
         .withLocation("pipeline/world_border")
         .withVertexShader("core/world_border")
         .withFragmentShader("core/world_border")
         .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
         .withColorTargetState(new ColorTargetState(BlendFunction.OVERLAY))
         .withCull(false)
         .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
         .withPrimitiveTopology(PrimitiveTopology.QUADS)
         .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 3.0F, 3.0F))
         .build()
   );
   public static final OitPipelineSet OIT_WORLD_BORDER = register(
      OitPipelineSet.builder(
            "world_border",
            RenderPipeline.builder(GLOBALS_SNIPPET)
               .withVertexShader("core/world_border")
               .withFragmentShader("core/world_border")
               .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
               .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
               .withCull(false)
               .withPrimitiveTopology(PrimitiveTopology.QUADS)
               .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
               .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false, 3.0F, 3.0F))
               .withShaderDefine("OIT_ADDITIVE")
         )
         .build()
   );
   public static final RenderPipeline OPAQUE_PARTICLE = register(
      RenderPipeline.builder(PARTICLE_SNIPPET).withLocation("pipeline/opaque_particle").withColorTargetState(ColorTargetState.DEFAULT).build()
   );
   public static final RenderPipeline TRANSLUCENT_PARTICLE = register(
      RenderPipeline.builder(PARTICLE_SNIPPET)
         .withLocation("pipeline/translucent_particle")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .build()
   );
   public static final OitPipelineSet OIT_PARTICLE = register(OitPipelineSet.builder("particle", RenderPipeline.builder(OIT_PARTICLE_SNIPPET)).build());
   public static final RenderPipeline WEATHER = register(
      RenderPipeline.builder(WEATHER_SNIPPET)
         .withLocation("pipeline/weather")
         .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
         .build()
   );
   public static final OitPipelineSet OIT_WEATHER = register(
      OitPipelineSet.builder("weather", RenderPipeline.builder(OIT_PARTICLE_SNIPPET).withCull(false)).build()
   );
   public static final RenderPipeline SKY = register(
      RenderPipeline.builder(MATRICES_FOG_SNIPPET)
         .withLocation("pipeline/sky")
         .withVertexShader("core/sky")
         .withFragmentShader("core/sky")
         .withVertexBinding(0, DefaultVertexFormat.POSITION)
         .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_FAN)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline END_SKY = register(
      RenderPipeline.builder(GLOBALS_SNIPPET)
         .withBindGroupLayout(BindGroupLayouts.PROJECTION)
         .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
         .withLocation("pipeline/end_sky")
         .withVertexShader("core/position_tex_color")
         .withFragmentShader("core/position_tex_color")
         .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
         .withPrimitiveTopology(PrimitiveTopology.QUADS)
         .build()
   );
   public static final RenderPipeline SUNRISE_SUNSET = register(
      RenderPipeline.builder(GLOBALS_SNIPPET)
         .withBindGroupLayout(BindGroupLayouts.PROJECTION)
         .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
         .withLocation("pipeline/sunrise_sunset")
         .withVertexShader("core/position_color")
         .withFragmentShader("core/position_color")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
         .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_FAN)
         .build()
   );
   public static final RenderPipeline STARS = register(
      RenderPipeline.builder(GLOBALS_SNIPPET)
         .withBindGroupLayout(BindGroupLayouts.PROJECTION)
         .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
         .withLocation("pipeline/stars")
         .withVertexShader("core/stars")
         .withFragmentShader("core/stars")
         .withColorTargetState(new ColorTargetState(BlendFunction.OVERLAY))
         .withVertexBinding(0, DefaultVertexFormat.POSITION)
         .withPrimitiveTopology(PrimitiveTopology.QUADS)
         .build()
   );
   public static final RenderPipeline CELESTIAL = register(
      RenderPipeline.builder(GLOBALS_SNIPPET)
         .withBindGroupLayout(BindGroupLayouts.PROJECTION)
         .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
         .withLocation("pipeline/celestial")
         .withVertexShader("core/position_tex")
         .withFragmentShader("core/position_tex")
         .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
         .withColorTargetState(new ColorTargetState(BlendFunction.OVERLAY))
         .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
         .withPrimitiveTopology(PrimitiveTopology.QUADS)
         .build()
   );
   public static final RenderPipeline GUI = register(RenderPipeline.builder(GUI_SNIPPET).withLocation("pipeline/gui").build());
   public static final RenderPipeline GUI_INVERT = register(
      RenderPipeline.builder(GUI_SNIPPET).withLocation("pipeline/gui_invert").withColorTargetState(new ColorTargetState(BlendFunction.INVERT)).build()
   );
   public static final RenderPipeline GUI_TEXT_HIGHLIGHT = register(
      RenderPipeline.builder(GUI_SNIPPET)
         .withLocation("pipeline/gui_text_highlight")
         .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
         .build()
   );
   public static final RenderPipeline GUI_TEXTURED = register(RenderPipeline.builder(GUI_TEXTURED_SNIPPET).withLocation("pipeline/gui_textured").build());
   public static final RenderPipeline GUI_TEXTURED_PREMULTIPLIED_ALPHA = register(
      RenderPipeline.builder(GUI_TEXTURED_SNIPPET)
         .withLocation("pipeline/gui_textured_premultiplied_alpha")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA))
         .build()
   );
   public static final RenderPipeline BLOCK_SCREEN_EFFECT = register(
      RenderPipeline.builder(GUI_TEXTURED_SNIPPET).withLocation("pipeline/block_screen_effect").build()
   );
   public static final RenderPipeline FIRE_SCREEN_EFFECT = register(
      RenderPipeline.builder(GUI_TEXTURED_SNIPPET).withLocation("pipeline/fire_screen_effect").build()
   );
   public static final RenderPipeline GUI_OPAQUE_TEXTURED_BACKGROUND = register(
      RenderPipeline.builder(GUI_TEXTURED_SNIPPET)
         .withLocation("pipeline/gui_opaque_textured_background")
         .withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, 15))
         .build()
   );
   public static final RenderPipeline GUI_NAUSEA_OVERLAY = register(
      RenderPipeline.builder(GUI_TEXTURED_SNIPPET)
         .withLocation("pipeline/gui_nausea_overlay")
         .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
         .build()
   );
   public static final RenderPipeline VIGNETTE = register(
      RenderPipeline.builder(GUI_TEXTURED_SNIPPET)
         .withLocation("pipeline/vignette")
         .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.ZERO, BlendFactor.ONE_MINUS_SRC_COLOR, BlendFactor.ZERO, BlendFactor.ONE)))
         .build()
   );
   public static final RenderPipeline CROSSHAIR = register(
      RenderPipeline.builder(GUI_TEXTURED_SNIPPET).withLocation("pipeline/crosshair").withColorTargetState(new ColorTargetState(BlendFunction.INVERT)).build()
   );
   public static final RenderPipeline MOJANG_LOGO = register(
      RenderPipeline.builder(GUI_TEXTURED_SNIPPET)
         .withLocation("pipeline/mojang_logo")
         .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE)))
         .build()
   );
   public static final RenderPipeline ENTITY_OUTLINE_BLIT = register(
      RenderPipeline.builder(GLOBALS_SNIPPET)
         .withLocation("pipeline/entity_outline_blit")
         .withVertexShader("core/screenquad")
         .withFragmentShader("core/blit_screen")
         .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
         .withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.ENTITY_OUTLINE_BLIT), GpuFormat.RGBA8_UNORM, 7))
         .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
         .build()
   );
   public static final RenderPipeline TRACY_BLIT = register(
      RenderPipeline.builder(GLOBALS_SNIPPET)
         .withLocation("pipeline/tracy_blit")
         .withVertexShader("core/screenquad")
         .withFragmentShader("core/blit_screen")
         .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
         .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline PANORAMA = register(
      RenderPipeline.builder(GLOBALS_SNIPPET)
         .withBindGroupLayout(BindGroupLayouts.PROJECTION)
         .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
         .withLocation("pipeline/panorama")
         .withVertexShader("core/panorama")
         .withFragmentShader("core/panorama")
         .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
         .withVertexBinding(0, DefaultVertexFormat.POSITION)
         .withPrimitiveTopology(PrimitiveTopology.QUADS)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline OUTLINE_CULL = register(RenderPipeline.builder(OUTLINE_SNIPPET).withLocation("pipeline/outline_cull").build());
   public static final RenderPipeline OUTLINE_NO_CULL = register(
      RenderPipeline.builder(OUTLINE_SNIPPET).withLocation("pipeline/outline_no_cull").withCull(false).build()
   );
   public static final RenderPipeline LIGHTMAP = register(
      RenderPipeline.builder(GLOBALS_SNIPPET)
         .withLocation("pipeline/lightmap")
         .withVertexShader("core/screenquad")
         .withFragmentShader("core/lightmap")
         .withBindGroupLayout(BindGroupLayouts.LIGHTMAP_INFO)
         .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
         .withColorTargetState(ColorTargetState.DEFAULT)
         .build()
   );
   public static final RenderPipeline.Snippet ANIMATE_SPRITE_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
      .withVertexShader("core/animate_sprite")
      .withBindGroupLayout(BindGroupLayouts.SPRITE_ANIMATION_INFO)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withColorTargetState(ColorTargetState.DEFAULT)
      .buildSnippet();
   public static final RenderPipeline ANIMATE_SPRITE_BLIT = register(
      RenderPipeline.builder(ANIMATE_SPRITE_SNIPPET)
         .withFragmentShader("core/animate_sprite_blit")
         .withLocation("pipeline/animate_sprite_blit")
         .withBindGroupLayout(BindGroupLayouts.SPRITE)
         .build()
   );
   public static final RenderPipeline ANIMATE_SPRITE_INTERPOLATE = register(
      RenderPipeline.builder(ANIMATE_SPRITE_SNIPPET)
         .withFragmentShader("core/animate_sprite_interpolate")
         .withLocation("pipeline/animate_sprite_interpolate")
         .withBindGroupLayout(BindGroupLayouts.CURRENT_SPRITE_NEXT_SPRITE)
         .build()
   );
   public static final RenderPipeline OIT_COMPOSITE = register(
      RenderPipeline.builder(OIT_SNIPPET)
         .withBindGroupLayout(BindGroupLayouts.PROJECTION)
         .withVertexShader("core/screenquad")
         .withFragmentShader("core/oit_composite")
         .withLocation("pipeline/oit_composite")
         .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
         .withBindGroupLayout(BindGroupLayouts.SAMPLER0_OIT_COEFFS_DEPTH_BOUNDS_SAMPLER)
         .withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA), GpuFormat.RGBA8_UNORM, 15))
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
         .build()
   );

   private static RenderPipeline register(final RenderPipeline pipeline) {
      PIPELINES_BY_LOCATION.put(pipeline.getLocation(), pipeline);
      return pipeline;
   }

   private static OitPipelineSet register(final OitPipelineSet oitPipelineSet) {
      PIPELINES_BY_LOCATION.put(oitPipelineSet.depthBoundsPipeline().getLocation(), oitPipelineSet.depthBoundsPipeline());
      PIPELINES_BY_LOCATION.put(oitPipelineSet.transmittancePipeline().getLocation(), oitPipelineSet.transmittancePipeline());
      PIPELINES_BY_LOCATION.put(oitPipelineSet.accumulatePipeline().getLocation(), oitPipelineSet.accumulatePipeline());
      return oitPipelineSet;
   }

   public static List<RenderPipeline> getStaticPipelines() {
      return PIPELINES_BY_LOCATION.values().stream().toList();
   }
}
