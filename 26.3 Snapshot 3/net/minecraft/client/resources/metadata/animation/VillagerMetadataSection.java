package net.minecraft.client.resources.metadata.animation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.StringRepresentable;

public record VillagerMetadataSection(VillagerMetadataSection.Hat hat) {
   public static final Codec<VillagerMetadataSection> CODEC = RecordCodecBuilder.create(
      i -> i.group(VillagerMetadataSection.Hat.CODEC.optionalFieldOf("hat", VillagerMetadataSection.Hat.NONE).forGetter(VillagerMetadataSection::hat))
         .apply(i, VillagerMetadataSection::new)
   );
   public static final MetadataSectionType<VillagerMetadataSection> TYPE = new MetadataSectionType<>("villager", CODEC);

   public enum Hat implements StringRepresentable {
      NONE("none"),
      PARTIAL("partial"),
      FULL("full");

      public static final Codec<VillagerMetadataSection.Hat> CODEC = StringRepresentable.fromEnum(VillagerMetadataSection.Hat::values);
      private final String name;

      Hat(final String name) {
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
