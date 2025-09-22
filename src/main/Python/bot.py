import discord
from discord.ext import commands

from os import listdir
from datetime import datetime

from google.auth.transport.requests import Request
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError

import os
from dotenv import load_dotenv
load_dotenv()

SCOPES = ["https://www.googleapis.com/auth/spreadsheets"]

class MyBot(commands.Bot):
    async def on_ready(self):
        print(f"[{datetime.now().strftime('%H:%M:%S')}] Logged on as {self.user}")
    
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
    
class SheetsManagement():
    def __init__(self):
        creds = None

        self.ADMIN_SHEET = os.environ.get("ADMIN_SHEET");

        if os.path.exists("token.json"):
            creds = Credentials.from_authorized_user_file("token.json", SCOPES)

        if not creds or not creds.valid:

            if creds and creds.expired and creds.refresh_token:
                creds.refresh(Request())
            else:
                BASE_DIR = os.path.dirname(os.path.abspath(__file__))
                CREDS_DIR = os.path.join(BASE_DIR, "credentials.json")

                flow = InstalledAppFlow.from_client_secrets_file(
                    CREDS_DIR, SCOPES
                )
                creds = flow.run_local_server(port=0)

            with open("token.json", "w") as token:
                token.write(creds.to_json())

        try:
            self.service = build("sheets", "v4", credentials=creds)
            self.sheet = self.service.spreadsheets()

        except HttpError as err:
            print(err)

    def write_data(self, data, query):
        body = {"values": data}
        result = (
            self.service.spreadsheets()
            .values()
            .update(
                spreadsheetId=self.ADMIN_SHEET,
                range=query,
                valueInputOption="USER_ENTERED",
                body=body,
            )
            .execute())
    

    def read_data(self, query):
        result = (
        self.sheet.values()
        .get(spreadsheetId=self.ADMIN_SHEET, range=query)
        .execute())

        return result.get("values", [])

def main():
    INTENTS = discord.Intents.default()
    INTENTS.message_content = True
    INTENTS.members = True

    client = MyBot(command_prefix='!', intents=INTENTS)
    
    client.run(os.environ.get('DISCORD_BOT_TOKEN'))

if __name__ == "__main__":
    main()