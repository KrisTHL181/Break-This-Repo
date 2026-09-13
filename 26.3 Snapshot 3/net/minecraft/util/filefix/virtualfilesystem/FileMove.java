package net.minecraft.util.filefix.virtualfilesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.file.Path;
import net.minecraft.util.ExtraCodecs;

public record FileMove(Path from, Path to) {
   public static Codec<FileMove> moveCodec(final Path fromDirectory, final Path toDirectory) {
      return RecordCodecBuilder.create(
         i -> i.group(
               ExtraCodecs.guardedPathCodec(fromDirectory).fieldOf("from").forGetter(r -> r.from),
               ExtraCodecs.guardedPathCodec(toDirectory).fieldOf("to").forGetter(r -> r.to)
            )
            .apply(i, FileMove::new)
      );
   }
}
