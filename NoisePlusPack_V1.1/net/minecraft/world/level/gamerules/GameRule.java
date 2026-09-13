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
      GameRuleCategory p_453014_,
      GameRuleType p_457146_,
      ArgumentType<T> p_459513_,
      GameRules.VisitorCaller<T> p_458863_,
      Codec<T> p_453195_,
      ToIntFunction<T> p_454698_,
      T p_460979_,
      FeatureFlagSet p_451008_
   ) {
      this.category = p_453014_;
      this.gameRuleType = p_457146_;
      this.argument = p_459513_;
      this.visitorCaller = p_458863_;
      this.valueCodec = p_453195_;
      this.commandResultFunction = p_454698_;
      this.defaultValue = p_460979_;
      this.requiredFeatures = p_451008_;
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

   public String getDescriptionId() {
      return Util.makeDescriptionId("gamerule", this.getIdentifier());
   }

   public String serialize(T p_455121_) {
      return p_455121_.toString();
   }

   public DataResult<T> deserialize(String p_454648_) {
      try {
         StringReader stringreader = new StringReader(p_454648_);
         T t = (T)this.argument.parse(stringreader);
         return stringreader.canRead() ? DataResult.error(() -> "Failed to deserialize; trailing characters", t) : DataResult.success(t);
      } catch (CommandSyntaxException commandsyntaxexception) {
         return DataResult.error(() -> "Failed to deserialize");
      }
   }

   public Class<T> valueClass() {
      return (Class<T>)this.defaultValue.getClass();
   }

   public void callVisitor(GameRuleTypeVisitor p_458312_) {
      this.visitorCaller.call(p_458312_, this);
   }

   public int getCommandResult(T p_453961_) {
      return this.commandResultFunction.applyAsInt(p_453961_);
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
