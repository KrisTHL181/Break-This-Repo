package com.mojang.blaze3d.vertex;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.vertex.VertexFormat;

public class DefaultVertexFormat {
   public static final String POSITION_SEMANTIC_NAME = "Position";
   public static final String COLOR_SEMANTIC_NAME = "Color";
   public static final String UV0_SEMANTIC_NAME = "UV0";
   public static final String UV1_SEMANTIC_NAME = "UV1";
   public static final String UV2_SEMANTIC_NAME = "UV2";
   public static final String UV3_SEMANTIC_NAME = "UV3";
   public static final String NORMAL_SEMANTIC_NAME = "Normal";
   public static final String LINE_WIDTH_SEMANTIC_NAME = "LineWidth";
   private static final GpuFormat POSITION_FORMAT = GpuFormat.RGB32_FLOAT;
   private static final GpuFormat COLOR_FORMAT = GpuFormat.RGBA8_UNORM;
   private static final GpuFormat UV0_FORMAT = GpuFormat.RG32_FLOAT;
   private static final GpuFormat UV1_FORMAT = GpuFormat.RG16_SINT;
   private static final GpuFormat UV2_FORMAT = GpuFormat.RG16_SINT;
   private static final GpuFormat UV3_FORMAT = GpuFormat.RG32_FLOAT;
   private static final GpuFormat NORMAL_FORMAT = GpuFormat.RGBA8_SNORM;
   private static final GpuFormat LINE_WIDTH_FORMAT = GpuFormat.R32_FLOAT;
   public static final VertexFormat BLOCK = VertexFormat.builder(0)
      .addAttribute("Position", POSITION_FORMAT)
      .addAttribute("Color", COLOR_FORMAT)
      .addAttribute("UV0", UV0_FORMAT)
      .addAttribute("UV2", UV2_FORMAT)
      .build();
   public static final VertexFormat ENTITY = VertexFormat.builder(0)
      .addAttribute("Position", POSITION_FORMAT)
      .addAttribute("Color", COLOR_FORMAT)
      .addAttribute("UV0", UV0_FORMAT)
      .addAttribute("UV1", UV1_FORMAT)
      .addAttribute("UV2", UV2_FORMAT)
      .addAttribute("Normal", NORMAL_FORMAT)
      .build();
   public static final VertexFormat ENTITY_GLINT_SPECIAL = VertexFormat.builder(0)
      .addAttribute("Position", POSITION_FORMAT)
      .addAttribute("Color", COLOR_FORMAT)
      .addAttribute("UV0", UV0_FORMAT)
      .addAttribute("UV1", UV1_FORMAT)
      .addAttribute("UV2", UV2_FORMAT)
      .addAttribute("UV3", UV3_FORMAT)
      .addAttribute("Normal", NORMAL_FORMAT)
      .build();
   public static final VertexFormat PARTICLE = VertexFormat.builder(0)
      .addAttribute("Position", POSITION_FORMAT)
      .addAttribute("UV0", UV0_FORMAT)
      .addAttribute("Color", COLOR_FORMAT)
      .addAttribute("UV2", UV2_FORMAT)
      .build();
   public static final VertexFormat POSITION = VertexFormat.builder(0).addAttribute("Position", POSITION_FORMAT).build();
   public static final VertexFormat POSITION_COLOR = VertexFormat.builder(0)
      .addAttribute("Position", POSITION_FORMAT)
      .addAttribute("Color", COLOR_FORMAT)
      .build();
   public static final VertexFormat POSITION_COLOR_NORMAL = VertexFormat.builder(0)
      .addAttribute("Position", POSITION_FORMAT)
      .addAttribute("Color", COLOR_FORMAT)
      .addAttribute("Normal", NORMAL_FORMAT)
      .build();
   public static final VertexFormat POSITION_COLOR_LIGHTMAP = VertexFormat.builder(0)
      .addAttribute("Position", POSITION_FORMAT)
      .addAttribute("Color", COLOR_FORMAT)
      .addAttribute("UV2", UV2_FORMAT)
      .build();
   public static final VertexFormat POSITION_TEX = VertexFormat.builder(0).addAttribute("Position", POSITION_FORMAT).addAttribute("UV0", UV0_FORMAT).build();
   public static final VertexFormat POSITION_TEX_COLOR = VertexFormat.builder(0)
      .addAttribute("Position", POSITION_FORMAT)
      .addAttribute("UV0", UV0_FORMAT)
      .addAttribute("Color", COLOR_FORMAT)
      .build();
   public static final VertexFormat POSITION_COLOR_TEX_LIGHTMAP = VertexFormat.builder(0)
      .addAttribute("Position", POSITION_FORMAT)
      .addAttribute("Color", COLOR_FORMAT)
      .addAttribute("UV0", UV0_FORMAT)
      .addAttribute("UV2", UV2_FORMAT)
      .build();
   public static final VertexFormat POSITION_TEX_LIGHTMAP_COLOR = VertexFormat.builder(0)
      .addAttribute("Position", POSITION_FORMAT)
      .addAttribute("UV0", UV0_FORMAT)
      .addAttribute("UV2", UV2_FORMAT)
      .addAttribute("Color", COLOR_FORMAT)
      .build();
   public static final VertexFormat POSITION_TEX_COLOR_NORMAL = VertexFormat.builder(0)
      .addAttribute("Position", POSITION_FORMAT)
      .addAttribute("UV0", UV0_FORMAT)
      .addAttribute("Color", COLOR_FORMAT)
      .addAttribute("Normal", NORMAL_FORMAT)
      .build();
   public static final VertexFormat POSITION_COLOR_LINE_WIDTH = VertexFormat.builder(0)
      .addAttribute("Position", POSITION_FORMAT)
      .addAttribute("Color", COLOR_FORMAT)
      .addAttribute("LineWidth", LINE_WIDTH_FORMAT)
      .build();
   public static final VertexFormat POSITION_COLOR_NORMAL_LINE_WIDTH = VertexFormat.builder(0)
      .addAttribute("Position", POSITION_FORMAT)
      .addAttribute("Color", COLOR_FORMAT)
      .addAttribute("Normal", NORMAL_FORMAT)
      .addAttribute("LineWidth", LINE_WIDTH_FORMAT)
      .build();
}
