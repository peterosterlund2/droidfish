/*
    DroidFish - An Android chess program.
    Copyright (C) 2011  Peter Österlund, peterosterlund2@gmail.com

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

import java.util.ArrayList;
import java.util.TreeMap;

import org.petero.droidfish.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/** Activity to edit PGN headers. */
public class EditHeaders extends Activity {

    private TextView event;
    private TextView site;
    private TextView date;
    private TextView round;
    private TextView white;
    private TextView black;
    private Button okButton;
    private Button cancelButton;

    private TreeMap<String,String> headers = new TreeMap<String,String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_headers);
        event = (TextView)findViewById(R.id.ed_header_event);
        site = (TextView)findViewById(R.id.ed_header_site);
        date = (TextView)findViewById(R.id.ed_header_date);
        round = (TextView)findViewById(R.id.ed_header_round);
        white = (TextView)findViewById(R.id.ed_header_white);
        black = (TextView)findViewById(R.id.ed_header_black);

        okButton = (Button)findViewById(R.id.ed_header_ok);
        cancelButton = (Button)findViewById(R.id.ed_header_cancel);

        Intent data = getIntent();
        Bundle bundle = data.getBundleExtra("org.petero.droidfish.headers");
        ArrayList<String> tags = bundle.getStringArrayList("tags");
        ArrayList<String> tagValues = bundle.getStringArrayList("tagValues");
        for (int i = 0; i < tags.size(); i++)
            headers.put(tags.get(i), tagValues.get(i));
        event.setText(headers.get("Event"));
        site .setText(headers.get("Site"));
        date .setText(headers.get("Date"));
        round.setText(headers.get("Round"));
        white.setText(headers.get("White"));
        black.setText(headers.get("Black"));

        okButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendBackResult();
            }
        });
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            sendBackResult();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private final void sendBackResult() {
        headers.put("Event", event.getText().toString().trim());
        headers.put("Site",  site .getText().toString().trim());
        headers.put("Date",  date .getText().toString().trim());
        headers.put("Round", round.getText().toString().trim());
        headers.put("White", white.getText().toString().trim());
        headers.put("Black", black.getText().toString().trim());

        Bundle bundle = new Bundle();
        ArrayList<String> tags = new ArrayList<String>();
        ArrayList<String> values = new ArrayList<String>();
        for (String k : headers.keySet()) {
            tags.add(k);
            values.add(headers.get(k));
        }
        bundle.putStringArrayList("tags", tags);
        bundle.putStringArrayList("tagValues", values);
        Intent data = new Intent();
        data.putExtra("org.petero.droidfish.headers", bundle);
        setResult(RESULT_OK, data);
        finish();
    }
}
