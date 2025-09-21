package com.uss.madsies;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    static String ADMIN_SHEET; // Stored privately
    static String PUBLIC_SHEET = "1HRoTkeSpNUK4u2ft08EimauMcTNlndrIYl-gLZUy-8E";
    static List<TeamData> teamsInfo = new ArrayList<>();
    static List<MatchUp> matches;

    public static void main(String... args) throws IOException, GeneralSecurityException
    {
        // Build a new authorized API client service.
        SheetsManagement.generateService();
        ADMIN_SHEET = SheetsManagement.getAdminSheet();

        getFullData(); // Initialises data into code

        // Initialise GUI
        SwingUtilities.invokeLater(() -> {
            GUIView view  = new GUIView();
            view.show();
        });
    }

    /**
     *  Creates new sheet and writes matchups for admins to submit match scores
     *
     * @param matches Current list of matches in round
     */
    public static void writeMatchupSheet(List<MatchUp> matches) throws IOException
    {
        int num = SheetsManagement.getSheetNumber();
        String range = "Match_"+num+"!A1";
        List<List<Object>> values = new ArrayList<>();

        // Headers for sheet (Readability)
        values.add(Arrays.asList("Team A", "Team B", "Team A Score", "Team B Score"));

        // Data
        for(MatchUp match : matches)
        {
            // Auto filling in bye scores
            int scoreA = 0;
            int scoreB = 0;
            if (match.team1.equals("BYE")) scoreB = 2;
            if (match.team2.equals("BYE")) scoreA = 2;

            values.add(Arrays.asList(match.team1, match.team2, scoreA, scoreB));
        }

        SheetsManagement.writeData(values, ADMIN_SHEET, range);
    }

    /**
        Reads data from match sheet and updates win/losses accordingly
     */
    public static void updateRecords() throws IOException
    {
        int num = SheetsManagement.getSheetNumber();
        String range = "Match_"+num+"!A2:D";

        List<List<Object>> data = SheetsManagement.fetchData(ADMIN_SHEET, range);

        Map<String, TeamData> teamMap = new HashMap<>();
        for (TeamData t : teamsInfo)
        {
            teamMap.put(t.teamName, t);
        }

        for (List<Object> row : data) {
            if (row.size() < 4) continue; // skip incomplete rows

            String teamA = row.get(0).toString();
            String teamB = row.get(1).toString();

            int scoreA = Integer.parseInt(row.get(2).toString());
            int scoreB = Integer.parseInt(row.get(3).toString());

            if (Objects.equals(teamA, "BYE"))
            {
                teamMap.get(teamB).wins++;
                teamMap.get(teamB).score += 3;
                teamMap.get(teamB).map_wins += 2;
                continue;
            }
            else if (Objects.equals(teamB, "BYE"))
            {
                teamMap.get(teamA).wins++;
                teamMap.get(teamA).score += 3;
                teamMap.get(teamA).map_wins += 2;
                continue;
            }
            else if (scoreA > scoreB) {
                teamMap.get(teamA).wins++;
                teamMap.get(teamB).losses++;
            } else if (scoreB > scoreA) {
                teamMap.get(teamB).wins++;
                teamMap.get(teamA).losses++;
            }
            teamMap.get(teamA).map_wins += scoreA;
            teamMap.get(teamB).map_wins += scoreB;
            teamMap.get(teamA).map_losses += scoreB;
            teamMap.get(teamB).map_losses += scoreA;
            teamMap.get(teamA).score = teamMap.get(teamA).wins * 3;
            teamMap.get(teamB).score = teamMap.get(teamB).wins * 3;
        }

    }

    /**
     *  For initial startup, sets up data and sorts by seed
     */

    public static void genericSetup() throws IOException {
        getFullData();
        sortTeams(true);
        rewriteData();
    }

    /**
     * Orders standings by correct order and writes back to disk
     */

    public static void fixStandings() throws IOException
    {
        sortTeams(false);
        rewriteData();
    }

    // Do this when matches are needed to be generated
    public static void generateRound() throws IOException
    {
        getFullData();
        matches = Matchmaker.createSwissMatchups(teamsInfo);
        SheetsManagement.createNewSheet();
        writeMatchupSheet(matches);
    }

    public static void cancelRound() throws IOException
    {
        matches.clear();
        int num = SheetsManagement.getSheetNumber();
        SheetsManagement.deleteSheet("Match_"+num);
        SheetsManagement.setSheetNumber(num-1);
    }

    // Do this when all data is filled and all matches are done
    public static void endRound() throws IOException
    {
        updateRecords();
        updateHistory(matches);
        updateOMWP();
        sortTeams(false);
        rewriteData();
    }

    /*
        Writes necessary information to the public view sheet, ran at the end of each week
     */

    public static void updatePublicStandings() throws IOException
    {
        sortTeams(false);
        List<List<Object>> sheetData = new ArrayList<>();
        //sheetData.add(Arrays.asList("Ranking", "Team", "Score", "Wins", "Losses", "OMWP"));
        int i = 1;
        for (TeamData t : teamsInfo)
        {
            sheetData.add(Arrays.asList(i, t.teamName, t.score, t.wins, t.losses, t.omwp));
            i++;
        }

        SheetsManagement.writeData(sheetData, PUBLIC_SHEET, "Standings!B5");
    }

    public static void copyRound()
    {
        StringSelection stringSelection = new StringSelection(Matchmaker.getMatchupsString(matches));
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
    }

    public static void copyNonCheckedIn()
    {
        getFullData(); // Load full data (may have been un-ticked since last check)

        StringBuilder sb = new StringBuilder();
        sb.append("Teams that have not Checked in:\n");
        for (TeamData t : teamsInfo)
        {
            if (!t.checkedIn)
            {
                sb.append(t.teamName).append("\n");
            }
        }
        StringSelection stringSelection = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
    }

    public static void wipeData()
    {
        for (TeamData t : teamsInfo)
        {
            t.Clear();
        }
        teamsInfo.clear();
        try
        {
            int num = SheetsManagement.getSheetNumber();
            for (int i = num; i > 0; i--)
            {
                SheetsManagement.deleteSheet("Match_"+i);
            }
            SheetsManagement.setSheetNumber(0);
            SheetsManagement.clearData("Datasheet!A2:Y");
            rewriteData();
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }

    }

    public static void rewriteData()
    {
        List<List<Object>> sheetData = new ArrayList<>();
        for (TeamData teamData : teamsInfo)
        {
            sheetData.add(teamData.convertToSpreadsheetRow());
        }
        if (teamsInfo.isEmpty()) sheetData.add(new ArrayList<>(List.of("")));
        SheetsManagement.writeData(sheetData, ADMIN_SHEET, "Datasheet!A2:Y");
    }

    public static void updateHistory(List<MatchUp> matches)
    {
        for (MatchUp m : matches) {
            addOpponent(m.team1, m.team2);
            addOpponent(m.team2, m.team1);
        }
    }

    public static void copyMissingMatches() throws IOException {
        // Go through matches in current round, print names of teams of unfinished games
        int num = SheetsManagement.getSheetNumber();
        String range = "Match_"+num+"!A2:D";
        List<List<Object>> data = SheetsManagement.fetchData(ADMIN_SHEET, range);

        StringBuilder sb = new StringBuilder();
        sb.append("Matches without a score:\n");

        for (List<Object> row : data)
        {
            if (row.get(2).equals("0") && row.get(3).equals("0"))
            {
                sb.append(row.get(0)).append(" vs ").append(row.get(1)).append("\n");
            }
        }

        StringSelection stringSelection = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
    }

    private static void addOpponent( String team, String opponent)
    {
        for (TeamData t : teamsInfo)
        {
            String teamName = t.teamName;
            if (teamName.equals(team))
            {
                t.history.add(opponent);
                return;
            }
        }
    }

    public static void updateOMWP() {

        Map<String, int[]> teamRecords = new HashMap<>();
        for (TeamData team : teamsInfo)
        {
            teamRecords.put(team.teamName, new int[]{team.wins, team.losses});
        }

        for (TeamData team : teamsInfo)
        {
            double sum = 0;
            int count = 0;

            for (String opp : team.history)
            {
                if (!teamRecords.containsKey(opp)) continue;

                int[] rec = teamRecords.get(opp);
                int oppWins = rec[0];
                int oppLosses = rec[1];

                int totalGames = oppWins + oppLosses;
                if (totalGames == 0) continue;

                double winPct = (double) oppWins / totalGames;
                sum += winPct;
                count++;
            }

            team.omwp = (float) ((count == 0) ? 0 : sum / count);
        }
    }

    public static void checkAllTeams(boolean in)
    {
        for (TeamData team : teamsInfo)
        {
            team.checkedIn = in;
        }

        rewriteData();


    }

    public static void addTeam(String name, int seeding)
    {
        TeamData data = new TeamData(name, seeding);
        teamsInfo.add(data);
    }

    public static void getFullData()
    {
        String range = "Datasheet!A2:ZZ";
        List<List<Object>> sheetData = SheetsManagement.fetchData(ADMIN_SHEET, range);

        List<TeamData> data = new ArrayList<>();
        for (List<Object> row : sheetData)
        {
            if (row.isEmpty()) return;
            data.add(new TeamData(row));
        }
        teamsInfo = data;
    }

    public static void sortTeams(boolean seeding)
    {
        /*
            Placing order Wins -> OMWP -> Map Wins -> Map Losses (Inv) -> H2H
         */
        if (!seeding)
        {
            teamsInfo.sort(Comparator.comparingInt((TeamData t) -> t.wins)
                    .thenComparingDouble((TeamData t) -> t.omwp)
                    .thenComparingInt((TeamData t) -> t.map_wins).reversed()
                    .thenComparingInt((TeamData t) -> t.map_losses).reversed()
                    .thenComparingDouble((TeamData t) -> t.seeding).reversed());
        }
        else {
            teamsInfo.sort(Comparator.comparingDouble(o -> o.seeding));
            teamsInfo = teamsInfo.reversed();
        }
    }

    public static void addSeedAndCreateTeams()
    {
        HashMap<String, Double> rankings = calculateSeedingRanks();
        for (Map.Entry<String, Double> entry : rankings.entrySet())
        {
            teamsInfo.add(new TeamData(entry.getKey(), entry.getValue()));
        }

        grantSeedingWins();
        rewriteData();
    }

    public static HashMap<String, Double> calculateSeedingRanks()
    {
        getFullData();
        List<List<Object>> seedData = SheetsManagement.fetchData(ADMIN_SHEET, "Seeding!A1:G");

        HashMap<String, Double> rankings = new HashMap<>();
        List<List<Object>> rawRankings = new ArrayList<>();
        rawRankings.add(new ArrayList<>());
        for (List<Object> row : seedData)
        {
            String name = row.getFirst().toString();
            ArrayList<Integer> ranks = (ArrayList<Integer>) new ArrayList<>(row.subList(2, row.size()))
                    .stream().map(o -> Integer.parseInt(o.toString())).collect(Collectors.toList());
            double rating = SeedingTools.calculateWeightedSeed(ranks);
            rankings.put(name, rating);
            rawRankings.add(new ArrayList<>(Collections.singleton(rating)));
        }

        rawRankings.removeFirst();
        SheetsManagement.writeData(rawRankings, ADMIN_SHEET, "Seeding!I1");

        return rankings;
    }

    public static void grantSeedingWins()
    {
        List<Integer> thresholds = SeedingTools.calcSeedingThresholds(teamsInfo.size());

        sortTeams(true);
        int count = 0;
        for (TeamData t : teamsInfo)
        {
            count++;
            if (count <= thresholds.getFirst())
            {
                t.addWins(3);
                continue;
            }
            if (count <= thresholds.get(1))
            {
                t.addWins(2);
                continue;
            }
            if (count <= thresholds.get(2)) {
                t.addWins(1);
            }
        }
    }
}