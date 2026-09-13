package net.minecraft.server.packs.repository;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FeatureFlagsMetadataSection;
import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.world.flag.FeatureFlagSet;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Pack {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PackLocationInfo location;
   private final Pack.ResourcesSupplier resources;
   private final Pack.Metadata metadata;
   private final PackSelectionConfig selectionConfig;

   public static @Nullable Pack readMetaAndCreate(
      PackLocationInfo p_333251_, Pack.ResourcesSupplier p_252210_, PackType p_250595_, PackSelectionConfig p_334202_
   ) {
      PackFormat packformat = SharedConstants.getCurrentVersion().packVersion(p_250595_);
      Pack.Metadata pack$metadata = readPackMetadata(p_333251_, p_252210_, packformat, p_250595_);
      return pack$metadata != null ? new Pack(p_333251_, p_252210_, pack$metadata, p_334202_) : null;
   }

   public Pack(PackLocationInfo p_330003_, Pack.ResourcesSupplier p_249377_, Pack.Metadata p_330761_, PackSelectionConfig p_334769_) {
      this.location = p_330003_;
      this.resources = p_249377_;
      this.metadata = p_330761_;
      this.selectionConfig = p_334769_;
   }

   public static Pack.@Nullable Metadata readPackMetadata(
      PackLocationInfo p_330799_, Pack.ResourcesSupplier p_331172_, PackFormat p_425340_, PackType p_426910_
   ) {
      try (PackResources packresources = p_331172_.openPrimary(p_330799_)) {
         PackMetadataSection packmetadatasection = packresources.getMetadataSection(PackMetadataSection.forPackType(p_426910_));
         if (packmetadatasection == null) {
            packmetadatasection = packresources.getMetadataSection(PackMetadataSection.FALLBACK_TYPE);
         }

         if (packmetadatasection == null) {
            LOGGER.warn("Missing metadata in pack {}", p_330799_.id());
            return null;
         } else {
            FeatureFlagsMetadataSection featureflagsmetadatasection = packresources.getMetadataSection(FeatureFlagsMetadataSection.TYPE);
            FeatureFlagSet featureflagset = featureflagsmetadatasection != null ? featureflagsmetadatasection.flags() : FeatureFlagSet.of();
            PackCompatibility packcompatibility = PackCompatibility.forVersion(packmetadatasection.supportedFormats(), p_425340_);
            OverlayMetadataSection overlaymetadatasection = packresources.getMetadataSection(OverlayMetadataSection.forPackType(p_426910_));
            List<String> list = overlaymetadatasection != null ? overlaymetadatasection.overlaysForVersion(p_425340_) : List.of();
            return new Pack.Metadata(packmetadatasection.description(), packcompatibility, featureflagset, list);
         }
      } catch (Exception exception) {
         LOGGER.warn("Failed to read pack {} metadata", p_330799_.id(), exception);
         return null;
      }
   }

   public PackLocationInfo location() {
      return this.location;
   }

   public Component getTitle() {
      return this.location.title();
   }

   public Component getDescription() {
      return this.metadata.description();
   }

   public Component getChatLink(boolean p_10438_) {
      return this.location.createChatLink(p_10438_, this.metadata.description);
   }

   public PackCompatibility getCompatibility() {
      return this.metadata.compatibility();
   }

   public FeatureFlagSet getRequestedFeatures() {
      return this.metadata.requestedFeatures();
   }

   public PackResources open() {
      return this.resources.openFull(this.location, this.metadata);
   }

   public String getId() {
      return this.location.id();
   }

   public PackSelectionConfig selectionConfig() {
      return this.selectionConfig;
   }

   public boolean isRequired() {
      return this.selectionConfig.required();
   }

   public boolean isFixedPosition() {
      return this.selectionConfig.fixedPosition();
   }

   public Pack.Position getDefaultPosition() {
      return this.selectionConfig.defaultPosition();
   }

   public PackSource getPackSource() {
      return this.location.source();
   }

   @Override
   public boolean equals(Object p_10448_) {
      if (this == p_10448_) {
         return true;
      } else {
         return p_10448_ instanceof Pack pack ? this.location.equals(pack.location) : false;
      }
   }

   @Override
   public int hashCode() {
      return this.location.hashCode();
   }

   public record Metadata(Component description, PackCompatibility compatibility, FeatureFlagSet requestedFeatures, List<String> overlays) {
   }

   public enum Position {
      TOP,
      BOTTOM;

      public <T> int insert(List<T> p_10471_, T p_10472_, Function<T, PackSelectionConfig> p_10473_, boolean p_10474_) {
         Pack.Position pack$position = p_10474_ ? this.opposite() : this;
         if (pack$position == BOTTOM) {
            int j;
            for (j = 0; j < p_10471_.size(); j++) {
               PackSelectionConfig packselectionconfig1 = p_10473_.apply(p_10471_.get(j));
               if (!packselectionconfig1.fixedPosition() || packselectionconfig1.defaultPosition() != this) {
                  break;
               }
            }

            p_10471_.add(j, p_10472_);
            return j;
         } else {
            int i;
            for (i = p_10471_.size() - 1; i >= 0; i--) {
               PackSelectionConfig packselectionconfig = p_10473_.apply(p_10471_.get(i));
               if (!packselectionconfig.fixedPosition() || packselectionconfig.defaultPosition() != this) {
                  break;
               }
            }

            p_10471_.add(i + 1, p_10472_);
            return i + 1;
         }
      }

      public Pack.Position opposite() {
         return this == TOP ? BOTTOM : TOP;
      }
   }

   public interface ResourcesSupplier {
      PackResources openPrimary(PackLocationInfo var1);

      PackResources openFull(PackLocationInfo var1, Pack.Metadata var2);
   }
}
