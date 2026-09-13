package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class CommandEncoder {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final GpuDeviceBackend device;
    private final CommandEncoderBackend backend;
    private boolean isInRenderPass;
    private final @Nullable TracyGpuProfiler profiler;

    public CommandEncoder(final @Nullable TracyGpuProfiler profiler, final GpuDeviceBackend device, final CommandEncoderBackend backend) {
        this.profiler = profiler;
        this.device = device;
        this.backend = backend;
    }

    protected CommandEncoderBackend backend() {
        return this.backend;
    }

    public void submit() {
        this.backend.submit();
        if (this.profiler != null) {
            this.profiler.endFrame();
        }
    }

    public TransientMemory transientMemory() {
        return this.backend.transientMemory();
    }

    protected boolean isInRenderPass() {
        return this.isInRenderPass;
    }

    public RenderPass createRenderPass(final Supplier<String> label, final GpuTextureView colorTexture, final Optional<Vector4fc> clearColor) {
        return this.createRenderPass(label, colorTexture, clearColor, null, OptionalDouble.empty());
    }

    public RenderPass createRenderPass(
        final Supplier<String> label,
        final GpuTextureView colorTexture,
        final Optional<Vector4fc> clearColor,
        final @Nullable GpuTextureView depthTexture,
        final OptionalDouble clearDepth
    ) {
        return this.createRenderPass(
            label, colorTexture, clearColor, depthTexture, clearDepth, new RenderPass.RenderArea(0, 0, colorTexture.getWidth(0), colorTexture.getHeight(0))
        );
    }

    public RenderPass createRenderPass(
        final Supplier<String> label,
        final GpuTextureView colorTexture,
        final Optional<Vector4fc> clearColor,
        final @Nullable GpuTextureView depthTexture,
        final OptionalDouble clearDepth,
        final RenderPass.RenderArea renderArea
    ) {
        RenderPassDescriptor descriptor = RenderPassDescriptor.create(label).withColorAttachment(colorTexture, clearColor);
        if (depthTexture != null) {
            descriptor.withDepthAttachment(depthTexture, clearDepth);
        }

        descriptor.withRenderArea(renderArea);
        return this.createRenderPass(descriptor);
    }

    public RenderPass createRenderPass(final RenderPassDescriptor descriptor) {
        if (this.isInRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        }

        int maxColorAttachments = RenderSystem.getDevice().getDeviceInfo().limits().maxColorAttachments();
        int colorAttachmentCount = descriptor.colorAttachments.size();
        if (colorAttachmentCount > maxColorAttachments) {
            throw new IllegalStateException(
                "Render pass created with " + colorAttachmentCount + " color attachments but device only supports " + maxColorAttachments
            );
        }

        int totalAttachments = colorAttachmentCount + (descriptor.depthAttachment != null ? 1 : 0);
        if (totalAttachments == 0) {
            throw new IllegalArgumentException("At least one attachment (depth or color) must be specified");
        }

        int attachmentWidth;
        int attachmentHeight;
        if (colorAttachmentCount != 0) {
            RenderPassDescriptor.Attachment<Optional<Vector4fc>> firstAttachment = descriptor.colorAttachments.getFirst();
            assert firstAttachment != null;
            attachmentWidth = firstAttachment.textureView().getWidth(0);
            attachmentHeight = firstAttachment.textureView().getHeight(0);
        } else {
            attachmentWidth = descriptor.depthAttachment.textureView().getWidth(0);
            attachmentHeight = descriptor.depthAttachment.textureView().getHeight(0);
        }

        if (descriptor.renderArea == null) {
            throw new IllegalArgumentException("RenderPassDescriptor.renderArea must be provided");
        }

        if (descriptor.renderArea.x() >= 0
            && descriptor.renderArea.y() >= 0
            && descriptor.renderArea.x() + descriptor.renderArea.width() <= attachmentWidth
            && descriptor.renderArea.y() + descriptor.renderArea.height() <= attachmentHeight) {
            for (int i = 0; i < colorAttachmentCount; i++) {
                RenderPassDescriptor.Attachment<Optional<Vector4fc>> colorAttachment = descriptor.colorAttachments.get(i);
                if (colorAttachment != null) {
                    GpuTextureView colorTexture = colorAttachment.textureView();
                    if (colorTexture.isClosed()) {
                        throw new IllegalStateException("Color texture " + i + " is closed");
                    }

                    if ((colorTexture.texture().usage() & 8) == 0) {
                        throw new IllegalStateException("Color texture " + i + " must have USAGE_RENDER_ATTACHMENT");
                    }

                    if (colorTexture.texture().getDepthOrLayers() > 1) {
                        throw new UnsupportedOperationException(
                            "Color texture " + i + ": Textures with multiple depths or layers are not yet supported as an attachment"
                        );
                    }

                    if (colorTexture.getWidth(0) != attachmentWidth || colorTexture.getHeight(0) != attachmentHeight) {
                        throw new IllegalArgumentException(
                            "Color texture "
                                + i
                                + ": size does not match expected attachment size. Is "
                                + colorTexture.getWidth(0)
                                + "x"
                                + colorTexture.getHeight(0)
                                + " expected "
                                + attachmentWidth
                                + "x"
                                + attachmentHeight
                        );
                    }
                }
            }

            if (descriptor.depthAttachment != null) {
                GpuTextureView depthTexture = descriptor.depthAttachment.textureView();
                if (depthTexture.isClosed()) {
                    throw new IllegalStateException("Depth texture is closed");
                }

                if ((depthTexture.texture().usage() & 8) == 0) {
                    throw new IllegalStateException("Depth texture must have USAGE_RENDER_ATTACHMENT");
                }

                if (depthTexture.texture().getDepthOrLayers() > 1) {
                    throw new UnsupportedOperationException("Depth texture: Textures with multiple depths or layers are not yet supported as an attachment");
                }

                if (depthTexture.getWidth(0) != attachmentWidth || depthTexture.getHeight(0) != attachmentHeight) {
                    throw new IllegalArgumentException(
                        "Depth texture: size does not match expected attachment size. Is "
                            + depthTexture.getWidth(0)
                            + "x"
                            + depthTexture.getHeight(0)
                            + " expected "
                            + attachmentWidth
                            + "x"
                            + attachmentHeight
                    );
                }
            }

            this.isInRenderPass = true;
            if (this.profiler != null) {
                this.profiler.pushZone(this, descriptor.label().get());
            }

            return new RenderPass(
                this.backend.createRenderPass(descriptor), this.device, descriptor.colorAttachments, this::submitRenderPass, descriptor.renderArea
            );
        } else {
            throw new IllegalArgumentException(
                "RenderPass render area " + descriptor.renderArea + " is out of bounds for texture of " + attachmentWidth + "x" + attachmentHeight
            );
        }
    }

    protected void submitRenderPass() {
        if (!this.isInRenderPass) {
            throw new IllegalStateException("Can't submit a render pass if one isn't open");
        }

        this.isInRenderPass = false;
        this.backend.submitRenderPass();
        if (this.profiler != null) {
            this.profiler.popZone(this);
        }
    }

    public void clearColorTexture(final GpuTexture colorTexture, final Vector4fc clearColor) {
        if (this.isInRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        }

        this.verifyColorTexture(colorTexture);
        this.backend.clearColorTexture(colorTexture, clearColor);
    }

    public void clearColorAndDepthTextures(final GpuTexture colorTexture, final Vector4fc clearColor, final GpuTexture depthTexture, final double clearDepth) {
        if (this.isInRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        }

        this.verifyColorTexture(colorTexture);
        this.verifyDepthTexture(depthTexture);
        this.backend.clearColorAndDepthTextures(colorTexture, clearColor, depthTexture, clearDepth);
    }

    public void clearColorAndDepthTextures(
        final GpuTexture colorTexture,
        final Vector4fc clearColor,
        final GpuTexture depthTexture,
        final double clearDepth,
        final int regionX,
        final int regionY,
        final int regionWidth,
        final int regionHeight
    ) {
        if (this.isInRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        }

        this.verifyColorTexture(colorTexture);
        this.verifyDepthTexture(depthTexture);
        this.verifyRegion(colorTexture, regionX, regionY, regionWidth, regionHeight);
        this.backend.clearColorAndDepthTextures(colorTexture, clearColor, depthTexture, clearDepth, regionX, regionY, regionWidth, regionHeight);
    }

    public void clearDepthTexture(final GpuTexture depthTexture, final double clearDepth) {
        if (this.isInRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        }

        this.verifyDepthTexture(depthTexture);
        this.backend.clearDepthTexture(depthTexture, clearDepth);
    }

    public void writeToBuffer(final GpuBufferSlice destination, final ByteBuffer data) {
        if (this.isInRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        }

        GpuBuffer buffer = destination.buffer();
        if (buffer.isClosed()) {
            throw new IllegalStateException("Buffer already closed");
        }

        if ((buffer.usage() & 8) == 0) {
            throw new IllegalStateException("Buffer needs USAGE_COPY_DST to be a destination for a copy");
        }

        int length = data.remaining();
        if (length > destination.length()) {
            throw new IllegalArgumentException(
                "Cannot write more data than the slice allows (attempting to write " + length + " bytes into a slice of length " + destination.length() + ")"
            );
        }

        if (destination.length() + destination.offset() > buffer.size()) {
            throw new IllegalArgumentException(
                "Cannot write more data than this buffer can hold (attempting to write "
                    + length
                    + " bytes at offset "
                    + destination.offset()
                    + " to "
                    + buffer.size()
                    + " size buffer)"
            );
        }

        this.backend.writeToBuffer(destination, data);
    }

    public void copyToBuffer(final GpuBufferSlice source, final GpuBufferSlice target) {
        if (this.isInRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        }

        GpuBuffer sourceBuffer = source.buffer();
        if (sourceBuffer.isClosed()) {
            throw new IllegalStateException("Source buffer already closed");
        }

        if ((sourceBuffer.usage() & 16) == 0) {
            throw new IllegalStateException("Source buffer needs USAGE_COPY_SRC to be a source for a copy");
        }

        GpuBuffer targetBuffer = target.buffer();
        if (targetBuffer.isClosed()) {
            throw new IllegalStateException("Target buffer already closed");
        }

        if ((targetBuffer.usage() & 8) == 0) {
            throw new IllegalStateException("Target buffer needs USAGE_COPY_DST to be a destination for a copy");
        }

        if (source.length() != target.length()) {
            throw new IllegalArgumentException(
                "Cannot copy from slice of size " + source.length() + " to slice of size " + target.length() + ", they must be equal"
            );
        }

        if (source.offset() + source.length() > sourceBuffer.size()) {
            throw new IllegalArgumentException(
                "Cannot copy more data than the source buffer holds (attempting to copy "
                    + source.length()
                    + " bytes at offset "
                    + source.offset()
                    + " from "
                    + sourceBuffer.size()
                    + " size buffer)"
            );
        }

        if (target.offset() + target.length() > targetBuffer.size()) {
            throw new IllegalArgumentException(
                "Cannot copy more data than the target buffer can hold (attempting to copy "
                    + target.length()
                    + " bytes at offset "
                    + target.offset()
                    + " to "
                    + targetBuffer.size()
                    + " size buffer)"
            );
        }

        this.backend.copyToBuffer(source, target);
    }

    public void writeToTexture(final GpuTexture destination, final NativeImage source) {
        int width = destination.getWidth(0);
        int height = destination.getHeight(0);
        if (source.getWidth() == width && source.getHeight() == height) {
            this.writeToTexture(destination, source, 0, 0, 0, 0);
        } else {
            throw new IllegalArgumentException(
                "Cannot replace texture of size " + width + "x" + height + " with image of size " + source.getWidth() + "x" + source.getHeight()
            );
        }
    }

    public void writeToTexture(
        final GpuTexture destination, final NativeImage source, final int mipLevel, final int depthOrLayer, final int destX, final int destY
    ) {
        if (this.isInRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        }

        if (destination.getFormat().componentType() != GpuFormat.ComponentType.UNORM_8) {
            throw new IllegalArgumentException("Destination texture for NativeImage writes must have component type of UNORM_8");
        }

        if (destination.getFormat().componentCount() != source.format().components()) {
            throw new IllegalArgumentException(
                "Destination(" + destination.getFormat() + ") texture for NativeImage(" + source.format() + ") write must have channel count matching source"
            );
        }

        if (mipLevel >= 0 && mipLevel < destination.getMipLevels()) {
            if (destX + source.getWidth() > destination.getWidth(mipLevel) || destY + source.getHeight() > destination.getHeight(mipLevel)) {
                throw new IllegalArgumentException(
                    "Dest texture ("
                        + source.getWidth()
                        + "x"
                        + source.getHeight()
                        + ") is not large enough to write a rectangle of "
                        + source.getWidth()
                        + "x"
                        + source.getHeight()
                        + " at "
                        + destX
                        + "x"
                        + destY
                        + " (at mip level "
                        + mipLevel
                        + ")"
                );
            }

            if (destination.isClosed()) {
                throw new IllegalStateException("Destination texture is closed");
            }

            if ((destination.usage() & 1) == 0) {
                throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
            }

            if (depthOrLayer >= destination.getDepthOrLayers()) {
                throw new UnsupportedOperationException("Depth or layer is out of range, must be >= 0 and < " + destination.getDepthOrLayers());
            }

            this.writeToTexture(destination, source.getPixelBytes(), mipLevel, depthOrLayer, destX, destY, source.getWidth(), source.getHeight());
        } else {
            throw new IllegalArgumentException("Invalid mipLevel " + mipLevel + ", must be >= 0 and < " + destination.getMipLevels());
        }
    }

    public void writeToTexture(
        final GpuTexture destination,
        final ByteBuffer source,
        final int mipLevel,
        final int depthOrLayer,
        final int destX,
        final int destY,
        final int width,
        final int height
    ) {
        if (this.isInRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        }

        if (mipLevel >= 0 && mipLevel < destination.getMipLevels()) {
            if (width * height * destination.getFormat().blockSize() > source.remaining()) {
                throw new IllegalArgumentException(
                    "Copy would overrun the source buffer (remaining length of "
                        + source.remaining()
                        + ", but copy is "
                        + width
                        + "x"
                        + height
                        + " of format "
                        + destination.getFormat()
                        + ")"
                );
            }

            if (destX + width > destination.getWidth(mipLevel) || destY + height > destination.getHeight(mipLevel)) {
                throw new IllegalArgumentException(
                    "Dest texture ("
                        + destination.getWidth(mipLevel)
                        + "x"
                        + destination.getHeight(mipLevel)
                        + ") is not large enough to write a rectangle of "
                        + width
                        + "x"
                        + height
                        + " at "
                        + destX
                        + "x"
                        + destY
                );
            }

            if (destination.isClosed()) {
                throw new IllegalStateException("Destination texture is closed");
            }

            if ((destination.usage() & 1) == 0) {
                throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
            }

            if (depthOrLayer >= destination.getDepthOrLayers()) {
                throw new UnsupportedOperationException("Depth or layer is out of range, must be >= 0 and < " + destination.getDepthOrLayers());
            }

            this.backend.writeToTexture(destination, source, mipLevel, depthOrLayer, destX, destY, width, height);
        } else {
            throw new IllegalArgumentException("Invalid mipLevel, must be >= 0 and < " + destination.getMipLevels());
        }
    }

    public void copyBufferToTexture(
        final GpuBufferSlice source,
        final int sourceX,
        final int sourceY,
        final int sourceWidth,
        final int sourceHeight,
        final GpuTexture destination,
        final int destinationX,
        final int destinationY,
        final int copyWidth,
        final int copyHeight,
        final int mipLevel,
        final int arrayLayer
    ) {
        if (this.isInRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        }

        if (mipLevel >= 0 && mipLevel < destination.getMipLevels()) {
            if (sourceX + copyWidth > sourceWidth || sourceY + copyHeight > sourceHeight) {
                throw new IllegalArgumentException(
                    "Copy source ("
                        + sourceWidth
                        + "x"
                        + sourceHeight
                        + ") is not large enough to read a rectangle of "
                        + copyWidth
                        + "x"
                        + copyHeight
                        + " from "
                        + sourceX
                        + "x"
                        + sourceY
                );
            }

            if ((long)sourceWidth * copyHeight * destination.getFormat().blockSize() > source.length()) {
                throw new IllegalArgumentException(
                    "Copy would overrun the source buffer (remaining length of "
                        + source.length()
                        + ", but copy is "
                        + copyWidth
                        + "x"
                        + copyHeight
                        + " of format "
                        + destination.getFormat()
                        + ")"
                );
            }

            if (destinationX + copyWidth > destination.getWidth(mipLevel) || destinationY + copyHeight > destination.getHeight(mipLevel)) {
                throw new IllegalArgumentException(
                    "Dest texture ("
                        + destination.getWidth(mipLevel)
                        + "x"
                        + destination.getHeight(mipLevel)
                        + ") is not large enough to write a rectangle of "
                        + copyWidth
                        + "x"
                        + copyHeight
                        + " at "
                        + destinationX
                        + "x"
                        + destinationY
                );
            }

            if (source.buffer().isClosed()) {
                throw new IllegalStateException("Source buffer is closed");
            }

            if ((source.buffer().usage() & 16) == 0) {
                throw new IllegalStateException("Source buffer must have USAGE_COPY_SRC to be a source for a read");
            }

            if (destination.isClosed()) {
                throw new IllegalStateException("Destination texture is closed");
            }

            if ((destination.usage() & 1) == 0) {
                throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
            }

            if (arrayLayer >= destination.getDepthOrLayers()) {
                throw new UnsupportedOperationException("Depth or layer is out of range, must be >= 0 and < " + destination.getDepthOrLayers());
            }

            this.backend
                .copyBufferToTexture(
                    source, sourceX, sourceY, sourceWidth, sourceHeight, destination, destinationX, destinationY, copyWidth, copyHeight, mipLevel, arrayLayer
                );
        } else {
            throw new IllegalArgumentException("Invalid mipLevel, must be >= 0 and < " + destination.getMipLevels());
        }
    }

    public void copyTextureToBuffer(final GpuTexture source, final GpuBuffer destination, final long offset, final Runnable callback, final int mipLevel) {
        if (this.isInRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        }

        this.backend.copyTextureToBuffer(source, destination, offset, callback, mipLevel);
    }

    public void copyTextureToBuffer(
        final GpuTexture source,
        final GpuBuffer destination,
        final long offset,
        final Runnable callback,
        final int mipLevel,
        final int x,
        final int y,
        final int width,
        final int height
    ) {
        if (this.isInRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        }

        if (mipLevel >= 0 && mipLevel < source.getMipLevels()) {
            if ((long)width * height * source.getFormat().blockSize() + offset > destination.size()) {
                throw new IllegalArgumentException(
                    "Buffer of size "
                        + destination.size()
                        + " is not large enough to hold "
                        + width
                        + "x"
                        + height
                        + " pixels ("
                        + source.getFormat().blockSize()
                        + " bytes each) starting from offset "
                        + offset
                );
            }

            if ((source.usage() & 2) == 0) {
                throw new IllegalArgumentException("Texture needs USAGE_COPY_SRC to be a source for a copy");
            }

            if ((destination.usage() & 8) == 0) {
                throw new IllegalArgumentException("Buffer needs USAGE_COPY_DST to be a destination for a copy");
            }

            if (x + width > source.getWidth(mipLevel) || y + height > source.getHeight(mipLevel)) {
                throw new IllegalArgumentException(
                    "Copy source texture ("
                        + source.getWidth(mipLevel)
                        + "x"
                        + source.getHeight(mipLevel)
                        + ") is not large enough to read a rectangle of "
                        + width
                        + "x"
                        + height
                        + " from "
                        + x
                        + ","
                        + y
                );
            }

            if (source.isClosed()) {
                throw new IllegalStateException("Source texture is closed");
            }

            if (destination.isClosed()) {
                throw new IllegalStateException("Destination buffer is closed");
            }

            if (source.getDepthOrLayers() > 1) {
                throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for copying");
            }

            this.backend.copyTextureToBuffer(source, destination, offset, callback, mipLevel, x, y, width, height);
        } else {
            throw new IllegalArgumentException("Invalid mipLevel " + mipLevel + ", must be >= 0 and < " + source.getMipLevels());
        }
    }

    public void copyTextureToTexture(
        final GpuTexture source,
        final GpuTexture destination,
        final int mipLevel,
        final int destX,
        final int destY,
        final int sourceX,
        final int sourceY,
        final int width,
        final int height
    ) {
        if (this.isInRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        }

        if (mipLevel >= 0 && mipLevel < source.getMipLevels() && mipLevel < destination.getMipLevels()) {
            if (destX + width > destination.getWidth(mipLevel) || destY + height > destination.getHeight(mipLevel)) {
                throw new IllegalArgumentException(
                    "Dest texture ("
                        + destination.getWidth(mipLevel)
                        + "x"
                        + destination.getHeight(mipLevel)
                        + ") is not large enough to write a rectangle of "
                        + width
                        + "x"
                        + height
                        + " at "
                        + destX
                        + "x"
                        + destY
                );
            }

            if (sourceX + width > source.getWidth(mipLevel) || sourceY + height > source.getHeight(mipLevel)) {
                throw new IllegalArgumentException(
                    "Source texture ("
                        + source.getWidth(mipLevel)
                        + "x"
                        + source.getHeight(mipLevel)
                        + ") is not large enough to read a rectangle of "
                        + width
                        + "x"
                        + height
                        + " at "
                        + sourceX
                        + "x"
                        + sourceY
                );
            }

            if (source.isClosed()) {
                throw new IllegalStateException("Source texture is closed");
            }

            if (destination.isClosed()) {
                throw new IllegalStateException("Destination texture is closed");
            }

            if ((source.usage() & 2) == 0) {
                throw new IllegalArgumentException("Texture needs USAGE_COPY_SRC to be a source for a copy");
            }

            if ((destination.usage() & 1) == 0) {
                throw new IllegalArgumentException("Texture needs USAGE_COPY_DST to be a destination for a copy");
            }

            if (source.getDepthOrLayers() > 1) {
                throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for copying");
            }

            if (destination.getDepthOrLayers() > 1) {
                throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for copying");
            }

            this.backend.copyTextureToTexture(source, destination, mipLevel, destX, destY, sourceX, sourceY, width, height);
        } else {
            throw new IllegalArgumentException(
                "Invalid mipLevel " + mipLevel + ", must be >= 0 and < " + source.getMipLevels() + " and < " + destination.getMipLevels()
            );
        }
    }

    public GpuFence createFence() {
        if (this.isInRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            return this.backend.createFence();
        }
    }

    public void writeTimestamp(final GpuQueryPool pool, final int index) {
        if (index >= 0 && index <= pool.size()) {
            this.backend.writeTimestamp(pool, index);
        } else {
            throw new IllegalStateException("Index " + index + " is out of range for query pool of size " + pool.size());
        }
    }

    private void verifyColorTexture(final GpuTexture colorTexture) {
        if (!colorTexture.getFormat().hasColorAspect()) {
            throw new IllegalStateException("Trying to clear a non-color texture as color");
        }

        if (colorTexture.isClosed()) {
            throw new IllegalStateException("Color texture is closed");
        }

        if ((colorTexture.usage() & 8) == 0) {
            throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT");
        }

        if ((colorTexture.usage() & 1) == 0) {
            throw new IllegalStateException("Color texture must have USAGE_COPY_DST");
        }

        if (colorTexture.getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Clearing a texture with multiple layers or depths is not yet supported");
        }
    }

    private void verifyDepthTexture(final GpuTexture depthTexture) {
        if (!depthTexture.getFormat().hasDepthAspect()) {
            throw new IllegalStateException("Trying to clear a non-depth texture as depth");
        }

        if (depthTexture.isClosed()) {
            throw new IllegalStateException("Depth texture is closed");
        }

        if ((depthTexture.usage() & 8) == 0) {
            throw new IllegalStateException("Depth texture must have USAGE_RENDER_ATTACHMENT");
        }

        if ((depthTexture.usage() & 1) == 0) {
            throw new IllegalStateException("Depth texture must have USAGE_COPY_DST");
        }

        if (depthTexture.getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Clearing a texture with multiple layers or depths is not yet supported");
        }
    }

    private void verifyRegion(final GpuTexture colorTexture, final int regionX, final int regionY, final int regionWidth, final int regionHeight) {
        if (regionX < 0 || regionX >= colorTexture.getWidth(0)) {
            throw new IllegalArgumentException("regionX should not be outside of the texture");
        }

        if (regionY < 0 || regionY >= colorTexture.getHeight(0)) {
            throw new IllegalArgumentException("regionY should not be outside of the texture");
        }

        if (regionWidth <= 0) {
            throw new IllegalArgumentException("regionWidth should be greater than 0");
        }

        if (regionX + regionWidth > colorTexture.getWidth(0)) {
            throw new IllegalArgumentException("regionWidth + regionX should be less than the texture width");
        }

        if (regionHeight <= 0) {
            throw new IllegalArgumentException("regionHeight should be greater than 0");
        }

        if (regionY + regionHeight > colorTexture.getHeight(0)) {
            throw new IllegalArgumentException("regionWidth + regionX should be less than the texture height");
        }
    }
}