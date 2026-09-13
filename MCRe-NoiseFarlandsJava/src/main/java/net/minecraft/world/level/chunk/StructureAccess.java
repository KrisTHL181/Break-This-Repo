package net.minecraft.world.level.chunk;

import java.util.Map;
import java.util.Set;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jspecify.annotations.Nullable;

/**
 * StructureAccess — 结构访问接口（MCRe NoiseFarlands 对象化版）
 * 原版结构引用以 long 打包键（ChunkPos.pack），本版以 ChunkPos 对象存储。
 */
public interface StructureAccess {
    @Nullable StructureStart getStartForStructure(Structure structure);

    void setStartForStructure(Structure structure, StructureStart structureStart);

    Set<ChunkPos> getReferencesForStructure(Structure structure);

    void addReferenceForStructure(Structure structure, ChunkPos reference);

    Map<Structure, Set<ChunkPos>> getAllReferences();

    void setAllReferences(Map<Structure, Set<ChunkPos>> data);
}
