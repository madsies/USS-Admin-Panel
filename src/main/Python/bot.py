import discord
from discord.ext import commands

from os import listdir
from datetime import datetime

import os
from dotenv import load_dotenv
load_dotenv()

class MyBot(commands.Bot):
    async def on_ready(self):
        print(f"[{datetime.now().strftime('%H:%M:%S')}] Logged on as {self.user}")
        return await super().on_ready()
    
    async def load_extension(self, name, *, package = None):
        return await super().load_extension(name, package=package)

    async def setup_hook(self):
        BASE_DIR = os.path.dirname(os.path.abspath(__file__))
        COGS_DIR = os.path.join(BASE_DIR, "cogs")
        for cog in os.listdir(COGS_DIR):
            if cog.endswith('.py') == True:
                print(f"LOADING: cogs.{cog[:-3]}")
                await self.load_extension(f'cogs.{cog[:-3]}')
                
        await self.tree.sync() # Updates Slash Commands
        return await super().setup_hook()    
    

def main():
    INTENTS = discord.Intents.default()
    INTENTS.message_content = True
    INTENTS.members = True

    client = MyBot(command_prefix='!', intents=INTENTS)
    
    client.run(os.environ.get('DISCORD_BOT_TOKEN'))
    

    

if __name__ == "__main__":
    main()