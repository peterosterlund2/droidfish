/*
    DroidFish - An Android chess program.
    Copyright (C) 2012  Peter Österlund, peterosterlund2@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.petero.droidfish;

/** Interface for user interface actions. */
public interface UIAction extends Runnable {
    /** Get a unique identifier for this action. */
    public String getId();

    /** Get name resource for the action. */
    public int getName();

    /** Get icon SVG resource or -1 for no icon. */
    public int getIcon();

    /** Return true if the action is currently enabled. */
    public boolean enabled();
}
