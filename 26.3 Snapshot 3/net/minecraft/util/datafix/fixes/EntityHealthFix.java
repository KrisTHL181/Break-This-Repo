package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Sets;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.Set;

public class EntityHealthFix extends DataFix {
   private static final Set<String> ENTITIES = Sets.newHashSet(
      new String[]{
         "ArmorStand",
         "Bat",
         "Blaze",
         "CaveSpider",
         "Chicken",
         "Cow",
         "Creeper",
         "EnderDragon",
         "Enderman",
         "Endermite",
         "EntityHorse",
         "Ghast",
         "Giant",
         "Guardian",
         "LavaSlime",
         "MushroomCow",
         "Ozelot",
         "Pig",
         "PigZombie",
         "Rabbit",
         "Sheep",
         "Shulker",
         "Silverfish",
         "Skeleton",
         "Slime",
         "SnowMan",
         "Spider",
         "Squid",
         "Villager",
         "VillagerGolem",
         "Witch",
         "WitherBoss",
         "Wolf",
         "Zombie"
      }
   );

   public EntityHealthFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   public Dynamic<?> fixTag(Dynamic<?> input) {
      Optional<Number> oldHealF = input.get("HealF").asNumber().result();
      Optional<Number> oldHealth = input.get("Health").asNumber().result();
      float health;
      if (oldHealF.isPresent()) {
         health = oldHealF.get().floatValue();
         input = input.remove("HealF");
      } else {
         if (!oldHealth.isPresent()) {
            return input;
         }

         health = oldHealth.get().floatValue();
      }

      return input.set("Health", input.createFloat(health));
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "EntityHealthFix", this.getInputSchema().getType(References.ENTITY), input -> input.update(DSL.remainderFinder(), this::fixTag)
      );
   }
}
