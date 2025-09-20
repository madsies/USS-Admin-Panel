package com.uss.madsies;


import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class GUIView
{
    private final JFrame frame;
    private final JButton sortSeeding;
    private final JButton generateMatches;
    private final JButton cancelMatches;
    public JButton endMatches;
    public JButton updatePublicBoard;

    public GUIView() {

        frame = new JFrame("USS Admin Control Panel: Division "+Main.DIVISION);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(3,1,10,10));

        Image icon = Toolkit.getDefaultToolkit().getImage(
                GUIView.class.getResource("/images/USS.png")
        );

        frame.setIconImage(icon);

        JToolBar tb = new JToolBar();
        JPanel panel = new JPanel();
        JComboBox combo = new JComboBox(new String[] {"One", "Two", "Three"});
        combo.addItemListener(e -> Main.changeDivision(combo.getSelectedItem().toString()));
        panel.add(combo);
        tb.add(panel);
        frame.add(tb);



        JPanel sortingPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        sortingPanel.setBorder(BorderFactory.createTitledBorder("Sorting"));

        sortSeeding = new JButton("Rank by Seed");
        sortSeeding.addActionListener(_ -> {
            try {
                Main.genericSetup();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        JButton sortPlacement = new JButton("Fix Rankings");
        sortPlacement.addActionListener(_ -> {
            try {
                Main.fixStandings();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        JButton reset = new JButton("Reset Datasheet");
        reset.addActionListener(_ -> {
            int result = JOptionPane.showConfirmDialog(
                    frame,                     // parent component
                    "Are you sure you want to reset the sheet data?", // message
                    "YOU ARE DELETING ALL DATA.",          // title
                    JOptionPane.YES_NO_OPTION, // options
                    JOptionPane.WARNING_MESSAGE // icon
            );
            if (result == JOptionPane.YES_OPTION) Main.wipeData();}
            );

        JButton setAllCheckIn = new JButton("Set All Check In");
        setAllCheckIn.addActionListener(_ -> {Main.checkAllTeams(true);});

        JButton setAllCheckOut = new JButton("Set All Check Out");
        setAllCheckOut.addActionListener(_ -> {Main.checkAllTeams(false);});

        sortingPanel.add(sortSeeding);
        sortingPanel.add(sortPlacement);
        sortingPanel.add(reset);
        sortingPanel.add(setAllCheckIn);
        sortingPanel.add(setAllCheckOut);

        cancelMatches = new JButton("Cancel Matches");
        endMatches = new JButton("End Matches");
        generateMatches = new JButton("Generate Matches");
        JButton copyMatches = new JButton("Copy Matches to Clipboard");
        JButton copyUnfinished = new JButton("Copy Unfinished Matches to Clipboard");
        copyMatches.addActionListener(_ -> {Main.copyRound();JOptionPane.showMessageDialog(frame, "Copied to clipboard");});
        generateMatches.addActionListener(_ -> {
            try {
                Main.generateRound();
                endMatches.setVisible(true);
                cancelMatches.setVisible(true);
                copyMatches.setVisible(true);
                copyUnfinished.setVisible(true);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        copyUnfinished.addActionListener(_ -> {
                try {
                    Main.copyMissingMatches();
                    JOptionPane.showMessageDialog(frame, "Copied to clipboard");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
        });



        endMatches.addActionListener(_ -> {
        int result = JOptionPane.showConfirmDialog(
                frame,                     // parent component
                "Are you sure all scores have been submitted?", // message
                "Confirm Completed?",          // title
                JOptionPane.YES_NO_OPTION, // options
                JOptionPane.WARNING_MESSAGE // icon
        );

        if (result == JOptionPane.YES_OPTION) {
            try {
                Main.endRound();
                endMatches.setVisible(false);
                cancelMatches.setVisible(false);
                copyMatches.setVisible(false);
                copyUnfinished.setVisible(false);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
         });

        cancelMatches.addActionListener(_ -> {
            int result = JOptionPane.showConfirmDialog(
                    frame,                     // parent component
                    "Are you sure you want to cancel the round?", // message
                    "Are you sure?",          // title
                    JOptionPane.YES_NO_OPTION, // options
                    JOptionPane.WARNING_MESSAGE // icon
            );
            if (result == JOptionPane.YES_OPTION) {
                try {
                    Main.cancelRound();
                    endMatches.setVisible(false);
                    cancelMatches.setVisible(false);
                    copyMatches.setVisible(false);
                    copyUnfinished.setVisible(false);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        endMatches.setVisible(false);
        cancelMatches.setVisible(false);
        copyMatches.setVisible(false);
        copyUnfinished.setVisible(false);

        JPanel matchPanel = new JPanel(new GridLayout(2,3,15,15));
        matchPanel.setBorder(BorderFactory.createTitledBorder("Matches"));
        matchPanel.add(generateMatches);
        matchPanel.add(cancelMatches);
        matchPanel.add(endMatches);
        matchPanel.add(copyMatches);
        matchPanel.add(copyUnfinished);

        updatePublicBoard = new JButton("Update Public Sheet");
        updatePublicBoard.addActionListener(_ -> {
            try {
                Main.updatePublicStandings();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        JButton copyUnreadyTeams = new JButton("Copy List of non-checked in teams");
        copyUnreadyTeams.addActionListener(_ ->
        {Main.copyNonCheckedIn();JOptionPane.showMessageDialog(frame, "Copied to clipboard");});

        JPanel publicPanel = new JPanel(new GridLayout(1,2,15,15));
        publicPanel.setBorder(BorderFactory.createTitledBorder("Public"));
        publicPanel.add(updatePublicBoard);
        publicPanel.add(copyUnreadyTeams);


        sortSeeding.setVisible(true);
        frame.add(sortingPanel);
        frame.add(matchPanel);
        frame.add(publicPanel);

        frame.setSize(1280, 720);
        frame.setLocationRelativeTo(null);

    }

    public void show()
    {
        frame.setVisible(true);
    }
}
