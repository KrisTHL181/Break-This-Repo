package net.minecraft.util.filefix.access;

import com.mojang.serialization.Dynamic;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

public class PlayerData extends CompressedNbt {
   private final int targetVersion;

   public PlayerData(final Path path, final int targetVersion) {
      super(path, CompressedNbt.MissingSeverity.NEUTRAL);
      this.targetVersion = targetVersion;
   }

   @Override
   public Optional<Dynamic<Tag>> read() throws IOException {
      return this.readFile().map(readData -> {
         int version = NbtUtils.getDataVersion((Dynamic<?>)readData);
         return DataFixers.getDataFixer().update(References.PLAYER, readData, version, this.targetVersion);
      });
   }

   @Override
   public <T> void write(final Dynamic<T> data) {
      Dynamic<T> withDataVersion = NbtUtils.addDataVersion(data, this.targetVersion);
      this.writeFile(withDataVersion);
   }
}
