package me.sablednah.zombiemod;

/**
 * Processes all repeating tasks that are NOT thread safe.
 * This function is executed every minute.  It must be kept light to prevent lag.
 *
 */
public class Housekeeping implements Runnable {
	public ZombieMod plugin;

	public Housekeeping(ZombieMod p) {
		this.plugin=p;
	}


	@Override
	public void run() {
		// Housekeeping -  nothing to do any more
		
	}

	
}

