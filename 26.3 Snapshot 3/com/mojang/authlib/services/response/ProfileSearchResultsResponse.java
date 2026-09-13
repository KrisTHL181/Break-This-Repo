package com.mojang.authlib.services.response;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public record ProfileSearchResultsResponse(List<NameAndId> profiles) {
   public static final Type LIST_TYPE = TypeToken.getParameterized(List.class, new Type[]{NameAndId.class}).getType();

   public static class Serializer implements JsonDeserializer<ProfileSearchResultsResponse> {
      public ProfileSearchResultsResponse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
         return new ProfileSearchResultsResponse((List<NameAndId>)context.deserialize(json, ProfileSearchResultsResponse.LIST_TYPE));
      }
   }
}
