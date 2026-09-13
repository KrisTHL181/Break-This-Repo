package net.minecraft.advancements;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CacheableFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public record AdvancementRewards(int experience, List<ResourceKey<LootTable>> loot, List<ResourceKey<Recipe<?>>> recipes, Optional<CacheableFunction> function) {
   public static final Codec<AdvancementRewards> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.INT.optionalFieldOf("experience", 0).forGetter(AdvancementRewards::experience),
            LootTable.KEY_CODEC.listOf().optionalFieldOf("loot", List.of()).forGetter(AdvancementRewards::loot),
            Recipe.KEY_CODEC.listOf().optionalFieldOf("recipes", List.of()).forGetter(AdvancementRewards::recipes),
            CacheableFunction.CODEC.optionalFieldOf("function").forGetter(AdvancementRewards::function)
         )
         .apply(i, AdvancementRewards::new)
   );
   public static final AdvancementRewards EMPTY = new AdvancementRewards(0, List.of(), List.of(), Optional.empty());

   public void grant(final ServerPlayer player) {
      player.giveExperiencePoints(this.experience);
      ServerLevel level = player.level();
      MinecraftServer server = level.getServer();
      LootParams params = new LootParams.Builder(level)
         .withParameter(LootContextParams.THIS_ENTITY, player)
         .withParameter(LootContextParams.ORIGIN, player.position())
         .create(LootContextParamSets.ADVANCEMENT_REWARD);
      boolean changes = false;

      for (ResourceKey<LootTable> lootTable : this.loot) {
         ObjectListIterator var8 = server.reloadableRegistries().getLootTable(lootTable).getRandomItems(params).iterator();

         while (var8.hasNext()) {
            ItemStack itemStack = (ItemStack)var8.next();
            if (player.addItem(itemStack)) {
               level.playSound(
                  null,
                  player.getX(),
                  player.getY(),
                  player.getZ(),
                  SoundEvents.ITEM_PICKUP,
                  SoundSource.PLAYERS,
                  0.2F,
                  ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
               );
               changes = true;
            } else {
               ItemEntity drop = player.drop(itemStack, false);
               if (drop != null) {
                  drop.setNoPickUpDelay();
                  drop.setTarget(player.getUUID());
               }
            }
         }
      }

      if (changes) {
         player.containerMenu.broadcastChanges();
      }

      if (!this.recipes.isEmpty()) {
         player.awardRecipesByKey(this.recipes);
      }

      this.function
         .flatMap(function -> function.get(server.getFunctions()))
         .ifPresent(
            function -> server.getFunctions()
               .execute(
                  (CommandFunction<CommandSourceStack>)function,
                  player.createCommandSourceStack().withSuppressedOutput().withPermission(LevelBasedPermissionSet.GAMEMASTER)
               )
         );
   }

   public static class Builder {
      private int experience;
      private final com.google.common.collect.ImmutableList.Builder<ResourceKey<LootTable>> loot = ImmutableList.builder();
      private final com.google.common.collect.ImmutableList.Builder<ResourceKey<Recipe<?>>> recipes = ImmutableList.builder();
      private Optional<Identifier> function = Optional.empty();

      public static AdvancementRewards.Builder experience(final int amount) {
         return new AdvancementRewards.Builder().addExperience(amount);
      }

      public AdvancementRewards.Builder addExperience(final int amount) {
         this.experience += amount;
         return this;
      }

      public static AdvancementRewards.Builder loot(final ResourceKey<LootTable> id) {
         return new AdvancementRewards.Builder().addLootTable(id);
      }

      public AdvancementRewards.Builder addLootTable(final ResourceKey<LootTable> id) {
         this.loot.add(id);
         return this;
      }

      public static AdvancementRewards.Builder recipe(final ResourceKey<Recipe<?>> id) {
         return new AdvancementRewards.Builder().addRecipe(id);
      }

      public AdvancementRewards.Builder addRecipe(final ResourceKey<Recipe<?>> id) {
         this.recipes.add(id);
         return this;
      }

      public static AdvancementRewards.Builder function(final Identifier id) {
         return new AdvancementRewards.Builder().runs(id);
      }

      public AdvancementRewards.Builder runs(final Identifier function) {
         this.function = Optional.of(function);
         return this;
      }

      public AdvancementRewards build() {
         return new AdvancementRewards(this.experience, this.loot.build(), this.recipes.build(), this.function.map(CacheableFunction::new));
      }
   }
}
