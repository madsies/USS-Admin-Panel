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

        data = self.manager.read_data("DataSheet!A1")

        await ctx.reply(data)

        # Check database if username is in list
        # if username in list
        #    if team checked in
        #       send "team already checked in"
        #    else
        #       check team in
        #else
        #    say you arent a captain


    @commands.hybrid_command(name="checkout")
    async def check_out(self, ctx):
        sender : discord.Member = ctx.author

        # Check database if username is in list
        # if username in list
        #    if team checked out
        #       send "team already checked out"
        #    else
        #       check team out
        #else
        #    say you arent a captain


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(CheckInCommands(bot))