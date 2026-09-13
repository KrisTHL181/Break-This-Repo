package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jspecify.annotations.Nullable;

public interface StructureAccess {
   @Nullable StructureStart getStartForStructure(Structure structure);

   void setStartForStructure(Structure structure, StructureStart structureStart);

   LongSet getReferencesForStructure(Structure structure);

   void addReferenceForStructure(Structure structure, long reference);

   Map<Structure, LongSet> getAllReferences();

   void setAllReferences(Map<Structure, LongSet> data);
}
