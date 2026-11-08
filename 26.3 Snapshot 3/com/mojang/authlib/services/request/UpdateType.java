package com.mojang.authlib.services.request;

import com.google.gson.annotations.SerializedName;

public enum UpdateType {
   @SerializedName("ADD")
   ADD,
   @SerializedName("REMOVE")
   REMOVE;
}
