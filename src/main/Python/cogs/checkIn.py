import discord
from discord.ext import commands

import sys
from pathlib import Path

parent_dir = Path(__file__).resolve().parent.parent
sys.path.append(str(parent_dir))

from bot import SheetsManagement

class CheckInCommands(commands.Cog):
    def __init__(self, bot):
        self.manager = SheetsManagement()
    
    @commands.hybrid_command(name="checkin")
    async def check_in(self, ctx):
        sender : discord.Member = ctx.author

        val = self.check_in_sheet(sender.name)
        await ctx.reply(val)

    @commands.hybrid_command(name="checkout")
    async def check_out(self, ctx):
        sender : discord.Member = ctx.author

        val = self.check_out_sheet(sender.name)
        await ctx.reply(val)

  
    def get_team_from_user(self, username : str):
        data : list = self.manager.read_data("TeamContact!A1:B")
        for row in data:
            if row[1] == username:
                print("Found user")
                return row[0]
            
        return "N/A"

    def find_and_flip_checkin(self, teamName : str, checkin : bool):
        if teamName == "N/A": return "You are not registerd as a captain\nIf you believe this is wrong contact an admin"
        data : list = self.manager.read_data("Datasheet!A2:C")

        i = 2
        for row in data:
            if row[0] == teamName:
                print("Team Found")
                flag = self.google_bool(row[2])
                if checkin and not flag:
                    self.manager.write_data([[checkin]], f"Datasheet!C{i}")
                    return f"Checked in {teamName}"
                elif checkin and flag:
                    return f"{teamName} is already checked in."
                elif not checkin and not flag:
                    return f"{teamName} is already checked out."
                else:
                    self.manager.write_data([[checkin]], f"Datasheet!C{i}")
                    return f"Checked out {teamName}"

            i = i+1

    def check_in_sheet(self, username:str):
        return self.find_and_flip_checkin(self.get_team_from_user(username),True)
    
    def check_out_sheet(self, username:str):
        return self.find_and_flip_checkin(self.get_team_from_user(username),False)
    
    def google_bool(self, value):
        if isinstance(value, bool):
            return value 
        if isinstance(value, str):
            return value.strip().upper() == "TRUE"
        return bool(value) 


        
        


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(CheckInCommands(bot))