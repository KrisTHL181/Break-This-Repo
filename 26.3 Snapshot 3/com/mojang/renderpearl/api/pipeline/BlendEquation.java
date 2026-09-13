package com.mojang.renderpearl.api.pipeline;

public record BlendEquation(BlendFactor sourceFactor, BlendFactor destFactor, BlendOp op) {
}
