package net.minecraft.data.structures;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Util;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class SnbtToNbt implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PackOutput output;
   private final Iterable<Path> inputFolders;
   private final List<SnbtToNbt.Filter> filters = Lists.newArrayList();

   public SnbtToNbt(PackOutput p_249104_, Iterable<Path> p_249523_) {
      this.output = p_249104_;
      this.inputFolders = p_249523_;
   }

   public SnbtToNbt addFilter(SnbtToNbt.Filter p_126476_) {
      this.filters.add(p_126476_);
      return this;
   }

   private CompoundTag applyFilters(String p_126461_, CompoundTag p_126462_) {
      CompoundTag compoundtag = p_126462_;

      for (SnbtToNbt.Filter snbttonbt$filter : this.filters) {
         compoundtag = snbttonbt$filter.apply(p_126461_, compoundtag);
      }

      return compoundtag;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput p_254336_) {
      Path path = this.output.getOutputFolder();
      List<CompletableFuture<?>> list = Lists.newArrayList();

      for (Path path1 : this.inputFolders) {
         list.add(
            CompletableFuture.<CompletableFuture<Void>>supplyAsync(
                  () -> {
                     try (Stream<Path> stream = Files.walk(path1)) {
                        return CompletableFuture.allOf(
                           stream.filter(p_126464_ -> p_126464_.toString().endsWith(".snbt")).map(p_448736_ -> CompletableFuture.runAsync(() -> {
                              SnbtToNbt.TaskResult snbttonbt$taskresult = this.readStructure(p_448736_, this.getName(path1, p_448736_));
                              this.storeStructureIfChanged(p_254336_, snbttonbt$taskresult, path);
                           }, Util.backgroundExecutor().forName("SnbtToNbt"))).toArray(CompletableFuture[]::new)
                        );
                     } catch (Exception exception) {
                        throw new RuntimeException("Failed to read structure input directory, aborting", exception);
                     }
                  },
                  Util.backgroundExecutor().forName("SnbtToNbt")
               )
               .thenCompose(p_253441_ -> (CompletionStage<Void>)p_253441_)
         );
      }

      return Util.sequenceFailFast(list);
   }

   @Override
   public final String getName() {
      return "SNBT -> NBT";
   }

   private String getName(Path p_126469_, Path p_126470_) {
      String s = p_126469_.relativize(p_126470_).toString().replaceAll("\\\\", "/");
      return s.substring(0, s.length() - ".snbt".length());
   }

   private SnbtToNbt.TaskResult readStructure(Path p_126466_, String p_126467_) {
      try (BufferedReader bufferedreader = Files.newBufferedReader(p_126466_)) {
         String s = IOUtils.toString(bufferedreader);
         CompoundTag compoundtag = this.applyFilters(p_126467_, NbtUtils.snbtToStructure(s));
         ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
         HashingOutputStream hashingoutputstream = new HashingOutputStream(Hashing.sha1(), bytearrayoutputstream);
         NbtIo.writeCompressed(compoundtag, hashingoutputstream);
         byte[] abyte = bytearrayoutputstream.toByteArray();
         HashCode hashcode = hashingoutputstream.hash();
         return new SnbtToNbt.TaskResult(p_126467_, abyte, hashcode);
      } catch (Throwable throwable1) {
         throw new SnbtToNbt.StructureConversionException(p_126466_, throwable1);
      }
   }

   private void storeStructureIfChanged(CachedOutput p_236394_, SnbtToNbt.TaskResult p_236395_, Path p_236396_) {
      Path path = p_236396_.resolve(p_236395_.name + ".nbt");

      try {
         p_236394_.writeIfNeeded(path, p_236395_.payload, p_236395_.hash);
      } catch (IOException ioexception) {
         LOGGER.error("Couldn't write structure {} at {}", new Object[]{p_236395_.name, path, ioexception});
      }
   }

   @FunctionalInterface
   public interface Filter {
      CompoundTag apply(String var1, CompoundTag var2);
   }

   static class StructureConversionException extends RuntimeException {
      public StructureConversionException(Path p_176820_, Throwable p_176821_) {
         super(p_176820_.toAbsolutePath().toString(), p_176821_);
      }
   }

   record TaskResult(String name, byte[] payload, HashCode hash) {
   }
}
