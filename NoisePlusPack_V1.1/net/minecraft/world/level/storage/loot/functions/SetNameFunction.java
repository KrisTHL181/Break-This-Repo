package net.minecraft.world.level.storage.loot.functions;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
   public static final MapCodec<SetNameFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_327622_ -> commonFields(p_327622_)
         .and(
            p_327622_.group(
               ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(p_297155_ -> p_297155_.name),
               LootContext.EntityTarget.CODEC.optionalFieldOf("entity").forGetter(p_297165_ -> p_297165_.resolutionContext),
               SetNameFunction.Target.CODEC.optionalFieldOf("target", SetNameFunction.Target.CUSTOM_NAME).forGetter(p_327618_ -> p_327618_.target)
            )
         )
         .apply(p_327622_, SetNameFunction::new)
   );
   private final Optional<Component> name;
   private final Optional<LootContext.EntityTarget> resolutionContext;
   private final SetNameFunction.Target target;

   private SetNameFunction(
      List<LootItemCondition> p_298434_, Optional<Component> p_299902_, Optional<LootContext.EntityTarget> p_300668_, SetNameFunction.Target p_327809_
   ) {
      super(p_298434_);
      this.name = p_299902_;
      this.resolutionContext = p_300668_;
      this.target = p_327809_;
   }

   @Override
   public LootItemFunctionType<SetNameFunction> getType() {
      return LootItemFunctions.SET_NAME;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return this.resolutionContext.<Set<ContextKey<?>>>map(p_450101_ -> Set.of(p_450101_.contextParam())).orElse(Set.of());
   }

   public static UnaryOperator<Component> createResolver(LootContext p_81140_, LootContext.@Nullable EntityTarget p_81141_) {
      if (p_81141_ != null) {
         Entity entity = p_81140_.getOptionalParameter(p_81141_.contextParam());
         if (entity != null) {
            CommandSourceStack commandsourcestack = entity.createCommandSourceStackForNameResolution(p_81140_.getLevel())
               .withPermission(LevelBasedPermissionSet.GAMEMASTER);
            return p_81147_ -> {
               try {
                  return ComponentUtils.updateForEntity(commandsourcestack, p_81147_, entity, 0);
               } catch (CommandSyntaxException commandsyntaxexception) {
                  LOGGER.warn("Failed to resolve text component", commandsyntaxexception);
                  return p_81147_;
               }
            };
         }
      }

      return p_81152_ -> p_81152_;
   }

   @Override
   public ItemStack run(ItemStack p_81137_, LootContext p_81138_) {
      this.name.ifPresent(p_327617_ -> p_81137_.set(this.target.component(), createResolver(p_81138_, this.resolutionContext.orElse(null)).apply(p_327617_)));
      return p_81137_;
   }

   public static LootItemConditionalFunction.Builder<?> setName(Component p_165460_, SetNameFunction.Target p_336143_) {
      return simpleBuilder(p_327621_ -> new SetNameFunction(p_327621_, Optional.of(p_165460_), Optional.empty(), p_336143_));
   }

   public static LootItemConditionalFunction.Builder<?> setName(Component p_165458_, SetNameFunction.Target p_335783_, LootContext.EntityTarget p_332047_) {
      return simpleBuilder(p_327626_ -> new SetNameFunction(p_327626_, Optional.of(p_165458_), Optional.of(p_332047_), p_335783_));
   }

   public enum Target implements StringRepresentable {
      CUSTOM_NAME("custom_name"),
      ITEM_NAME("item_name");

      public static final Codec<SetNameFunction.Target> CODEC = StringRepresentable.fromEnum(SetNameFunction.Target::values);
      private final String name;

      Target(final String p_333129_) {
         this.name = p_333129_;
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
