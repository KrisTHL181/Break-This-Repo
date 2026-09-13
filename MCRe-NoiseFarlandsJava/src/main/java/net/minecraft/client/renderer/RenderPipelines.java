package net.minecraft.client.renderer;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderPipelines {
    private static final Map<Identifier, RenderPipeline> PIPELINES_BY_LOCATION = new HashMap<>();
    private static final float ALPHA_CUTOUT_THRESHOLD_DEFAULT = 0.1F;
    private static final float ALPHA_CUTOUT_THRESHOLD_CUTOUT_TERRAIN = 0.5F;
    private static final RenderPipeline.Snippet GLOBALS_SNIPPET = RenderPipeline.builder().withBindGroupLayout(BindGroupLayouts.GLOBALS).buildSnippet();
    private static final RenderPipeline.Snippet MATRICES_FOG_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.FOG)
        .buildSnippet();
    private static final RenderPipeline.Snippet MATRICES_FOG_LIGHT_DIR_SNIPPET = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
        .withBindGroupLayout(BindGroupLayouts.LIGHTING)
        .buildSnippet();
    private static final RenderPipeline.Snippet GENERIC_BLOCKS_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
        .withBindGroupLayout(BindGroupLayouts.FOG)
        .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
        .withVertexBinding(0, DefaultVertexFormat.BLOCK)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .buildSnippet();
    private static final RenderPipeline.Snippet TERRAIN_SNIPPET = RenderPipeline.builder(GENERIC_BLOCKS_SNIPPET)
        .withBindGroupLayout(BindGroupLayouts.PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.CHUNK_SECTION)
        .withBindGroupLayout(BindGroupLayouts.TERRAIN_INFO)
        .withVertexShader("core/terrain")
        .withFragmentShader("core/terrain")
        .buildSnippet();
    // MCRe：26.3 MultiDrawIndirect 移植——MultiDraw 地形 snippet（instanced section 数据走 vertex binding 1，共享 TerrainUniform）
    private static final RenderPipeline.Snippet MULTIDRAW_TERRAIN_SNIPPET = RenderPipeline.builder(GENERIC_BLOCKS_SNIPPET)
        .withBindGroupLayout(BindGroupLayouts.PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.TERRAIN_INFO)
        .withVertexShader("core/terrain")
        .withFragmentShader("core/terrain")
        .withShaderDefine("MULTIDRAW_TERRAIN")
        .withVertexBinding(1, DefaultVertexFormat.CHUNK_DATA_INSTANCED)
        .buildSnippet();
    private static final RenderPipeline.Snippet BLOCK_SNIPPET = RenderPipeline.builder(GENERIC_BLOCKS_SNIPPET)
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withVertexShader("core/block")
        .withFragmentShader("core/block")
        .buildSnippet();
    private static final RenderPipeline.Snippet ENTITY_SNIPPET = RenderPipeline.builder(MATRICES_FOG_LIGHT_DIR_SNIPPET)
        .withVertexShader("core/entity")
        .withFragmentShader("core/entity")
        .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
        .withVertexBinding(0, DefaultVertexFormat.ENTITY)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withDepthStencilState(DepthStencilState.DEFAULT)
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
    private static final RenderPipeline.Snippet TEXT_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .buildSnippet();
    private static final RenderPipeline.Snippet WORLD_TEXT_SNIPPET = RenderPipeline.builder(TEXT_SNIPPET)
        .withBindGroupLayout(BindGroupLayouts.FOG)
        .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .buildSnippet();
    private static final RenderPipeline.Snippet END_PORTAL_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.FOG)
        .withVertexShader("core/rendertype_end_portal")
        .withFragmentShader("core/rendertype_end_portal")
        .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER1)
        .withVertexBinding(0, DefaultVertexFormat.POSITION)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .buildSnippet();
    private static final RenderPipeline.Snippet CLOUDS_SNIPPET = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
        .withVertexShader("core/rendertype_clouds")
        .withFragmentShader("core/rendertype_clouds")
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withBindGroupLayout(BindGroupLayouts.CLOUD_INFO)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .buildSnippet();
    private static final RenderPipeline.Snippet LINES_SNIPPET = RenderPipeline.builder(MATRICES_FOG_SNIPPET)
        .withVertexShader("core/rendertype_lines")
        .withFragmentShader("core/rendertype_lines")
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
        .withPrimitiveTopology(PrimitiveTopology.LINES)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .buildSnippet();
    private static final RenderPipeline.Snippet DEBUG_FILLED_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withVertexShader("core/position_color")
        .withFragmentShader("core/position_color")
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
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
    private static final RenderPipeline.Snippet WEATHER_SNIPPET = RenderPipeline.builder(PARTICLE_SNIPPET)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .buildSnippet();
    private static final RenderPipeline.Snippet GUI_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withVertexShader("core/gui")
        .withFragmentShader("core/gui")
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .buildSnippet();
    private static final RenderPipeline.Snippet GUI_TEXTURED_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withVertexShader("core/position_tex_color")
        .withFragmentShader("core/position_tex_color")
        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .buildSnippet();
    private static final RenderPipeline.Snippet GUI_TEXT_SNIPPET = RenderPipeline.builder(TEXT_SNIPPET)
        .withShaderDefine("IS_GUI")
        .withDepthStencilState(Optional.empty())
        .buildSnippet();
    private static final RenderPipeline.Snippet OUTLINE_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withVertexShader("core/rendertype_outline")
        .withFragmentShader("core/rendertype_outline")
        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .buildSnippet();
    public static final RenderPipeline.Snippet POST_PROCESSING_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .buildSnippet();
    public static final RenderPipeline SOLID_BLOCK = register(RenderPipeline.builder(BLOCK_SNIPPET).withLocation("pipeline/solid_block").build());
    public static final RenderPipeline SOLID_TERRAIN = register(RenderPipeline.builder(TERRAIN_SNIPPET).withLocation("pipeline/solid_terrain").build());
    // MCRe：26.3 MultiDrawIndirect 移植——MultiDraw 变体
    public static final RenderPipeline SOLID_TERRAIN_MULTIDRAW = register(
        RenderPipeline.builder(MULTIDRAW_TERRAIN_SNIPPET).withLocation("pipeline/solid_terrain_multidraw").build()
    );
    public static final RenderPipeline WIREFRAME = register(
        RenderPipeline.builder(TERRAIN_SNIPPET).withLocation("pipeline/wireframe").withPolygonMode(PolygonMode.WIREFRAME).build()
    );
    public static final RenderPipeline WIREFRAME_MULTIDRAW = register(
        RenderPipeline.builder(MULTIDRAW_TERRAIN_SNIPPET).withLocation("pipeline/wireframe_multidraw").withPolygonMode(PolygonMode.WIREFRAME).build()
    );
    public static final RenderPipeline CUTOUT_BLOCK = register(
        RenderPipeline.builder(BLOCK_SNIPPET).withLocation("pipeline/cutout_block").withShaderDefine("ALPHA_CUTOUT", 0.5F).build()
    );
    public static final RenderPipeline CUTOUT_TERRAIN = register(
        RenderPipeline.builder(TERRAIN_SNIPPET).withLocation("pipeline/cutout_terrain").withShaderDefine("ALPHA_CUTOUT", 0.5F).build()
    );
    public static final RenderPipeline CUTOUT_TERRAIN_MULTIDRAW = register(
        RenderPipeline.builder(MULTIDRAW_TERRAIN_SNIPPET)
            .withLocation("pipeline/cutout_terrain_multidraw")
            .withShaderDefine("ALPHA_CUTOUT", 0.5F)
            .build()
    );
    public static final RenderPipeline TRANSLUCENT_TERRAIN = register(
        RenderPipeline.builder(TERRAIN_SNIPPET)
            .withLocation("pipeline/translucent_terrain")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .build()
    );
    public static final RenderPipeline TRANSLUCENT_TERRAIN_MULTIDRAW = register(
        RenderPipeline.builder(MULTIDRAW_TERRAIN_SNIPPET)
            .withLocation("pipeline/translucent_terrain_multidraw")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
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
            .withDepthStencilState(new DepthStencilState(CompareOp.EQUAL, false))
            .build()
    );
    public static final RenderPipeline ARMOR_TRANSLUCENT = register(
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
        RenderPipeline.builder(ENTITY_SNIPPET).withLocation("pipeline/entity_solid").withBindGroupLayout(BindGroupLayouts.SAMPLER1).build()
    );
    public static final RenderPipeline ENTITY_SOLID_Z_OFFSET_FORWARD = register(
        RenderPipeline.builder(ENTITY_SNIPPET).withLocation("pipeline/entity_solid_offset_forward").withBindGroupLayout(BindGroupLayouts.SAMPLER1).build()
    );
    public static final RenderPipeline ENTITY_CUTOUT_CULL = register(
        RenderPipeline.builder(ENTITY_SNIPPET)
            .withLocation("pipeline/entity_cutout_cull")
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .build()
    );
    public static final RenderPipeline ENTITY_CUTOUT = register(
        RenderPipeline.builder(ENTITY_SNIPPET)
            .withLocation("pipeline/entity_cutout")
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("PER_FACE_LIGHTING")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withCull(false)
            .build()
    );
    public static final RenderPipeline ENTITY_CUTOUT_Z_OFFSET = register(
        RenderPipeline.builder(ENTITY_SNIPPET)
            .withLocation("pipeline/entity_cutout_z_offset")
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("PER_FACE_LIGHTING")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withCull(false)
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
    public static final RenderPipeline ENTITY_TRANSLUCENT_CULL = register(
        RenderPipeline.builder(ENTITY_SNIPPET)
            .withLocation("pipeline/entity_translucent_cull")
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build()
    );
    public static final RenderPipeline END_CRYSTAL_BEAM = register(
        RenderPipeline.builder(ENTITY_SNIPPET)
            .withLocation("pipeline/end_crystal_beam")
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("NO_OVERLAY")
            .withCull(false)
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
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withCull(false)
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(DepthStencilState.DEFAULT)
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
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build()
    );
    public static final RenderPipeline ENTITY_SHADOW = register(
        RenderPipeline.builder(MATRICES_FOG_SNIPPET)
            .withLocation("pipeline/entity_shadow")
            .withVertexShader("core/rendertype_entity_shadow")
            .withFragmentShader("core/rendertype_entity_shadow")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build()
    );
    public static final RenderPipeline ITEM_CUTOUT = register(
        RenderPipeline.builder(ITEM_SNIPPET).withLocation("pipeline/item_cutout").withShaderDefine("ALPHA_CUTOUT", 0.1F).build()
    );
    public static final RenderPipeline ITEM_TRANSLUCENT = register(
        RenderPipeline.builder(ITEM_SNIPPET)
            .withLocation("pipeline/item_translucent")
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build()
    );
    public static final RenderPipeline BEACON_BEAM_OPAQUE = register(
        RenderPipeline.builder(BEACON_BEAM_SNIPPET).withLocation("pipeline/beacon_beam_opaque").build()
    );
    public static final RenderPipeline BEACON_BEAM_TRANSLUCENT = register(
        RenderPipeline.builder(BEACON_BEAM_SNIPPET)
            .withLocation("pipeline/beacon_beam_translucent")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build()
    );
    public static final RenderPipeline LEASH = register(
        RenderPipeline.builder(MATRICES_FOG_SNIPPET)
            .withLocation("pipeline/leash")
            .withVertexShader("core/rendertype_leash")
            .withFragmentShader("core/rendertype_leash")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
            .withCull(false)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_LIGHTMAP)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .build()
    );
    public static final RenderPipeline WATER_MASK = register(
        RenderPipeline.builder(GLOBALS_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withLocation("pipeline/water_mask")
            .withVertexShader("core/rendertype_water_mask")
            .withFragmentShader("core/rendertype_water_mask")
            .withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT), GpuFormat.RGBA8_UNORM, 0))
            .withVertexBinding(0, DefaultVertexFormat.POSITION)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .build()
    );
    public static final RenderPipeline GLINT = register(
        RenderPipeline.builder(GLOBALS_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
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
        RenderPipeline.builder(GLOBALS_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            .withLocation("pipeline/crumbling")
            .withVertexShader("core/rendertype_crumbling")
            .withFragmentShader("core/rendertype_crumbling")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.DST_COLOR, BlendFactor.SRC_COLOR, BlendFactor.ONE, BlendFactor.ZERO)))
            .withVertexBinding(0, DefaultVertexFormat.BLOCK)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false, 1.0F, 10.0F))
            .build()
    );
    public static final RenderPipeline TEXT = register(
        RenderPipeline.builder(WORLD_TEXT_SNIPPET).withLocation("pipeline/text").withVertexShader("core/text").withFragmentShader("core/text").build()
    );
    public static final RenderPipeline GUI_TEXT = register(
        RenderPipeline.builder(GUI_TEXT_SNIPPET).withLocation("pipeline/gui_text").withVertexShader("core/text").withFragmentShader("core/text").build()
    );
    public static final RenderPipeline TEXT_BACKGROUND = register(
        RenderPipeline.builder(GLOBALS_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            .withLocation("pipeline/text_background")
            .withVertexShader("core/text_background")
            .withFragmentShader("core/text_background")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_LIGHTMAP)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .build()
    );
    public static final RenderPipeline TEXT_GRAYSCALE = register(
        RenderPipeline.builder(WORLD_TEXT_SNIPPET)
            .withLocation("pipeline/text_grayscale")
            .withVertexShader("core/text")
            .withFragmentShader("core/text")
            .withShaderDefine("IS_GRAYSCALE")
            .build()
    );
    public static final RenderPipeline GUI_TEXT_GRAYSCALE = register(
        RenderPipeline.builder(GUI_TEXT_SNIPPET)
            .withLocation("pipeline/gui_text_grayscale")
            .withVertexShader("core/text")
            .withFragmentShader("core/text")
            .withShaderDefine("IS_GRAYSCALE")
            .build()
    );
    public static final RenderPipeline TEXT_POLYGON_OFFSET = register(
        RenderPipeline.builder(WORLD_TEXT_SNIPPET)
            .withLocation("pipeline/text_polygon_offset")
            .withVertexShader("core/text")
            .withFragmentShader("core/text")
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 1.0F, 10.0F))
            .build()
    );
    public static final RenderPipeline TEXT_GRAYSCALE_POLYGON_OFFSET = register(
        RenderPipeline.builder(WORLD_TEXT_SNIPPET)
            .withLocation("pipeline/text_grayscale_polygon_offset")
            .withVertexShader("core/text")
            .withFragmentShader("core/text")
            .withShaderDefine("IS_GRAYSCALE")
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 1.0F, 10.0F))
            .build()
    );
    public static final RenderPipeline TEXT_SEE_THROUGH = register(
        RenderPipeline.builder(TEXT_SNIPPET)
            .withLocation("pipeline/text_see_through")
            .withVertexShader("core/text")
            .withFragmentShader("core/text")
            .withShaderDefine("IS_SEE_THROUGH")
            .withDepthStencilState(Optional.empty())
            .build()
    );
    public static final RenderPipeline TEXT_BACKGROUND_SEE_THROUGH = register(
        RenderPipeline.builder(GLOBALS_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withLocation("pipeline/text_background_see_through")
            .withVertexShader("core/text_background")
            .withFragmentShader("core/text_background")
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withShaderDefine("IS_SEE_THROUGH")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(Optional.empty())
            .build()
    );
    public static final RenderPipeline TEXT_GRAYSCALE_SEE_THROUGH = register(
        RenderPipeline.builder(TEXT_SNIPPET)
            .withLocation("pipeline/text_grayscale_see_through")
            .withVertexShader("core/text")
            .withFragmentShader("core/text")
            .withShaderDefine("IS_GRAYSCALE")
            .withShaderDefine("IS_SEE_THROUGH")
            .withDepthStencilState(Optional.empty())
            .build()
    );
    public static final RenderPipeline LIGHTNING = register(
        RenderPipeline.builder(MATRICES_FOG_SNIPPET)
            .withLocation("pipeline/lightning")
            .withVertexShader("core/rendertype_lightning")
            .withFragmentShader("core/rendertype_lightning")
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .build()
    );
    public static final RenderPipeline DRAGON_RAYS = register(
        RenderPipeline.builder(MATRICES_FOG_SNIPPET)
            .withLocation("pipeline/dragon_rays")
            .withVertexShader("core/rendertype_lightning")
            .withFragmentShader("core/rendertype_lightning")
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withDepthStencilState(DepthStencilState.DEFAULT)
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
        RenderPipeline.builder(LINES_SNIPPET)
            .withLocation("pipeline/lines_translucent")
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build()
    );
    public static final RenderPipeline LINES_DEPTH_BIAS = register(
        RenderPipeline.builder(LINES_SNIPPET)
            .withLocation("pipeline/lines_depth_bias")
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
    public static final RenderPipeline DEBUG_POINTS = register(
        RenderPipeline.builder(GLOBALS_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withLocation("pipeline/debug_points")
            .withVertexShader("core/debug_point")
            .withFragmentShader("core/position_color")
            .withCull(false)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_LINE_WIDTH)
            .withPrimitiveTopology(PrimitiveTopology.POINTS)
            .withDepthStencilState(DepthStencilState.DEFAULT)
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
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_FAN)
            .build()
    );
    public static final RenderPipeline WORLD_BORDER = register(
        RenderPipeline.builder(GLOBALS_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withLocation("pipeline/world_border")
            .withVertexShader("core/rendertype_world_border")
            .withFragmentShader("core/rendertype_world_border")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.OVERLAY))
            .withCull(false)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 3.0F, 3.0F))
            .build()
    );
    public static final RenderPipeline OPAQUE_PARTICLE = register(RenderPipeline.builder(PARTICLE_SNIPPET).withLocation("pipeline/opaque_particle").build());
    public static final RenderPipeline TRANSLUCENT_PARTICLE = register(
        RenderPipeline.builder(PARTICLE_SNIPPET)
            .withLocation("pipeline/translucent_particle")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build()
    );
    public static final RenderPipeline WEATHER_DEPTH_WRITE = register(
        RenderPipeline.builder(WEATHER_SNIPPET).withLocation("pipeline/weather_depth_write").build()
    );
    public static final RenderPipeline WEATHER_NO_DEPTH_WRITE = register(
        RenderPipeline.builder(WEATHER_SNIPPET)
            .withLocation("pipeline/weather_no_depth_write")
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build()
    );
    public static final RenderPipeline SKY = register(
        RenderPipeline.builder(MATRICES_FOG_SNIPPET)
            .withLocation("pipeline/sky")
            .withVertexShader("core/sky")
            .withFragmentShader("core/sky")
            .withVertexBinding(0, DefaultVertexFormat.POSITION)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_FAN)
            .build()
    );
    public static final RenderPipeline END_SKY = register(
        RenderPipeline.builder(GLOBALS_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
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
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
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
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
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
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
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
        RenderPipeline.builder(GUI_TEXTURED_SNIPPET)
            .withLocation("pipeline/crosshair")
            .withColorTargetState(new ColorTargetState(BlendFunction.INVERT))
            .build()
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
            .build()
    );
    public static final RenderPipeline PANORAMA = register(
        RenderPipeline.builder(GLOBALS_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withLocation("pipeline/panorama")
            .withVertexShader("core/panorama")
            .withFragmentShader("core/panorama")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withVertexBinding(0, DefaultVertexFormat.POSITION)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
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
            .build()
    );
    public static final RenderPipeline.Snippet ANIMATE_SPRITE_SNIPPET = RenderPipeline.builder(GLOBALS_SNIPPET)
        .withVertexShader("core/animate_sprite")
        .withBindGroupLayout(BindGroupLayouts.SPRITE_ANIMATION_INFO)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
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

    private static RenderPipeline register(final RenderPipeline pipeline) {
        PIPELINES_BY_LOCATION.put(pipeline.getLocation(), pipeline);
        return pipeline;
    }

    public static List<RenderPipeline> getStaticPipelines() {
        return PIPELINES_BY_LOCATION.values().stream().toList();
    }
}