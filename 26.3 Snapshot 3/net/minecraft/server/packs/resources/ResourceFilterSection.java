package net.minecraft.server.packs.resources;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.IdentifierPattern;

public class ResourceFilterSection {
   private static final Codec<ResourceFilterSection> CODEC = RecordCodecBuilder.create(
      i -> i.group(Codec.list(IdentifierPattern.CODEC).fieldOf("block").forGetter(o -> o.blockList)).apply(i, ResourceFilterSection::new)
   );
   public static final MetadataSectionType<ResourceFilterSection> TYPE = new MetadataSectionType<>("filter", CODEC);
   private final List<IdentifierPattern> blockList;

   public ResourceFilterSection(final List<IdentifierPattern> blockList) {
      this.blockList = List.copyOf(blockList);
   }

   public boolean isNamespaceFiltered(final String namespace) {
      return this.blockList.stream().anyMatch(p -> p.namespacePredicate().test(namespace));
   }

   public boolean isPathFiltered(final String path) {
      return this.blockList.stream().anyMatch(p -> p.pathPredicate().test(path));
   }
}
