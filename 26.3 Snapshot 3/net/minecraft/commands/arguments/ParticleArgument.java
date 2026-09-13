package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;

public class ParticleArgument implements ArgumentType<ParticleOptions> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "particle{foo:bar}");
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_PARTICLE = new DynamicCommandExceptionType(
      value -> Component.translatableEscape("particle.notFound", value)
   );
   public static final DynamicCommandExceptionType ERROR_INVALID_OPTIONS = new DynamicCommandExceptionType(
      message -> Component.translatableEscape("particle.invalidOptions", message)
   );
   private final HolderLookup.Provider registries;
   private static final TagParser<?> VALUE_PARSER = TagParser.create(NbtOps.INSTANCE);

   public ParticleArgument(final CommandBuildContext context) {
      this.registries = context;
   }

   public static ParticleArgument particle(final CommandBuildContext context) {
      return new ParticleArgument(context);
   }

   public static ParticleOptions getParticle(final CommandContext<CommandSourceStack> context, final String name) {
      return (ParticleOptions)context.getArgument(name, ParticleOptions.class);
   }

   public ParticleOptions parse(final StringReader reader) throws CommandSyntaxException {
      return readParticle(reader, this.registries);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static ParticleOptions readParticle(final StringReader reader, final HolderLookup.Provider registries) throws CommandSyntaxException {
      ParticleType<?> type = readParticleType(reader, registries.lookupOrThrow(Registries.PARTICLE_TYPE));
      return readParticle(VALUE_PARSER, reader, (ParticleType<ParticleOptions>)type, registries);
   }

   private static ParticleType<?> readParticleType(final StringReader reader, final HolderLookup<ParticleType<?>> particles) throws CommandSyntaxException {
      Identifier id = Identifier.read(reader);
      ResourceKey<ParticleType<?>> key = ResourceKey.create(Registries.PARTICLE_TYPE, id);
      return particles.get(key).orElseThrow(() -> ERROR_UNKNOWN_PARTICLE.createWithContext(reader, id)).value();
   }

   private static <T extends ParticleOptions, O> T readParticle(
      final TagParser<O> parser, final StringReader reader, final ParticleType<T> type, final HolderLookup.Provider registries
   ) throws CommandSyntaxException {
      RegistryOps<O> ops = registries.createSerializationContext(parser.getOps());
      O extraData;
      if (reader.canRead() && reader.peek() == '{') {
         extraData = parser.parseAsArgument(reader);
      } else {
         extraData = ops.emptyMap();
      }

      return (T)type.codec().codec().parse(ops, extraData).getOrThrow(ERROR_INVALID_OPTIONS::create);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
      HolderLookup.RegistryLookup<ParticleType<?>> particles = this.registries.lookupOrThrow(Registries.PARTICLE_TYPE);
      return SharedSuggestionProvider.suggestResource(particles.listElementIds().map(ResourceKey::identifier), builder);
   }
}
