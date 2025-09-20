package com.uss.madsies;

import java.util.ArrayList;
import java.util.List;

/*
    Data class to handle team data more easily
    Has functions to convert to and from spreadsheet data

 */

public class TeamData
{
    String teamName;
    int seeding;
    boolean checkedIn;
    int score; // Wins * 3
    int wins;
    int losses;
    int map_wins;
    int map_losses;
    float omwp;
    List<String> history;

    /*
        Conversion from spreadsheet data to data class
     */

    public TeamData(List<Object> spreadsheetRow)
    {
        teamName = (String)spreadsheetRow.get(0);
        seeding = Integer.parseInt((String)spreadsheetRow.get(1));
        checkedIn = Boolean.parseBoolean((String) spreadsheetRow.get(2));
        score = Integer.parseInt((String)spreadsheetRow.get(3));
        wins = Integer.parseInt((String)spreadsheetRow.get(4));
        losses = Integer.parseInt((String)spreadsheetRow.get(5));
        map_wins = Integer.parseInt((String)spreadsheetRow.get(6));
        map_losses = Integer.parseInt((String)spreadsheetRow.get(7));
        omwp = Float.parseFloat((String)spreadsheetRow.get(8));
        history = new ArrayList<>();
        for(int i = 9; i < spreadsheetRow.size(); i++)
        {
            history.add((String) spreadsheetRow.get(i));
        }
    }

    /*
        For team creation in-code
     */

    public TeamData(String teamName, int seeding)
    {
        this.teamName = teamName;
        this.seeding = seeding;
        checkedIn = false;
        score = 0;
        wins = 0;
        losses = 0;
        map_wins = 0;
        map_losses = 0;
        omwp = 0;
        history = new ArrayList<>();
    }

    public List<Object> convertToSpreadsheetRow()
    {
        List<Object> row = new ArrayList<Object>();
        row.add(teamName);
        row.add(seeding);
        row.add(checkedIn);
        row.add(score);
        row.add(wins);
        row.add(losses);
        row.add(map_wins);
        row.add(map_losses);
        row.add(omwp);
        row.addAll(history);
        return row;
    }

    public int getWins() {return wins;}
    public int getMapWins()   { return map_wins; }
    public int getMapLosses() { return map_losses; }
    public float getOMWP() {return omwp;}

 }
