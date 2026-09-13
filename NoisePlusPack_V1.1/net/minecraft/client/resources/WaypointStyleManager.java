package net.minecraft.client.resources;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.waypoints.WaypointStyleAsset;
import net.minecraft.world.waypoints.WaypointStyleAssets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WaypointStyleManager extends SimpleJsonResourceReloadListener<WaypointStyle> {
   private static final FileToIdConverter ASSET_LISTER = FileToIdConverter.json("waypoint_style");
   private static final WaypointStyle MISSING = new WaypointStyle(0, 1, List.of(MissingTextureAtlasSprite.getLocation()));
   private Map<ResourceKey<WaypointStyleAsset>, WaypointStyle> waypointStyles = Map.of();

   public WaypointStyleManager() {
      super(WaypointStyle.CODEC, ASSET_LISTER);
   }

   protected void apply(Map<Identifier, WaypointStyle> p_408124_, ResourceManager p_409101_, ProfilerFiller p_409080_) {
      this.waypointStyles = p_408124_.entrySet()
         .stream()
         .collect(Collectors.toUnmodifiableMap(p_448424_ -> ResourceKey.create(WaypointStyleAssets.ROOT_ID, p_448424_.getKey()), Entry::getValue));
   }

   public WaypointStyle get(ResourceKey<WaypointStyleAsset> p_406604_) {
      return this.waypointStyles.getOrDefault(p_406604_, MISSING);
   }
}
