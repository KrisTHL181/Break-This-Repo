/**
 * This file is part of pinyin4j (http://sourceforge.net/projects/pinyin4j/) and distributed under
 * GNU GENERAL PUBLIC LICENSE (GPL).
 * 
 * pinyin4j is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * pinyin4j is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with pinyin4j.
 */

package net.sourceforge.pinyin4j;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

/**
 * Helper class reading the JSON mapping resources shipped with pinyin4j.
 *
 */
class JsonHelper {
  private static final Gson GSON = new Gson();

  private static final Type MAPPING_TYPE =
      new TypeToken<Map<String, Map<String, String>>>() {}.getType();

  /**
   * Read a mapping resource that maps an unformatted Hanyu Pinyin string to the
   * corresponding presentations in other Romanization systems.
   *
   * @param resourceName
   *            the classpath name of the JSON resource
   * @return the parsed mapping; an empty map if the resource cannot be read
   */
  static Map<String, Map<String, String>> readMappingResource(String resourceName) {
    InputStream stream = null;
    try {
      stream = ResourceHelper.getResourceInputStream(resourceName);
      Reader reader = new InputStreamReader(stream, "UTF-8");
      Map<String, Map<String, String>> mapping = GSON.fromJson(reader, MAPPING_TYPE);
      return null != mapping ? mapping : Collections.<String, Map<String, String>>emptyMap();
    } catch (IOException e) {
      e.printStackTrace();
      return Collections.<String, Map<String, String>>emptyMap();
    } finally {
      if (null != stream) {
        try {
          stream.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }
  }
}
