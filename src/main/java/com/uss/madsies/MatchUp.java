package com.uss.madsies;

public class MatchUp
{
    public MatchUp(String team1, String team2)
    {
        this.team1 = team1;
        this.team2 = team2;
    }
    public String team1;
    public String team2;
    public int scoreTeam1;
    public int scoreTeam2;

    @Override
    public String toString()
    {
        return team1 + " vs " + team2;
    }
}
