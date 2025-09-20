package com.uss.madsies;

import java.util.*;

public class Matchmaker
{
    /*
        // Pool teams by number of wins
        // Place 1st in pool vs last in pool
        // If odd number in pool, downfloat the weakest to the lower class
        // If odd in total, give a bye to the highest team in the lowest class

        @param sheetData Raw sheet data from google sheets
        @return List of Matchup objects for optimal matches
        @
     */

    public static List<MatchUp> createSwissMatchups(List<TeamData> teamData)
    {


        List<MatchUp> matchups = new ArrayList<MatchUp>();
        List<String> currentPool = new ArrayList<>();
        int poolSize = 0;
        int bracketWins = -1;

        for (TeamData team : teamData)
        {
            if (bracketWins == -1) bracketWins = team.wins;

            if (bracketWins == team.wins)
            {
                poolSize++;
                currentPool.add(team.teamName);
            }
            else
            {
                String downFloat = "";
                if (poolSize % 2 != 0)
                {
                    downFloat = currentPool.getLast();
                    currentPool.removeLast();
                    poolSize--;
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

                if (!Objects.equals(downFloat, ""))
                {
                    currentPool.add(downFloat);
                    poolSize++;
                    bracketWins--;
                }
                currentPool.add(team.teamName);
                poolSize++;
            }
        }

        System.out.println("Pool size: " + poolSize);
        if (poolSize % 2 != 0)
        {
            currentPool.add("BYE");
            poolSize++;
        }
        for (int i = 0; i < poolSize/2; i++)
        {
            matchups.add(new MatchUp(currentPool.get(i), currentPool.get(poolSize-1-i)));
        }
        currentPool.clear();


        for (MatchUp matchup : matchups)
        {
            System.out.println(matchup.toString());
        }


        return matchups;
    }
}
