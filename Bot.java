import java.util.Random;
import java.util.List;
import java.util.Iterator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.regex.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONObject;
import org.json.JSONArray;

import sx.blah.discord.*;
import sx.blah.discord.api.*;
import sx.blah.discord.api.events.*;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.handle.audio.impl.AudioManager;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.*;
import sx.blah.discord.util.*;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.audio.*;
import sx.blah.discord.util.audio.events.*;
import sx.blah.discord.util.audio.providers.FileProvider;

public class Bot implements Runnable
{
    private boolean done;
    private String apiKey;

    private static volatile IDiscordClient client;
    private static volatile Random rng;
    
    private static volatile String commandPrefix;
    private static volatile HashMap<String,Integer> commands;

    private static volatile SoundQueue soundQueue;
    
    private static volatile java.io.File soundEffectsPath;

    public static void main(String[] args)
    {
	if (args.length != 2)
	    {
		System.out.println("Usage: " + System.getProperty("sun.java.command") + " <API key> <command json file>");
		return;
	    }
	
	Bot bot = new Bot(args[0],args[1]);
	
	Thread t = new Thread(bot);
	t.start();
	
	while (!bot.isDone())
	    {
		try
		    {
			Thread.sleep(1000);
		    }
		catch (InterruptedException e)
		    {
		    }
	    }

	System.out.println("Good bye");
    }

    public boolean isDone()
    {
	return done;
    }
    
    public Bot(String k, String commandFile)
    {
	apiKey = k;
	rng = new Random(System.currentTimeMillis());
	
	soundQueue = new SoundQueue();
	Thread sq = new Thread(soundQueue);
	sq.start();
	    
	commands = new HashMap<String,Integer>();

	try
	    {
		String data = "";
		for (Object obj : Files.lines(Paths.get("./"+commandFile)).toArray())
		{
		    String s = obj.toString();
		    data = data + System.getProperty("line.separator") + s;
		}

		//parse commands.json
		JSONObject obj = new JSONObject(data);

		commandPrefix = obj.getString("prefix");
		
		soundEffectsPath = new java.io.File(obj.getString("sfxPath"));
		
		JSONArray commandsArray = obj.getJSONArray("commands");
		for (int i=0;i<commandsArray.length();i++)
		    {
			JSONObject command = (JSONObject)commandsArray.get(i);
			Integer id = command.getInt("id");
			JSONArray keywords = command.getJSONArray("keywords");
			for (int j=0;j<keywords.length();j++)
			    {
				String keyword = (String)keywords.get(j);
				commands.put(keyword,id);
			    }
		    }
	    }
	catch (Exception e)
	    {
		System.out.println(e.getMessage());
		System.out.println("As such I was unable to load the commands file: " + commandFile);
		e.printStackTrace();
		System.exit(1);
	    }
    }
    
    public void run()
    {
	done = false;
	
	ClientBuilder clientBuilder = new ClientBuilder(); // Creates the ClientBuilder instance
	clientBuilder.withToken(apiKey); // Adds the login info to the builder

	try
	    {
		client = clientBuilder.login(); // Creates the client instance and logs the client in
    
		EventDispatcher dispatcher = client.getDispatcher(); // Gets the EventDispatcher instance for this client instance
		dispatcher.registerListener(this); // registers this class as a listener
	    }
	catch (DiscordException e)
	    {
		System.out.println("Error occored while logging in: " + e.getMessage());
		done = true;
	    }
    }
    
    @EventSubscriber
    public void onReadyEvent(ReadyEvent event)
    {
	System.out.println("Logged in");
	/*
	Radio rd = new Radio();
	Thread t = new Thread(rd);
	t.start();
	*/
    }
    
    @EventSubscriber
    public void onMessageRecievedEvent(MessageReceivedEvent event)
    {
	MessageHandler mh = new MessageHandler(event.getMessage());

	Thread t = new Thread(mh);
	t.start();
    }
    /*
    @EventSubscriber
    public void onTrackFinishEvent(TrackFinishEvent event)
    {
	if (!event.getNewTrack().isPresent()) //only bother if there's nothing left
	    {
		File path = new File("/media/amethyst/");
	    }
    }
*/
    private class MessageHandler implements Runnable
    {
	private IMessage message;
	public MessageHandler(IMessage m)
	{
	    message = m;
	}

