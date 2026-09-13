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
   private static final @Nullable String[] ID_TO_ENTITY = (String[])DataFixUtils.make(new String[256], p_326596_ -> {
      p_326596_[1] = "Item";
      p_326596_[2] = "XPOrb";
      p_326596_[7] = "ThrownEgg";
      p_326596_[8] = "LeashKnot";
      p_326596_[9] = "Painting";
      p_326596_[10] = "Arrow";
      p_326596_[11] = "Snowball";
      p_326596_[12] = "Fireball";
      p_326596_[13] = "SmallFireball";
      p_326596_[14] = "ThrownEnderpearl";
      p_326596_[15] = "EyeOfEnderSignal";
      p_326596_[16] = "ThrownPotion";
      p_326596_[17] = "ThrownExpBottle";
      p_326596_[18] = "ItemFrame";
      p_326596_[19] = "WitherSkull";
      p_326596_[20] = "PrimedTnt";
      p_326596_[21] = "FallingSand";
      p_326596_[22] = "FireworksRocketEntity";
      p_326596_[23] = "TippedArrow";
      p_326596_[24] = "SpectralArrow";
      p_326596_[25] = "ShulkerBullet";
      p_326596_[26] = "DragonFireball";
      p_326596_[30] = "ArmorStand";
      p_326596_[41] = "Boat";
      p_326596_[42] = "MinecartRideable";
      p_326596_[43] = "MinecartChest";
      p_326596_[44] = "MinecartFurnace";
      p_326596_[45] = "MinecartTNT";
      p_326596_[46] = "MinecartHopper";
      p_326596_[47] = "MinecartSpawner";
      p_326596_[40] = "MinecartCommandBlock";
      p_326596_[50] = "Creeper";
      p_326596_[51] = "Skeleton";
      p_326596_[52] = "Spider";
      p_326596_[53] = "Giant";
      p_326596_[54] = "Zombie";
      p_326596_[55] = "Slime";
      p_326596_[56] = "Ghast";
      p_326596_[57] = "PigZombie";
      p_326596_[58] = "Enderman";
      p_326596_[59] = "CaveSpider";
      p_326596_[60] = "Silverfish";
      p_326596_[61] = "Blaze";
      p_326596_[62] = "LavaSlime";
      p_326596_[63] = "EnderDragon";
      p_326596_[64] = "WitherBoss";
      p_326596_[65] = "Bat";
      p_326596_[66] = "Witch";
      p_326596_[67] = "Endermite";
      p_326596_[68] = "Guardian";
      p_326596_[69] = "Shulker";
      p_326596_[90] = "Pig";
      p_326596_[91] = "Sheep";
      p_326596_[92] = "Cow";
      p_326596_[93] = "Chicken";
      p_326596_[94] = "Squid";
      p_326596_[95] = "Wolf";
      p_326596_[96] = "MushroomCow";
      p_326596_[97] = "SnowMan";
      p_326596_[98] = "Ozelot";
      p_326596_[99] = "VillagerGolem";
      p_326596_[100] = "EntityHorse";
      p_326596_[101] = "Rabbit";
      p_326596_[120] = "Villager";
      p_326596_[200] = "EnderCrystal";
   });

   public ItemSpawnEggFix(Schema p_16034_, boolean p_16035_) {
      super(p_16034_, p_16035_);
   }

   public TypeRewriteRule makeRule() {
      Schema schema = this.getInputSchema();
      Type<?> type = schema.getType(References.ITEM_STACK);
      OpticFinder<Pair<String, String>> opticfinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<String> opticfinder1 = DSL.fieldFinder("id", DSL.string());
      OpticFinder<?> opticfinder2 = type.findField("tag");
      OpticFinder<?> opticfinder3 = opticfinder2.type().findField("EntityTag");
      OpticFinder<?> opticfinder4 = DSL.typeFinder(schema.getTypeRaw(References.ENTITY));
      return this.fixTypeEverywhereTyped(
         "ItemSpawnEggFix",
         type,
         p_390286_ -> {
            Optional<Pair<String, String>> optional = p_390286_.getOptional(opticfinder);
            if (optional.isPresent() && Objects.equals(optional.get().getSecond(), "minecraft:spawn_egg")) {
               Dynamic<?> dynamic = (Dynamic<?>)p_390286_.get(DSL.remainderFinder());
               short short1 = dynamic.get("Damage").asShort((short)0);
               Optional<? extends Typed<?>> optional1 = p_390286_.getOptionalTyped(opticfinder2);
               Optional<? extends Typed<?>> optional2 = optional1.flatMap(p_145417_ -> p_145417_.getOptionalTyped(opticfinder3));
               Optional<? extends Typed<?>> optional3 = optional2.flatMap(p_145414_ -> p_145414_.getOptionalTyped(opticfinder4));
               Optional<String> optional4 = optional3.flatMap(p_145406_ -> p_145406_.getOptional(opticfinder1));
               Typed<?> typed = p_390286_;
               String s = ID_TO_ENTITY[short1 & 0xFF];
               if (s != null && (optional4.isEmpty() || !Objects.equals(optional4.get(), s))) {
                  Typed<?> typed1 = p_390286_.getOrCreateTyped(opticfinder2);
                  Dynamic<?> dynamic1 = (Dynamic<?>)DataFixUtils.orElse(
                     typed1.getOptionalTyped(opticfinder3).map(p_390287_ -> (Dynamic)p_390287_.write().getOrThrow()), dynamic.emptyMap()
                  );
                  dynamic1 = dynamic1.set("id", dynamic1.createString(s));
                  typed = p_390286_.set(opticfinder2, ExtraDataFixUtils.readAndSet(typed1, opticfinder3, dynamic1));
               }

               if (short1 != 0) {
                  dynamic = dynamic.set("Damage", dynamic.createShort((short)0));
                  typed = typed.set(DSL.remainderFinder(), dynamic);
               }

               return typed;
            } else {
               return p_390286_;
            }
         }
      );
   }
}
