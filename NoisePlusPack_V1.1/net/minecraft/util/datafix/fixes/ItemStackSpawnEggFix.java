package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemStackSpawnEggFix extends DataFix {
   private final String itemType;
   private static final Map<String, String> MAP = (Map<String, String>)DataFixUtils.make(Maps.newHashMap(), p_261434_ -> {
      p_261434_.put("minecraft:bat", "minecraft:bat_spawn_egg");
      p_261434_.put("minecraft:blaze", "minecraft:blaze_spawn_egg");
      p_261434_.put("minecraft:cave_spider", "minecraft:cave_spider_spawn_egg");
      p_261434_.put("minecraft:chicken", "minecraft:chicken_spawn_egg");
      p_261434_.put("minecraft:cow", "minecraft:cow_spawn_egg");
      p_261434_.put("minecraft:creeper", "minecraft:creeper_spawn_egg");
      p_261434_.put("minecraft:donkey", "minecraft:donkey_spawn_egg");
      p_261434_.put("minecraft:elder_guardian", "minecraft:elder_guardian_spawn_egg");
      p_261434_.put("minecraft:ender_dragon", "minecraft:ender_dragon_spawn_egg");
      p_261434_.put("minecraft:enderman", "minecraft:enderman_spawn_egg");
      p_261434_.put("minecraft:endermite", "minecraft:endermite_spawn_egg");
      p_261434_.put("minecraft:evocation_illager", "minecraft:evocation_illager_spawn_egg");
      p_261434_.put("minecraft:ghast", "minecraft:ghast_spawn_egg");
      p_261434_.put("minecraft:guardian", "minecraft:guardian_spawn_egg");
      p_261434_.put("minecraft:horse", "minecraft:horse_spawn_egg");
      p_261434_.put("minecraft:husk", "minecraft:husk_spawn_egg");
      p_261434_.put("minecraft:iron_golem", "minecraft:iron_golem_spawn_egg");
      p_261434_.put("minecraft:llama", "minecraft:llama_spawn_egg");
      p_261434_.put("minecraft:magma_cube", "minecraft:magma_cube_spawn_egg");
      p_261434_.put("minecraft:mooshroom", "minecraft:mooshroom_spawn_egg");
      p_261434_.put("minecraft:mule", "minecraft:mule_spawn_egg");
      p_261434_.put("minecraft:ocelot", "minecraft:ocelot_spawn_egg");
      p_261434_.put("minecraft:pufferfish", "minecraft:pufferfish_spawn_egg");
      p_261434_.put("minecraft:parrot", "minecraft:parrot_spawn_egg");
      p_261434_.put("minecraft:pig", "minecraft:pig_spawn_egg");
      p_261434_.put("minecraft:polar_bear", "minecraft:polar_bear_spawn_egg");
      p_261434_.put("minecraft:rabbit", "minecraft:rabbit_spawn_egg");
      p_261434_.put("minecraft:sheep", "minecraft:sheep_spawn_egg");
      p_261434_.put("minecraft:shulker", "minecraft:shulker_spawn_egg");
      p_261434_.put("minecraft:silverfish", "minecraft:silverfish_spawn_egg");
      p_261434_.put("minecraft:skeleton", "minecraft:skeleton_spawn_egg");
      p_261434_.put("minecraft:skeleton_horse", "minecraft:skeleton_horse_spawn_egg");
      p_261434_.put("minecraft:slime", "minecraft:slime_spawn_egg");
      p_261434_.put("minecraft:snow_golem", "minecraft:snow_golem_spawn_egg");
      p_261434_.put("minecraft:spider", "minecraft:spider_spawn_egg");
      p_261434_.put("minecraft:squid", "minecraft:squid_spawn_egg");
      p_261434_.put("minecraft:stray", "minecraft:stray_spawn_egg");
      p_261434_.put("minecraft:turtle", "minecraft:turtle_spawn_egg");
      p_261434_.put("minecraft:vex", "minecraft:vex_spawn_egg");
      p_261434_.put("minecraft:villager", "minecraft:villager_spawn_egg");
      p_261434_.put("minecraft:vindication_illager", "minecraft:vindication_illager_spawn_egg");
      p_261434_.put("minecraft:witch", "minecraft:witch_spawn_egg");
      p_261434_.put("minecraft:wither", "minecraft:wither_spawn_egg");
      p_261434_.put("minecraft:wither_skeleton", "minecraft:wither_skeleton_spawn_egg");
      p_261434_.put("minecraft:wolf", "minecraft:wolf_spawn_egg");
      p_261434_.put("minecraft:zombie", "minecraft:zombie_spawn_egg");
      p_261434_.put("minecraft:zombie_horse", "minecraft:zombie_horse_spawn_egg");
      p_261434_.put("minecraft:zombie_pigman", "minecraft:zombie_pigman_spawn_egg");
      p_261434_.put("minecraft:zombie_villager", "minecraft:zombie_villager_spawn_egg");
   });

   public ItemStackSpawnEggFix(Schema p_262024_, boolean p_262020_, String p_261847_) {
      super(p_262024_, p_262020_);
      this.itemType = p_261847_;
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<Pair<String, String>> opticfinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<String> opticfinder1 = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
      OpticFinder<?> opticfinder2 = type.findField("tag");
      OpticFinder<?> opticfinder3 = opticfinder2.type().findField("EntityTag");
      return this.fixTypeEverywhereTyped("ItemInstanceSpawnEggFix" + this.getOutputSchema().getVersionKey(), type, p_16105_ -> {
         Optional<Pair<String, String>> optional = p_16105_.getOptional(opticfinder);
         if (optional.isPresent() && Objects.equals(optional.get().getSecond(), this.itemType)) {
            Typed<?> typed = p_16105_.getOrCreateTyped(opticfinder2);
            Typed<?> typed1 = typed.getOrCreateTyped(opticfinder3);
            Optional<String> optional1 = typed1.getOptional(opticfinder1);
            if (optional1.isPresent()) {
               return p_16105_.set(opticfinder, Pair.of(References.ITEM_NAME.typeName(), MAP.getOrDefault(optional1.get(), "minecraft:pig_spawn_egg")));
            }
         }

         return p_16105_;
      });
   }
}
