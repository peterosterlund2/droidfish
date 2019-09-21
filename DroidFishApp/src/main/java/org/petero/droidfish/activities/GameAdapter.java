/*
    DroidFish - An Android chess program.
    Copyright (C) 2019  Peter Österlund, peterosterlund2@gmail.com

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

package org.petero.droidfish.activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * An adapter for displaying an ArrayList<GameInfo> in a ListView.
 */
public class GameAdapter<T> extends BaseAdapter implements Filterable {
    private ArrayList<T> origValues; // Unfiltered values
    private ArrayList<T> values;     // Filtered values. Equal to origValues if no filter used
    private final LayoutInflater inflater;
    private int resource;
    private GameFilter filter;       // Initialized at first use

    public GameAdapter(Context context, int resource, ArrayList<T> objects) {
        origValues = objects;
        values = objects;
        inflater = LayoutInflater.from(context);
        this.resource = resource;
    }

    public ArrayList<T> getValues() {
        return values;
    }

    @Override
    public int getCount() {
        return values.size();
    }

    @Override
    public T getItem(int position) {
        return values.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView view;
        if (convertView == null)
            view = (TextView) inflater.inflate(resource, parent, false);
        else
            view = (TextView) convertView;
        view.setText(getItem(position).toString());
        return view;
    }

    @Override
    public Filter getFilter() {
        if (filter == null)
            filter = new GameFilter();
        return filter;
    }

    private class GameFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults res = new FilterResults();
            if (constraint == null || constraint.length() == 0) {
                res.values = origValues;
                res.count = origValues.size();
            } else {
                String s = constraint.toString().toLowerCase();
                ArrayList<T> newValues = new ArrayList<>();
                for (T item : origValues)
                    if (matchItem(item, s))
                        newValues.add(item);
                res.values = newValues;
                res.count = newValues.size();
            }
            return res;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            values = (ArrayList<T>) results.values;
            notifyDataSetChanged();
        }
    }

    /** Return true if matchStr matches item.
     * @param item The item to check. The toString() value converted to lowercase is used.
     * @param matchStr The match string. Must be lowercase. */
    static <U> boolean matchItem(U item, String matchStr) {
        return item.toString().toLowerCase().contains(matchStr);
    }
}
