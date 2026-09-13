package com.mojang.realmsclient.dto;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RegionSelectionPreferenceDto implements ReflectionBasedSerialization {
   public static final RegionSelectionPreferenceDto DEFAULT = new RegionSelectionPreferenceDto(RegionSelectionPreference.AUTOMATIC_OWNER, null);
   @SerializedName("regionSelectionPreference")
   @JsonAdapter(RegionSelectionPreference.RegionSelectionPreferenceJsonAdapter.class)
   public final RegionSelectionPreference regionSelectionPreference;
   @SerializedName("preferredRegion")
   @JsonAdapter(RealmsRegion.RealmsRegionJsonAdapter.class)
   public @Nullable RealmsRegion preferredRegion;

   public RegionSelectionPreferenceDto(RegionSelectionPreference p_408403_, @Nullable RealmsRegion p_410468_) {
      this.regionSelectionPreference = p_408403_;
      this.preferredRegion = p_410468_;
   }

   public RegionSelectionPreferenceDto copy() {
      return new RegionSelectionPreferenceDto(this.regionSelectionPreference, this.preferredRegion);
   }
}
