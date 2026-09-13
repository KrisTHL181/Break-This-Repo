package net.minecraft.client.renderer;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class WeatherEffectRenderer implements AutoCloseable {
    private static final Identifier RAIN_LOCATION = Identifier.withDefaultNamespace("textures/environment/rain.png");
    private static final Identifier SNOW_LOCATION = Identifier.withDefaultNamespace("textures/environment/snow.png");
    private static final int RAIN_TABLE_SIZE = 32;
    private static final int HALF_RAIN_TABLE_SIZE = 16;
    private static final int INDICES_PER_COLUMN = 6;
    private final float[] columnSizeX = new float[1024];
    private final float[] columnSizeZ = new float[1024];
    private @Nullable GpuBuffer vertexBuffer;

    public WeatherEffectRenderer() {
        for (int z = 0; z < 32; z++) {
            for (int x = 0; x < 32; x++) {
                float deltaX = x - 16;
                float deltaZ = z - 16;
                float distance = Mth.length(deltaX, deltaZ);
                this.columnSizeX[z * 32 + x] = -deltaZ / distance;
                this.columnSizeZ[z * 32 + x] = deltaX / distance;
            }
        }
    }

    public void extractRenderState(final ClientLevel level, final float partialTicks, final Vec3 cameraPos, final WeatherRenderState renderState) {
        renderState.intensity = level.getRainLevel(partialTicks);
        if (!(renderState.intensity <= 0.0F)) {
            renderState.radius = Minecraft.getInstance().options.weatherRadius().get();
            int cameraBlockX = Mth.floor(cameraPos.x);
            int cameraBlockY = Mth.floor(cameraPos.y);
            int cameraBlockZ = Mth.floor(cameraPos.z);
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            RandomSource random = RandomSource.createThreadLocalInstance();

            long zStart = (long) cameraBlockZ - renderState.radius;
            long zEnd = (long) cameraBlockZ + renderState.radius;
            for (long zL = zStart; zL <= zEnd; zL++) {
                int z = (int) zL;
                long xStart = (long) cameraBlockX - renderState.radius;
                long xEnd = (long) cameraBlockX + renderState.radius;
                for (long xL = xStart; xL <= xEnd; xL++) {
                    int x = (int) xL;
                    int terrainHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                    int y0 = Math.max(cameraBlockY - renderState.radius, terrainHeight);
                    int y1 = Math.max(cameraBlockY + renderState.radius, terrainHeight);
                    if (y1 - y0 != 0) {
                        Biome.Precipitation precipitation = level.getPrecipitationAt(mutablePos.set(x, cameraBlockY, z));
                        if (precipitation != Biome.Precipitation.NONE) {
                            int seed = x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761;
                            random.setSeed(seed);
                            int lightSampleY = Math.max(cameraBlockY, terrainHeight);
                            int lightCoords = LightCoordsUtil.getLightCoords(level, mutablePos.set(x, lightSampleY, z));
                            if (precipitation == Biome.Precipitation.RAIN) {
                                renderState.rainColumns
                                        .add(this.createRainColumnInstance(random, level.getGameTime(), x, y0, y1, z, lightCoords, partialTicks));
                            } else if (precipitation == Biome.Precipitation.SNOW) {
                                renderState.snowColumns
                                        .add(this.createSnowColumnInstance(random, level.getGameTime(), x, y0, y1, z, lightCoords, partialTicks));
                            }
                        }
                    }
                }
            }
        }
    }

    private void renderWeather(final RenderPass renderPass, final AbstractTexture texture, final int startColumn, final int columnCount) {
        renderPass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
        renderPass.drawIndexed(columnCount * 6, 1, startColumn * 6, 0, 0);
    }

    private GpuBuffer uploadVertexBuffer(final ByteBuffer buffer) {
        GpuDevice device = RenderSystem.getDevice();
        if (this.vertexBuffer == null || this.vertexBuffer.size() < buffer.remaining()) {
            if (this.vertexBuffer != null) {
                this.vertexBuffer.close();
            }

            this.vertexBuffer = device.createBuffer(() -> "Weather Vertex Buffer", 40, buffer.remaining());
        }

        device.createCommandEncoder().writeToBuffer(this.vertexBuffer.slice(), buffer);
        return this.vertexBuffer;
    }

    public void render(final Vec3 cameraPos, final WeatherRenderState renderState) {
        int columnCount = renderState.rainColumns.size() + renderState.snowColumns.size();
        if (columnCount != 0) {
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            AbstractTexture rainTexture = textureManager.getTexture(RAIN_LOCATION);
            AbstractTexture snowTexture = textureManager.getTexture(SNOW_LOCATION);
            RenderTarget weatherRenderTarget = OutputTarget.WEATHER_TARGET.getRenderTarget();
            GpuTextureView colorTexture = weatherRenderTarget.getColorTextureView();
            GpuTextureView depthTexture = weatherRenderTarget.getDepthTextureView();
            RenderPipeline renderPipeline = Minecraft.getInstance().gameRenderer.gameRenderState().useShaderTransparency()
                    ? RenderPipelines.WEATHER_DEPTH_WRITE
                    : RenderPipelines.WEATHER_NO_DEPTH_WRITE;

            GpuBuffer vertexBuffer;
            GpuBuffer indexBuffer;
            IndexType indexType;
            try (ByteBufferBuilder builder = ByteBufferBuilder.exactlySized(columnCount * DefaultVertexFormat.PARTICLE.getVertexSize() * 4)) {
                BufferBuilder bufferBuilder = new BufferBuilder(builder, PrimitiveTopology.QUADS, DefaultVertexFormat.PARTICLE);
                this.renderInstances(bufferBuilder, renderState.rainColumns, cameraPos, 1.0F, renderState.radius, renderState.intensity);
                this.renderInstances(bufferBuilder, renderState.snowColumns, cameraPos, 0.8F, renderState.radius, renderState.intensity);

                try (MeshData mesh = bufferBuilder.buildOrThrow()) {
                    vertexBuffer = this.uploadVertexBuffer(mesh.vertexBuffer());
                    RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(mesh.drawState().primitiveTopology());
                    indexBuffer = autoIndices.getBuffer(mesh.drawState().indexCount());
                    indexType = autoIndices.type();
                }
            }

            GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy());

            try (RenderPass renderPass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(() -> "Weather Effect", colorTexture, Optional.empty(), depthTexture, OptionalDouble.empty())) {
                renderPass.setPipeline(renderPipeline);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setUniform("DynamicTransforms", dynamicTransforms);
                renderPass.bindTexture(
                        "Sampler2", Minecraft.getInstance().gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
                );
                renderPass.setIndexBuffer(indexBuffer, indexType);
                renderPass.setVertexBuffer(0, vertexBuffer.slice());
                this.renderWeather(renderPass, rainTexture, 0, renderState.rainColumns.size());
                this.renderWeather(renderPass, snowTexture, renderState.rainColumns.size(), renderState.snowColumns.size());
            }
        }
    }

    private WeatherEffectRenderer.ColumnInstance createRainColumnInstance(
            final RandomSource random,
            final long ticks,
            final int x,
            final int bottomY,
            final int topY,
            final int z,
            final int lightCoords,
            final float partialTicks) {
        int wrappedTicks = (int) (ticks & 131071L);
        int tickOffset = x * x * 3121 + x * 45238971 + z * z * 418711 + z * 13761 & 0xFF;
        float blockPosRainSpeed = 3.0F + random.nextFloat();
        float textureOffset = -(wrappedTicks + tickOffset + partialTicks) / 32.0F * blockPosRainSpeed;
        float wrappedTextureOffset = textureOffset % 32.0F;
        return new WeatherEffectRenderer.ColumnInstance(x, z, bottomY, topY, 0.0F, wrappedTextureOffset, lightCoords);
    }

    private WeatherEffectRenderer.ColumnInstance createSnowColumnInstance(
            final RandomSource random,
            final long ticks,
            final int x,
            final int bottomY,
            final int topY,
            final int z,
            final int lightCoords,
            final float partialTicks) {
        int wrappedTicks = (int) (ticks & 131071L);
        float time = wrappedTicks + partialTicks;
        float u = (float) (random.nextDouble() + time * 0.01F * (float) random.nextGaussian());
        float v = (float) (random.nextDouble() + time * (float) random.nextGaussian() * 0.001F);
        float vOffset = -((float) (ticks & 511L) + partialTicks) / 512.0F;
        int brightenedLightCoords = LightCoordsUtil.pack((LightCoordsUtil.block(lightCoords) * 3 + 15) / 4, (LightCoordsUtil.sky(lightCoords) * 3 + 15) / 4);
        return new WeatherEffectRenderer.ColumnInstance(x, z, bottomY, topY, u, vOffset + v, brightenedLightCoords);
    }

    private void renderInstances(
            final VertexConsumer builder,
            final List<WeatherEffectRenderer.ColumnInstance> columns,
            final Vec3 cameraPos,
            final float maxAlpha,
            final int radius,
            final float intensity) {
        if (!columns.isEmpty()) {
            float radiusSq = radius * radius;

            for (WeatherEffectRenderer.ColumnInstance column : columns) {
                float relativeX = (float) (column.x + 0.5 - cameraPos.x);
                float relativeZ = (float) (column.z + 0.5 - cameraPos.z);
                float distanceSq = (float) Mth.lengthSquared(relativeX, relativeZ);
                float alpha = Mth.lerp(Math.min(distanceSq / radiusSq, 1.0F), maxAlpha, 0.5F) * intensity;
                int color = ARGB.white(alpha);
                int index = (column.z - Mth.floor(cameraPos.z) + 16) * 32 + column.x - Mth.floor(cameraPos.x) + 16;
                float halfSizeX = this.columnSizeX[index] / 2.0F;
                float halfSizeZ = this.columnSizeZ[index] / 2.0F;
                float x0 = relativeX - halfSizeX;
                float x1 = relativeX + halfSizeX;
                float y1 = (float) (column.topY - cameraPos.y);
                float y0 = (float) (column.bottomY - cameraPos.y);
                float z0 = relativeZ - halfSizeZ;
                float z1 = relativeZ + halfSizeZ;
                float u0 = column.uOffset + 0.0F;
                float u1 = column.uOffset + 1.0F;
                float v0 = column.bottomY * 0.25F + column.vOffset;
                float v1 = column.topY * 0.25F + column.vOffset;
                builder.addVertex(x0, y1, z0).setUv(u0, v0).setColor(color).setLight(column.lightCoords);
                builder.addVertex(x1, y1, z1).setUv(u1, v0).setColor(color).setLight(column.lightCoords);
                builder.addVertex(x1, y0, z1).setUv(u1, v1).setColor(color).setLight(column.lightCoords);
                builder.addVertex(x0, y0, z0).setUv(u0, v1).setColor(color).setLight(column.lightCoords);
            }
        }
    }

    @Override
    public void close() {
        if (this.vertexBuffer != null) {
            this.vertexBuffer.close();
            this.vertexBuffer = null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record ColumnInstance(int x, int z, int bottomY, int topY, float uOffset, float vOffset, int lightCoords) {}
}