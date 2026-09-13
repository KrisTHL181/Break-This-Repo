package net.minecraft.world.level.storage.loot.functions;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SetNameFunction extends LootItemConditionalFunction {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<SetNameFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i)
         .and(
            i.group(
               ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(f -> f.name),
               LootContext.EntityTarget.CODEC.optionalFieldOf("entity").forGetter(f -> f.resolutionContext),
               SetNameFunction.Target.CODEC.optionalFieldOf("target", SetNameFunction.Target.CUSTOM_NAME).forGetter(f -> f.target)
            )
         )
         .apply(i, SetNameFunction::new)
   );
   private final Optional<Component> name;
   private final Optional<LootContext.EntityTarget> resolutionContext;
   private final SetNameFunction.Target target;

   private SetNameFunction(
      final List<LootItemCondition> predicates,
      final Optional<Component> name,
      final Optional<LootContext.EntityTarget> resolutionContext,
      final SetNameFunction.Target target
   ) {
      super(predicates);
      this.name = name;
      this.resolutionContext = resolutionContext;
      this.target = target;
   }

   @Override
   public MapCodec<SetNameFunction> codec() {
      return MAP_CODEC;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return (Set<ContextKey<?>>)DataFixUtils.orElse(this.resolutionContext.map(target -> Set.of(target.contextParam())), Set.of());
   }

   public static UnaryOperator<Component> createResolver(final LootContext context, final LootContext.@Nullable EntityTarget entityTarget) {
      if (entityTarget != null) {
         Entity entity = context.getOptionalParameter(entityTarget.contextParam());
         if (entity != null) {
            CommandSourceStack commandSourceStack = entity.createCommandSourceStackForNameResolution(context.getLevel())
               .withPermission(LevelBasedPermissionSet.GAMEMASTER);
            ResolutionContext resolutionContext = ResolutionContext.create(commandSourceStack);
            return line -> {
               try {
                  return ComponentUtils.resolve(resolutionContext, line);
               } catch (CommandSyntaxException e) {
                  LOGGER.warn("Failed to resolve text component", e);
                  return line;
               }
            };
         }
      }

      return line -> line;
   }

   @Override
   public ItemStack run(final ItemStack itemStack, final LootContext context) {
      this.name.ifPresent(name -> itemStack.set(this.target.component(), createResolver(context, this.resolutionContext.orElse(null)).apply(name)));
      return itemStack;
   }

   public static LootItemConditionalFunction.Builder<?> setName(final Component value, final SetNameFunction.Target target) {
      return simpleBuilder(conditions -> new SetNameFunction(conditions, Optional.of(value), Optional.empty(), target));
   }

   public static LootItemConditionalFunction.Builder<?> setName(
      final Component value, final SetNameFunction.Target target, final LootContext.EntityTarget resolutionContext
   ) {
      return simpleBuilder(conditions -> new SetNameFunction(conditions, Optional.of(value), Optional.of(resolutionContext), target));
   }

   public enum Target implements StringRepresentable {
      CUSTOM_NAME("custom_name"),
      ITEM_NAME("item_name");

      public static final Codec<SetNameFunction.Target> CODEC = StringRepresentable.fromEnum(SetNameFunction.Target::values);
      private final String name;

      Target(final String name) {
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public DataComponentType<Component> component() {
         return switch (this) {
            case CUSTOM_NAME -> DataComponents.CUSTOM_NAME;
            case ITEM_NAME -> DataComponents.ITEM_NAME;
         };
      }
   }
}
