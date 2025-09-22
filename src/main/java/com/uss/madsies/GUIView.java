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
    public TextArea errorText;

    public boolean matchStatus;

    public void setMatchStatus(boolean ms)
    {
        this.matchStatus = ms;
    }

    public GUIView() {
        matchStatus = Main.isCurrentMatch;
        frame = new JFrame("USS Admin Control Panel:");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(3,1,10,10));

        errorText = new TextArea();
        errorText.setEditable(false);

        Image icon = Toolkit.getDefaultToolkit().getImage(
                GUIView.class.getResource("/images/USS.png")
        );

        frame.setIconImage(icon);

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
                JOptionPane.showMessageDialog(frame, e.getMessage());
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
            if (result == JOptionPane.YES_OPTION)
            {
                Main.wipeData();
                Main.addSeedAndCreateTeams();
            }}
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
            } catch (RuntimeException ex)
            {
                JOptionPane.showMessageDialog(frame, ex.getMessage());
            }
        });

        copyUnfinished.addActionListener(_ -> {
                try {
                    Main.copyMissingMatches();
                    JOptionPane.showMessageDialog(frame, "Copied to clipboard");
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(frame, e.getMessage());
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
                JOptionPane.showMessageDialog(frame, ex.getMessage());
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
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        });

        if (!matchStatus) {
            endMatches.setVisible(false);
            cancelMatches.setVisible(false);
            copyMatches.setVisible(false);
            copyUnfinished.setVisible(false);
        }
        else
        {
            endMatches.setVisible(true);
            cancelMatches.setVisible(true);
            copyMatches.setVisible(true);
            copyUnfinished.setVisible(true);
        }


        JPanel matchPanel = new JPanel(new GridLayout(2,3,15,15));
        matchPanel.setBorder(BorderFactory.createTitledBorder("Matches"));
        matchPanel.add(generateMatches);
        matchPanel.add(cancelMatches);
        matchPanel.add(endMatches);
        matchPanel.add(copyMatches);
        matchPanel.add(copyUnfinished);

        updatePublicBoard = new JButton("Update Public Sheet");
        updatePublicBoard.addActionListener(_ -> {
            Main.updatePublicStandings();
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
