package mod.ApocGaming.EndReset;


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class EndReset extends JavaPlugin implements Listener, Runnable
{
  private final HashMap<String, HashMap<String, Long>> v10chunks = new HashMap<String, HashMap<String, Long>>();
  private final HashMap<String, Long> cvs = new HashMap<String, Long>();
  
  private final HashSet<String> reg = new HashSet<String>();
  
  private final HashMap<String, Integer> pids = new HashMap<String, Integer>();
  private long it = 15;
  private final HashMap<String, V10World> forceReset = new HashMap<String, V10World>();
  private final HashMap<String, Short> dragonAmount = new HashMap<String, Short>();
  
  private final HashMap<String, RegenThread> threads = new HashMap<String, RegenThread>();
  private final HashMap<String, Long> suspendedTasks = new HashMap<String, Long>();
  
  public void onEnable()
  {
	Server s = getServer();
	Logger log = getLogger();
	BukkitScheduler bs = s.getScheduler();
	
	for(World w: s.getWorlds())
	{
	  if(w.getEnvironment() != Environment.THE_END)
		continue;
	  onWorldLoad(new WorldLoadEvent(w));
	}
	
	bs.scheduleSyncRepeatingTask(this, this, 1L, 1L);
	bs.scheduleSyncRepeatingTask(this, new ForceThread(), 20L, 72000L);
	
	PluginManager pm = s.getPluginManager();
	pm.registerEvents(this, this);
	log.info("v"+getDescription().getVersion()+" enabled!");
  }
  
  public void onDisable()
  {
	Server s = getServer();
	s.getScheduler().cancelTasks(this);

	s.getLogger().info("["+getName()+"] disabled!");
  }
  
  public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args)
  {
	if(!sender.hasPermission("endreset.config"))
	  return true;
	
	return true;
  }
  
  private void regen(World world)
  {
	String wn = world.getName();
	long cv = cvs.get(wn) + 1;
	if(cv == Long.MAX_VALUE)
	  cv = Long.MIN_VALUE;
	cvs.put(wn, cv);
	for(Chunk c: world.getLoadedChunks())
	  onChunkLoad(new ChunkLoadEvent(c, false));
	
	short a;
	if(dragonAmount.containsKey(wn))
	  a = dragonAmount.get(wn);
	else
	  a = 1;
	if(a > 1)
	{
	  a--;
	  Location loc = world.getSpawnLocation();
	  loc.setY(world.getMaxHeight() - 1);
	  for(short i = 0; i < a; i++)
		world.spawnEntity(loc, EntityType.ENDER_DRAGON);
	}
	getServer().broadcastMessage(ChatColor.RED + "[Server] " + ChatColor.YELLOW + "- The End has been reset!");
  }
  
  public void run()
  {
	int pc;
	int pid;
	BukkitScheduler s = getServer().getScheduler();
	for(World w: getServer().getWorlds())
	{
	  if(w.getEnvironment() != Environment.THE_END)
		continue;
	  String wn = w.getName();
	  if(!reg.contains(wn))
		continue;
	  pc = w.getPlayers().size();
	  if(pc < 1 && !pids.containsKey(wn))
	  {
		long tr;
		if(!suspendedTasks.containsKey(wn))
		  tr = it;
		else
		{
		  tr = suspendedTasks.get(wn);
		  suspendedTasks.remove(wn);
		}
		RegenThread t = new RegenThread(wn, w.getFullTime() + tr);
		pids.put(wn, s.scheduleSyncDelayedTask(EndReset.this, t, tr));
		threads.put(wn, t);
	  }
	  else if(pc > 0 && pids.containsKey(wn))
	  {
		pid = pids.get(wn);
		s.cancelTask(pid);
		pids.remove(wn);
		suspendedTasks.put(wn, threads.get(wn).getRemainingDelay());
		threads.remove(wn);
	  }
	}
  }
  
  private class RegenThread implements Runnable
  {
	private final String wn;
	private final long toRun;
	
	private RegenThread(String wn, long toRun)
	{
	  this.wn = wn;
	  this.toRun = toRun;
	}
	
	public void run()
	{
	  if(!pids.containsKey(wn))
		return;
	  World w = getServer().getWorld(wn);
	  if(w != null)
		regen(w);
	  reg.remove(wn);
	  pids.remove(wn);
	  threads.remove(this);
	}
	
	long getRemainingDelay()
	{
	  World w = getServer().getWorld(wn);
	  if(w == null)
		return -1;
	  return toRun - w.getFullTime();
	}
  }
  
  private class ForceThread implements Runnable
  {
	public void run()
	{
	  if(forceReset.isEmpty())
		return;
	  long now = System.currentTimeMillis() * 1000;
	  V10World vw;
	  Server s = getServer();
	  for(Entry<String, V10World> e: forceReset.entrySet())
	  {
		vw = e.getValue();
		if(vw.lastReset + vw.hours >= now)
		{
		  regen(s.getWorld(e.getKey()));
		  vw.lastReset = now;
		}
	  }
	}
  }
  
  @EventHandler(priority = EventPriority.MONITOR)
  public void onEntityDeath(EntityDeathEvent event)
  {
	  Entity e = event.getEntity();
	  if(e instanceof EnderDragon){
		  World w = e.getWorld();
		  Random r = new Random();
		  Player p = w.getPlayers().get(r.nextInt(w.getPlayers().size()));
		  List<Player> players = w.getPlayers();
		  
		  for(int i = 0; i < players.size(); i++){
			  
			  players.get(i).performCommand("spawn");
			  
		  }
		  try{
			  ItemStack egg = new ItemStack(Material.DRAGON_EGG, 1);
			  p.getInventory().addItem(egg);
			  p.setLevel(p.getLevel() + 75);
		  }
		  catch(Exception exc){
			  
		  }
		  regen(w);
	  }
	  
	/*Entity e = event.getEntity();
	if(!(e instanceof EnderDragon))
	  return;
	World w = e.getWorld();
	if(w.getEnvironment() != Environment.THE_END)
	  return;
	String wn = w.getName();
	if(dontHandle.contains(wn))
	  return;
	reg.add(wn);
	save = true;*/
  }
  
  @EventHandler(priority = EventPriority.LOWEST)
  public void onChunkLoad(ChunkLoadEvent event)
  {
	if(event.getWorld().getEnvironment() != Environment.THE_END)
	  return;
	World world = event.getWorld();
	String wn = world.getName();
	HashMap<String, Long> worldMap;
	if(v10chunks.containsKey(wn))
	  worldMap = v10chunks.get(wn);
	else
	{
	  worldMap = new HashMap<String, Long>();
	  v10chunks.put(wn, worldMap);
	}
	
	Chunk chunk = event.getChunk();
	int x = chunk.getX();
	int z = chunk.getZ();
	String hash = x+"/"+z;
	long cv = cvs.get(wn);
	
	if(worldMap.containsKey(hash))
	{
	  if(worldMap.get(hash) != cv)
	  {
		for(Entity e: chunk.getEntities())
		  e.remove();
		world.regenerateChunk(x, z);
		worldMap.put(hash, cv);
	  }
	}
	else
	  worldMap.put(hash, cv);
  }
  
  @EventHandler(priority = EventPriority.LOWEST)
  public void onWorldLoad(WorldLoadEvent event)
  {
	World w = event.getWorld();
	if(w.getEnvironment() != Environment.THE_END)
	  return;
	String wn = w.getName();
	if(!cvs.containsKey(wn))
	{
	  cvs.put(wn, Long.MIN_VALUE);
	}
  }
}
