package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import org.jspecify.annotations.Nullable;

public class ItemSpawnEggFix extends DataFix {
   private static final @Nullable String[] ID_TO_ENTITY = (String[])DataFixUtils.make(new String[256], map -> {
      map[1] = "Item";
      map[2] = "XPOrb";
      map[7] = "ThrownEgg";
      map[8] = "LeashKnot";
      map[9] = "Painting";
      map[10] = "Arrow";
      map[11] = "Snowball";
      map[12] = "Fireball";
      map[13] = "SmallFireball";
      map[14] = "ThrownEnderpearl";
      map[15] = "EyeOfEnderSignal";
      map[16] = "ThrownPotion";
      map[17] = "ThrownExpBottle";
      map[18] = "ItemFrame";
      map[19] = "WitherSkull";
      map[20] = "PrimedTnt";
      map[21] = "FallingSand";
      map[22] = "FireworksRocketEntity";
      map[23] = "TippedArrow";
      map[24] = "SpectralArrow";
      map[25] = "ShulkerBullet";
      map[26] = "DragonFireball";
      map[30] = "ArmorStand";
      map[41] = "Boat";
      map[42] = "MinecartRideable";
      map[43] = "MinecartChest";
      map[44] = "MinecartFurnace";
      map[45] = "MinecartTNT";
      map[46] = "MinecartHopper";
      map[47] = "MinecartSpawner";
      map[40] = "MinecartCommandBlock";
      map[50] = "Creeper";
      map[51] = "Skeleton";
      map[52] = "Spider";
      map[53] = "Giant";
      map[54] = "Zombie";
      map[55] = "Slime";
      map[56] = "Ghast";
      map[57] = "PigZombie";
      map[58] = "Enderman";
      map[59] = "CaveSpider";
      map[60] = "Silverfish";
      map[61] = "Blaze";
      map[62] = "LavaSlime";
      map[63] = "EnderDragon";
      map[64] = "WitherBoss";
      map[65] = "Bat";
      map[66] = "Witch";
      map[67] = "Endermite";
      map[68] = "Guardian";
      map[69] = "Shulker";
      map[90] = "Pig";
      map[91] = "Sheep";
      map[92] = "Cow";
      map[93] = "Chicken";
      map[94] = "Squid";
      map[95] = "Wolf";
      map[96] = "MushroomCow";
      map[97] = "SnowMan";
      map[98] = "Ozelot";
      map[99] = "VillagerGolem";
      map[100] = "EntityHorse";
      map[101] = "Rabbit";
      map[120] = "Villager";
      map[200] = "EnderCrystal";
   });

   public ItemSpawnEggFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   public TypeRewriteRule makeRule() {
      Schema inputSchema = this.getInputSchema();
      Type<?> itemStackType = inputSchema.getType(References.ITEM_STACK);
      OpticFinder<Pair<String, String>> idFinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<String> entityIdFinder = DSL.fieldFinder("id", DSL.string());
      OpticFinder<?> tagFinder = itemStackType.findField("tag");
      OpticFinder<?> entityTagFinder = tagFinder.type().findField("EntityTag");
      OpticFinder<?> entityFinder = DSL.typeFinder(inputSchema.getTypeRaw(References.ENTITY));
      return this.fixTypeEverywhereTyped(
         "ItemSpawnEggFix",
         itemStackType,
         input -> {
            Optional<Pair<String, String>> id = input.getOptional(idFinder);
            if (id.isPresent() && Objects.equals(id.get().getSecond(), "minecraft:spawn_egg")) {
               Dynamic<?> rest = (Dynamic<?>)input.get(DSL.remainderFinder());
               short damage = rest.get("Damage").asShort((short)0);
               Optional<? extends Typed<?>> tagOptional = input.getOptionalTyped(tagFinder);
               Optional<? extends Typed<?>> entityTreeOptional = tagOptional.flatMap(value -> value.getOptionalTyped(entityTagFinder));
               Optional<? extends Typed<?>> entityOptional = entityTreeOptional.flatMap(value -> value.getOptionalTyped(entityFinder));
               Optional<String> oldId = entityOptional.flatMap(value -> value.getOptional(entityIdFinder));
               Typed<?> output = input;
               String entityName = ID_TO_ENTITY[damage & 0xFF];
               if (entityName != null && (oldId.isEmpty() || !Objects.equals(oldId.get(), entityName))) {
                  Typed<?> tag = input.getOrCreateTyped(tagFinder);
                  Dynamic<?> entityTag = (Dynamic<?>)DataFixUtils.orElse(
                     tag.getOptionalTyped(entityTagFinder).map(entityTree -> (Dynamic)entityTree.write().getOrThrow()), rest.emptyMap()
                  );
                  entityTag = entityTag.set("id", entityTag.createString(entityName));
                  output = output.set(tagFinder, ExtraDataFixUtils.readAndSet(tag, entityTagFinder, entityTag));
               }

               if (damage != 0) {
                  rest = rest.set("Damage", rest.createShort((short)0));
                  output = output.set(DSL.remainderFinder(), rest);
               }

               return output;
            } else {
               return input;
            }
         }
      );
   }
}
