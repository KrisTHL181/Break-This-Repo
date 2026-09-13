package net.minecraft.commands.synchronization;

import com.google.common.collect.Maps;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Locale;
import java.util.Map;
import net.minecraft.commands.arguments.AngleArgument;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.HeightmapTypeArgument;
import net.minecraft.commands.arguments.HexColorArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.ObjectiveCriteriaArgument;
import net.minecraft.commands.arguments.OperationArgument;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.commands.arguments.ResourceSelectorArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.ScoreboardSlotArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.SlotsArgument;
import net.minecraft.commands.arguments.StyleArgument;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.commands.arguments.TemplateMirrorArgument;
import net.minecraft.commands.arguments.TemplateRotationArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.SwizzleArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.commands.synchronization.brigadier.DoubleArgumentInfo;
import net.minecraft.commands.synchronization.brigadier.FloatArgumentInfo;
import net.minecraft.commands.synchronization.brigadier.IntegerArgumentInfo;
import net.minecraft.commands.synchronization.brigadier.LongArgumentInfo;
import net.minecraft.commands.synchronization.brigadier.StringArgumentSerializer;
import net.minecraft.core.Registry;

public class ArgumentTypeInfos {
   private static final Map<Class<?>, ArgumentTypeInfo<?, ?>> BY_CLASS = Maps.newHashMap();

   private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> ArgumentTypeInfo<A, T> register(
      Registry<ArgumentTypeInfo<?, ?>> p_235387_, String p_235388_, Class<? extends A> p_235389_, ArgumentTypeInfo<A, T> p_235390_
   ) {
      BY_CLASS.put(p_235389_, p_235390_);
      return Registry.register(p_235387_, p_235388_, p_235390_);
   }

