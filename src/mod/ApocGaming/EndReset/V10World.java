package mod.ApocGaming.EndReset;


import java.io.Serializable;

class V10World implements Serializable
{
  private static final long serialVersionUID = 1L;
  
  final long hours;
  long lastReset;
  
  V10World(long hours)
  {
	this.hours = hours * 60 * 60;
	lastReset = System.currentTimeMillis() * 1000;
  }
}
