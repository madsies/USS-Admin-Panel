import discord
from discord.ext import commands

import sys
from pathlib import Path

parent_dir = Path(__file__).resolve().parent.parent

sys.path.append(str(parent_dir))

from bot import SheetsManagement

USS_COLOUR = 0x992299
ADMIN_ROLE_NAME = "ow admin"
LEAD_ROLE_NAME = "staff lead"

def is_admin():
    async def predicate(ctx):
        return any((role.name.lower() == ADMIN_ROLE_NAME or role.name.lower() == LEAD_ROLE_NAME) for role in ctx.author.roles)
    return commands.check(predicate)

class CheckInCommands(commands.Cog):
    def __init__(self, bot):
        self.manager = SheetsManagement()
        self.teamsMapped = {}
        self.teamsMapped_user = {}
        self.checkInActive = False
        self.sync_team_data()
    
    @commands.hybrid_command(name="checkin")
    async def check_in(self, ctx):
        sender : discord.Member = ctx.author

        if (self.checkInActive):
            message = self.check_in_sheet(sender.name)
        else:
            message = "Check-Ins are currently closed."

        embed = discord.Embed(title="USS Checkin", description=message, colour=USS_COLOUR)
        embed.set_thumbnail(url="https://pbs.twimg.com/profile_images/1969783689090363392/v_27TFgp_400x400.jpg")

        await ctx.reply(embed=embed)

    @commands.hybrid_command(name="checkout")
    async def check_out(self, ctx):
        sender : discord.Member = ctx.author

        if (self.checkInActive):
            message = self.check_out_sheet(sender.name)
        else:
            message = "Check-Ins are currently closed."
            
        embed = discord.Embed(title="USS Checkin", description=message, colour=USS_COLOUR)
        embed.set_thumbnail(url="https://pbs.twimg.com/profile_images/1969783689090363392/v_27TFgp_400x400.jpg")

        await ctx.reply(embed=embed)

    @commands.hybrid_command(name="getcaptain")
    async def get_btag(self, ctx, team :str):
        if team.lower() not in self.teamsMapped:
            message = f"Could not find team {team}"
        else:
            message = f"""**{self.teamsMapped[team.lower()]['formalised_name']}**\nCaptain's Discord: {self.teamsMapped[team.lower()]['discord']}\n
            **Players:**
            {self.teamsMapped[team.lower()]['bnet']}
            {self.teamsMapped[team.lower()]['bnet2']}
            {self.teamsMapped[team.lower()]['bnet3']}
            {self.teamsMapped[team.lower()]['bnet4']}
            {self.teamsMapped[team.lower()]['bnet5']}
            """
        embed = discord.Embed(title="USS Checkin", description=message, colour=USS_COLOUR)
        embed.set_thumbnail(url="https://pbs.twimg.com/profile_images/1969783689090363392/v_27TFgp_400x400.jpg")
        await ctx.reply(embed=embed)

    @commands.hybrid_command(name="refreshteamdata")
    @is_admin()
    async def refresh_team_data(self, ctx):
        self.sync_team_data()
        await ctx.reply("Refreshed data")

    @commands.hybrid_group(name="admincheckin")
    async def admincheckin(self, ctx):
        return

    @admincheckin.command(name="open")
    @is_admin()
    async def open_check_in(self, ctx):
        self.checkInActive = True;
        await ctx.reply("Check-in has been opened")

    @admincheckin.command(name="close")
    @is_admin()
    async def open_check_in(self, ctx):
        self.checkInActive = False;
        await ctx.reply("Check-in has been closed")

    @admincheckin.command(name="status")
    @is_admin()
    async def open_check_in(self, ctx):
        if (self.checkInActive):
            await ctx.reply("Check-in is open")
        else:
            await ctx.reply("Check-in is closed")
    
    @commands.Cog.listener()
    async def on_command_error(self, ctx, error):
        if isinstance(error, commands.CheckFailure):
            await ctx.reply("You need the **admin** role to use this command.")

    def sync_team_data(self):
        self.teamsMapped.clear()
        data : list = self.manager.read_data("TeamContact!A2:G")
        for row in data:
            self.teamsMapped[row[0].lower()] = {"discord":row[1], "bnet": row[2], "bnet2": row[3], "bnet3": row[4], "bnet4": row[5], "bnet5": row[6], "formalised_name": row[0]}
            self.teamsMapped_user[row[1].lower()] = {"team_name":row[0], "bnet": row[2], "bnet2": row[3], "bnet3": row[4], "bnet4": row[5], "bnet5": row[6]}

        print(self.teamsMapped)

    def get_team_from_user(self, username : str):
        if username not in self.teamsMapped_user: return "N/A"

        return self.teamsMapped_user[username]["team_name"]

    def find_and_flip_checkin(self, teamName : str, checkin : bool):
        if teamName == "N/A": return "You are not registered as a captain\n\nIf you believe this is wrong, please contact an admin"
        data : list = self.manager.read_data("Datasheet!A2:C")

        i = 2
        for row in data:
            if row[0] == teamName:
                flag = self.google_bool(row[2])
                if checkin and not flag:
                    self.manager.write_data([[checkin]], f"Datasheet!C{i}")
                    return f"Checked in {teamName}"
                elif checkin and flag:
                    return f"{teamName} are already checked in."
                elif not checkin and not flag:
                    return f"{teamName} are already checked out."
                else:
                    self.manager.write_data([[checkin]], f"Datasheet!C{i}")
                    return f"Checked out {teamName}"

            i = i+1

    def check_in_sheet(self, username:str):
        return self.find_and_flip_checkin(self.get_team_from_user(username.lower()),True)
    
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