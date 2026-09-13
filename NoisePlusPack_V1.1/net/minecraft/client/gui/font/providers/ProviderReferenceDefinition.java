package net.minecraft.client.gui.font.providers;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ProviderReferenceDefinition(Identifier id) implements GlyphProviderDefinition {
   public static final MapCodec<ProviderReferenceDefinition> CODEC = RecordCodecBuilder.mapCodec(
      p_447992_ -> p_447992_.group(Identifier.CODEC.fieldOf("id").forGetter(ProviderReferenceDefinition::id))
         .apply(p_447992_, ProviderReferenceDefinition::new)
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
