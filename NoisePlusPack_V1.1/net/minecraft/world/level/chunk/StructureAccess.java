package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jspecify.annotations.Nullable;

public interface StructureAccess {
   @Nullable StructureStart getStartForStructure(Structure var1);

   void setStartForStructure(Structure var1, StructureStart var2);

   LongSet getReferencesForStructure(Structure var1);

   void addReferenceForStructure(Structure var1, long var2);

   Map<Structure, LongSet> getAllReferences();

   void setAllReferences(Map<Structure, LongSet> var1);
}
