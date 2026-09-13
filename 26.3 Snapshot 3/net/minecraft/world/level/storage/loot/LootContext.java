package net.minecraft.world.level.storage.loot;

import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.slot.SlotSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jspecify.annotations.Nullable;

public class LootContext {
   private final LootParams params;
   private final RandomSource random;
   private final HolderGetter.Provider lootDataResolver;
   private final Set<LootContext.VisitedEntry<?>> visitedElements = Sets.newLinkedHashSet();

   private LootContext(final LootParams params, final RandomSource random, final HolderGetter.Provider lootDataResolver) {
      this.params = params;
      this.random = random;
      this.lootDataResolver = lootDataResolver;
   }

   public boolean hasParameter(final ContextKey<?> key) {
      return this.params.contextMap().has(key);
   }

   public <T> T getParameter(final ContextKey<T> key) {
      return this.params.contextMap().getOrThrow(key);
   }

   public <T> @Nullable T getOptionalParameter(final ContextKey<T> key) {
      return this.params.contextMap().getOptional(key);
   }

   public void addDynamicDrops(final Identifier location, final Consumer<ItemStack> output) {
      this.params.addDynamicDrops(location, output);
   }

   public boolean hasVisitedElement(final LootContext.VisitedEntry<?> element) {
      return this.visitedElements.contains(element);
   }

   public boolean pushVisitedElement(final LootContext.VisitedEntry<?> element) {
      return this.visitedElements.add(element);
   }

   public void popVisitedElement(final LootContext.VisitedEntry<?> element) {
      this.visitedElements.remove(element);
   }

   public HolderGetter.Provider getResolver() {
      return this.lootDataResolver;
   }

   public RandomSource getRandom() {
      return this.random;
   }

   public float getLuck() {
      return this.params.getLuck();
   }

   public ServerLevel getLevel() {
      return this.params.getLevel();
   }

   public static LootContext.VisitedEntry<LootTable> createVisitedEntry(final LootTable table) {
      return new LootContext.VisitedEntry<>(LootDataType.TABLE, table);
   }

   public static LootContext.VisitedEntry<LootItemCondition> createVisitedEntry(final LootItemCondition predicate) {
      return new LootContext.VisitedEntry<>(LootDataType.PREDICATE, predicate);
   }

   public static LootContext.VisitedEntry<LootItemFunction> createVisitedEntry(final LootItemFunction modifier) {
      return new LootContext.VisitedEntry<>(LootDataType.MODIFIER, modifier);
   }

   public static LootContext.VisitedEntry<SlotSource> createVisitedEntry(final SlotSource slotSource) {
      return new LootContext.VisitedEntry<>(LootDataType.SLOT_SOURCE, slotSource);
   }

   public enum BlockEntityTarget implements StringRepresentable, LootContextArg.SimpleGetter<BlockEntity> {
      BLOCK_ENTITY("block_entity", LootContextParams.BLOCK_ENTITY);

      private final String name;
      private final ContextKey<? extends BlockEntity> param;

      BlockEntityTarget(final String name, final ContextKey<? extends BlockEntity> param) {
         this.name = name;
         this.param = param;
      }

      @Override
      public ContextKey<? extends BlockEntity> contextParam() {
         return this.param;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }

   public static class Builder {
      private final LootParams params;
      private @Nullable RandomSource random;

      public Builder(final LootParams params) {
         this.params = params;
      }

      public LootContext.Builder withOptionalRandomSeed(final long seed) {
         if (seed != 0L) {
            this.random = RandomSource.create(seed);
         }

         return this;
      }

      public LootContext.Builder withOptionalRandomSource(final RandomSource randomSource) {
         this.random = randomSource;
         return this;
      }

      public ServerLevel getLevel() {
         return this.params.getLevel();
      }

      public LootContext create(final Optional<Identifier> randomSequenceKey) {
         ServerLevel level = this.getLevel();
         MinecraftServer server = level.getServer();
         RandomSource random = Optional.ofNullable(this.random).or(() -> randomSequenceKey.map(server::getRandomSequence)).orElseGet(level::getRandom);
         return new LootContext(this.params, random, server.reloadableRegistries().lookup());
      }
   }

   public enum EntityTarget implements StringRepresentable, LootContextArg.SimpleGetter<Entity> {
      THIS("this", LootContextParams.THIS_ENTITY),
      ATTACKER("attacker", LootContextParams.ATTACKING_ENTITY),
      DIRECT_ATTACKER("direct_attacker", LootContextParams.DIRECT_ATTACKING_ENTITY),
      ATTACKING_PLAYER("attacking_player", LootContextParams.LAST_DAMAGE_PLAYER),
      TARGET_ENTITY("target_entity", LootContextParams.TARGET_ENTITY),
      INTERACTING_ENTITY("interacting_entity", LootContextParams.INTERACTING_ENTITY);

      public static final StringRepresentable.EnumCodec<LootContext.EntityTarget> CODEC = StringRepresentable.fromEnum(LootContext.EntityTarget::values);
      private final String name;
      private final ContextKey<? extends Entity> param;

      EntityTarget(final String name, final ContextKey<? extends Entity> param) {
         this.name = name;
         this.param = param;
      }

      @Override
      public ContextKey<? extends Entity> contextParam() {
         return this.param;
      }

      public static LootContext.EntityTarget getByName(final String name) {
         LootContext.EntityTarget target = CODEC.byName(name);
         if (target != null) {
            return target;
         } else {
            throw new IllegalArgumentException("Invalid entity target " + name);
         }
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }

   public enum ItemStackTarget implements StringRepresentable, LootContextArg.SimpleGetter<ItemInstance> {
      TOOL("tool", LootContextParams.TOOL);

      private final String name;
      private final ContextKey<? extends ItemInstance> param;

      ItemStackTarget(final String name, final ContextKey<? extends ItemInstance> param) {
         this.name = name;
         this.param = param;
      }

      @Override
      public ContextKey<? extends ItemInstance> contextParam() {
         return this.param;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }

   public record VisitedEntry<T extends Validatable>(LootDataType<T> type, T value) {
   }
}
