package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.Arrays;
import java.util.function.Function;

public class EntityProjectileOwnerFix extends DataFix {
   public EntityProjectileOwnerFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      Schema inputSchema = this.getInputSchema();
      return this.fixTypeEverywhereTyped("EntityProjectileOwner", inputSchema.getType(References.ENTITY), this::updateProjectiles);
   }

   private Typed<?> updateProjectiles(Typed<?> input) {
      input = this.updateEntity(input, "minecraft:egg", this::updateOwnerThrowable);
      input = this.updateEntity(input, "minecraft:ender_pearl", this::updateOwnerThrowable);
      input = this.updateEntity(input, "minecraft:experience_bottle", this::updateOwnerThrowable);
      input = this.updateEntity(input, "minecraft:snowball", this::updateOwnerThrowable);
      input = this.updateEntity(input, "minecraft:potion", this::updateOwnerThrowable);
      input = this.updateEntity(input, "minecraft:llama_spit", this::updateOwnerLlamaSpit);
      input = this.updateEntity(input, "minecraft:arrow", this::updateOwnerArrow);
      input = this.updateEntity(input, "minecraft:spectral_arrow", this::updateOwnerArrow);
      return this.updateEntity(input, "minecraft:trident", this::updateOwnerArrow);
   }

   private Dynamic<?> updateOwnerArrow(final Dynamic<?> tag) {
      long mostSignificantBits = tag.get("OwnerUUIDMost").asLong(0L);
      long leastSignificantBits = tag.get("OwnerUUIDLeast").asLong(0L);
      return this.setUUID(tag, mostSignificantBits, leastSignificantBits).remove("OwnerUUIDMost").remove("OwnerUUIDLeast");
   }

   private Dynamic<?> updateOwnerLlamaSpit(final Dynamic<?> tag) {
      OptionalDynamic<?> owner = tag.get("Owner");
      long mostSignificantBits = owner.get("OwnerUUIDMost").asLong(0L);
      long leastSignificantBits = owner.get("OwnerUUIDLeast").asLong(0L);
      return this.setUUID(tag, mostSignificantBits, leastSignificantBits).remove("Owner");
   }

   private Dynamic<?> updateOwnerThrowable(final Dynamic<?> tag) {
      String ownerKey = "owner";
      OptionalDynamic<?> owner = tag.get("owner");
      long mostSignificantBits = owner.get("M").asLong(0L);
      long leastSignificantBits = owner.get("L").asLong(0L);
      return this.setUUID(tag, mostSignificantBits, leastSignificantBits).remove("owner");
   }

   private Dynamic<?> setUUID(final Dynamic<?> tag, final long mostSignificantBits, final long leastSignificantBits) {
      String name = "OwnerUUID";
      return mostSignificantBits != 0L && leastSignificantBits != 0L
         ? tag.set("OwnerUUID", tag.createIntList(Arrays.stream(createUUIDArray(mostSignificantBits, leastSignificantBits))))
         : tag;
   }

   private static int[] createUUIDArray(final long mostSignificantBits, final long leastSignificantBits) {
      return new int[]{(int)(mostSignificantBits >> 32), (int)mostSignificantBits, (int)(leastSignificantBits >> 32), (int)leastSignificantBits};
   }

   private Typed<?> updateEntity(final Typed<?> input, final String name, final Function<Dynamic<?>, Dynamic<?>> function) {
      Type<?> oldType = this.getInputSchema().getChoiceType(References.ENTITY, name);
      Type<?> newType = this.getOutputSchema().getChoiceType(References.ENTITY, name);
      return input.updateTyped(DSL.namedChoice(name, oldType), newType, entity -> entity.update(DSL.remainderFinder(), function));
   }
}
