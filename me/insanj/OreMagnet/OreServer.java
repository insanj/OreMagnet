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

import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.plugin.Plugin;

import com.iConomy.iConomy;

public class OreServer extends ServerListener 
{
    private OreMagnet plugin;

    public OreServer(OreMagnet instance) {
        plugin = instance;
    }

    @Override
    public void onPluginDisable(PluginDisableEvent event) 
    {
        if (plugin.iConomy != null) {
            if (event.getPlugin().getDescription().getName().equals("iConomy")) {
                plugin.iConomy = null;
                System.out.println("{OreMagnet} did not detect the iConomy plugin, which is fine, don't worry.");
            }
        }//end if
    }//end onPluginDisable()

    @Override
    public void onPluginEnable(PluginEnableEvent event) 
    {
        if (plugin.iConomy == null) {
            Plugin iConomy = plugin.getServer().getPluginManager().getPlugin("iConomy");

            if (iConomy != null) {
                if (iConomy.isEnabled() && iConomy.getClass().getName().equals("com.iConomy.iConomy")) {
                    plugin.iConomy = (iConomy)iConomy;
                    System.out.println("{OreMagnet} is now successfully using the iConomy plugin! The amount recorded in the config file will be subtracted from the account under the user's name!");
                }
            }
        }//end if
    }//end onPluginEnable()
    
}//end class