	private String processCommands(IMessage message)
	{
	    //setup command hashmap
	    String content = message.getContent();

	    if (!content.startsWith(commandPrefix)) return "";
	    List<IUser> userMentions = message.getMentions();
	    List<IRole> roleMentions = message.getRoleMentions();
	    List<IChannel> channelMentions = message.getChannelMentions();

	    int command = 0;
	    command = commands.get(content.toLowerCase().trim().split(" ")[0].replace(commandPrefix,"")); //grabs the first word, stripping the prefix
	    if (command == 0) return ""; //if we don't get a valid command return an empty string
	    else if (command == 1)
		{
		    return "Hello " + message.getAuthor().mention(true) + "!";
		}
	    else if (command == 2)
		{
		    String out = "";
		    String[] dice = content.toLowerCase().trim().split(" ");
		    for (String s : dice)
			{
			    if (s.contains("d")) //if we have a xdy designation 
				{
				    String[] die = s.split("d");
				    int multiplier = 1;
				    if (die[0].compareTo("") != 0)
					{
					    try
						{
						    multiplier = Integer.parseInt(die[0]);
						}
					    catch (NumberFormatException e){}
					}
				    
				    int dieType = 6;
				    try
					{
					    dieType = Integer.parseInt(die[1]);
					}
				    catch (NumberFormatException e){}
				    for (int i=0; i<multiplier; i++)
					{
					    out += String.format(" %d/%d", (Math.abs(rng.nextInt())%dieType) +1, dieType);
					}
				}
			    else if (s.contains("-")) //x-y designation
				{
				    String[] bounds = s.split("-");
				    int start = 1;
				    int end = 100;
				    try
					{
					    start = Integer.parseInt(bounds[0]);
					    end = Integer.parseInt(bounds[1]);
					    if (start > end)
						{
						    int temp = start;
						    start = end;
						    end = temp;
						}
					}
				    catch (NumberFormatException e){}
				    out += String.format(" %d-%d: %d",start,end,(Math.abs(rng.nextInt())%((end+1)-start))+start);
				}
			}
		    return out;
		}
	    else if (command == 3 || command == 4)
		{
		    //strip out the command
		    String search = "^.*(" + content.trim().replaceFirst("^[^ ]* ","").replace(" ","|") + ").*$";
		    ArrayList<String> matches = new ArrayList<String>();
		    for (String s : soundEffectsPath.list())
			{
			    try
				{
				    if (s.matches(search))
					{
					    matches.add(s);
					}
				}
			    catch (PatternSyntaxException e)
				{
				    return e.getMessage();
				}
			}
		    if (command == 4)
			{
			    String out = "Sound effects matching regex '" + search.replace("*","\\*").replace("_","\\_") + "':\n";
			    for (Object s : matches.toArray())
				{
				    out = out + (String)s + "\n";
				}
			    return out;
			}
		    if (command == 3)
			{
			    String song = matches.get(rng.nextInt(matches.size()));
			    soundQueue.enqueueSound(message.getAuthor(),soundEffectsPath + "/" + song);
			    return "Enqueuing '" + song + "'";
			}
		}
	    else if (command == 99)
		{
		    System.exit(0);
		}
/*	    else if (command == 2) //queue a url
		{
		    try {
			audioPlayer.queue(new URL(content.toLowerCase().trim().split(" ")[1]));
		    }
		    catch (Exception e)
			{
			    //do nothing
			}
		}*/
	    /*
	      ...
	      Additional functionality
	      ...
	    */
	    return "";
	}
	   
	public void run()
	{
	    IGuild guild = message.getGuild();
	    IChannel channel = message.getChannel();
	    System.out.println(message.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ": " +
			       ((guild != null) ? guild.getName() + " -> " + message.getChannel().getName() : "PM") + " <" +
			       message.getAuthor().getName() + ">: " +  message.getContent());

	    if (message.getAuthor().isBot()) return; //don't bother processing other bots or ourselves

	    //here is where the magic happens
	    String output = processCommands(message);

	    if (output.compareTo("") == 0) return;
	    
	    MessageBuilder mb = new MessageBuilder(client);
	    mb.withChannel(channel);	

	    //if the message is over 2000 characters, split at the last space
	    ArrayList<String> outputQueue = new ArrayList<String>();
	    while (output.length() > 2000)
		{
		    int pos = output.lastIndexOf(32,1999);
		    outputQueue.add(output.substring(0,pos));
		    output = output.substring(pos+1);
		}
	    outputQueue.add(output);
	    
	    while (!outputQueue.isEmpty())
		{
		    mb.withContent(outputQueue.remove(0));
	    
		    while (true)
			{
			    try
				{
				    mb.send();
				    break;
				}
			    catch (RateLimitException e)
				{
				    try
					{
					    Thread.sleep(Math.abs(rng.nextLong())%30000);
					}
				    catch (InterruptedException ex)
					{
					}
				}
			    catch (MissingPermissionsException e)
				{
				    System.out.println("Missing permissions to send reply.");
				    break;
				}
			    catch (DiscordException e)
				{
				    System.out.println("Error sending message: " + e.getMessage());
				    break;
				}
			}
		}
	}
    }

