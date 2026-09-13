package net.minecraft.util.filefix.fixes;

import com.mojang.datafixers.schemas.Schema;
import java.util.List;
import java.util.Map;
import net.minecraft.util.filefix.FileFix;
import net.minecraft.util.filefix.access.FileRelation;
import net.minecraft.util.filefix.operations.FileFixOperations;

public class DimensionStorageFileFix extends FileFix {
   public DimensionStorageFileFix(final Schema schema) {
      super(schema);
   }

   @Override
   public void makeFixer() {
      this.addFileFixOperation(
         FileFixOperations.applyInFolders(
            FileRelation.DIMENSIONS_DATA,
            List.of(
               FileFixOperations.move("chunks.dat", "minecraft/chunk_tickets.dat"),
               FileFixOperations.move("raids.dat", "minecraft/raids.dat"),
               FileFixOperations.move("world_border.dat", "minecraft/world_border.dat")
            )
         )
      );
      this.addFileFixOperation(
         FileFixOperations.groupMove(
            Map.of(
               "data",
               "dimensions/minecraft/overworld/data/minecraft",
               "DIM-1/data",
               "dimensions/minecraft/the_nether/data/minecraft",
               "DIM1/data",
               "dimensions/minecraft/the_end/data/minecraft"
            ),
            List.of(
               FileFixOperations.move("chunks.dat", "chunk_tickets.dat"),
               FileFixOperations.moveSimple("raids.dat"),
               FileFixOperations.move("raids_end.dat", "raids.dat"),
               FileFixOperations.moveSimple("world_border.dat")
            )
         )
      );
      this.addFileFixOperation(FileFixOperations.delete("DIM1/data"));
      this.addFileFixOperation(FileFixOperations.delete("DIM-1/data"));
      this.addFileFixOperation(
         FileFixOperations.applyInFolders(
            FileRelation.DATA,
            List.of(
               FileFixOperations.move("scoreboard.dat", "minecraft/scoreboard.dat"),
               FileFixOperations.move("stopwatches.dat", "minecraft/stopwatches.dat"),
               FileFixOperations.moveRegex("command_storage_([a-z0-9_.-]+)\\.dat", "$1/command_storage\\.dat"),
               FileFixOperations.move("idcounts.dat", "minecraft/maps/last_id.dat"),
               FileFixOperations.moveRegex("map_(\\d+)\\.dat", "minecraft/maps/$1\\.dat"),
               FileFixOperations.move("random_sequences.dat", "minecraft/random_sequences.dat")
            )
         )
      );
      this.addFileFixOperation(
         FileFixOperations.groupMove(
            Map.of("", "dimensions/minecraft/overworld", "DIM-1", "dimensions/minecraft/the_nether", "DIM1", "dimensions/minecraft/the_end"),
            List.of(FileFixOperations.moveSimple("region"), FileFixOperations.moveSimple("entities"), FileFixOperations.moveSimple("poi"))
         )
      );
      this.addFileFixOperation(FileFixOperations.delete("DIM-1"));
      this.addFileFixOperation(FileFixOperations.delete("DIM1"));
   }
}
