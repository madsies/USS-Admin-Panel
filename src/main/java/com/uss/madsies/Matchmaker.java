package com.uss.madsies;

import java.awt.event.WindowStateListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Matchmaker
{
    public static List<MatchUp> createSwissMatchups(List<List<Object>> sheetData)
    {
        // Pool teams by number of wins
        // Place 1st in pool vs last in pool
        // If odd number in pool, downfloat the weakest to the lower class
        // If odd in total, give a bye to the highest team in the lowest class

        List<MatchUp> matchups = new ArrayList<MatchUp>();

        List<String> currentPool = new ArrayList<>();
        int poolSize = 0;
        int bracketWins = -1;

        for (List<Object> teamData : sheetData)
        {
            int wins = Integer.parseInt(teamData.get(4).toString());

            if (bracketWins == -1) bracketWins = wins;

            if (bracketWins == wins)
            {
                poolSize++;
                currentPool.add(teamData.getFirst().toString());
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
                currentPool.add(teamData.getFirst().toString());
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
