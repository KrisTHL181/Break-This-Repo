package net.minecraft.commands.arguments.selector;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.CompilableString;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntitySelector {
   public static final int INFINITE = Integer.MAX_VALUE;
   public static final BiConsumer<Vec3, List<? extends Entity>> ORDER_ARBITRARY = (p, c) -> {};
   private static final EntityTypeTest<Entity, ?> ANY_TYPE = new EntityTypeTest<Entity, Entity>() {
      public Entity tryCast(final Entity entity) {
         return entity;
      }

      @Override
      public Class<? extends Entity> getBaseClass() {
         return Entity.class;
      }
   };
   public static final Codec<CompilableString<EntitySelector>> COMPILABLE_CODEC = CompilableString.codec(
      new CompilableString.CommandParserHelper<EntitySelector>() {
         protected EntitySelector parse(final StringReader reader) throws CommandSyntaxException {
            return new EntitySelectorParser(reader, true).parse();
         }

         @Override
         protected String errorMessage(final String original, final CommandSyntaxException exception) {
            return "Invalid selector component: " + original + ": " + exception.getMessage();
         }
      }
   );
   private final int maxResults;
   private final boolean includesEntities;
   private final boolean worldLimited;
   private final List<Predicate<Entity>> contextFreePredicates;
   private final MinMaxBounds.@Nullable Doubles range;
   private final Function<Vec3, Vec3> position;
   private final @Nullable AABB aabb;
   private final BiConsumer<Vec3, List<? extends Entity>> order;
   private final boolean currentEntity;
   private final @Nullable String playerName;
   private final @Nullable UUID entityUUID;
   private final EntityTypeTest<Entity, ?> type;
   private final boolean usesSelector;

   public EntitySelector(
      final int maxResults,
      final boolean includesEntities,
      final boolean worldLimited,
      final List<Predicate<Entity>> contextFreePredicates,
      final MinMaxBounds.@Nullable Doubles range,
      final Function<Vec3, Vec3> position,
      final @Nullable AABB aabb,
      final BiConsumer<Vec3, List<? extends Entity>> order,
      final boolean currentEntity,
      final @Nullable String playerName,
      final @Nullable UUID entityUUID,
      final @Nullable EntityType<?> type,
      final boolean usesSelector
   ) {
      this.maxResults = maxResults;
      this.includesEntities = includesEntities;
      this.worldLimited = worldLimited;
      this.contextFreePredicates = contextFreePredicates;
      this.range = range;
      this.position = position;
      this.aabb = aabb;
      this.order = order;
      this.currentEntity = currentEntity;
      this.playerName = playerName;
      this.entityUUID = entityUUID;
      this.type = type == null ? ANY_TYPE : type;
      this.usesSelector = usesSelector;
   }

   public int getMaxResults() {
      return this.maxResults;
   }

   public boolean includesEntities() {
      return this.includesEntities;
   }

   public boolean isSelfSelector() {
      return this.currentEntity;
   }

   public boolean isWorldLimited() {
      return this.worldLimited;
   }

   public boolean usesSelector() {
      return this.usesSelector;
   }

   private void checkPermissions(final CommandSourceStack sender) throws CommandSyntaxException {
      if (this.usesSelector && !sender.permissions().hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS)) {
         throw EntityArgument.ERROR_SELECTORS_NOT_ALLOWED.create();
      }
   }

   public Entity findSingleEntity(final CommandSourceStack sender) throws CommandSyntaxException {
      this.checkPermissions(sender);
      List<? extends Entity> entities = this.findEntities(sender);
      if (entities.isEmpty()) {
         throw EntityArgument.NO_ENTITIES_FOUND.create();
      } else if (entities.size() > 1) {
         throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
      } else {
         return entities.get(0);
      }
   }

   public List<? extends Entity> findEntities(final CommandSourceStack sender) throws CommandSyntaxException {
      this.checkPermissions(sender);
      if (!this.includesEntities) {
         return this.findPlayers(sender);
      }

      if (this.playerName != null) {
         ServerPlayer result = sender.getServer().getPlayerList().getPlayerByName(this.playerName);
         return result == null ? List.of() : List.of(result);
      }

      if (this.entityUUID != null) {
         for (ServerLevel level : sender.getServer().getAllLevels()) {
            Entity entity = level.getEntity(this.entityUUID);
            if (entity != null) {
               if (entity.getType().isEnabled(sender.enabledFeatures())) {
                  return List.of(entity);
               }
               break;
            }
         }

         return List.of();
      } else {
         Vec3 pos = this.position.apply(sender.getPosition());
         AABB absoluteAabb = this.getAbsoluteAabb(pos);
         if (this.currentEntity) {
            Predicate<Entity> predicate = this.getPredicate(pos, absoluteAabb, null);
            return sender.getEntity() != null && predicate.test(sender.getEntity()) ? List.of(sender.getEntity()) : List.of();
         }

         Predicate<Entity> predicate = this.getPredicate(pos, absoluteAabb, sender.enabledFeatures());
         List<Entity> result = new ObjectArrayList();
         if (this.isWorldLimited()) {
            this.addEntities(result, sender.getLevel(), absoluteAabb, predicate);
         } else {
            for (ServerLevel level : sender.getServer().getAllLevels()) {
               this.addEntities(result, level, absoluteAabb, predicate);
            }
         }

         return this.sortAndLimit(pos, result);
      }
   }

   private void addEntities(final List<Entity> result, final ServerLevel level, final @Nullable AABB absoluteAABB, final Predicate<Entity> predicate) {
      int limit = this.getResultLimit();
      if (result.size() < limit) {
         if (absoluteAABB != null) {
            level.getEntities(this.type, absoluteAABB, predicate, result, limit);
         } else {
            level.getEntities(this.type, predicate, result, limit);
         }
      }
   }

   private int getResultLimit() {
      return this.order == ORDER_ARBITRARY ? this.maxResults : Integer.MAX_VALUE;
   }

   public ServerPlayer findSinglePlayer(final CommandSourceStack sender) throws CommandSyntaxException {
      this.checkPermissions(sender);
      List<ServerPlayer> players = this.findPlayers(sender);
      if (players.size() != 1) {
         throw EntityArgument.NO_PLAYERS_FOUND.create();
      } else {
         return players.get(0);
      }
   }

   public List<ServerPlayer> findPlayers(final CommandSourceStack sender) throws CommandSyntaxException {
      this.checkPermissions(sender);
      if (this.playerName != null) {
         ServerPlayer result = sender.getServer().getPlayerList().getPlayerByName(this.playerName);
         return result == null ? List.of() : List.of(result);
      }

      if (this.entityUUID != null) {
         ServerPlayer result = sender.getServer().getPlayerList().getPlayer(this.entityUUID);
         return result == null ? List.of() : List.of(result);
      }

      Vec3 pos = this.position.apply(sender.getPosition());
      AABB absoluteAabb = this.getAbsoluteAabb(pos);
      Predicate<Entity> predicate = this.getPredicate(pos, absoluteAabb, null);
      if (this.currentEntity) {
         return sender.getEntity() instanceof ServerPlayer player && predicate.test(player) ? List.of(player) : List.of();
      }

      int limit = this.getResultLimit();
      List<ServerPlayer> result;
      if (this.isWorldLimited()) {
         result = sender.getLevel().getPlayers(predicate, limit);
      } else {
         result = new ObjectArrayList();

         for (ServerPlayer player : sender.getServer().getPlayerList().getPlayers()) {
            if (predicate.test(player)) {
               result.add(player);
               if (result.size() >= limit) {
                  return result;
               }
            }
         }
      }

      return this.sortAndLimit(pos, result);
   }

   private @Nullable AABB getAbsoluteAabb(final Vec3 pos) {
      return this.aabb != null ? this.aabb.move(pos) : null;
   }

   private Predicate<Entity> getPredicate(final Vec3 pos, final @Nullable AABB absoluteAabb, final @Nullable FeatureFlagSet enabledFeatures) {
      boolean filterFeatures = enabledFeatures != null;
      boolean filterAabb = absoluteAabb != null;
      boolean filterRange = this.range != null;
      int extraCount = (filterFeatures ? 1 : 0) + (filterAabb ? 1 : 0) + (filterRange ? 1 : 0);
      List<Predicate<Entity>> completePredicates;
      if (extraCount == 0) {
         completePredicates = this.contextFreePredicates;
      } else {
         List<Predicate<Entity>> predicates = new ObjectArrayList(this.contextFreePredicates.size() + extraCount);
         predicates.addAll(this.contextFreePredicates);
         if (filterFeatures) {
            predicates.add(e -> e.getType().isEnabled(enabledFeatures));
         }

         if (filterAabb) {
            predicates.add(e -> absoluteAabb.intersects(e.getBoundingBox()));
         }

         if (filterRange) {
            predicates.add(e -> this.range.matchesSqr(e.distanceToSqr(pos)));
         }

         completePredicates = predicates;
      }

      return Util.allOf(completePredicates);
   }

   private <T extends Entity> List<T> sortAndLimit(final Vec3 pos, final List<T> result) {
      if (result.size() > 1) {
         this.order.accept(pos, result);
      }

      return result.subList(0, Math.min(this.maxResults, result.size()));
   }

   public static Component joinNames(final List<? extends Entity> entities) {
      return ComponentUtils.formatList(entities, Entity::getDisplayName);
   }
}
