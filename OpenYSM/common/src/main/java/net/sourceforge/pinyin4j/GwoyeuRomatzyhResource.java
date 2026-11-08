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

/**
 *
 */
package net.sourceforge.pinyin4j;

import java.util.Map;

/**
 * A class contains resource that translates from Hanyu Pinyin to Gwoyeu
 * Romatzyh
 *
 * @author Li Min (xmlerlimin@gmail.com)
 *
 */
class GwoyeuRomatzyhResource {
  /**
   * Maps an unformatted Hanyu Pinyin string to its Gwoyeu Romatzyh
   * presentations, keyed by the tone-specific tag name.
   */
  private Map<String, Map<String, String>> pinyinToGwoyeuMappingMap;

  /**
   * @param pinyinToGwoyeuMappingMap
   *            The pinyinToGwoyeuMappingMap to set.
   */
  private void setPinyinToGwoyeuMappingMap(Map<String, Map<String, String>> pinyinToGwoyeuMappingMap) {
    this.pinyinToGwoyeuMappingMap = pinyinToGwoyeuMappingMap;
  }

  /**
   * @return Returns the pinyinToGwoyeuMappingMap.
   */
  Map<String, Map<String, String>> getPinyinToGwoyeuMappingMap() {
    return pinyinToGwoyeuMappingMap;
  }

  /**
   * Private constructor as part of the singleton pattern.
   */
  private GwoyeuRomatzyhResource() {
    initializeResource();
  }

  /**
   * Initialize the map containing Hanyu Pinyin to Gwoyeu mapping
   */
  private void initializeResource() {
    final String mappingFileName = "/pinyindb/pinyin_gwoyeu_mapping.json";
    setPinyinToGwoyeuMappingMap(JsonHelper.readMappingResource(mappingFileName));
  }

  /**
   * Singleton factory method.
   *
   * @return the one and only MySingleton.
   */
  static GwoyeuRomatzyhResource getInstance() {
    return GwoyeuRomatzyhSystemResourceHolder.theInstance;
  }

  /**
   * Singleton implementation helper.
   */
  private static class GwoyeuRomatzyhSystemResourceHolder {
    static final GwoyeuRomatzyhResource theInstance = new GwoyeuRomatzyhResource();
  }
}
