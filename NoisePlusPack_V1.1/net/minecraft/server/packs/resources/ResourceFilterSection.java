package net.minecraft.server.packs.resources;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.IdentifierPattern;

public class ResourceFilterSection {
   private static final Codec<ResourceFilterSection> CODEC = RecordCodecBuilder.create(
      p_449194_ -> p_449194_.group(Codec.list(IdentifierPattern.CODEC).fieldOf("block").forGetter(p_215520_ -> p_215520_.blockList))
         .apply(p_449194_, ResourceFilterSection::new)
   );
   public static final MetadataSectionType<ResourceFilterSection> TYPE = new MetadataSectionType<>("filter", CODEC);
   private final List<IdentifierPattern> blockList;

   public ResourceFilterSection(List<IdentifierPattern> p_215518_) {
      this.blockList = List.copyOf(p_215518_);
   }

   public boolean isNamespaceFiltered(String p_215524_) {
      return this.blockList.stream().anyMatch(p_449196_ -> p_449196_.namespacePredicate().test(p_215524_));
   }

   public boolean isPathFiltered(String p_215529_) {
      return this.blockList.stream().anyMatch(p_449198_ -> p_449198_.pathPredicate().test(p_215529_));
   }
}
