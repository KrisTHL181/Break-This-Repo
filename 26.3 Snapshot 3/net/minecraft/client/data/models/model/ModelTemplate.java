package net.minecraft.client.data.models.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModelTemplate {
   private final Optional<Identifier> model;
   private final Set<TextureSlot> requiredSlots;
   private final Optional<String> suffix;

   public ModelTemplate(final Optional<Identifier> model, final Optional<String> suffix, final TextureSlot... requiredSlots) {
      this.model = model;
      this.suffix = suffix;
      this.requiredSlots = ImmutableSet.copyOf(requiredSlots);
   }

   public Identifier getDefaultModelLocation(final Block block) {
      return ModelLocationUtils.getModelLocation(block, this.suffix.orElse(""));
   }

   public Identifier create(final Block block, final TextureMapping textures, final BiConsumer<Identifier, ModelInstance> output) {
      return this.create(ModelLocationUtils.getModelLocation(block, this.suffix.orElse("")), textures, output);
   }

   public Identifier createWithSuffix(
      final Block block, final String extraSuffix, final TextureMapping textures, final BiConsumer<Identifier, ModelInstance> output
   ) {
      return this.create(ModelLocationUtils.getModelLocation(block, extraSuffix + this.suffix.orElse("")), textures, output);
   }

   public Identifier createWithOverride(
      final Block block, final String suffixOverride, final TextureMapping textures, final BiConsumer<Identifier, ModelInstance> output
   ) {
      return this.create(ModelLocationUtils.getModelLocation(block, suffixOverride), textures, output);
   }

   public Identifier create(final Item item, final TextureMapping textures, final BiConsumer<Identifier, ModelInstance> output) {
      return this.create(ModelLocationUtils.getModelLocation(item, this.suffix.orElse("")), textures, output);
   }

   public Identifier create(final Identifier target, final TextureMapping textures, final BiConsumer<Identifier, ModelInstance> output) {
      Map<TextureSlot, Material> slots = this.createMap(textures);
      output.accept(target, () -> {
         JsonObject result = new JsonObject();
         this.model.ifPresent(m -> result.addProperty("parent", m.toString()));
         if (!slots.isEmpty()) {
            JsonObject textureObj = new JsonObject();
            slots.forEach((slot, value) -> {
               JsonElement valueJson = (JsonElement)Material.CODEC.encodeStart(JsonOps.INSTANCE, value).getOrThrow();
               textureObj.add(slot.getId(), valueJson);
            });
            result.add("textures", textureObj);
         }

         return result;
      });
      return target;
   }

   private Map<TextureSlot, Material> createMap(final TextureMapping mapping) {
      return Streams.concat(new Stream[]{this.requiredSlots.stream(), mapping.getForced()})
         .collect(ImmutableMap.toImmutableMap(Function.identity(), mapping::get));
   }
}
