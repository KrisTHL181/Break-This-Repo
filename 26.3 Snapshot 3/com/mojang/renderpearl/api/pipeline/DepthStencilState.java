package com.mojang.renderpearl.api.pipeline;

public record DepthStencilState(CompareOp depthTest, boolean writeDepth, float depthBiasScaleFactor, float depthBiasConstant) {
   public static final DepthStencilState DEFAULT = new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true);

   public DepthStencilState(final CompareOp depthTest, final boolean depthWrite) {
      this(depthTest, depthWrite, 0.0F, 0.0F);
   }
}