   public static ArgumentTypeInfo<?, ?> bootstrap(Registry<ArgumentTypeInfo<?, ?>> p_235385_) {
      register(p_235385_, "brigadier:bool", BoolArgumentType.class, SingletonArgumentInfo.contextFree(BoolArgumentType::bool));
      register(p_235385_, "brigadier:float", FloatArgumentType.class, new FloatArgumentInfo());
      register(p_235385_, "brigadier:double", DoubleArgumentType.class, new DoubleArgumentInfo());
      register(p_235385_, "brigadier:integer", IntegerArgumentType.class, new IntegerArgumentInfo());
      register(p_235385_, "brigadier:long", LongArgumentType.class, new LongArgumentInfo());
      register(p_235385_, "brigadier:string", StringArgumentType.class, new StringArgumentSerializer());
      register(p_235385_, "entity", EntityArgument.class, new EntityArgument.Info());
      register(p_235385_, "game_profile", GameProfileArgument.class, SingletonArgumentInfo.contextFree(GameProfileArgument::gameProfile));
      register(p_235385_, "block_pos", BlockPosArgument.class, SingletonArgumentInfo.contextFree(BlockPosArgument::blockPos));
      register(p_235385_, "column_pos", ColumnPosArgument.class, SingletonArgumentInfo.contextFree(ColumnPosArgument::columnPos));
      register(p_235385_, "vec3", Vec3Argument.class, SingletonArgumentInfo.contextFree(Vec3Argument::vec3));
      register(p_235385_, "vec2", Vec2Argument.class, SingletonArgumentInfo.contextFree(Vec2Argument::vec2));
      register(p_235385_, "block_state", BlockStateArgument.class, SingletonArgumentInfo.contextAware(BlockStateArgument::block));
      register(p_235385_, "block_predicate", BlockPredicateArgument.class, SingletonArgumentInfo.contextAware(BlockPredicateArgument::blockPredicate));
      register(p_235385_, "item_stack", ItemArgument.class, SingletonArgumentInfo.contextAware(ItemArgument::item));
      register(p_235385_, "item_predicate", ItemPredicateArgument.class, SingletonArgumentInfo.contextAware(ItemPredicateArgument::itemPredicate));
      register(p_235385_, "color", ColorArgument.class, SingletonArgumentInfo.contextFree(ColorArgument::color));
      register(p_235385_, "hex_color", HexColorArgument.class, SingletonArgumentInfo.contextFree(HexColorArgument::hexColor));
      register(p_235385_, "component", ComponentArgument.class, SingletonArgumentInfo.contextAware(ComponentArgument::textComponent));
      register(p_235385_, "style", StyleArgument.class, SingletonArgumentInfo.contextAware(StyleArgument::style));
      register(p_235385_, "message", MessageArgument.class, SingletonArgumentInfo.contextFree(MessageArgument::message));
      register(p_235385_, "nbt_compound_tag", CompoundTagArgument.class, SingletonArgumentInfo.contextFree(CompoundTagArgument::compoundTag));
      register(p_235385_, "nbt_tag", NbtTagArgument.class, SingletonArgumentInfo.contextFree(NbtTagArgument::nbtTag));
      register(p_235385_, "nbt_path", NbtPathArgument.class, SingletonArgumentInfo.contextFree(NbtPathArgument::nbtPath));
      register(p_235385_, "objective", ObjectiveArgument.class, SingletonArgumentInfo.contextFree(ObjectiveArgument::objective));
      register(p_235385_, "objective_criteria", ObjectiveCriteriaArgument.class, SingletonArgumentInfo.contextFree(ObjectiveCriteriaArgument::criteria));
      register(p_235385_, "operation", OperationArgument.class, SingletonArgumentInfo.contextFree(OperationArgument::operation));
      register(p_235385_, "particle", ParticleArgument.class, SingletonArgumentInfo.contextAware(ParticleArgument::particle));
      register(p_235385_, "angle", AngleArgument.class, SingletonArgumentInfo.contextFree(AngleArgument::angle));
      register(p_235385_, "rotation", RotationArgument.class, SingletonArgumentInfo.contextFree(RotationArgument::rotation));
      register(p_235385_, "scoreboard_slot", ScoreboardSlotArgument.class, SingletonArgumentInfo.contextFree(ScoreboardSlotArgument::displaySlot));
      register(p_235385_, "score_holder", ScoreHolderArgument.class, new ScoreHolderArgument.Info());
      register(p_235385_, "swizzle", SwizzleArgument.class, SingletonArgumentInfo.contextFree(SwizzleArgument::swizzle));
      register(p_235385_, "team", TeamArgument.class, SingletonArgumentInfo.contextFree(TeamArgument::team));
      register(p_235385_, "item_slot", SlotArgument.class, SingletonArgumentInfo.contextFree(SlotArgument::slot));
      register(p_235385_, "item_slots", SlotsArgument.class, SingletonArgumentInfo.contextFree(SlotsArgument::slots));
      register(p_235385_, "resource_location", IdentifierArgument.class, SingletonArgumentInfo.contextFree(IdentifierArgument::id));
      register(p_235385_, "function", FunctionArgument.class, SingletonArgumentInfo.contextFree(FunctionArgument::functions));
      register(p_235385_, "entity_anchor", EntityAnchorArgument.class, SingletonArgumentInfo.contextFree(EntityAnchorArgument::anchor));
      register(p_235385_, "int_range", RangeArgument.Ints.class, SingletonArgumentInfo.contextFree(RangeArgument::intRange));
      register(p_235385_, "float_range", RangeArgument.Floats.class, SingletonArgumentInfo.contextFree(RangeArgument::floatRange));
      register(p_235385_, "dimension", DimensionArgument.class, SingletonArgumentInfo.contextFree(DimensionArgument::dimension));
      register(p_235385_, "gamemode", GameModeArgument.class, SingletonArgumentInfo.contextFree(GameModeArgument::gameMode));
      register(p_235385_, "time", TimeArgument.class, new TimeArgument.Info());
      register(p_235385_, "resource_or_tag", fixClassType(ResourceOrTagArgument.class), new ResourceOrTagArgument.Info());
      register(p_235385_, "resource_or_tag_key", fixClassType(ResourceOrTagKeyArgument.class), new ResourceOrTagKeyArgument.Info());
      register(p_235385_, "resource", fixClassType(ResourceArgument.class), new ResourceArgument.Info());
      register(p_235385_, "resource_key", fixClassType(ResourceKeyArgument.class), new ResourceKeyArgument.Info());
      register(p_235385_, "resource_selector", fixClassType(ResourceSelectorArgument.class), new ResourceSelectorArgument.Info());
      register(p_235385_, "template_mirror", TemplateMirrorArgument.class, SingletonArgumentInfo.contextFree(TemplateMirrorArgument::templateMirror));
      register(p_235385_, "template_rotation", TemplateRotationArgument.class, SingletonArgumentInfo.contextFree(TemplateRotationArgument::templateRotation));
      register(p_235385_, "heightmap", HeightmapTypeArgument.class, SingletonArgumentInfo.contextFree(HeightmapTypeArgument::heightmap));
      register(p_235385_, "loot_table", ResourceOrIdArgument.LootTableArgument.class, SingletonArgumentInfo.contextAware(ResourceOrIdArgument::lootTable));
      register(
         p_235385_, "loot_predicate", ResourceOrIdArgument.LootPredicateArgument.class, SingletonArgumentInfo.contextAware(ResourceOrIdArgument::lootPredicate)
      );
      register(
         p_235385_, "loot_modifier", ResourceOrIdArgument.LootModifierArgument.class, SingletonArgumentInfo.contextAware(ResourceOrIdArgument::lootModifier)
      );
      register(p_235385_, "dialog", ResourceOrIdArgument.DialogArgument.class, SingletonArgumentInfo.contextAware(ResourceOrIdArgument::dialog));
      return register(p_235385_, "uuid", UuidArgument.class, SingletonArgumentInfo.contextFree(UuidArgument::uuid));
   }

   private static <T extends ArgumentType<?>> Class<T> fixClassType(Class<? super T> p_235396_) {
      return (Class<T>)p_235396_;
   }

   public static boolean isClassRecognized(Class<?> p_235392_) {
      return BY_CLASS.containsKey(p_235392_);
   }

   public static <A extends ArgumentType<?>> ArgumentTypeInfo<A, ?> byClass(A p_235383_) {
      ArgumentTypeInfo<?, ?> argumenttypeinfo = BY_CLASS.get(p_235383_.getClass());
      if (argumenttypeinfo == null) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "Unrecognized argument type %s (%s)", p_235383_, p_235383_.getClass()));
      } else {
         return (ArgumentTypeInfo<A, ?>)argumenttypeinfo;
      }
   }

   public static <A extends ArgumentType<?>> ArgumentTypeInfo.Template<A> unpack(A p_235394_) {
      return byClass(p_235394_).unpack(p_235394_);
   }
}
