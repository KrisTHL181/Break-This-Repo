package net.minecraft.server.jsonrpc.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.net.URI;
import java.net.URISyntaxException;

public class ReferenceUtil {
   public static final Codec<URI> REFERENCE_CODEC = Codec.STRING.comapFlatMap(string -> {
      try {
         return DataResult.success(new URI(string));
      } catch (URISyntaxException e) {
         return DataResult.error(e::getMessage);
      }
   }, URI::toString);

   public static URI createLocalReference(final String typeId) {
      return URI.create("#/components/schemas/" + typeId);
   }
}
