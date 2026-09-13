package com.mojang.renderpearl.api.pipeline;

public record BlendFunction(BlendEquation color, BlendEquation alpha) {
   public static final BlendFunction LIGHTNING = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE);
   public static final BlendFunction GLINT = new BlendFunction(BlendFactor.SRC_COLOR, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE);
   public static final BlendFunction OVERLAY = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO);
   public static final BlendFunction TRANSLUCENT = new BlendFunction(
      BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA
   );
   public static final BlendFunction TRANSLUCENT_PREMULTIPLIED_ALPHA = new BlendFunction(
      BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA
   );
   public static final BlendFunction ADDITIVE = new BlendFunction(BlendFactor.ONE, BlendFactor.ONE);
   public static final BlendFunction ENTITY_OUTLINE_BLIT = new BlendFunction(
      BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ZERO, BlendFactor.ONE
   );
   public static final BlendFunction INVERT = new BlendFunction(
      BlendFactor.ONE_MINUS_DST_COLOR, BlendFactor.ONE_MINUS_SRC_COLOR, BlendFactor.ONE, BlendFactor.ZERO
   );
   public static final BlendFunction MAX = new BlendFunction(BlendFactor.ONE, BlendFactor.ONE, BlendOp.MAX);

   public BlendFunction(
      final BlendFactor srcColorFactor,
      final BlendFactor dstColorFactor,
      final BlendOp colorOp,
      final BlendFactor srcAlphaFactor,
      final BlendFactor dstAlphaFactor,
      final BlendOp alphaOp
   ) {
      this(new BlendEquation(srcColorFactor, dstColorFactor, colorOp), new BlendEquation(srcAlphaFactor, dstAlphaFactor, alphaOp));
   }

   public BlendFunction(final BlendFactor srcColorFactor, final BlendFactor dstColorFactor, final BlendFactor srcAlphaFactor, final BlendFactor dstAlphaFactor) {
      this(srcColorFactor, dstColorFactor, BlendOp.ADD, srcAlphaFactor, dstAlphaFactor, BlendOp.ADD);
   }

   public BlendFunction(final BlendEquation equation) {
      this(equation, equation);
   }

   public BlendFunction(final BlendFactor srcFactor, final BlendFactor dstFactor, final BlendOp op) {
      this(new BlendEquation(srcFactor, dstFactor, op));
   }

   public BlendFunction(final BlendFactor srcFactor, final BlendFactor dstFactor) {
      this(new BlendEquation(srcFactor, dstFactor, BlendOp.ADD));
   }
}
