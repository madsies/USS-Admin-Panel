package com.uss.madsies;

import java.util.ArrayList;
import java.util.List;

public class SeedingTools
{
    public static final double HIGH_THRESHOLD_PERCENT = 15;
    public static final double MID_THRESHOLD_PERCENT = 40;
    public static final double LOW_THRESHOLD_PERCENT = 70;

    /**
     *  Sorts Team rankings with highest rank first
     *
     * @param data The array of player ranks to convert
     * @return Sorted array of player data
     */

    public static List<Integer> sortByRank(List<Integer> data)
    {
        data.sort(Integer::compareTo);
        return data.reversed();
    }

    /**
        Intakes array of ranks, and gives it a seeding rank

        Highest rank *1.5, *1.3, *1.1 and base for others

        @param data The array of player ranks to convert
        @return The teams calculated seeding score
     */
    public static float calculateWeightedSeed(List<Integer> data)
    {
        data = sortByRank(data);
        float weightedScore = (float) Math.floor
                (data.get(0)*1.5f + data.get(1) * 1.3f + data.get(2) * 1.1f + data.get(3)+data.get(4));

        return weightedScore / 5.9f;
    }

    /**
     *  Tiebreaker for teams that have equal seeding score
     *  The team with the highest ranked 1st player is chosen, if a tie, 2nd player
     *  If all players have identical ranks, the 1st team is chosen in tiebreak.
     *
     * @param team1 First team in comparison
     * @param team2 Team to compare to
     * @return If the left team should be higher seed
     */

    public static boolean seedTiebreaker(List<Integer> team1, List<Integer> team2)
    {
        List<Integer> sortedTeam1 = sortByRank(team1);
        List<Integer> sortedTeam2 = sortByRank(team2);

        for (int idx = 0; idx < sortedTeam1.size(); idx++)
        {
            if (sortedTeam1.get(idx) > sortedTeam2.get(idx)) return true;
            if (sortedTeam2.get(idx) > sortedTeam1.get(idx)) return false;
        }
        return true;
    }

    /**
     *  Returns a list of cutoffs of teams that gain seeding points
     *
     *  @param teamCount Number of teams in tournament
     *  @return List of the cut-off points of seed-based point boundaries
     */

    public static List<Integer> calcSeedingThresholds(int teamCount)
    {
        List<Integer> thresholds = calcSeedingThreshold_light(teamCount);
        thresholds.add((int) Math.ceil(LOW_THRESHOLD_PERCENT /100 * teamCount));

        return thresholds;
    }

    public static List<Integer> calcSeedingThreshold_light(int teamCount)
    {
        List<Integer> thresholds = new ArrayList<Integer>();
        thresholds.add((int) Math.ceil(HIGH_THRESHOLD_PERCENT /100 * teamCount));
        thresholds.add((int) Math.ceil(MID_THRESHOLD_PERCENT /100 * teamCount));
        return thresholds;
    }
}
