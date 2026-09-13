package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;

public class SpawnArmorTrimsCommand {
   private static final List<ResourceKey<TrimPattern>> VANILLA_TRIM_PATTERNS = List.of(
      TrimPatterns.SENTRY,
      TrimPatterns.DUNE,
      TrimPatterns.COAST,
      TrimPatterns.WILD,
      TrimPatterns.WARD,
      TrimPatterns.EYE,
      TrimPatterns.VEX,
      TrimPatterns.TIDE,
      TrimPatterns.SNOUT,
      TrimPatterns.RIB,
      TrimPatterns.SPIRE,
      TrimPatterns.WAYFINDER,
      TrimPatterns.SHAPER,
      TrimPatterns.SILENCE,
      TrimPatterns.RAISER,
      TrimPatterns.HOST,
      TrimPatterns.FLOW,
      TrimPatterns.BOLT
   );
   private static final List<ResourceKey<TrimMaterial>> VANILLA_TRIM_MATERIALS = List.of(
      TrimMaterials.QUARTZ,
      TrimMaterials.IRON,
      TrimMaterials.NETHERITE,
      TrimMaterials.REDSTONE,
      TrimMaterials.COPPER,
      TrimMaterials.GOLD,
      TrimMaterials.EMERALD,
      TrimMaterials.DIAMOND,
      TrimMaterials.LAPIS,
      TrimMaterials.AMETHYST,
      TrimMaterials.RESIN
   );
   private static final ToIntFunction<ResourceKey<TrimPattern>> TRIM_PATTERN_ORDER = Util.createIndexLookup(VANILLA_TRIM_PATTERNS);
   private static final ToIntFunction<ResourceKey<TrimMaterial>> TRIM_MATERIAL_ORDER = Util.createIndexLookup(VANILLA_TRIM_MATERIALS);
   private static final DynamicCommandExceptionType ERROR_INVALID_PATTERN = new DynamicCommandExceptionType(
      value -> Component.translatableEscape("Invalid pattern", value)
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("spawn_armor_trims")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(
                  Commands.literal("*_lag_my_game")
                     .executes(c -> spawnAllArmorTrims((CommandSourceStack)c.getSource(), ((CommandSourceStack)c.getSource()).getPlayerOrException()))
               ))
            .then(
               Commands.argument("pattern", ResourceKeyArgument.key(Registries.TRIM_PATTERN))
                  .executes(
                     c -> spawnArmorTrim(
                        (CommandSourceStack)c.getSource(),
                        ((CommandSourceStack)c.getSource()).getPlayerOrException(),
                        ResourceKeyArgument.getRegistryKey(c, "pattern", Registries.TRIM_PATTERN, ERROR_INVALID_PATTERN)
                     )
                  )
            )
      );
   }

   private static int spawnAllArmorTrims(final CommandSourceStack source, final Player player) {
      return spawnArmorTrims(source, player, source.getServer().registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).listElements());
   }

   private static int spawnArmorTrim(final CommandSourceStack source, final Player player, final ResourceKey<TrimPattern> pattern) {
      return spawnArmorTrims(source, player, Stream.of(source.getServer().registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).get(pattern).orElseThrow()));
   }

   private static int spawnArmorTrims(final CommandSourceStack source, final Player player, final Stream<Holder.Reference<TrimPattern>> patterns) {
      ServerLevel level = source.getLevel();
      List<Holder.Reference<TrimPattern>> sortedPatterns = patterns.sorted(Comparator.comparing(h -> TRIM_PATTERN_ORDER.applyAsInt(h.key()))).toList();
      List<Holder.Reference<TrimMaterial>> sortedMaterials = level.registryAccess()
         .lookupOrThrow(Registries.TRIM_MATERIAL)
         .listElements()
         .sorted(Comparator.comparing(h -> TRIM_MATERIAL_ORDER.applyAsInt(h.key())))
         .toList();
      List<Holder.Reference<Item>> equippableItems = findEquippableItemsWithAssets(level.registryAccess().lookupOrThrow(Registries.ITEM));
      BlockPos origin = player.blockPosition().relative(player.getDirection(), 5);
      double padding = 3.0;

      for (int materialIndex = 0; materialIndex < sortedMaterials.size(); materialIndex++) {
         Holder.Reference<TrimMaterial> material = sortedMaterials.get(materialIndex);

         for (int patternIndex = 0; patternIndex < sortedPatterns.size(); patternIndex++) {
            Holder.Reference<TrimPattern> pattern = sortedPatterns.get(patternIndex);
            ArmorTrim trim = new ArmorTrim(material, pattern);

            for (int itemIndex = 0; itemIndex < equippableItems.size(); itemIndex++) {
               Holder.Reference<Item> equippableItem = equippableItems.get(itemIndex);
               double x = origin.getX() + 0.5 - itemIndex * 3.0;
               double y = origin.getY() + 0.5 + materialIndex * 3.0;
               double z = origin.getZ() + 0.5 + patternIndex * 10;
               ArmorStand armorStand = new ArmorStand(level, x, y, z);
               armorStand.setYRot(180.0F);
               armorStand.setNoGravity(true);
               ItemStack stack = new ItemStack(equippableItem);
               Equippable equippable = Objects.requireNonNull(stack.get(DataComponents.EQUIPPABLE));
               stack.set(DataComponents.TRIM, trim);
               armorStand.setItemSlot(equippable.slot(), stack);
               if (itemIndex == 0) {
                  armorStand.setCustomName(
                     trim.pattern().value().copyWithStyle(trim.material()).copy().append(" & ").append(trim.material().value().description())
                  );
                  armorStand.setCustomNameVisible(true);
               } else {
                  armorStand.setInvisible(true);
               }

               level.addFreshEntity(armorStand);
            }
         }
      }

      source.sendSuccess(() -> Component.literal("Armorstands with trimmed armor spawned around you"), true);
      return 1;
   }

   private static List<Holder.Reference<Item>> findEquippableItemsWithAssets(final HolderLookup<Item> items) {
      List<Holder.Reference<Item>> result = new ArrayList<>();
      items.listElements().forEach(item -> {
         Equippable equippable = item.components().get(DataComponents.EQUIPPABLE);
         if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR && equippable.assetId().isPresent()) {
            result.add((Holder.Reference<Item>)item);
         }
      });
      return result;
   }
}
