package net.minecraft.util.filefix.fixes;

import com.google.common.collect.Streams;
import com.mojang.datafixers.schemas.Schema;
import java.util.stream.Stream;
import net.minecraft.util.filefix.FileFix;
import net.minecraft.util.filefix.operations.FileFixOperations;

public class RemoveObsoleteFilesFileFix extends FileFix {
   public RemoveObsoleteFilesFileFix(final Schema schema) {
      super(schema);
   }

   @Override
   public void makeFixer() {
      this.addFileFixOperation(FileFixOperations.delete("data/villages.dat"));
      this.addFileFixOperation(FileFixOperations.delete("data/villages_end.dat"));
      this.addFileFixOperation(FileFixOperations.delete("data/villages_nether.dat"));
      this.addFileFixOperation(FileFixOperations.delete("data/advancements"));
      this.addFileFixOperation(FileFixOperations.delete("data/functions"));
      Streams.concat(
            new Stream[]{
               LegacyStructureFileFix.OVERWORLD_LEGACY_STRUCTURES.stream(),
               Stream.of("Village"),
               LegacyStructureFileFix.NETHER_LEGACY_STRUCTURES.stream(),
               LegacyStructureFileFix.END_LEGACY_STRUCTURES.stream()
            }
         )
         .forEach(leftoverStructure -> {
            this.addFileFixOperation(FileFixOperations.delete("data/" + leftoverStructure + ".dat"));
            this.addFileFixOperation(FileFixOperations.delete("data/" + leftoverStructure + "_index.dat"));
         });
   }
}
