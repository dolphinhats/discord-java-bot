# Discord General Use API Bot

## Requires
   - [DiscordJ4](https://github.com/austinv11/Discord4J) simply download the shaded jar from [here](https://austinv11.github.io/Discord4J/downloads.html) (currently only tested with version 2.8.1 however most newer versions should still work) or build it yourself and place it in the root directory of this repo. (todo: automate this process)
   - [JSON-java](https://github.com/stleary/JSON-java) This will be downloaded and compiled automatically by the Makefile script.

## Settings

   - `commands.json` contains mappings between keywords and values which are referenced in the Bot class in the `processCommand` function which processes the input and perfoms whatever commands are chosen.

   - `api-key.txt` contains the api key for the bot obtained from [here](https://discordapp.com/developers/applications/me). This is required to be set manually. 

## To run

Typing ```$ make run``` will compile the json package and Bot.java, then run the Bot class with the API key given by `api-key.txt`.

## Feature Requests and Ideas

   - [x] Roll dice
   - [x] Voice chat sound effects
   - [] Cross-server posting
   - [] Wiktionary search
   - [] Redesign command methodology
      - Command interface classes?
      - Python plugins?
      - Events?
   - [] Command !help function