package net.minecraft.client.data.models.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelTemplate {
   private final Optional<Identifier> model;
   private final Set<TextureSlot> requiredSlots;
   private final Optional<String> suffix;

   public ModelTemplate(Optional<Identifier> p_378830_, Optional<String> p_378577_, TextureSlot... p_376741_) {
      this.model = p_378830_;
      this.suffix = p_378577_;
      this.requiredSlots = ImmutableSet.copyOf(p_376741_);
   }

   public Identifier getDefaultModelLocation(Block p_376208_) {
      return ModelLocationUtils.getModelLocation(p_376208_, this.suffix.orElse(""));
   }

   public Identifier create(Block p_460196_, TextureMapping p_375480_, BiConsumer<Identifier, ModelInstance> p_378140_) {
      return this.create(ModelLocationUtils.getModelLocation(p_460196_, this.suffix.orElse("")), p_375480_, p_378140_);
   }

   public Identifier createWithSuffix(Block p_377656_, String p_376203_, TextureMapping p_376258_, BiConsumer<Identifier, ModelInstance> p_375987_) {
      return this.create(ModelLocationUtils.getModelLocation(p_377656_, p_376203_ + this.suffix.orElse("")), p_376258_, p_375987_);
   }

   public Identifier createWithOverride(Block p_377908_, String p_378134_, TextureMapping p_376015_, BiConsumer<Identifier, ModelInstance> p_378800_) {
      return this.create(ModelLocationUtils.getModelLocation(p_377908_, p_378134_), p_376015_, p_378800_);
   }

   public Identifier create(Item p_459753_, TextureMapping p_378305_, BiConsumer<Identifier, ModelInstance> p_376589_) {
      return this.create(ModelLocationUtils.getModelLocation(p_459753_, this.suffix.orElse("")), p_378305_, p_376589_);
   }

   public Identifier create(Identifier p_461037_, TextureMapping p_377818_, BiConsumer<Identifier, ModelInstance> p_375928_) {
      Map<TextureSlot, Identifier> map = this.createMap(p_377818_);
      p_375928_.accept(p_461037_, () -> {
         JsonObject jsonobject = new JsonObject();
         this.model.ifPresent(p_447949_ -> jsonobject.addProperty("parent", p_447949_.toString()));
         if (!map.isEmpty()) {
            JsonObject jsonobject1 = new JsonObject();
            map.forEach((p_447946_, p_447947_) -> jsonobject1.addProperty(p_447946_.getId(), p_447947_.toString()));
            jsonobject.add("textures", jsonobject1);
         }

         return jsonobject;
      });
      return p_461037_;
   }

   private Map<TextureSlot, Identifier> createMap(TextureMapping p_378668_) {
      return Streams.concat(new Stream[]{this.requiredSlots.stream(), p_378668_.getForced()})
         .collect(ImmutableMap.toImmutableMap(Function.identity(), p_378668_::get));
   }
}
