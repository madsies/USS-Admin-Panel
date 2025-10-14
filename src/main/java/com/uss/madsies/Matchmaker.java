package com.uss.madsies;

import java.util.*;

public class Matchmaker {
    /*
        // Pool teams by number of wins
        // Place 1st in pool vs last in pool
        // If odd number in pool, downfloat the weakest to the lower class
        // If odd in total, give a bye to the highest team in the lowest class

        @param sheetData Raw sheet data from Google sheets
        @return List of Matchup objects for optimal matches
     */

    public static List<MatchUp> createSwissMatchups(List<TeamData> teamData) {

        List<MatchUp> matchups = new ArrayList<>();
        List<TeamData> currentPool = new ArrayList<>();
        TeamData downFloat = null;
        TeamData possibleDownFloat = null;
        int poolSize = 0;
        int bracketWins = -1;

        for (TeamData team : teamData)
        {
            if (!team.checkedIn) continue;
            System.out.println(poolSize);
            if (bracketWins == -1) bracketWins = team.wins;

            if (bracketWins == team.wins)
            {
                poolSize++;
                currentPool.add(team);
            }
            else
            {
                // Checks if previous pool had a downfloat, stops high teams cascading down to low games
                if (downFloat != null)
                {
                    possibleDownFloat = currentPool.get(1);
                }
                else
                {
                    possibleDownFloat = currentPool.getFirst();
                }

                if (poolSize % 2 != 0)
                {
                    downFloat = possibleDownFloat;  // Downfloat team choice
                    currentPool.remove(possibleDownFloat);
                    poolSize--;
                }
                else
                {
                    downFloat = null;
                }

                if (poolSize != 0)
                {
                    for (int i = 0; i < poolSize / 2; i++)
                    {
                        matchups.add(new MatchUp(currentPool.get(i), currentPool.get(poolSize - 1 - i)));
                    }
                }

                currentPool.clear();
                poolSize = 0;

                if (!Objects.equals(downFloat, null)) {
                    currentPool.add(downFloat);
                    poolSize++;
                    bracketWins--;
                }
                currentPool.add(team);
                poolSize++;
            }
        }

        if (poolSize % 2 != 0) {
            currentPool.add(new TeamData("BYE", -1));
            poolSize++;
        }
        for (int i = 0; i < poolSize / 2; i++)
        {
            matchups.add(new MatchUp(currentPool.get(i), currentPool.get(poolSize - 1 - i)));
        }
        currentPool.clear();


        for (MatchUp matchup : matchups)
        {
            System.out.println(matchup.toString());
        }

        return matchups;
    }

    public static String getMatchupsString(List<MatchUp> matchups)
    {
        StringBuilder sb = new StringBuilder();
        for (MatchUp match : matchups)
        {
            sb.append(match.toString()).append("\n");
        }
        return sb.toString();
    }
}
