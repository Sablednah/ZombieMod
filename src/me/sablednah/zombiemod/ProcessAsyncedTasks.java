package me.sablednah.zombiemod;
/**
 * Processes all repeating tasks that ARE thread safe.
 * This thread can contain more intensive processing - but must only use thread safe Bukkit functions.
 *
 */

public class ProcessAsyncedTasks implements Runnable {
	public ZombieMod plugin;

	public ProcessAsyncedTasks(ZombieMod p) {
		this.plugin=p;
	}

	@Override
	public void run() {

	}
}
		
