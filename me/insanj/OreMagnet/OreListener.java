/*
 Created by Julian Weiss (insanj), updates frequent on Google+ (and sometimes Twitter)!

 Please do not modify or decompile at any date, but feel free to distribute with credit.
 Designed and created entirely on Friday, June 8th, 2011.
 Last edited on: 8/2/11

 OreMagnet Version 1.2_02!
 Special thanks to: 
 		Aaron Zehm, for some alpha testing and brainstorming.
 		Matthew Weiss, for practicality-checks, resource lister, and for being an idea-bouncing wall.
		nossr50, for helping solve a big problem with implementing mcMMO in the Beta version.

 Works with the current CraftBukkit Build (#1000).
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

import java.io.File; 
import java.util.Scanner;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;

import com.gmail.nossr50.mcMMO;
import com.iConomy.iConomy;
import com.iConomy.system.Holdings;

public class OreListener extends BlockListener
{
	public OreMagnet plugin;
	public static mcMMO mmoPlugin;
	
	
	public OreListener(OreMagnet instance)
	{
		plugin = instance;
	}
	
	public void onBlockDamage(BlockDamageEvent event)
	{		
		PluginManager tmp = event.getPlayer().getServer().getPluginManager();
		
		if(plugin.mmoChecker)
			mmoPlugin = (mcMMO) tmp.getPlugin("mcMMO");
	    
		Block block = event.getBlock();

		if(plugin.enabled(event.getPlayer()))
		{
			if( isMineral(block) && inhand(event.getPlayer().getItemInHand().getTypeId()) )
			{
				if(plugin.checkCooldown(event.getPlayer()) == 0)
				{
					int id = block.getTypeId();
					int counter = 0;	
					String userRadius = "6";

					try{
						Scanner outdoors = new Scanner(new File("plugins/OreMagnet/config.txt"));
						boolean tester = false;				    	
				    	
						while(outdoors.hasNextLine() ){
				    		String next = outdoors.nextLine();
							if(next.contains(event.getPlayer().getDisplayName() + " radius: " ) ){
								userRadius = next.substring(event.getPlayer().getDisplayName().length() + 9);
								tester = true;
							}
						}//end while
						
						if(!tester){
							outdoors.reset();
							while(outdoors.hasNextLine() ){
					    		String next = outdoors.nextLine();
								if(next.contains("mine_radius: " ) ){
									userRadius = next.substring(13);
								}
							}//end while
						}//end if
						
					}//end try
					
					catch(Exception e){
						System.out.println("Couldn't find set radius for player " + event.getPlayer().getDisplayName() + ", or any default!");
					}
					
					double intUserRadius = 6;
					
					try{
						intUserRadius = Double.parseDouble(userRadius);
					} catch(Exception e){
						System.out.println("{OreMagnet} says something's really iffy with the radius settings in the config file! I'll live, but I'm not happy about it.");
					}

					for(int radius = 1; radius <= intUserRadius; radius++ )
					{
						int x0 = block.getX();
						int y0 = block.getY();
						int z0 = block.getZ();
						for(int theta = 0; theta < 180; ++theta) 
						{
							for(int phi = 0; phi < 360; ++phi) 
							{
								double x = x0 + radius*Math.sin(Math.toRadians(theta))*Math.cos(Math.toRadians(phi));
								double y = y0 + radius*Math.sin(Math.toRadians(theta))*Math.sin(Math.toRadians(phi));
								double z = z0 + radius*Math.cos(Math.toRadians(theta));

								//All blocks.
								if( (new Location(event.getBlock().getWorld(), x, y, z).getBlock().getTypeId() == id) && (id != 73 || id != 74) )
								{						
									Location currLoc = new Location(event.getBlock().getWorld(), x, y, z);
									currLoc.getBlock().setTypeId(0);
									counter++;

									Block broken = currLoc.getBlock();
									BlockBreakEvent breaker = new BlockBreakEvent(broken, event.getPlayer());
									onBlockBreak(breaker);

									if(plugin.mmoChecker)								
										mmoPlugin.addXp(event.getPlayer(), "mining", plugin.mmoExp);

								}//end if

								//All redstone blocks.
								else if( (id == 73 || id == 74) && ( (new Location(event.getBlock().getWorld(), x, y, z).getBlock().getTypeId() == 73) || (new Location(event.getBlock().getWorld(), x, y, z).getBlock().getTypeId() == 74)) )
								{
									Location currLoc = new Location(event.getBlock().getWorld(), x, y, z);
									currLoc.getBlock().setTypeId(0);
									counter++;

									Block broken = currLoc.getBlock();
									BlockBreakEvent breaker = new BlockBreakEvent(broken, event.getPlayer());
									onBlockBreak(breaker);

									if(plugin.mmoChecker)
										mmoPlugin.addXp(event.getPlayer(), "mining", plugin.mmoExp);


								}//end if

							}//end for
						}//end for
					}//end for


					if(id == 73 || id == 74 || id == 89 || id == 82)
						event.getPlayer().getInventory().addItem(new ItemStack(getItem(id), counter * 3));

					else if( id == 21 )
						event.getPlayer().getInventory().addItem(new ItemStack(351, (counter * 3), (short)4));
					
					else 
						event.getPlayer().getInventory().addItem(new ItemStack(getItem(id), counter));

					durability(event.getPlayer());
					
					if(plugin.iConomy != null){
						Holdings balance = iConomy.getAccount(event.getPlayer().getDisplayName()).getHoldings();
						balance.subtract(monies()); 
					}
				    
					event.getPlayer().sendMessage(ChatColor.DARK_GREEN + "Guess what? A total of " + counter + " blocks were added to your inventory!" );
					
					try{
						plugin.resetCooldown(event.getPlayer());
					} 
					
					catch(Exception e) {
						System.out.println("{OreMagnet} couldn't quite reset " + event.getPlayer().getDisplayName() + "'s cooldown: " + e.getMessage());
					}
					
				}//end if cooldown
			
				else
					event.getPlayer().sendMessage(ChatColor.AQUA + "You have " + plugin.checkCooldown(event.getPlayer()) + " seconds remaining to use OreMagnet!");
					
			}//end if
		}//end plugin enabled
		
	}//end onBlockDamage()
	
	@SuppressWarnings("deprecation")
	private void durability(Player player) {
		boolean set = false;
		String loss = "";

		try{
			Scanner outdoors = new Scanner(new File("plugins/OreMagnet/config.txt"));
			
			while(outdoors.hasNextLine() ){
				String next = outdoors.nextLine();
				if(next.contains("durability_loss: ")){
					loss = next.substring(17);
					set = true;
				}
			}//end while
		}//end try
		
		catch(Exception e){
			System.out.println("{OreMagnet} couldn't read the config file for durability loss!");
		}
		
		//If the tools that a user must have in hand are set...
		if(set){
			if(lossPossible(player.getItemInHand().getTypeId())){
				short after = (short) (player.getItemInHand().getDurability() + Integer.parseInt(loss));
                player.getInventory().getItemInHand().setDurability(after);
                player.updateInventory();
			}//end if
			
			else if(player.getItemInHand().getAmount() > 0)
				player.getItemInHand().setAmount(player.getItemInHand().getAmount() - 1);
				
			else
				player.damage(1);
		}//end if
	}//end durability()

	private int getItem(int id){		
		if(id == 16) 
			return 263;
						
		if(id == 56)
			return 264;
		
		if(id == 73 || id == 74)
			return 331;
		
		if(id == 89)
			return 348;
		
		if(id == 31)
			return 295;
		
		if(id == 59)
			return 296;
		
		if(id == 2)
			return 3;
		
		if(id == 82)
			return 337;
		
		return id;
	}//end getItem()
	
	private boolean lossPossible(int id){
		return (id >= 256 && id <= 259) || (id == 261) || (id >= 267 && id <= 279) || (id >= 283  && id <= 286) || (id >= 290 && id <= 294) || (id >= 298 && id <= 317) || (id == 246) || (id == 359);
	}//end lossPossible()
	
	private boolean isMineral(Block block){
		try{
			Scanner outdoors = new Scanner(new File("plugins/OreMagnet/config.txt"));
			String item = "";
			boolean set = false;				    	
	    	
			while(outdoors.hasNextLine() ){
	    		String next = outdoors.nextLine();
				if(next.contains("mine_minerals: ")){
					item = next.substring(15);
					set = true;
				}
			}//end while
			
			if(set){
				String[] items = item.split(", ");
				
				for(int i = 0; i < items.length; i++)
					if(Integer.parseInt(items[i]) == block.getTypeId())
						return true;
			}//end if			
		} catch(Exception e){
			System.out.println("{OreMagnet} couldn't find the config file, or there was an error in the \"mine_minerals\" line! :O");
		}
		
		return false;
		
	}//end ore()
	
	//Sees if what is inhand is something listed in the config, or a pickaxe.
	public boolean inhand(int id){
		try{
			Scanner outdoors = new Scanner(new File("plugins/OreMagnet/config.txt"));
			String tool = "";
			boolean set = false;				    	
	    	
			while(outdoors.hasNextLine() ){
	    		String next = outdoors.nextLine();
				if(next.contains("mine_tools: ")){
					tool = next.substring(12);
					set = true;
				}
			}//end while
			
			if(set){
				String[] tools = tool.split(", ");
				
				for(int i = 0; i < tools.length; i++)
					if(Integer.parseInt(tools[i]) == id)
						return true;
			}//end if			
		} catch(Exception e){
			System.out.println("{OreMagnet} couldn't find the config file, or there was an error in the \"mine_tools\" line! :O");
		}
		
		return false;
		
	}//end inhand()
	
	public double monies(){
		try{
			Scanner outdoors = new Scanner(new File("plugins/OreMagnet/config.txt"));
			String tool = "";
			
			while(outdoors.hasNextLine() ){
				String next = outdoors.nextLine();
				if(next.contains("iConomy_price: "))
					tool = next.substring(15);
			}//end while	
			
			return Double.parseDouble(tool);

		} catch(Exception e){
			System.out.println("{OreMagnet} couldn't read the \"iConomy_price\" line in the config file, or it isn't valid!!!");
			return 0;
		}
		
	}//end monies()
}//end OreListener()
