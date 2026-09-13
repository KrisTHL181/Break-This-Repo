package net.minecraft.world.level.gamerules;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Objects;
import java.util.function.ToIntFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlagSet;

public final class GameRule<T> implements FeatureElement {
   private final GameRuleCategory category;
   private final GameRuleType gameRuleType;
   private final ArgumentType<T> argument;
   private final GameRules.VisitorCaller<T> visitorCaller;
   private final Codec<T> valueCodec;
   private final ToIntFunction<T> commandResultFunction;
   private final T defaultValue;
   private final FeatureFlagSet requiredFeatures;

   public GameRule(
      final GameRuleCategory category,
      final GameRuleType gameRuleType,
      final ArgumentType<T> argument,
      final GameRules.VisitorCaller<T> visitorCaller,
      final Codec<T> valueCodec,
      final ToIntFunction<T> commandResultFunction,
      final T defaultValue,
      final FeatureFlagSet requiredFeatures
   ) {
      this.category = category;
      this.gameRuleType = gameRuleType;
      this.argument = argument;
      this.visitorCaller = visitorCaller;
      this.valueCodec = valueCodec;
      this.commandResultFunction = commandResultFunction;
      this.defaultValue = defaultValue;
      this.requiredFeatures = requiredFeatures;
   }

   @Override
   public String toString() {
      return this.id();
   }

   public String id() {
      return this.getIdentifier().toShortString();
   }

   public Identifier getIdentifier() {
      return Objects.requireNonNull(BuiltInRegistries.GAME_RULE.getKey(this));
   }

   public Identifier getIdentifierWithFallback() {
      return Objects.requireNonNullElse(BuiltInRegistries.GAME_RULE.getKey(this), Identifier.withDefaultNamespace("unregistered_sadface"));
   }

   public String getDescriptionId() {
      return Util.makeDescriptionId("gamerule", this.getIdentifier());
   }

   public String serialize(final T value) {
      return value.toString();
   }

   public DataResult<T> deserialize(final String value) {
      try {
         StringReader reader = new StringReader(value);
         T result = (T)this.argument.parse(reader);
         return reader.canRead() ? DataResult.error(() -> "Failed to deserialize; trailing characters", result) : DataResult.success(result);
      } catch (CommandSyntaxException ignored) {
         return DataResult.error(() -> "Failed to deserialize");
      }
   }

   public Class<T> valueClass() {
      return (Class<T>)this.defaultValue.getClass();
   }

   public void callVisitor(final GameRuleTypeVisitor visitor) {
      this.visitorCaller.call(visitor, this);
   }

   public int getCommandResult(final T value) {
      return this.commandResultFunction.applyAsInt(value);
   }

   public GameRuleCategory category() {
      return this.category;
   }

   public GameRuleType gameRuleType() {
      return this.gameRuleType;
   }

   public ArgumentType<T> argument() {
      return this.argument;
   }

   public Codec<T> valueCodec() {
      return this.valueCodec;
   }

   public T defaultValue() {
      return this.defaultValue;
   }

   @Override
   public FeatureFlagSet requiredFeatures() {
      return this.requiredFeatures;
   }
}
