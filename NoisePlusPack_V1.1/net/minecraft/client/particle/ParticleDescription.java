package net.minecraft.client.particle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ParticleDescription {
   private final List<Identifier> textures;

   private ParticleDescription(List<Identifier> p_107281_) {
      this.textures = p_107281_;
   }

   public List<Identifier> getTextures() {
      return this.textures;
   }

   public static ParticleDescription fromJson(JsonObject p_107286_) {
      JsonArray jsonarray = GsonHelper.getAsJsonArray(p_107286_, "textures", null);
      if (jsonarray == null) {
         return new ParticleDescription(List.of());
      }

      List<Identifier> list = Streams.stream(jsonarray)
         .map(p_107284_ -> GsonHelper.convertToString(p_107284_, "texture"))
         .map(Identifier::parse)
         .collect(ImmutableList.toImmutableList());
      return new ParticleDescription(list);
   }
}
