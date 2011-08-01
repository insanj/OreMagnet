/*
 Created by Julian Weiss (insanj), updates frequent on Google+ (and sometimes Twitter)!

 Please do not modify or decompile at any date, but feel free to distribute with credit.
 Designed and created entirely on Friday, June 8th, 2011.
 Last edited on: 7/31/11

 OreMagnet Version 1.2!
 Special thanks to: 
 		Aaron Zehm, for some alpha testing and brainstorming.
 		Matthew Weiss, for practicality-checks, resource lister, and for being an idea-bouncing wall.
		nossr50, for helping solve a big problem with implementing mcMMO in the Beta version.

 Works with the current CraftBukkit Build (#953).
 All other information should be available at bukkit.org under OreMagnet.

 Currently supports:
		Permissions plugin, version 3.1.6!
		McMMO
		iConomy!

 THIS VERSION CURRENT HAS THREE CLASSES:
			OreMagnet.java
			OreListener.java
			OreServer.java

*/

package me.insanj.OreMagnet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.nossr50.mcMMO;
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.iConomy.*;

public class OreMagnet extends JavaPlugin
{
	
	private static final Logger log = Logger.getLogger("Minecraft");
	private final OreListener blockListener = new OreListener(this);
	private final OreServer serverListener = new OreServer(this);
	public final ArrayList<Player> MagnetUsers = new ArrayList<Player>();
	
	public static PermissionHandler permissionHandler;
	public static mcMMO mmoPlugin;
	public boolean mmoChecker;
    public iConomy iConomy;
	
	public static ArrayList<String> text = new ArrayList<String>();
	public static ArrayList<String> text2 = new ArrayList<String>();
	public static String directory = "plugins/OreMagnet";
	String version = "1.2";
	
	static double cooldown;
	static boolean runtime = true;
	public Integer mmoExp = 5;

	//When the plugin is enabled...
	@Override
	public void onEnable()
	{
		log.info("{OreMagnet} version " + version + " (by insanj) has been enabled!");
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.BLOCK_DAMAGE, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLUGIN_ENABLE, serverListener, Event.Priority.Normal, this);
	    pm.registerEvent(Event.Type.PLUGIN_DISABLE, serverListener, Event.Priority.Normal, this);
		
		addLines();
		
		//If the config file doesn't already exist, generate the two text files.
		if( !(new File("plugins/OreMagnet/config.txt").exists()) ){
			try{
				//README
				new File(directory).mkdir();
				File file = new File("plugins/OreMagnet/readme.txt");
				Writer output = new BufferedWriter(new FileWriter(file));
				
				for(int i = 0; i < text.size(); i++)
					output.write(text.get(i));
				
				output.close();
				
				//MAIN CONFIG
				File config = new File("plugins/OreMagnet/config.txt");
				BufferedWriter configWriter = new BufferedWriter(new FileWriter(config));
				
				for(int i = 0; i < text2.size(); i++)
					configWriter.write(text2.get(i));
				
				configWriter.flush();
				
				log.info("{OreMagnet} successfully created the OreMagnet folder and containing files!");

			}//end try
			
			//If there's a problem generating!
			catch (Exception e){
				log.severe("{OreMagnet} had a problem creating/storing in the directory! Error: " + e.getMessage());
			}
			
		}//end if

		//If the directory DOES exist...
		else{
			boolean regenerate = false;
			
			//Reads from the config file, sees if the property "regenerate: true" exists.
			try{
				Scanner outdoors = new Scanner(new File("plugins/OreMagnet/config.txt"));
				
				while( outdoors.hasNextLine() ){
		    		String next = outdoors.nextLine();
					if(next.contains("regenerate: ") )
						regenerate = Boolean.parseBoolean(next.substring(12));
				}//end while
				
			}//end try
			catch(Exception e){
				log.severe("{OreMagnet} had some trouble accessing its \"config.txt\" file! >:( ");
			}
			
			//Regenerate as if the directory never existed, if the property tests true.
			if(regenerate){
				try{
					//README
					new File(directory).mkdir();
					File file = new File("plugins/OreMagnet/readme.txt");
					Writer output = new BufferedWriter(new FileWriter(file));
				
					for(int i = 0; i < text.size(); i++)
						output.write(text.get(i));
				
					output.close();
				
					//MAIN CONFIG
					File config = new File("plugins/OreMagnet/config.txt");
					BufferedWriter configWriter = new BufferedWriter(new FileWriter(config));
				
					for(int i = 0; i < text2.size(); i++)
						configWriter.write(text2.get(i));
				
					configWriter.close();	
					
					log.info("{OreMagnet} successfully regenerated the OreMagnet folder and containing files!");
				}
				catch (Exception e){
					log.info("{OreMagnet} had a problem creating/storing in the directory! Error: " + e.getMessage());
				}
			}//end if
			
		}//end else
		