    private class SoundQueue implements Runnable
    {
	private volatile ArrayList<SoundPlayer> queue;

	public SoundQueue()
	{
	    queue = new ArrayList<SoundPlayer>();
	}

	//gets the voice channel and parses the sound effect
	public void enqueueSound(IUser user, String soundPath)
	{
	    IVoiceChannel voiceChannel = null;
	    
	    for (IVoiceState i : user.getVoiceStatesLong().values())
		{
		    if (i.getChannel() != null)
			{
			    voiceChannel = i.getChannel();
			    break;
			}
		}

	    queue.add(new SoundPlayer(voiceChannel,soundPath));
	}

	//dequeues sound effect and plays it, checking for any additional sounds afterwards
	public void run()
	{
	    while (true)
		{
		    if (queue.size() > 0)
			{
			    SoundPlayer soundPlayer = queue.remove(0);
			    Thread t = new Thread(soundPlayer);
			    t.start();
			    try
				{
				    t.join();
				    
				}
			    catch (Exception e)
				{
				}
			}
		    Thread.yield();
		}
	}

    }
    
    private class SoundPlayer implements Runnable
    {
	private AudioPlayer audioPlayer;
	private IVoiceChannel voiceChannel;
	private String soundPath;
	
	public SoundPlayer(IVoiceChannel vc, String sp)
	{
	    soundPath = sp;
	    voiceChannel = vc;

	    IGuild guild = voiceChannel.getGuild();
	    AudioManager audioManager = (AudioManager)guild.getAudioManager();
	    audioPlayer = AudioPlayer.getAudioPlayerForAudioManager(audioManager);
	}
	
	public void run()
	{
	    try
		{
		    voiceChannel.join();
		    AudioPlayer.Track track = audioPlayer.queue(new java.io.File(soundPath));

		    while (track.isReady() || track.getCurrentTrackTime() < track.getTotalTrackTime())
			{
			    Thread.yield();
			}
		}
	    catch (Exception ex)
		{
		    System.out.println(ex.getMessage());
		}
	    voiceChannel.leave();
	}
    }
    /*
    private class Radio implements Runnable
    {
	public Radio()
	{
	    //ehh
	}

	public void run()
	{
	    try
		{
		    //start the audio player
		    IGuild guild = client.getGuildByID(new Long("84732992869629952"));
		    IVoiceChannel voiceChannel = guild.getVoiceChannelByID(new Long("102785386186575872"));
		    
		    try
			{
			    voiceChannel.join();
			}
		    catch (Exception ex)
			{
			    //do nothing
			}

		    audioManager = (AudioManager)guild.getAudioManager();
		    audioPlayer = AudioPlayer.getAudioPlayerForAudioManager(audioManager);

		    List<AudioPlayer.Track> playlist = audioPlayer.getPlaylist();

		    while (true)
			{

			    try
				{
				    audioPlayer.clear();
				    audioPlayer.queue(new java.io.File("/tmp/radio"));
				    audioPlayer.setVolume(new Float(0.9));
				    AudioPlayer.Track track = audioPlayer.getCurrentTrack();

				    System.out.println("Encoding: " + track.getStream().getFormat().toString());
				}
			    catch(Exception e)
				{
				    System.out.println("Error: " + e.getMessage());
				    e.printStackTrace();
				}
			    try
				{
				    System.out.println("Sleeping...");
				    Thread.sleep(60000);
				}
			    catch (InterruptedException e)
				{
				}
			}
		
		    //audioPlayer.queue(new java.io.File("/media/amethyst/muchmusic/dolphinhats/Ass & Tittiez - Crizzly & Logun-82827465.mp3"));
		
		}
	    catch (Exception e)
		{
		    System.out.println("Error: " + e.getMessage());
		}	
	    
	}
    }
    */
}

