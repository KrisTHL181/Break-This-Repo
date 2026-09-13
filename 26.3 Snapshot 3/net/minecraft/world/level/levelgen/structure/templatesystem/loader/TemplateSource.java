package net.minecraft.world.level.levelgen.structure.templatesystem.loader;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixer;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public abstract class TemplateSource {
   private final DataFixer fixerUpper;
   private final HolderGetter<Block> blockLookup;

   protected TemplateSource(final DataFixer fixerUpper, final HolderGetter<Block> blockLookup) {
      this.fixerUpper = fixerUpper;
      this.blockLookup = blockLookup;
   }

   public abstract Optional<StructureTemplate> load(Identifier id);

   public abstract Stream<Identifier> list();

   protected Optional<StructureTemplate> load(final IoSupplier<InputStream> opener, final boolean asText, final Consumer<Throwable> onError) {
      try (
         InputStream rawInput = opener.get();
         InputStream input = new FastBufferedInputStream(rawInput);
      ) {
         CompoundTag structureTag;
         if (asText) {
            structureTag = readTextStructure(input);
         } else {
            structureTag = readStructure(input);
         }

         return Optional.of(this.readStructure(structureTag));
      } catch (FileNotFoundException e) {
         return Optional.empty();
      } catch (Throwable e) {
         onError.accept(e);
         return Optional.empty();
      }
   }

   private static CompoundTag readStructure(final InputStream input) throws IOException {
      return NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
   }

   private static CompoundTag readTextStructure(final InputStream input) throws IOException, CommandSyntaxException {
      try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
         String contents = reader.readAllAsString();
         return NbtUtils.snbtToStructure(contents);
      }
   }

   private StructureTemplate readStructure(final CompoundTag tag) {
      StructureTemplate structureTemplate = new StructureTemplate();
      int version = NbtUtils.getDataVersion(tag, 500);
      structureTemplate.load(this.blockLookup, DataFixTypes.STRUCTURE.updateToCurrentVersion(this.fixerUpper, tag, version));
      return structureTemplate;
   }
}
