/*
 * HDSL
 * Copyright (C) 2026  HDSL contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.setting;

/// Receives the old and the new value of something that changed.
///
/// The desktop build used the toolkit's change-listener type; HDSL-web has no
/// UI toolkit, and the observable value itself was never used — only the two
/// values.
@FunctionalInterface
public interface ChangeListener<T> {
    /// Called after the value changed.
    ///
    /// @param oldValue the value before the change
    /// @param newValue the value after the change
    void onChange(T oldValue, T newValue);
}
