package org.petero.droidfish.book;

import java.util.ArrayList;

import org.petero.droidfish.book.DroidBook.BookEntry;
import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.Position;

/** Opening book containing all moves that define the ECO opening classifications. */
public class EcoBook implements IOpeningBook {
    private boolean enabled = false;

    /** Constructor. */
    EcoBook() {
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public void setOptions(BookOptions options) {
        enabled = options.filename.equals("eco:");
    }

    @Override
    public ArrayList<BookEntry> getBookEntries(Position pos) {
        ArrayList<Move> moves = EcoDb.getInstance().getMoves(pos);
        ArrayList<BookEntry> entries = new ArrayList<BookEntry>();
        for (int i = 0; i < moves.size(); i++) {
            BookEntry be = new BookEntry(moves.get(i));
            be.weight = 10000 - i;
            entries.add(be);
        }
        return entries;
    }
}
