package net.minecraft.client.resources.model;

import com.mojang.math.Quadrant;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.SimpleUnbakedGeometry;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class MissingBlockModel {
   private static final String TEXTURE_SLOT = "missingno";
   public static final Identifier LOCATION = Identifier.withDefaultNamespace("builtin/missing");

   public static UnbakedModel missingModel() {
      BlockElementFace.UVs blockelementface$uvs = new BlockElementFace.UVs(0.0F, 0.0F, 16.0F, 16.0F);
      Map<Direction, BlockElementFace> map = Util.makeEnumMap(
         Direction.class, p_393957_ -> new BlockElementFace(p_393957_, -1, "missingno", blockelementface$uvs, Quadrant.R0)
      );
      BlockElement blockelement = new BlockElement(new Vector3f(0.0F, 0.0F, 0.0F), new Vector3f(16.0F, 16.0F, 16.0F), map);
      return new BlockModel(
         new SimpleUnbakedGeometry(List.of(blockelement)),
         null,
         null,
         ItemTransforms.NO_TRANSFORMS,
         new TextureSlots.Data.Builder()
            .addReference("particle", "missingno")
            .addTexture("missingno", new Material(TextureAtlas.LOCATION_BLOCKS, MissingTextureAtlasSprite.getLocation()))
            .build(),
         null
      );
   }
}
