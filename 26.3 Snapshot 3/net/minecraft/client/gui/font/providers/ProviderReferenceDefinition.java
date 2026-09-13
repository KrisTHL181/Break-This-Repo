package net.minecraft.client.gui.font.providers;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record ProviderReferenceDefinition(Identifier id) implements GlyphProviderDefinition {
   public static final MapCodec<ProviderReferenceDefinition> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Identifier.CODEC.fieldOf("id").forGetter(ProviderReferenceDefinition::id)).apply(i, ProviderReferenceDefinition::new)
   );

   @Override
   public GlyphProviderType type() {
      return GlyphProviderType.REFERENCE;
   }

   @Override
   public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack() {
      return Either.right(new GlyphProviderDefinition.Reference(this.id));
   }
}
