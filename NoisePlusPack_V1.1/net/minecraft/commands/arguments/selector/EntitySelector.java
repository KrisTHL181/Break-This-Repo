package net.minecraft.commands.arguments.selector;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
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
   public static final BiConsumer<Vec3, List<? extends Entity>> ORDER_ARBITRARY = (p_261404_, p_261405_) -> {};
   private static final EntityTypeTest<Entity, ?> ANY_TYPE = new EntityTypeTest<Entity, Entity>() {
      public Entity tryCast(Entity p_175109_) {
         return p_175109_;
      }

      @Override
      public Class<? extends Entity> getBaseClass() {
         return Entity.class;
      }
   };
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
      int p_121125_,
      boolean p_121126_,
      boolean p_121127_,
      List<Predicate<Entity>> p_345250_,
      MinMaxBounds.@Nullable Doubles p_458999_,
      Function<Vec3, Vec3> p_121130_,
      @Nullable AABB p_121131_,
      BiConsumer<Vec3, List<? extends Entity>> p_121132_,
      boolean p_121133_,
      @Nullable String p_121134_,
      @Nullable UUID p_121135_,
      @Nullable EntityType<?> p_121136_,
      boolean p_121137_
   ) {
      this.maxResults = p_121125_;
      this.includesEntities = p_121126_;
      this.worldLimited = p_121127_;
      this.contextFreePredicates = p_345250_;
      this.range = p_458999_;
      this.position = p_121130_;
      this.aabb = p_121131_;
      this.order = p_121132_;
      this.currentEntity = p_121133_;
      this.playerName = p_121134_;
      this.entityUUID = p_121135_;
      this.type = p_121136_ == null ? ANY_TYPE : p_121136_;
      this.usesSelector = p_121137_;
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

   private void checkPermissions(CommandSourceStack p_121169_) throws CommandSyntaxException {
      if (this.usesSelector && !p_121169_.permissions().hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS)) {
         throw EntityArgument.ERROR_SELECTORS_NOT_ALLOWED.create();
      }
   }

   public Entity findSingleEntity(CommandSourceStack p_121140_) throws CommandSyntaxException {
      this.checkPermissions(p_121140_);
      List<? extends Entity> list = this.findEntities(p_121140_);
      if (list.isEmpty()) {
         throw EntityArgument.NO_ENTITIES_FOUND.create();
      } else if (list.size() > 1) {
         throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
      } else {
         return list.get(0);
      }
   }

   public List<? extends Entity> findEntities(CommandSourceStack p_121161_) throws CommandSyntaxException {
      this.checkPermissions(p_121161_);
      if (!this.includesEntities) {
         return this.findPlayers(p_121161_);
      }

      if (this.playerName != null) {
         ServerPlayer serverplayer = p_121161_.getServer().getPlayerList().getPlayerByName(this.playerName);
         return serverplayer == null ? List.of() : List.of(serverplayer);
      }

      if (this.entityUUID != null) {
         for (ServerLevel serverlevel1 : p_121161_.getServer().getAllLevels()) {
            Entity entity = serverlevel1.getEntity(this.entityUUID);
            if (entity != null) {
               if (entity.getType().isEnabled(p_121161_.enabledFeatures())) {
                  return List.of(entity);
               }
               break;
            }
         }

         return List.of();
      } else {
         Vec3 vec3 = this.position.apply(p_121161_.getPosition());
         AABB aabb = this.getAbsoluteAabb(vec3);
         if (this.currentEntity) {
            Predicate<Entity> predicate1 = this.getPredicate(vec3, aabb, null);
            return p_121161_.getEntity() != null && predicate1.test(p_121161_.getEntity()) ? List.of(p_121161_.getEntity()) : List.of();
         }

         Predicate<Entity> predicate = this.getPredicate(vec3, aabb, p_121161_.enabledFeatures());
         List<Entity> list = new ObjectArrayList();
         if (this.isWorldLimited()) {
            this.addEntities(list, p_121161_.getLevel(), aabb, predicate);
         } else {
            for (ServerLevel serverlevel : p_121161_.getServer().getAllLevels()) {
               this.addEntities(list, serverlevel, aabb, predicate);
            }
         }

         return this.sortAndLimit(vec3, list);
      }
   }

   private void addEntities(List<Entity> p_121155_, ServerLevel p_121156_, @Nullable AABB p_343022_, Predicate<Entity> p_121158_) {
      int i = this.getResultLimit();
      if (p_121155_.size() < i) {
         if (p_343022_ != null) {
            p_121156_.getEntities(this.type, p_343022_, p_121158_, p_121155_, i);
         } else {
            p_121156_.getEntities(this.type, p_121158_, p_121155_, i);
         }
      }
   }

   private int getResultLimit() {
      return this.order == ORDER_ARBITRARY ? this.maxResults : Integer.MAX_VALUE;
   }

   public ServerPlayer findSinglePlayer(CommandSourceStack p_121164_) throws CommandSyntaxException {
      this.checkPermissions(p_121164_);
      List<ServerPlayer> list = this.findPlayers(p_121164_);
      if (list.size() != 1) {
         throw EntityArgument.NO_PLAYERS_FOUND.create();
      } else {
         return list.get(0);
      }
   }

   public List<ServerPlayer> findPlayers(CommandSourceStack p_121167_) throws CommandSyntaxException {
      this.checkPermissions(p_121167_);
      if (this.playerName != null) {
         ServerPlayer serverplayer2 = p_121167_.getServer().getPlayerList().getPlayerByName(this.playerName);
         return serverplayer2 == null ? List.of() : List.of(serverplayer2);
      }

      if (this.entityUUID != null) {
         ServerPlayer serverplayer1 = p_121167_.getServer().getPlayerList().getPlayer(this.entityUUID);
         return serverplayer1 == null ? List.of() : List.of(serverplayer1);
      }

      Vec3 vec3 = this.position.apply(p_121167_.getPosition());
      AABB aabb = this.getAbsoluteAabb(vec3);
      Predicate<Entity> predicate = this.getPredicate(vec3, aabb, null);
      if (this.currentEntity) {
         return p_121167_.getEntity() instanceof ServerPlayer serverplayer3 && predicate.test(serverplayer3) ? List.of(serverplayer3) : List.of();
      }

      int i = this.getResultLimit();
      List<ServerPlayer> list;
      if (this.isWorldLimited()) {
         list = p_121167_.getLevel().getPlayers(predicate, i);
      } else {
         list = new ObjectArrayList();

         for (ServerPlayer serverplayer : p_121167_.getServer().getPlayerList().getPlayers()) {
            if (predicate.test(serverplayer)) {
               list.add(serverplayer);
               if (list.size() >= i) {
                  return list;
               }
            }
         }
      }

      return this.sortAndLimit(vec3, list);
   }

   private @Nullable AABB getAbsoluteAabb(Vec3 p_345041_) {
      return this.aabb != null ? this.aabb.move(p_345041_) : null;
   }

   private Predicate<Entity> getPredicate(Vec3 p_121145_, @Nullable AABB p_343863_, @Nullable FeatureFlagSet p_343062_) {
      boolean flag = p_343062_ != null;
      boolean flag1 = p_343863_ != null;
      boolean flag2 = this.range != null;
      int i = (flag ? 1 : 0) + (flag1 ? 1 : 0) + (flag2 ? 1 : 0);
      List<Predicate<Entity>> list;
      if (i == 0) {
         list = this.contextFreePredicates;
      } else {
         List<Predicate<Entity>> list1 = new ObjectArrayList(this.contextFreePredicates.size() + i);
         list1.addAll(this.contextFreePredicates);
         if (flag) {
            list1.add(p_340975_ -> p_340975_.getType().isEnabled(p_343062_));
         }

         if (flag1) {
            list1.add(p_121143_ -> p_343863_.intersects(p_121143_.getBoundingBox()));
         }

         if (flag2) {
            list1.add(p_448537_ -> this.range.matchesSqr(p_448537_.distanceToSqr(p_121145_)));
         }

         list = list1;
      }

      return Util.allOf(list);
   }

   private <T extends Entity> List<T> sortAndLimit(Vec3 p_121150_, List<T> p_121151_) {
      if (p_121151_.size() > 1) {
         this.order.accept(p_121150_, p_121151_);
      }

      return p_121151_.subList(0, Math.min(this.maxResults, p_121151_.size()));
   }

   public static Component joinNames(List<? extends Entity> p_175104_) {
      return ComponentUtils.formatList(p_175104_, Entity::getDisplayName);
   }
}
