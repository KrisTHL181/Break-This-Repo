package net.minecraft.client.resources.model.cuboid;

import com.mojang.math.Quadrant;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.joml.Vector3f;

public class MissingCuboidModel {
   private static final String TEXTURE_SLOT = "missingno";
   public static final Identifier LOCATION = Identifier.withDefaultNamespace("builtin/missing");

   public static UnbakedModel missingModel() {
      CuboidFace.UVs fullFaceUv = new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F);
      Map<Direction, CuboidFace> faces = Util.makeEnumMap(Direction.class, direction -> new CuboidFace(direction, -1, "missingno", fullFaceUv, Quadrant.R0));
      CuboidModelElement cube = new CuboidModelElement(new Vector3f(0.0F, 0.0F, 0.0F), new Vector3f(16.0F, 16.0F, 16.0F), faces);
      return new CuboidModel(
         new UnbakedCuboidGeometry(List.of(cube)),
         null,
         null,
         ItemTransforms.NO_TRANSFORMS,
         new TextureSlots.Data.Builder()
            .addReference("particle", "missingno")
            .addTexture("missingno", new Material(MissingTextureAtlasSprite.getLocation()))
            .build(),
         null
      );
   }
}
