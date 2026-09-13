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
      p_390099_ -> Component.translatableEscape("Invalid pattern", p_390099_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_266758_) {
      p_266758_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("spawn_armor_trims")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(
                  Commands.literal("*_lag_my_game")
                     .executes(
                        p_390098_ -> spawnAllArmorTrims(
                           (CommandSourceStack)p_390098_.getSource(), ((CommandSourceStack)p_390098_.getSource()).getPlayerOrException()
                        )
                     )
               ))
            .then(
               Commands.argument("pattern", ResourceKeyArgument.key(Registries.TRIM_PATTERN))
                  .executes(
                     p_390097_ -> spawnArmorTrim(
                        (CommandSourceStack)p_390097_.getSource(),
                        ((CommandSourceStack)p_390097_.getSource()).getPlayerOrException(),
                        ResourceKeyArgument.getRegistryKey(p_390097_, "pattern", Registries.TRIM_PATTERN, ERROR_INVALID_PATTERN)
                     )
                  )
            )
      );
   }

   private static int spawnAllArmorTrims(CommandSourceStack p_391426_, Player p_392292_) {
      return spawnArmorTrims(p_391426_, p_392292_, p_391426_.getServer().registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).listElements());
   }

   private static int spawnArmorTrim(CommandSourceStack p_392029_, Player p_392364_, ResourceKey<TrimPattern> p_395572_) {
      return spawnArmorTrims(
         p_392029_, p_392364_, Stream.of(p_392029_.getServer().registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).get(p_395572_).orElseThrow())
      );
   }

   private static int spawnArmorTrims(CommandSourceStack p_266993_, Player p_266983_, Stream<Holder.Reference<TrimPattern>> p_395212_) {
      ServerLevel serverlevel = p_266993_.getLevel();
      List<Holder.Reference<TrimPattern>> list = p_395212_.sorted(Comparator.comparing(p_390096_ -> TRIM_PATTERN_ORDER.applyAsInt(p_390096_.key()))).toList();
      List<Holder.Reference<TrimMaterial>> list1 = serverlevel.registryAccess()
         .lookupOrThrow(Registries.TRIM_MATERIAL)
         .listElements()
         .sorted(Comparator.comparing(p_390102_ -> TRIM_MATERIAL_ORDER.applyAsInt(p_390102_.key())))
         .toList();
      List<Holder.Reference<Item>> list2 = findEquippableItemsWithAssets(serverlevel.registryAccess().lookupOrThrow(Registries.ITEM));
      BlockPos blockpos = p_266983_.blockPosition().relative(p_266983_.getDirection(), 5);
      double d0 = 3.0;

      for (int i = 0; i < list1.size(); i++) {
         Holder.Reference<TrimMaterial> reference = list1.get(i);

         for (int j = 0; j < list.size(); j++) {
            Holder.Reference<TrimPattern> reference1 = list.get(j);
            ArmorTrim armortrim = new ArmorTrim(reference, reference1);

            for (int k = 0; k < list2.size(); k++) {
               Holder.Reference<Item> reference2 = list2.get(k);
               double d1 = blockpos.getX() + 0.5 - k * 3.0;
               double d2 = blockpos.getY() + 0.5 + i * 3.0;
               double d3 = blockpos.getZ() + 0.5 + j * 10;
               ArmorStand armorstand = new ArmorStand(serverlevel, d1, d2, d3);
               armorstand.setYRot(180.0F);
               armorstand.setNoGravity(true);
               ItemStack itemstack = new ItemStack(reference2);
               Equippable equippable = Objects.requireNonNull(itemstack.get(DataComponents.EQUIPPABLE));
               itemstack.set(DataComponents.TRIM, armortrim);
               armorstand.setItemSlot(equippable.slot(), itemstack);
               if (k == 0) {
                  armorstand.setCustomName(
                     armortrim.pattern().value().copyWithStyle(armortrim.material()).copy().append(" & ").append(armortrim.material().value().description())
                  );
                  armorstand.setCustomNameVisible(true);
               } else {
                  armorstand.setInvisible(true);
               }

               serverlevel.addFreshEntity(armorstand);
            }
         }
      }

      p_266993_.sendSuccess(() -> Component.literal("Armorstands with trimmed armor spawned around you"), true);
      return 1;
   }

   private static List<Holder.Reference<Item>> findEquippableItemsWithAssets(HolderLookup<Item> p_394557_) {
      List<Holder.Reference<Item>> list = new ArrayList<>();
      p_394557_.listElements().forEach(p_390101_ -> {
         Equippable equippable = p_390101_.value().components().get(DataComponents.EQUIPPABLE);
         if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR && equippable.assetId().isPresent()) {
            list.add((Holder.Reference<Item>)p_390101_);
         }
      });
      return list;
   }
}
