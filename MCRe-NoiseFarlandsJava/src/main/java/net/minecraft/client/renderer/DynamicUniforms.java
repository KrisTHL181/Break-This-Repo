package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import java.nio.ByteBuffer;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

@OnlyIn(Dist.CLIENT)
public class DynamicUniforms implements AutoCloseable {
    private static final Vector4fc WHITE = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Vector3fc NO_OFFSET = new Vector3f();
    private static final Matrix4fc IDENTITY_TEXTURE_TRANSFORM = new Matrix4f();
    public static final int TRANSFORM_UBO_SIZE = new Std140SizeCalculator().putMat4f().putVec4().putVec3().putMat4f().get();
    // MCRe：26.3 MultiDrawIndirect 移植——ChunkSection 精简为 16 字节（x,y,z,visibility），矩阵移至共享 TerrainUniform
    public static final int CHUNK_SECTION_UBO_SIZE = new Std140SizeCalculator().putIVec3().putFloat().get();
    public static final int TERRAIN_INFO_UBO_SIZE = new Std140SizeCalculator().putMat4f().putIVec2().get();
    private static final int INITIAL_CAPACITY = 2;
    private final DynamicUniformStorage<DynamicUniforms.Transform> transforms = new DynamicUniformStorage<>("Dynamic Transforms UBO", TRANSFORM_UBO_SIZE, 2);
    private final DynamicUniformStorage<DynamicUniforms.TerrainTransform> terrainInfo = new DynamicUniformStorage<>("Terrain Info UBO", TERRAIN_INFO_UBO_SIZE, 2);
    private final DynamicUniformStorage<DynamicUniforms.ChunkSectionInfo> chunkSections = new DynamicUniformStorage<>(
        "Chunk Sections UBO", CHUNK_SECTION_UBO_SIZE, 2
    );
    // MCRe：26.3 MultiDrawIndirect 移植——indirect 命令 buffer（VkDrawIndexedIndirectCommand 20 字节/条）+ instanced section 数据（16 字节/条）
    private final DynamicGpuDataStorage<DynamicUniforms.IndexedDraw> chunkSectionCommands = new DynamicGpuDataStorage<>(
        "Chunk Sections Command Buffer", 20, com.mojang.blaze3d.buffers.GpuBuffer.USAGE_INDIRECT_PARAMETERS, 512
    );
    private final DynamicGpuDataStorage<DynamicUniforms.ChunkSectionInfo> chunkSectionsInstanced = new DynamicGpuDataStorage<>(
        "Chunk Sections Instanced", 16, com.mojang.blaze3d.buffers.GpuBuffer.USAGE_VERTEX, 32
    );

    public void reset() {
        this.transforms.endFrame();
        this.terrainInfo.endFrame();
        this.chunkSections.endFrame();
        this.chunkSectionCommands.endFrame();
        this.chunkSectionsInstanced.endFrame();
    }

    @Override
    public void close() {
        this.transforms.close();
        this.terrainInfo.close();
        this.chunkSections.close();
        this.chunkSectionCommands.close();
        this.chunkSectionsInstanced.close();
    }

    public GpuBufferSlice writeTransform(final Matrix4f modelView) {
        return this.writeTransform(new DynamicUniforms.Transform(modelView, WHITE, NO_OFFSET, IDENTITY_TEXTURE_TRANSFORM));
    }

    public GpuBufferSlice writeTransform(final Matrix4f modelView, final Vector4f colorModulator) {
        return this.writeTransform(new DynamicUniforms.Transform(modelView, colorModulator, NO_OFFSET, IDENTITY_TEXTURE_TRANSFORM));
    }

    public GpuBufferSlice writeTransform(final Matrix4f modelView, final Matrix4f textureMatrix) {
        return this.writeTransform(new DynamicUniforms.Transform(modelView, WHITE, NO_OFFSET, textureMatrix));
    }

    public GpuBufferSlice writeTransform(final Matrix4f modelView, final Vector4f colorModulator, final Vector3f modelOffset, final Matrix4f textureMatrix) {
        return this.writeTransform(new DynamicUniforms.Transform(modelView, colorModulator, modelOffset, textureMatrix));
    }

    public GpuBufferSlice writeTransform(final DynamicUniforms.Transform uniform) {
        return this.transforms.writeUniform(uniform);
    }

    public GpuBufferSlice[] writeTransforms(final DynamicUniforms.Transform... transforms) {
        return this.transforms.writeUniforms(transforms);
    }

    public GpuBufferSlice writeTerrainTransform(final Matrix4fc modelView, final int textureAtlasWidth, final int textureAtlasHeight) {
        return this.terrainInfo.writeUniform(new DynamicUniforms.TerrainTransform(modelView, textureAtlasWidth, textureAtlasHeight));
    }

    public GpuBufferSlice[] writeChunkSections(final DynamicUniforms.ChunkSectionInfo... infos) {
        return this.chunkSections.writeUniforms(infos);
    }

    /** MCRe：MultiDrawIndirect——批量写入 instanced section 数据（16 字节/条，vertex binding 1） */
    public GpuBufferSlice writeChunkSectionsInstanced(final List<DynamicUniforms.ChunkSectionInfo> infos) {
        return this.chunkSectionsInstanced.writeDataBatched(infos);
    }

    /** MCRe：MultiDrawIndirect——批量写入 indirect 绘制命令（每组 draw 一个 buffer slice） */
    public GpuBufferSlice[] writeChunkSectionCommands(final List<List<DynamicUniforms.IndexedDraw>> draws) {
        return this.chunkSectionCommands.writeDataBatchedMultiple(draws);
    }

    @OnlyIn(Dist.CLIENT)
    public record ChunkSectionInfo(int x, int y, int z, float visibility) implements DynamicUniformStorage.DynamicUniform, DynamicGpuDataStorage.DynamicGpuData {
        @Override
        public void write(final ByteBuffer buffer) {
            buffer.putInt(this.x).putInt(this.y).putInt(this.z).putFloat(this.visibility);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record TerrainTransform(Matrix4fc modelView, int textureAtlasWidth, int textureAtlasHeight)
        implements DynamicUniformStorage.DynamicUniform {
        @Override
        public void write(final ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer).putMat4f(this.modelView).putIVec2(this.textureAtlasWidth, this.textureAtlasHeight);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record IndexedDraw(int indexCount, int instanceCount, int firstIndex, int baseVertex, int baseInstance)
        implements DynamicGpuDataStorage.DynamicGpuData {
        @Override
        public void write(final ByteBuffer buffer) {
            buffer.putInt(this.indexCount).putInt(this.instanceCount).putInt(this.firstIndex).putInt(this.baseVertex).putInt(this.baseInstance);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Transform(Matrix4fc modelView, Vector4fc colorModulator, Vector3fc modelOffset, Matrix4fc textureMatrix)
        implements DynamicUniformStorage.DynamicUniform {
        @Override
        public void write(final ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer).putMat4f(this.modelView).putVec4(this.colorModulator).putVec3(this.modelOffset).putMat4f(this.textureMatrix);
        }
    }
}