package net.minecraft.util.filefix.access;

import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.serialization.Dynamic;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;

public class SavedDataNbt extends CompressedNbt {
   private final TypeReference type;
   private final int targetVersion;

   public SavedDataNbt(final TypeReference type, final Path path, final int targetVersion, final CompressedNbt.MissingSeverity missingSeverity) {
      super(path, missingSeverity);
      this.type = type;
      this.targetVersion = targetVersion;
   }

   @Override
   public Optional<Dynamic<Tag>> read() throws IOException {
      return this.readFile().map(readData -> {
         int version = NbtUtils.getDataVersion((Dynamic<?>)readData);
         return DataFixers.getDataFixer().update(this.type, readData, version, this.targetVersion).get("data").orElseEmptyMap();
      });
   }

   @Override
   public <T> void write(final Dynamic<T> data) {
      Dynamic<T> dataTag = data.emptyMap().set("data", data);
      Dynamic<T> wrappedAndWithDataVersion = NbtUtils.addDataVersion(dataTag, this.targetVersion);
      this.writeFile(wrappedAndWithDataVersion);
   }
}
