package net.minecraft.client.resources.metadata.gui;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record GuiMetadataSection(GuiSpriteScaling scaling) {
   public static final GuiMetadataSection DEFAULT = new GuiMetadataSection(GuiSpriteScaling.DEFAULT);
   public static final Codec<GuiMetadataSection> CODEC = RecordCodecBuilder.create(
      p_325573_ -> p_325573_.group(GuiSpriteScaling.CODEC.optionalFieldOf("scaling", GuiSpriteScaling.DEFAULT).forGetter(GuiMetadataSection::scaling))
         .apply(p_325573_, GuiMetadataSection::new)
   );
   public static final MetadataSectionType<GuiMetadataSection> TYPE = new MetadataSectionType<>("gui", CODEC);
}
