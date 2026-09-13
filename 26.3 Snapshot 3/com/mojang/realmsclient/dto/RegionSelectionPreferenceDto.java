package com.mojang.realmsclient.dto;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import org.jspecify.annotations.Nullable;

public class RegionSelectionPreferenceDto implements ReflectionBasedSerialization {
   public static final RegionSelectionPreferenceDto DEFAULT = new RegionSelectionPreferenceDto(RegionSelectionPreference.AUTOMATIC_OWNER, null);
   @SerializedName("regionSelectionPreference")
   @JsonAdapter(RegionSelectionPreference.RegionSelectionPreferenceJsonAdapter.class)
   public final RegionSelectionPreference regionSelectionPreference;
   @SerializedName("preferredRegion")
   @JsonAdapter(RealmsRegion.RealmsRegionJsonAdapter.class)
   public @Nullable RealmsRegion preferredRegion;

   public RegionSelectionPreferenceDto(final RegionSelectionPreference regionSelectionPreference, final @Nullable RealmsRegion preferredRegion) {
      this.regionSelectionPreference = regionSelectionPreference;
      this.preferredRegion = preferredRegion;
   }

   public RegionSelectionPreferenceDto copy() {
      return new RegionSelectionPreferenceDto(this.regionSelectionPreference, this.preferredRegion);
   }
}
