package com.uss.madsies;


import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class GUIView
{
    private JFrame frame;
    private JButton sortSeeding;
    private JButton generateMatches;
    private final JButton cancelMatches;
    public JButton endMatches;

    public GUIView() {

        frame = new JFrame("USS Admin Control Panel");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel label = new JLabel("Hello, Swing!", JLabel.CENTER);
        sortSeeding = new JButton("Sort Seeding");
        sortSeeding.addActionListener(e -> {
            try {
                Main.genericSetup();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        cancelMatches = new JButton("Cancel Matches");
        endMatches = new JButton("End Matches");
        generateMatches = new JButton("Generate Matches");
        generateMatches.addActionListener(e -> {
            try {
                Main.generateRound();
                endMatches.setVisible(true);
                cancelMatches.setVisible(true);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        endMatches.addActionListener(e -> {
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
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        cancelMatches.addActionListener(e -> {
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
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        endMatches.setVisible(false);
        cancelMatches.setVisible(false);

        sortSeeding.setVisible(true);
        frame.setLayout(new FlowLayout(FlowLayout.CENTER));
        frame.add(label);
        frame.add(sortSeeding);
        frame.add(generateMatches);
        frame.add(cancelMatches);
        frame.add(endMatches);

        frame.setSize(1280, 720);
        frame.setLocationRelativeTo(null);

    }

    public void show()
    {
        frame.setVisible(true);
    }
}