		setupPermissions();
		setupmcMMO();
	}
	
	@Override
	public void onDisable()
	{
		log.info("{OreMagnet} version " + version + " (by insanj) has been disabled!");
	}
	
	//When the user calls any command with "/magnet" as the beginning.
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		if(!(sender instanceof Player)){
			log.info("{OreMagnet} doesn't think it's very funny that something that isn't a Player thinks it can mine ores. :P");
			return true;
		}
		
		Player player = (Player) sender;
		
		//For just the normal OreMagnet.
		if( commandLabel.equalsIgnoreCase("magnet") && permissionsTester(player, new String[] {"use", "always"}) ){
			if(args.length == 0){
				toggleMagnet(player);
				return true;
			}//end args if
			
			//For setting a personal radius.
			else if(args.length > 1){
				if( (args[0].equals("radius") && args.length == 2) && permissionsTester(player, new String[] {"radius"}) ){
					try{
						if( !configContains(player.getDisplayName() + " radius: ") ){
							FileWriter writer = new FileWriter("plugins/OreMagnet/config.txt", true);
						
							double userRadius = Double.parseDouble(args[1]);
							writer.write(player.getDisplayName() + " radius: " + userRadius + "\n");
							player.sendMessage(ChatColor.GREEN + "Your personal radius of " + userRadius + " has been set!");
							writer.close();
						}//end if
						
						else{
							amendProperty( (player.getDisplayName() + " radius: "), (player.getDisplayName() + " radius: " + Double.parseDouble(args[1])) );
							player.sendMessage(ChatColor.GREEN + "Your personal radius of " + Double.parseDouble(args[1]) + " has been set!");
						}//end else
						
					}//end try
					
					catch(Exception e){
						System.out.println(e.getMessage());
						player.sendMessage(ChatColor.RED + "Uh-oh, there was something wrong with your syntax or the config file!\n" + ChatColor.GRAY + " Try: /magnet radius [#] .");
					}
					
					return true;

				}//end if
				
				//For setting a personal cooldown.
				else if( (args[0].equals("cooldown") && args.length == 2) && permissionsTester(player, new String[] {"cooldown"}) ){
					try{
						if( !configContains(player.getDisplayName() + " cooldown: ") ){
							FileWriter writer = new FileWriter("plugins/OreMagnet/config.txt", true);

							double userCooldown = Double.parseDouble(args[1]);
							writer.write(player.getDisplayName() + " cooldown: " + userCooldown + "\n");
							player.sendMessage(ChatColor.YELLOW + "Your personal cooldown of " + userCooldown + " has been set!");
							writer.close();
						}//end if
						
						else{
							amendProperty( (player.getDisplayName() + " cooldown: "), (player.getDisplayName() + " cooldown: " + Double.parseDouble(args[1])) );
							player.sendMessage(ChatColor.YELLOW + "Your personal cooldown of " + Double.parseDouble(args[1]) + " has been set!");
						}//end else
						
					}//end try
					catch(Exception e){
						player.sendMessage(ChatColor.RED + "Uh-oh, there was something wrong with your syntax or the config file!\n" + ChatColor.GRAY + " Try: /magnet cooldown [#] .");
					}
					
					return true;
				}//end else if
			}//end else if 
			
		}//end commandlabel if
		
		return false;
		
	}//end onCommand()
	
	//Toggles whether or not the plugin is enabled for a user.
	private void toggleMagnet(Player sender){
		if( !enabled(sender) ){
			MagnetUsers.add(sender);
			sender.sendMessage(ChatColor.BLUE + "OreMagnet has been enabled! " + ChatColor.GRAY + "(/magnet)");
		}
		
		else {
			MagnetUsers.remove(sender);
			sender.sendMessage(ChatColor.RED + "OreMagnet has been disabled!");
		}
		
	}//end toggleMagnet()
	
	//Checks if the plugin is enabled for a user.
	public boolean enabled(Player player){
		return MagnetUsers.contains(player);
		
	}//end enabled()
	
	//Checks if a cooldown is still active for a given player (returns amount left).
	public double checkCooldown(Player player){		
		return cooldown;
		
	}//end checkCooldown()
	
	//Resets the cooldown for a player.
	public TimerTask resetCooldown(Player player) throws InterruptedException{
		String strCooldown = "";
		boolean found = false;

		//Reads from the config, sees what the cooldown for the player is (if set).
		try{
			Scanner outdoors = new Scanner(new File("plugins/OreMagnet/config.txt"));
			
			while( outdoors.hasNextLine() ){
	    		String next = outdoors.nextLine();
				if(next.contains(player.getDisplayName() + " cooldown: ") ){
					strCooldown = next.substring(player.getDisplayName().length() + 11);
					found = true;
				}
			}//end while
			
			if(!found){
				while( outdoors.hasNextLine() ){
		    		String next = outdoors.nextLine();
					if(next.contains(player.getDisplayName() + "mine_cooldown: ") ){
						strCooldown = next.substring(15);
						found = true;
					}
				}//end while
			}//end if
			
		} catch (Exception e){
			log.severe("{OreMagnet} had trouble with reading a cooldown, when " + player.getDisplayName() + " used OreMagnet! Check the config!" );
		}
		

		try{
			cooldown = Double.parseDouble(strCooldown);
		}catch(Exception e){
			log.severe("{OreMagnet} uhh... It looks like the global cooldown, and/or " + player.getDisplayName() + "'s cooldown is messed up... Defaulting to 10 seconds.");
			cooldown = 10;
		}

		cooldownManager();
		return null;
		
	}//end resetCooldown()
	
	//Manages the cooldown: waits a second, then updates the cooldown, until cooldown == 0.
	private void cooldownManager() throws InterruptedException{
		Timer countdown = new Timer();
		
		while(cooldown > 0 && runtime == true){
			runtime = false;
			countdown.schedule(new minusTask(), 500);
		}
		
	}//end cooldownManager()
	
	//Creates a type of task that minuses the cooldown by one. Neat!
	class minusTask extends TimerTask {

		@Override
		public void run()  {
			cooldown = cooldown - .5;
			runtime = true;
			
			try {
				cooldownManager();
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
			
		}//end run
	}//end class TimerTask

	//Sets up the Permissions plugin.
	private void setupPermissions() {
	    if (permissionHandler != null) 
	    	return;
	    
	    Plugin permissionsPlugin = this.getServer().getPluginManager().getPlugin("Permissions");
	    
	    if (permissionsPlugin == null) 
	    {
	        log.warning("{OreMagnet} didn't find a permissions setup, and is defaulting to OP-only.");
	        return;
	    }
	    
	    permissionHandler = ((Permissions) permissionsPlugin).getHandler();
	    log.info("{OreMagnet} found a permissions system, and will use " + ((Permissions)permissionsPlugin).getDescription().getFullName() + "!");
	    
	}//end setupPermissions()
	
	//Sets up the mcMMO plugin.
	private void setupmcMMO(){
	    if (mmoPlugin != null){
	    	mmoChecker = true;
	    	return;
	    }
	    
	    Plugin mcMMOChecker = this.getServer().getPluginManager().getPlugin("mcMMO");

	    if (mcMMOChecker == null){
	        log.info("{OreMagnet} didn't find the mcMMO plugin, but, that's cool, no problem.");
	        mmoChecker = false;
	        return;
	    }
	    
	    try{
	    	String exp = "5";
	    	Scanner outdoors = new Scanner(new File("plugins/OreMagnet/config.txt"));
	    	
	    	while(outdoors.hasNextLine() ){
	    		String next = outdoors.nextLine();
	    		if(next.contains("mcMMO_exp: " ) )
	    			exp = next.substring(11);
	    		
	    	}//end while
			
	    	mmoExp = Integer.parseInt(exp);
	    	log.info("{OreMagnet} notices you're using the mcMMO plugin, that's cool, we'll give everyone " + mmoExp + " exp for every gathered mineral.  (Tip: You can change this in the config file!)");
	    }//end try
	    
	    catch(Exception e){
	    	log.severe("{OreMagnet} can't find the \"mcMMO exp: #\" line in the config file! I'll live but I'm not happy about it!");
	    	log.info("{OreMagnet} notices you're using the mcMMO plugin, that's cool, we'll give everyone " + mmoExp + " exp for every gathered mineral.");
	    }
	    
    	mmoChecker = true;
	}//end setupMMO()
	
	public boolean permissionsTester(Player player, String[] nodes){		
		
		if( permissionHandler == null )
			if( player.isOp() )
				return true;
		
		else
			for(int i = 0; nodes.length > i; i++ )
				if( OreMagnet.permissionHandler.has(player, ("OreMagnet." + nodes[i])) )
					return true;
		
		return false;
		
	}//end permissionsTester()
	
	public void addLines(){
		
		text.add("OREMAGNET was created entirely by Julian (insanj) Weiss. Please do not modify, but feel free to distribute with credit.");
		text.add("\nAll needed information for this plugin can be found in the Bukkit forums under OreMagnet.");
		text.add("\nFrequent updates and news can be found on Google+ (and sometimes Twitter) from Julian (insanj) Weiss!");

		text.add("\n\nThis is an automatically generated configuration folder, created by OreMagnet.");
		text.add("\nIf you are not familiar with the formatting and abilities that the files in here contain, please do not modify them.");
		text.add("\nAlso, if, for some reason, you feel these files are fautly, you can change the regenerate property in the config file, or just delete the entire folder.");
		text.add("\nAfter reading this file, feel free to delete it, or even add more information, but every time there is a file regeneration, it will be automatically recreated.");
		
		text2.add("This is an automatically generated configuration file, that holds all the OreMagnet persistant information.");
		text2.add("\nTampering with this file directly is highly unadvised, and can cause major errors with OreMagnet!");
		text2.add("\nChanging the \"regenerate: false\" property to \"true\" will make this file regenerate from default.");
		text2.add("\nIF YOU WANT TO ADD ANNOTATIONS, OR ANY COMMENTS, JUST ADD THEM FREELY ON A SEPERATE LINE FROM THE PROPERTIES, NO # OR ANYTHING NEEDED!");

		text2.add("\n\nmine_minerals: 21, 16, 14, 56, 73, 74, 15");
		text2.add("\nmine_tools: 257, 270, 274, 278, 285");
		text2.add("\ndurability_loss: 15");
		text2.add("\nmine_radius: 6");
		text2.add("\nmine_cooldown: 10");
		text2.add("\nregenerate: false\n\n");
		
		text2.add("\n\nThe amount chosen below for the iConomy price will be subtracted from the account that is under the user's name.");
		text2.add("\nmcMMO_exp: 10");
		text2.add("\niConomy_price: 0");
		
		
	}//end addLines()
	
	public boolean configContains(String check) throws FileNotFoundException{
		
		Scanner outdoors = new Scanner(new File("plugins/OreMagnet/config.txt"));
		
		while( outdoors.hasNextLine() )
			if( outdoors.nextLine().contains(check))
				return true;
			
		return false;
		
	}//end configContains()
	
	//Replaces a line that contains the "before" with the "after" line, by rewriting the file through an array.
	public void amendProperty(String before, String after) throws IOException{
		
		boolean multiple = false;
		File config = new File("plugins/OreMagnet/config.txt");
		Scanner outdoors = new Scanner(config);
		ArrayList<String> contents = new ArrayList<String>();
		
		while(outdoors.hasNextLine())
    		contents.add(outdoors.nextLine());
		
		for(int i = 0; i < contents.size(); i++){
			if(contents.get(i).contains(before)){
				if(multiple == false){
					contents.set(i, after);
					multiple = true;
				}else
					contents.remove(i);
			}//end if
		}//end for
		
		FileWriter configWriter = new FileWriter(config, false);

		for(int i = 0; i < contents.size(); i++)
			configWriter.write(contents.get(i) + "\n");
		
		configWriter.close();
				
	}//end amendProperty()

}//end class

/****************************Current Contents of the PLUGIN.YML**************************
 
name: OreMagnet
main: me.insanj.OreMagnet.OreMagnet
author: insanj
version: 1.1
commands:
  magnet:
    description: Smoothly removes all the adjacent materials/blocks from a hit block, with a ton of malleable properties.
    permissions: |
             -"OreMagnet.use"
             -"OreMagnet.radius"
             -"OreMagnet.cooldown"
             -"OreMagnet.always"
    usage: |
             /<command>
             /<command> radius [#]
             /<command> cooldown [#]

*****************************************************************************************/
