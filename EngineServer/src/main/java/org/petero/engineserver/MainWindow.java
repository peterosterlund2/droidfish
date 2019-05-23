/*
    EngineServer - Network engine server for DroidFish
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

package org.petero.engineserver;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


/** Displays the GUI to configure engine settings. */
public class MainWindow {
    private JFrame frame;
    private JCheckBox[] enabled;
    private JTextField[] port;
    private JTextField[] filename;
    private JTextField[] arguments;
    private JButton[] browse;

    private EngineServer server;
    private EngineConfig[] configs;

    public MainWindow(EngineServer server, EngineConfig[] configs) {
        this.server = server;
        this.configs = configs;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                initUI();
            }
        });
    }

    private void initUI() {
        final int numEngines = configs.length;

        frame = new JFrame();
        enabled = new JCheckBox[numEngines];
        port = new JTextField[numEngines];
        filename = new JTextField[numEngines];
        arguments = new JTextField[numEngines];
        browse = new JButton[numEngines];

        Container pane = frame.getContentPane();
        frame.setTitle("Chess Engine Server");
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                server.shutdown();
            }
        });

        GridBagLayout layout = new GridBagLayout();
        pane.setLayout(layout);
        int row = 0;

        GridBagConstraints constr = new GridBagConstraints();
        Insets inset = new Insets(0, 0, 5, 5);
        constr.insets = inset;
        constr.gridx = 0;
        constr.gridy = row;
        pane.add(new JLabel("Enabled"), constr);

        constr = new GridBagConstraints();
        constr.insets = inset;
        constr.gridx = 1;
        constr.gridy = row;
        pane.add(new JLabel("Port"), constr);

        constr = new GridBagConstraints();
        constr.insets = inset;
        constr.gridx = 2;
        constr.gridy = row;
        pane.add(new JLabel("Program and arguments"), constr);

        row++;

        for (int r = 0; r < numEngines; r++) {
            final int engineNo = r;
            final EngineConfig config = configs[r];
            enabled[r] = new JCheckBox("");
            constr = new GridBagConstraints();
            constr.insets = inset;
            constr.gridx = 0;
            constr.gridy = row;
            pane.add(enabled[r], constr);
            enabled[r].setSelected(config.enabled);
            enabled[r].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    enabledChanged(engineNo);
                }
            });

            port[r] = new JTextField();
            constr = new GridBagConstraints();
            constr.anchor = GridBagConstraints.WEST;
            constr.insets = inset;
            constr.gridx = 1;
            constr.gridy = row;
            pane.add(port[r], constr);
            port[r].setColumns(5);
            port[r].setText(Integer.toString(config.port));
            port[r].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    portChanged(engineNo);
                }
            });
            port[r].addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent event) {
                    portChanged(engineNo);
                }
            });

            filename[r] = new JTextField();
            constr = new GridBagConstraints();
            constr.insets = inset;
            constr.fill = GridBagConstraints.HORIZONTAL;
            constr.gridx = 2;
            constr.gridy = row;
            pane.add(filename[r], constr);
            filename[r].setColumns(40);
            filename[r].setText(config.filename);
            filename[r].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    filenameChanged(engineNo);
                }
            });
            filename[r].addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent event) {
                    filenameChanged(engineNo);
                }
            });

            arguments[r] = new JTextField();
            constr = new GridBagConstraints();
            constr.insets = inset;
            constr.fill = GridBagConstraints.HORIZONTAL;
            constr.gridx = 2;
            constr.gridy = row + 1;
            pane.add(arguments[r], constr);
            arguments[r].setColumns(40);
            arguments[r].setText(config.arguments);
            arguments[r].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    argumentsChanged(engineNo);
                }
            });
            arguments[r].addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent event) {
                    argumentsChanged(engineNo);
                }
            });

            browse[r] = new JButton("Browse");
            constr = new GridBagConstraints();
            constr.anchor = GridBagConstraints.NORTH;
            constr.insets = inset;
            constr.gridx = 3;
            constr.gridy = row;
            constr.gridheight = 2;
            pane.add(browse[r], constr);
            browse[r].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    browseFile(engineNo);
                }
            });

            row += 2;
        }

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void enabledChanged(int engineNo) {
        EngineConfig config = configs[engineNo];
        boolean e = enabled[engineNo].isSelected();
        if (e != config.enabled) {
            config.enabled = e;
            server.configChanged(engineNo);
        }
    }

    private void portChanged(int engineNo) {
        EngineConfig config = configs[engineNo];
        try {
            int p = Integer.valueOf(port[engineNo].getText().trim());
            if (p >= 1024 && p < 65536 && p != config.port) {
                config.port = p;
                server.configChanged(engineNo);
            }
        } catch (NumberFormatException ignore) {
        }
    }

    private void filenameChanged(int engineNo) {
        EngineConfig config = configs[engineNo];
        String fn = filename[engineNo].getText().trim();
        if (!fn.equals(config.filename)) {
            config.filename = fn;
            server.configChanged(engineNo);
        }
    }

    private void browseFile(int engineNo) {
        String fn = filename[engineNo].getText();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select chess engine");
        chooser.setSelectedFile(new File(fn));
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
            fn = chooser.getSelectedFile().getAbsolutePath();
        filename[engineNo].setText(fn);
        filenameChanged(engineNo);
    }

    private void argumentsChanged(int engineNo) {
        EngineConfig config = configs[engineNo];
        String args = arguments[engineNo].getText().trim();
        if (!args.equals(config.arguments)) {
            config.arguments = args;
            server.configChanged(engineNo);
        }
    }

    public void reportError(String title, String message) {
        StringBuilder sb = new StringBuilder();
        int lineLen = 100;
        while (message.length() > lineLen) {
            sb.append(message.substring(0, lineLen));
            sb.append('\n');
            message = message.substring(lineLen);
        }
        sb.append(message);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(frame, sb.toString(), title, JOptionPane.ERROR_MESSAGE);
            }
        });
   }
}
