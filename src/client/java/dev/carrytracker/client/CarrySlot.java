package dev.carrytracker.client;

public final class CarrySlot {
	public int id;
	public String username;
	public int carries;
	public int goal;
	public int kills;

	public CarrySlot() {
	}

	public CarrySlot(int id, String username, int goal) {
		this.id = id;
		this.username = username;
		this.goal = Math.max(1, goal);
		this.kills = 0;
	}

	public int killGoal() {
		return Math.max(1, goal);
	}

	public boolean isComplete() {
		return kills >= killGoal();
	}
}
