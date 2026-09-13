package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MapRenderer {
   private static final float MAP_Z_OFFSET = -0.01F;
   private static final float DECORATION_Z_OFFSET = -0.001F;
   public static final int WIDTH = 128;
   public static final int HEIGHT = 128;
   private final TextureAtlas decorationSprites;
   private final MapTextureManager mapTextureManager;

   public MapRenderer(AtlasManager p_427168_, MapTextureManager p_364062_) {
      this.decorationSprites = p_427168_.getAtlasOrThrow(AtlasIds.MAP_DECORATIONS);
      this.mapTextureManager = p_364062_;
   }

   public void render(MapRenderState p_362792_, PoseStack p_362536_, SubmitNodeCollector p_424405_, boolean p_369246_, int p_369313_) {
      p_424405_.submitCustomGeometry(p_362536_, RenderTypes.text(p_362792_.texture), (p_427836_, p_426898_) -> {
         p_426898_.addVertex(p_427836_, 0.0F, 128.0F, -0.01F).setColor(-1).setUv(0.0F, 1.0F).setLight(p_369313_);
         p_426898_.addVertex(p_427836_, 128.0F, 128.0F, -0.01F).setColor(-1).setUv(1.0F, 1.0F).setLight(p_369313_);
         p_426898_.addVertex(p_427836_, 128.0F, 0.0F, -0.01F).setColor(-1).setUv(1.0F, 0.0F).setLight(p_369313_);
         p_426898_.addVertex(p_427836_, 0.0F, 0.0F, -0.01F).setColor(-1).setUv(0.0F, 0.0F).setLight(p_369313_);
      });
      int i = 0;

      for (MapRenderState.MapDecorationRenderState maprenderstate$mapdecorationrenderstate : p_362792_.decorations) {
         if (!p_369246_ || maprenderstate$mapdecorationrenderstate.renderOnFrame) {
            p_362536_.pushPose();
            p_362536_.translate(maprenderstate$mapdecorationrenderstate.x / 2.0F + 64.0F, maprenderstate$mapdecorationrenderstate.y / 2.0F + 64.0F, -0.02F);
            p_362536_.mulPose(Axis.ZP.rotationDegrees(maprenderstate$mapdecorationrenderstate.rot * 360 / 16.0F));
            p_362536_.scale(4.0F, 4.0F, 3.0F);
            p_362536_.translate(-0.125F, 0.125F, 0.0F);
            TextureAtlasSprite textureatlassprite = maprenderstate$mapdecorationrenderstate.atlasSprite;
            if (textureatlassprite != null) {
               float f = i * -0.001F;
               p_424405_.submitCustomGeometry(
                  p_362536_,
                  RenderTypes.text(textureatlassprite.atlasLocation()),
                  (p_430773_, p_430494_) -> {
                     p_430494_.addVertex(p_430773_, -1.0F, 1.0F, f)
                        .setColor(-1)
                        .setUv(textureatlassprite.getU0(), textureatlassprite.getV0())
                        .setLight(p_369313_);
                     p_430494_.addVertex(p_430773_, 1.0F, 1.0F, f)
                        .setColor(-1)
                        .setUv(textureatlassprite.getU1(), textureatlassprite.getV0())
                        .setLight(p_369313_);
                     p_430494_.addVertex(p_430773_, 1.0F, -1.0F, f)
                        .setColor(-1)
                        .setUv(textureatlassprite.getU1(), textureatlassprite.getV1())
                        .setLight(p_369313_);
                     p_430494_.addVertex(p_430773_, -1.0F, -1.0F, f)
                        .setColor(-1)
                        .setUv(textureatlassprite.getU0(), textureatlassprite.getV1())
                        .setLight(p_369313_);
                  }
               );
               p_362536_.popPose();
            }

            if (maprenderstate$mapdecorationrenderstate.name != null) {
               Font font = Minecraft.getInstance().font;
               float f1 = font.width(maprenderstate$mapdecorationrenderstate.name);
               float f2 = Mth.clamp(25.0F / f1, 0.0F, 0.6666667F);
               p_362536_.pushPose();
               p_362536_.translate(
                  maprenderstate$mapdecorationrenderstate.x / 2.0F + 64.0F - f1 * f2 / 2.0F,
                  maprenderstate$mapdecorationrenderstate.y / 2.0F + 64.0F + 4.0F,
                  -0.025F
               );
               p_362536_.scale(f2, f2, -1.0F);
               p_362536_.translate(0.0F, 0.0F, 0.1F);
               p_424405_.order(1)
                  .submitText(
                     p_362536_,
                     0.0F,
                     0.0F,
                     maprenderstate$mapdecorationrenderstate.name.getVisualOrderText(),
                     false,
                     Font.DisplayMode.NORMAL,
                     p_369313_,
                     -1,
                     Integer.MIN_VALUE,
                     0
                  );
               p_362536_.popPose();
            }

            i++;
         }
      }
   }

   public void extractRenderState(MapId p_369210_, MapItemSavedData p_363765_, MapRenderState p_362963_) {
      p_362963_.texture = this.mapTextureManager.prepareMapTexture(p_369210_, p_363765_);
      p_362963_.decorations.clear();

      for (MapDecoration mapdecoration : p_363765_.getDecorations()) {
         p_362963_.decorations.add(this.extractDecorationRenderState(mapdecoration));
      }
   }

   private MapRenderState.MapDecorationRenderState extractDecorationRenderState(MapDecoration p_369459_) {
      MapRenderState.MapDecorationRenderState maprenderstate$mapdecorationrenderstate = new MapRenderState.MapDecorationRenderState();
      maprenderstate$mapdecorationrenderstate.atlasSprite = this.decorationSprites.getSprite(p_369459_.getSpriteLocation());
      maprenderstate$mapdecorationrenderstate.x = p_369459_.x();
      maprenderstate$mapdecorationrenderstate.y = p_369459_.y();
      maprenderstate$mapdecorationrenderstate.rot = p_369459_.rot();
      maprenderstate$mapdecorationrenderstate.name = p_369459_.name().orElse(null);
      maprenderstate$mapdecorationrenderstate.renderOnFrame = p_369459_.renderOnFrame();
      return maprenderstate$mapdecorationrenderstate;
   }
}
