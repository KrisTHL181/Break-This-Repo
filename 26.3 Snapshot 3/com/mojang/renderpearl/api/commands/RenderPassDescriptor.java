package com.mojang.renderpearl.api.commands;

import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

public class RenderPassDescriptor {
   private final Supplier<String> label;
   private final List<RenderPassDescriptor.@Nullable Attachment<Optional<Vector4fc>>> colorAttachments;
   private final RenderPassDescriptor.@Nullable Attachment<OptionalDouble> depthAttachment;
   private final RenderPass.RenderArea renderArea;

   private RenderPassDescriptor(
      final Supplier<String> label,
      final List<RenderPassDescriptor.@Nullable Attachment<Optional<Vector4fc>>> colorAttachments,
      final RenderPassDescriptor.@Nullable Attachment<OptionalDouble> depthAttachment,
      final RenderPass.RenderArea renderArea
   ) {
      this.label = label;
      this.colorAttachments = Collections.unmodifiableList(new ArrayList<>(colorAttachments));
      this.depthAttachment = depthAttachment;
      this.renderArea = renderArea;
   }

   public static RenderPassDescriptor.Builder builder(final Supplier<String> label) {
      return new RenderPassDescriptor.Builder(label);
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

   public RenderPass.RenderArea renderArea() {
      return this.renderArea;
   }

   public record Attachment<T>(GpuTextureView textureView, T clearValue) {
   }

   public static class Builder {
      private final Supplier<String> label;
      private final List<RenderPassDescriptor.@Nullable Attachment<Optional<Vector4fc>>> colorAttachments = new ArrayList<>();
      private RenderPassDescriptor.@Nullable Attachment<OptionalDouble> depthAttachment;
      private RenderPass.@Nullable RenderArea renderArea;

      private Builder(final Supplier<String> label) {
         this.label = label;
      }

      public RenderPassDescriptor.Builder withColorAttachment(final GpuTextureView textureView) {
         this.colorAttachments.add(new RenderPassDescriptor.Attachment<>(textureView, Optional.empty()));
         return this;
      }

      public RenderPassDescriptor.Builder withColorAttachment(final GpuTextureView textureView, final Optional<Vector4fc> clearValue) {
         this.colorAttachments.add(new RenderPassDescriptor.Attachment<>(textureView, clearValue));
         return this;
      }

      public RenderPassDescriptor.Builder withUnusedColorAttachment() {
         this.colorAttachments.add(null);
         return this;
      }

      public RenderPassDescriptor.Builder withDepthAttachment(final GpuTextureView textureView) {
         this.depthAttachment = new RenderPassDescriptor.Attachment<>(textureView, OptionalDouble.empty());
         return this;
      }

      public RenderPassDescriptor.Builder withDepthAttachment(final GpuTextureView textureView, final OptionalDouble clearValue) {
         this.depthAttachment = new RenderPassDescriptor.Attachment<>(textureView, clearValue);
         return this;
      }

      public RenderPassDescriptor.Builder withRenderArea(final RenderPass.RenderArea renderArea) {
         this.renderArea = renderArea;
         return this;
      }

      public RenderPassDescriptor build() {
         RenderPass.RenderArea renderArea = this.renderArea != null ? this.renderArea : defaultRenderArea(this.colorAttachments, this.depthAttachment);
         return new RenderPassDescriptor(this.label, this.colorAttachments, this.depthAttachment, renderArea);
      }

      private static RenderPass.RenderArea defaultRenderArea(
         final List<RenderPassDescriptor.@Nullable Attachment<Optional<Vector4fc>>> colorAttachments,
         final RenderPassDescriptor.@Nullable Attachment<OptionalDouble> depthAttachment
      ) {
         int width = 0;
         int height = 0;
         if (!colorAttachments.isEmpty()) {
            for (RenderPassDescriptor.Attachment<Optional<Vector4fc>> colorAttachment : colorAttachments) {
               if (colorAttachment != null) {
                  GpuTextureView textureView = colorAttachment.textureView();
                  width = textureView.getWidth(0);
                  height = textureView.getHeight(0);
               }
            }
         } else if (depthAttachment != null) {
            width = depthAttachment.textureView().getWidth(0);
            height = depthAttachment.textureView().getHeight(0);
         }

         return new RenderPass.RenderArea(0, 0, width, height);
      }
   }
}
