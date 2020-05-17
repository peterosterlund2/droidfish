/*
    DroidFish - An Android chess program.
    Copyright (C) 2017  Peter Österlund, peterosterlund2@gmail.com

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

package org.petero.droidfish.book;

import java.util.ArrayList;

import org.petero.droidfish.book.DroidBook.BookEntry;
import org.petero.droidfish.gamelogic.Position;

class NoBook implements IOpeningBook {
    private boolean enabled = false;

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public void setOptions(BookOptions options) {
        enabled = options.filename.equals("nobook:");
    }

    @Override
    public ArrayList<BookEntry> getBookEntries(BookPosInput posInput) {
        return null;
    }
}
