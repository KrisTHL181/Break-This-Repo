package net.minecraft.client.renderer;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.WorldBorderRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class WorldBorderRenderer implements AutoCloseable {
    public static final Identifier FORCEFIELD_LOCATION = Identifier.withDefaultNamespace("textures/misc/forcefield.png");
    private boolean needsRebuild = true;
    private double lastMinX;
    private double lastMinZ;
    private double lastBorderMinX;
    private double lastBorderMaxX;
    private double lastBorderMinZ;
    private double lastBorderMaxZ;
    private final GpuBuffer worldBorderBuffer = RenderSystem.getDevice()
        .createBuffer(() -> "World border vertex buffer", 40, 16L * DefaultVertexFormat.POSITION_TEX.getVertexSize());
    private final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);

    @Override
    public void close() {
        this.worldBorderBuffer.close();
    }

    private void rebuildWorldBorderBuffer(
        final WorldBorderRenderState state,
        final double renderDistance,
        final double cameraZ,
        final double cameraX,
        final float halfHeightY,
        final float v1,
        final float v0
    ) {
        try (ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION_TEX.getVertexSize() * 4 * 4)) {
            double borderMinX = state.minX;
            double borderMaxX = state.maxX;
            double borderMinZ = state.minZ;
            double borderMaxZ = state.maxZ;
            double minZ = Math.max(Mth.floor(cameraZ - renderDistance), borderMinZ);
            double maxZ = Math.min(Mth.ceil(cameraZ + renderDistance), borderMaxZ);
            float u0z = (Mth.floor(minZ) & 1) * 0.5F;
            float u1z = (float)(maxZ - minZ) / 2.0F;
            double minX = Math.max(Mth.floor(cameraX - renderDistance), borderMinX);
            double maxX = Math.min(Mth.ceil(cameraX + renderDistance), borderMaxX);
            float u0x = (Mth.floor(minX) & 1) * 0.5F;
            float u1x = (float)(maxX - minX) / 2.0F;
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX);
            bufferBuilder.addVertex(0.0F, -halfHeightY, (float)(borderMaxZ - minZ)).setUv(u0x, v1);
            bufferBuilder.addVertex((float)(maxX - minX), -halfHeightY, (float)(borderMaxZ - minZ)).setUv(u1x + u0x, v1);
            bufferBuilder.addVertex((float)(maxX - minX), halfHeightY, (float)(borderMaxZ - minZ)).setUv(u1x + u0x, v0);
            bufferBuilder.addVertex(0.0F, halfHeightY, (float)(borderMaxZ - minZ)).setUv(u0x, v0);
            bufferBuilder.addVertex(0.0F, -halfHeightY, 0.0F).setUv(u0z, v1);
            bufferBuilder.addVertex(0.0F, -halfHeightY, (float)(maxZ - minZ)).setUv(u1z + u0z, v1);
            bufferBuilder.addVertex(0.0F, halfHeightY, (float)(maxZ - minZ)).setUv(u1z + u0z, v0);
            bufferBuilder.addVertex(0.0F, halfHeightY, 0.0F).setUv(u0z, v0);
            bufferBuilder.addVertex((float)(maxX - minX), -halfHeightY, 0.0F).setUv(u0x, v1);
            bufferBuilder.addVertex(0.0F, -halfHeightY, 0.0F).setUv(u1x + u0x, v1);
            bufferBuilder.addVertex(0.0F, halfHeightY, 0.0F).setUv(u1x + u0x, v0);
            bufferBuilder.addVertex((float)(maxX - minX), halfHeightY, 0.0F).setUv(u0x, v0);
            bufferBuilder.addVertex((float)(borderMaxX - minX), -halfHeightY, (float)(maxZ - minZ)).setUv(u0z, v1);
            bufferBuilder.addVertex((float)(borderMaxX - minX), -halfHeightY, 0.0F).setUv(u1z + u0z, v1);
            bufferBuilder.addVertex((float)(borderMaxX - minX), halfHeightY, 0.0F).setUv(u1z + u0z, v0);
            bufferBuilder.addVertex((float)(borderMaxX - minX), halfHeightY, (float)(maxZ - minZ)).setUv(u0z, v0);

            try (MeshData meshData = bufferBuilder.buildOrThrow()) {
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.worldBorderBuffer.slice(), meshData.vertexBuffer());
            }

            this.lastBorderMinX = borderMinX;
            this.lastBorderMaxX = borderMaxX;
            this.lastBorderMinZ = borderMinZ;
            this.lastBorderMaxZ = borderMaxZ;
            this.lastMinX = minX;
            this.lastMinZ = minZ;
            this.needsRebuild = false;
        }
    }

    public void extract(
        final WorldBorder border, final float deltaPartialTick, final Vec3 cameraPos, final double renderDistance, final WorldBorderRenderState state
    ) {
        state.minX = border.getMinX(deltaPartialTick);
        state.maxX = border.getMaxX(deltaPartialTick);
        state.minZ = border.getMinZ(deltaPartialTick);
        state.maxZ = border.getMaxZ(deltaPartialTick);
        if ((
                !(cameraPos.x < state.maxX - renderDistance)
                    || !(cameraPos.x > state.minX + renderDistance)
                    || !(cameraPos.z < state.maxZ - renderDistance)
                    || !(cameraPos.z > state.minZ + renderDistance)
            )
            && !(cameraPos.x < state.minX - renderDistance)
            && !(cameraPos.x > state.maxX + renderDistance)
            && !(cameraPos.z < state.minZ - renderDistance)
            && !(cameraPos.z > state.maxZ + renderDistance)) {
            state.alpha = 1.0 - border.getDistanceToBorder(cameraPos.x, cameraPos.z) / renderDistance;
            state.alpha = Math.pow(state.alpha, 4.0);
            state.alpha = Mth.clamp(state.alpha, 0.0, 1.0);
            state.tint = border.getStatus().getColor();
        } else {
            state.alpha = 0.0;
        }
    }

    public void render(final WorldBorderRenderState state, final Vec3 cameraPos, final double renderDistance, final double depthFar) {
        if (!(state.alpha <= 0.0)) {
            double cameraX = cameraPos.x;
            double cameraZ = cameraPos.z;
            float halfHeightY = (float)depthFar;
            float red = ARGB.red(state.tint) / 255.0F;
            float green = ARGB.green(state.tint) / 255.0F;
            float blue = ARGB.blue(state.tint) / 255.0F;
            float offset = (float)(Util.getMillis() % 3000L) / 3000.0F;
            float v0 = (float)(-Mth.frac(cameraPos.y * 0.5));
            float v1 = v0 + halfHeightY;
            if (this.shouldRebuildWorldBorderBuffer(state)) {
                this.rebuildWorldBorderBuffer(state, renderDistance, cameraZ, cameraX, halfHeightY, v1, v0);
            }

            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            AbstractTexture abstractTexture = textureManager.getTexture(FORCEFIELD_LOCATION);
            RenderPipeline renderPipeline = RenderPipelines.WORLD_BORDER;
            RenderTarget mainRenderTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();
            RenderTarget weatherTarget = Minecraft.getInstance().levelRenderer.weatherTarget();
            GpuTextureView colorTexture;
            GpuTextureView depthTexture;
            if (weatherTarget != null) {
                colorTexture = weatherTarget.getColorTextureView();
                depthTexture = weatherTarget.getDepthTextureView();
            } else {
                colorTexture = mainRenderTarget.getColorTextureView();
                depthTexture = mainRenderTarget.getDepthTextureView();
            }

            GpuBuffer indexBuffer = this.indices.getBuffer(6);
            GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(
                    RenderSystem.getModelViewMatrixCopy(),
                    new Vector4f(red, green, blue, (float)state.alpha),
                    new Vector3f((float)(this.lastMinX - cameraX), (float)(-cameraPos.y), (float)(this.lastMinZ - cameraZ)),
                    new Matrix4f().translation(offset, offset, 0.0F)
                );

            try (RenderPass renderPass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(() -> "World border", colorTexture, Optional.empty(), depthTexture, OptionalDouble.empty())) {
                renderPass.setPipeline(renderPipeline);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setUniform("DynamicTransforms", dynamicTransforms);
                renderPass.setIndexBuffer(indexBuffer, this.indices.type());
                renderPass.bindTexture("Sampler0", abstractTexture.getTextureView(), abstractTexture.getSampler());
                renderPass.setVertexBuffer(0, this.worldBorderBuffer.slice());
                ArrayList<RenderPass.Draw<WorldBorderRenderer>> draws = new ArrayList<>();

                for (WorldBorderRenderState.DistancePerDirection distancePerDirection : state.closestBorder(cameraX, cameraZ)) {
                    if (distancePerDirection.distance() < renderDistance) {
                        int sideIndex = distancePerDirection.direction().get2DDataValue();
                        draws.add(new RenderPass.Draw<>(0, this.worldBorderBuffer, indexBuffer, this.indices.type(), 6 * sideIndex, 6, 0));
                    }
                }

                renderPass.drawMultipleIndexed(draws, null, null, Collections.emptyList(), this);
            }
        }
    }

    public void invalidate() {
        this.needsRebuild = true;
    }

    private boolean shouldRebuildWorldBorderBuffer(final WorldBorderRenderState state) {
        return this.needsRebuild
            || state.minX != this.lastBorderMinX
            || state.minZ != this.lastBorderMinZ
            || state.maxX != this.lastBorderMaxX
            || state.maxZ != this.lastBorderMaxZ;
    }
}