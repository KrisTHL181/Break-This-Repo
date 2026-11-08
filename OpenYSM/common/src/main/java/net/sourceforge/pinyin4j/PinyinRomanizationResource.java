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

import java.util.Map;

/**
 * Contains the resource supporting translations among different Chinese
 * Romanization systems
 *
 * @author Li Min (xmlerlimin@gmail.com)
 *
 */
class PinyinRomanizationResource {
  /**
   * Maps an unformatted Hanyu Pinyin string to its presentations in the other
   * Romanization systems, keyed by the system tag name.
   */
  private Map<String, Map<String, String>> pinyinMappingMap;

  /**
   * @param pinyinMappingMap
   *            The pinyinMappingMap to set.
   */
  private void setPinyinMappingMap(Map<String, Map<String, String>> pinyinMappingMap) {
    this.pinyinMappingMap = pinyinMappingMap;
  }

  /**
   * @return Returns the pinyinMappingMap.
   */
  Map<String, Map<String, String>> getPinyinMappingMap() {
    return pinyinMappingMap;
  }

  /**
   * Private constructor as part of the singleton pattern.
   */
  private PinyinRomanizationResource() {
    initializeResource();
  }

  /**
   * Initialize the map containing variable PinYin representations
   */
  private void initializeResource() {
    final String mappingFileName = "/pinyindb/pinyin_mapping.json";
    setPinyinMappingMap(JsonHelper.readMappingResource(mappingFileName));
  }

  /**
   * Singleton factory method.
   *
   * @return the one and only MySingleton.
   */
  static PinyinRomanizationResource getInstance() {
    return PinyinRomanizationSystemResourceHolder.theInstance;
  }

  /**
   * Singleton implementation helper.
   */
  private static class PinyinRomanizationSystemResourceHolder {
    static final PinyinRomanizationResource theInstance = new PinyinRomanizationResource();
  }
}
