package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RenderPassDescriptor {
    private final Supplier<String> label;
    public List<RenderPassDescriptor.@Nullable Attachment<Optional<Vector4fc>>> colorAttachments = new ArrayList<>();
    public RenderPassDescriptor.@Nullable Attachment<OptionalDouble> depthAttachment;
    public RenderPass.@Nullable RenderArea renderArea;

    public static RenderPassDescriptor create(final Supplier<String> label) {
        return new RenderPassDescriptor(label);
    }

    private RenderPassDescriptor(final Supplier<String> label) {
        this.label = label;
    }

    public RenderPassDescriptor withColorAttachment(final GpuTextureView textureView) {
        this.colorAttachments.add(new RenderPassDescriptor.Attachment<>(textureView, Optional.empty()));
        return this;
    }

    public RenderPassDescriptor withColorAttachment(final GpuTextureView textureView, final Optional<Vector4fc> clearValue) {
        this.colorAttachments.add(new RenderPassDescriptor.Attachment<>(textureView, clearValue));
        return this;
    }

    public RenderPassDescriptor withUnusedColorAttachment() {
        this.colorAttachments.add(null);
        return this;
    }

    public RenderPassDescriptor withDepthAttachment(final GpuTextureView textureView) {
        this.depthAttachment = new RenderPassDescriptor.Attachment<>(textureView, OptionalDouble.empty());
        return this;
    }

    public RenderPassDescriptor withDepthAttachment(final GpuTextureView textureView, final OptionalDouble clearValue) {
        this.depthAttachment = new RenderPassDescriptor.Attachment<>(textureView, clearValue);
        return this;
    }

    public RenderPassDescriptor withRenderArea(final RenderPass.RenderArea renderArea) {
        this.renderArea = renderArea;
        return this;
    }

    public Supplier<String> label() {
        return this.label;
    }

    public List<RenderPassDescriptor.@Nullable Attachment<Optional<Vector4fc>>> colorAttachments() {
        return this.colorAttachments;
    }

    public RenderPassDescriptor.@Nullable Attachment<OptionalDouble> depthAttachment() {
        return this.depthAttachment;
    }

    @OnlyIn(Dist.CLIENT)
    public record Attachment<T>(GpuTextureView textureView, T clearValue) {
    }
}