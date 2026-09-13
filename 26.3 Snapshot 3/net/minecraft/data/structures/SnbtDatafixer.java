package net.minecraft.data.structures;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.data.CachedOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.Bootstrap;

public class SnbtDatafixer {
   public static void main(final String[] args) throws IOException {
      SharedConstants.setVersion(DetectedVersion.BUILT_IN);
      Bootstrap.bootStrap();

      for (String dir : args) {
         updateInDirectory(dir);
      }
   }

   private static void updateInDirectory(final String structureDir) throws IOException {
      try (Stream<Path> walk = Files.walk(Paths.get(structureDir))) {
         walk.filter(path -> path.toString().endsWith(".snbt")).forEach(path -> {
            try {
               String snbt = Files.readString(path);
               CompoundTag readSnbt = NbtUtils.snbtToStructure(snbt);
               CompoundTag updatedTag = StructureUpdater.update(path.toString(), readSnbt);
               NbtToSnbt.writeSnbt(CachedOutput.NO_CACHE, path, NbtUtils.structureToSnbt(updatedTag));
            } catch (CommandSyntaxException | IOException e) {
               throw new RuntimeException(e);
            }
         });
      }
   }
}
