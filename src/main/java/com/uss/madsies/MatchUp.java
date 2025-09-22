package com.uss.madsies;

public class MatchUp
{
    public MatchUp(TeamData team1, TeamData team2)
    {
        this.team1 = team1;
        this.team2 = team2;
    }
    public TeamData team1;
    public TeamData team2;

    @Override
    public String toString()
    {
        return team1.teamName + " vs " + team2.teamName;
    }
}
