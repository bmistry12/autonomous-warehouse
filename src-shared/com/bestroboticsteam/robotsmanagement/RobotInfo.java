package com.bestroboticsteam.robotsmanagement;

import java.awt.Point;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import com.bestroboticsteam.communication.Communicatable;
import com.bestroboticsteam.communication.MyDataInputStream;
import com.bestroboticsteam.communication.MyDataOutputStream;
import com.bestroboticsteam.jobs.JobInfo;

public class RobotInfo implements Communicatable {
	private String name;
	private Point position;
	private Direction direction;
	private JobInfo currentJob = new JobInfo();
	private boolean wasJobCancelled = false;
	private LinkedList<Point> currentPath = new LinkedList<Point>();
	private float maxCapacity;
	private float currentLoad = 0;
	

	public RobotInfo(String name, Point position, Direction direction, float maxCapacity) {
		super();
		this.name = name;
		this.position = position;
		this.direction = direction;
		this.maxCapacity = maxCapacity;
	}
	
	public RobotInfo() {}

	// returns null if whole path was finished
	public Direction move() {
		if(currentPath.isEmpty())
			return null;
			
		Point newPos = currentPath.get(0);
		currentPath.remove(0);
		Direction newDir;

		if(position.distance(newPos) != 1
		&& position.distance(newPos) != 0)
				throw new IllegalArgumentException("wp: " + position + " " + newPos);
				
		if(position.equals(newPos))
			return Direction.WAIT;
		
		if(position.x-1 == newPos.x)
			newDir = Direction.LEFT; //turn west
		else if(position.x+1 == newPos.x)
			newDir = Direction.RIGHT; //turn east
		else if(position.y+1 == newPos.y)
			newDir = Direction.FORWARD; //turn north
		else //if(position.y-1 == newPos.y)
			newDir = Direction.BACKWARD; //turn south
		
		position = newPos;
		return turn(newDir);
	}
	
	public void cancelJob(){
		wasJobCancelled = true;
	}
	
	public boolean wasJobCancelled(){
		return wasJobCancelled;
	}

	public void pickAll(){
		currentLoad += currentJob.getWeight()*currentJob.getQuantity();
		currentJob.pickAll();
		if(currentJob.isDropPoint())
			currentLoad = 0;
	}

	public boolean finished() {
		return currentJob.getQuantity() <= 0;
	}
	
	public String getName(){
		return name;
	}

	public Point getPosition() {
		return position;
	}
	
	public Direction getDirection() {
		return direction;
	}
	
	public float getMaxCapacity(){
		return maxCapacity;
	}
	
	public float getCurrentLoad(){
		return currentLoad;
	}

	public void setCurrentJob(JobInfo job) {
		wasJobCancelled = false;
		currentJob = job;
	}

	public JobInfo getCurrentJob() {
		return currentJob;
	}
	
	public void setCurrentPath(LinkedList<Point> path){
		currentPath = path;
	}
	
	public LinkedList<Point> getCurrentPath() {
		return currentPath;
	}

	private Direction turn(Direction goal) {
		Direction turnSide;
		
		if (direction == goal)
			turnSide = Direction.FORWARD;
		else if ((direction.ordinal() + 1) == goal.ordinal()
			  || (direction.ordinal() + 1) >= 4 && (direction.ordinal() + 2) % 5 == goal.ordinal())
			turnSide = Direction.RIGHT;
		else if ((direction.ordinal() + 2) == goal.ordinal()
			  || (direction.ordinal() + 2) >= 4 && (direction.ordinal() + 3) % 5 == goal.ordinal())
			turnSide = Direction.BACKWARD;
		else// if(direction.ordinal() == (goal.ordinal()+3) || direction.ordinal() == (goal.ordinal() + 5 + 4) % 5)
			turnSide =  Direction.LEFT;
		
		direction = goal;
		return turnSide;
	}

	@Override
	public synchronized void sendObject(MyDataOutputStream o) throws IOException {
		// this.name
		o.writeString(this.name);
		// this.position
		o.writePoint(this.position);
		// this.direction
		o.writeInt(this.direction.ordinal());
		//this.maxCapacity
		o.writeFloat(this.maxCapacity);
		// this.currentJob
		if (this.currentJob == null) {
			// We tell other side that this is null
			o.writeInt(0);
		}
		else {
			// We tell other side that this is not null
			o.writeInt(1);
			this.currentJob.sendObject(o);
		}
		//this.wasJobCancelled
		o.writeBoolean(this.wasJobCancelled);
		// this.currentPath
		o.writeInt(this.currentPath.size());
		for (Iterator<Point> iterator = currentPath.iterator(); iterator.hasNext();) {
			Point point = (Point) iterator.next();
//			System.out.println(point);
			//Button.waitForAnyPress();
			o.writePoint(point);
		}
	}

	@Override
	public synchronized RobotInfo receiveObject(MyDataInputStream i) throws IOException {
		this.name = i.readString();
		this.position = i.readPoint();
		this.direction = Direction.values()[i.readInt()];
		this.maxCapacity = i.readFloat();
		int currentJobIsNotNull = i.readInt();
		if (currentJobIsNotNull == 1) {
			// currentJob received is not null
			this.currentJob.receiveObject(i);
		}
		else {
			// currentJob received is null
			//System.out.println("Setting currentJob to null");
			this.currentJob = null;
		}
		this.wasJobCancelled = i.readBoolean();
		// currentPath
		int pathSize = i.readInt();
		this.currentPath.clear();
		for (int j = 0; j < pathSize; j++) {
			this.currentPath.add(j, i.readPoint());
		}
		return this;
	}
}