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
      p_308358_ -> Component.translatableEscape("particle.notFound", p_308358_)
   );
   public static final DynamicCommandExceptionType ERROR_INVALID_OPTIONS = new DynamicCommandExceptionType(
      p_325596_ -> Component.translatableEscape("particle.invalidOptions", p_325596_)
   );
   private final HolderLookup.Provider registries;
   private static final TagParser<?> VALUE_PARSER = TagParser.create(NbtOps.INSTANCE);

   public ParticleArgument(CommandBuildContext p_249844_) {
      this.registries = p_249844_;
   }

   public static ParticleArgument particle(CommandBuildContext p_251304_) {
      return new ParticleArgument(p_251304_);
   }

   public static ParticleOptions getParticle(CommandContext<CommandSourceStack> p_103938_, String p_103939_) {
      return (ParticleOptions)p_103938_.getArgument(p_103939_, ParticleOptions.class);
   }

   public ParticleOptions parse(StringReader p_103933_) throws CommandSyntaxException {
      return readParticle(p_103933_, this.registries);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static ParticleOptions readParticle(StringReader p_249275_, HolderLookup.Provider p_333534_) throws CommandSyntaxException {
      ParticleType<?> particletype = readParticleType(p_249275_, p_333534_.lookupOrThrow(Registries.PARTICLE_TYPE));
      return readParticle(VALUE_PARSER, p_249275_, (ParticleType<ParticleOptions>)particletype, p_333534_);
   }

   private static ParticleType<?> readParticleType(StringReader p_249621_, HolderLookup<ParticleType<?>> p_248983_) throws CommandSyntaxException {
      Identifier identifier = Identifier.read(p_249621_);
      ResourceKey<ParticleType<?>> resourcekey = ResourceKey.create(Registries.PARTICLE_TYPE, identifier);
      return p_248983_.get(resourcekey).orElseThrow(() -> ERROR_UNKNOWN_PARTICLE.createWithContext(p_249621_, identifier)).value();
   }

   private static <T extends ParticleOptions, O> T readParticle(
      TagParser<O> p_397288_, StringReader p_103935_, ParticleType<T> p_103936_, HolderLookup.Provider p_329867_
   ) throws CommandSyntaxException {
      RegistryOps<O> registryops = p_329867_.createSerializationContext(p_397288_.getOps());
      O o;
      if (p_103935_.canRead() && p_103935_.peek() == '{') {
         o = p_397288_.parseAsArgument(p_103935_);
      } else {
         o = registryops.emptyMap();
      }

      return (T)p_103936_.codec().codec().parse(registryops, o).getOrThrow(ERROR_INVALID_OPTIONS::create);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_103948_, SuggestionsBuilder p_103949_) {
      HolderLookup.RegistryLookup<ParticleType<?>> registrylookup = this.registries.lookupOrThrow(Registries.PARTICLE_TYPE);
      return SharedSuggestionProvider.suggestResource(registrylookup.listElementIds().map(ResourceKey::identifier), p_103949_);
   }
}
