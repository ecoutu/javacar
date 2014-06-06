package cargame;

import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.*;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;


/**
 * The <code>Car</code> object represents a car in the game, which moves,
 * accelerates, brakes and turns. The object runs as a thread, updating its
 * location depending on its speed and direction. Other objects can cause it
 * to turn, accelerate and brake in order to operate it as if it where a
 * "normal car." 
 * @author Eric Coutu - ecoutu - 0523365
 */
class Car extends JComponent implements Runnable {
	private BufferedImage icon;
	private double x;
	private double y;
	private double speed;
	private double maxSpeed;
	// the factor of the acceleration that the brakes de-accelerate
	private int brakeRate = 10; 
	private double acceleration;
	// the rolling de-acceleration of a car in motion
	private double deAcceleration; 
	/* The angle of the car. 0 lies along the +x axis of the Cartesian plane
	 * and positive relative values are counter-clockwise.
	 */
	private double angle;

	/**
	 * Creates a new <code>Car</code> object, and starts the thread that
	 * updates its coordinates.
	 * @param icon An image file that the car will be drawn as.
	 * @param acceleration The acceleration of this car.
	 * @param maxSpeed The maximum speed of this car.
	 * @param x The starting x coordinate.
	 * @param y The starting y coordinate.
	 */
	public Car(BufferedImage icon, int acceleration, int maxSpeed, int x, int y) {

		Thread thread;

		this.x = x;
		this.y = y;
		this.angle = 0;
		this.acceleration = acceleration;
		this.maxSpeed = maxSpeed;

		/*
		 * Average de-acceleration of a vehicle is calculated by:
		 * deltaF = F - rollingResistanceF
		 * m * deltaA = m*forwardA - rollingRestianceCoefficient * (m*gravityA)
		 * deltaA = forwardA - rollingResistanceCoefficient * gravityA
		 * From this, the de-acceleration caused by rolling resistance 
		 * is the rollingResistanceCoefficient * gravityAcceleration
		 * I used the coefficient for asphalt, which is 0.03.
		 */
		this.deAcceleration = 0.2943;

		this.icon = icon;

		thread = new Thread ((Runnable)this);
		thread.start();
	}

	/**
	 * The paint method, that will render this <code>Car</code> object, at
	 * its current rotation and position.
	 * @param g The <code>Graphics</code> object passed to the paint call.
	 */

	public void paint(Graphics g) {
		int x = new Double(this.x).intValue();
		int y = new Double(this.y).intValue();

		// rotate the image to that of the current car angle.
		// rotation is done from the center of the car.
		((Graphics2D)g).rotate(Math.toRadians(0-this.angle), 
				x+(this.icon.getWidth()/2), 
				y+(this.icon.getHeight()/2));

		g.drawImage(this.icon, x, y, null);
	}

	/**
	 * Update the coordinates of this object, based on its current speed and
	 * coordinates over a duration of time.
	 * @param duration The number of milliseconds that have passed.
	 */
	private void updateCoordinates(int duration) {
		// get the bounds of this window, we can't go outside of it
		Dimension size = this.getParent().getSize();
		double deltaX, deltaY;
		// the Cartesian sector that the car is currently in
		int sector = new Double(this.angle / 90).intValue();
		// the angle of the car relative to the sector
		double theta = this.angle % 90;
		// change in distance vector
		double delta = this.speed * (duration / 1000.0);

		// calculate deltaX and deltaY for each sector
		if (sector == 0 || sector == 2) {			
			deltaX = Math.cos(Math.toRadians(theta)) * delta;
			deltaY = Math.sin(Math.toRadians(theta)) * delta;
			if (sector == 0)
				deltaY = 0 - deltaY;
			else
				deltaX = 0 - deltaX;
		}
		else {
			deltaX = Math.sin(Math.toRadians(theta)) * delta;
			deltaY = Math.cos(Math.toRadians(theta)) * delta;
			if (sector == 1) {
				deltaX = 0 - deltaX;
				deltaY = 0 - deltaY;
			}
		}
		// check to make sure new coordinates are within bounds of window
		if ((this.x + deltaX) < 0 ||
				(this.x + deltaX) > (size.getWidth() - 50) || 
				(this.y + deltaY) < 0 || 
				(this.y + deltaY) > (size.getHeight() - 50)) {
			this.setSpeed(0);
		}
		else {
			this.x += deltaX;
			this.y += deltaY;
		}
	}

	/**
	 * The main loop for this objects thread updates the position of this
	 * <code>Car</code> object and applies deacceleration caused by rolling
	 * resistance.
	 */
	public void run() {
		int sleepTime = 10;
		try {
			while (true) {
				Thread.sleep(sleepTime);
				rollingDeacceleration(sleepTime);
				updateCoordinates(sleepTime);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Deaccelerates this object based on the rolling resistance over a period
	 * of time.
	 * @param duration The time in milliseconds that the rolling deacceleration should be applied for.
	 */
	private void rollingDeacceleration(double duration) {
		double amount = this.deAcceleration * (duration / 10);
		// can only the cause to slow and stop, not move backwards
		if (Math.abs(this.speed) - amount < 0) {
			this.setSpeed(0);
			return;
		}
		
		if (this.speed < 0)
			this.setSpeed(this.speed + amount);
		else if (this.speed > 0)
			this.setSpeed(this.speed - amount);
	}

	/**
	 * Accelerate this object at its acceleration speed for an amount of time.
	 * @param duration The time in milliseconds that the car should accelerate for.
	 */
	public void accelerate(double duration) {
		this.accelerate(duration, 1);
	}
	
	/**
	 * Accelerate this object at its acceleration speed for an amount of time
	 * and multiply it by a factor.
	 * @param duration The time in milliseconds that the car should accelerate for.
	 * @param factor Multiple to adjust the acceleration by.
	 */
	public void accelerate(double duration, int factor) {
		double newSpeed = this.speed + 
			factor * this.acceleration * (duration / 1000);
		if (Math.abs(newSpeed) <= this.maxSpeed)
			this.speed = newSpeed;
	}

	/**
	 * Apply the brakes of this car, causing it to deaccelerate. The speed of
	 * the car will never change directions.
	 * @param duration The time in seconds for which the brakes are applied for.
	 */
	public void applyBrakes(double duration) {
		if (this.speed < 0) { // moving backwards
			this.accelerate(duration, this.brakeRate);
			if (this.speed > 0) // now moving forwards
				this.setSpeed(0);
		}
		else if (this.speed > 0) { // moving forwards
			this.accelerate(duration,-this.brakeRate);
			if (this.speed < 0) // now moving backwards
				this.setSpeed(0);
		}
	}

	/**
	 * Return the current speed of this <code>Car</code> object.
	 * @return The current speed of this object in pixels/second.
	 */
	public double getSpeed() {
		return speed;
	}
	
	/**
	 * Set the current speed of this <code>Car</code> object up to its maximum
	 * speed. Note that this is private, as it is not possible to change the
	 * speed of a car without acceleration.
	 * @param speed The new speed, in pixels/second.
	 */
	private void setSpeed(double speed) {
		if (Math.abs(speed) < this.maxSpeed)
			this.speed = speed;
	}
	
	/**
	 * Turn this car at an angle relative to its current direction. The car
	 * must be in motion in order for it to change direction.
	 * @param angle The angle, in degrees, that this car should turn.
	 */
	public void turnVehicle(double angle) {
		if (this.speed > 0)
			this.angle += angle;
		// when in reverse the car turns towards the direction of the steering
		else if (this.speed < 0)
			this.angle -= angle;
		// car must be in motion to turn
		else
			this.angle += 5*angle;

		// mod the new angle to 360 degrees
		if (this.angle >= 360)
			this.angle %= 360;
		else if (this.angle <= 0)
			this.angle = (360 + this.angle) % 360;
	}
}

/**
 * The <code>Game</code> object coordinates the GUI, game logic and user input
 * for the car driving game. It runs as an independent thread, updating the
 * acceleration of the car based on user input, causes the car to repaint
 * itself and updates the GUI in response to changes in the cars speed.
 * @author Eric Coutu - ecoutu - 0523365
 *
 */
class Game implements Runnable, KeyListener {
	private GameInterface window;
	private Car car;
	// each Active key reflects the keyboard control keys currently depressed
	private boolean spaceActive;
	private boolean leftActive;
	private boolean rightActive;
	private boolean upActive;
	private boolean downActive;

	/**
	 * Allocate a <code>Game</code> object.
	 * @param carIcon The path to an image icon to use to display the car the at the user will drive.
	 */
	public Game (String carIcon) {
		Thread thread;
		BufferedImage icon = null;
		window = new GameInterface("3750 Assignment 3 - Sudo Walking!", this);
		// create the icon object for the car
		try {
			URL url = getClass().getClassLoader().getResource(carIcon);
			icon = ImageIO.read(url.openStream());

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		
		car = new Car(icon, 1000, 500, 100, 100);
		window.add(car, BorderLayout.CENTER); // add the car to the GUI

		thread = new Thread((Runnable)this);
		thread.start();
	}

	/**
	 * Event handler for a key being pressed.
	 */
	public void keyPressed(KeyEvent e) {
		this.setKey(e.getKeyCode(), true);
	}
	
	/**
	 * Event handler for a key being released.
	 */
	public void keyReleased(KeyEvent e) {
		this.setKey(e.getKeyCode(), false);
	}
	
	/**
	 * Event handler for a key being typed. Currently a stub, the game UI is
	 * only concerned with the separate pressing and releasing of key events.
	 */
	public void keyTyped(KeyEvent e) {
		return;
	}

	/**
	 * Update the status of the game based on key events. Keys currently used
	 * by the game are the up, down, left and right arrows and the space key.
	 * @param keyCode The key code of the key that has been triggered.
	 * @param status Reflects the status of the key that has been triggered: <code>true</code> is a key pressed and <code>false</code> is a key released.
	 */
	public void setKey(int keyCode, boolean status) {
		switch(keyCode) {
		case KeyEvent.VK_LEFT:
			this.leftActive = status;
			break;
		case KeyEvent.VK_RIGHT:
			this.rightActive = status;
			break;
		case KeyEvent.VK_UP:
			this.upActive = status;
			break;
		case KeyEvent.VK_DOWN:
			this.downActive = status;
			break;
		case KeyEvent.VK_SPACE:
			this.spaceActive = status;
			break;
		default:
			break;
		}		
	}

	/**
	 * The primary loop for this object polls the keys that have been
	 * activated by the user and accelerates the car accordingly. If the
	 * left key is active, the car is rotated negative 2 degrees. If the
	 * right key is active, the car is rotated 2 degrees.
	 *
	 */	
	public void run () {
		int sleepTime = 20;
		try {
			while (true) {
				Thread.sleep(sleepTime);
				if (this.leftActive)
					this.car.turnVehicle(5);
				if (this.rightActive)
					this.car.turnVehicle(-5);

				if (this.spaceActive)
					this.car.applyBrakes(sleepTime);
				else if (this.upActive)
					this.car.accelerate(sleepTime);
				else if (this.downActive)
					this.car.accelerate(sleepTime, -1);

				this.car.repaint();
				this.window.setSpeed(this.car.getSpeed());

			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

/**
 * The <code>GameInterface</code> class implements the graphical user interface
 * to the car driving game. The class is implemented as a wrapper for a
 * JFrame object.
 * @author Eric Coutu - ecoutu - 0523365
 *
 */
class GameInterface {
	private JFrame window;
	private Container pane;
	private JLabel speedLabel;

	/**
	 * Allocate a <code>GameInterface</code> object, creating a
	 * <code>JFrame</code> object and adding all the GUI elements for the game.
	 * @param title The title of the window that will be created.
	 * @param handler A <code>KeyListener</code> that will handle all keyboard input for this <code>GameInterface</code>.
	 */
	public GameInterface(String title, KeyListener handler) {
		JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEADING));
		// create the JFrame and GUI
		window = new JFrame(title);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setSize(500,500);
		window.setLocationRelativeTo(null);

		pane = window.getContentPane();

		speedLabel = new JLabel("speed");
		statusBar.add(new JLabel("Speed: "));
		statusBar.add(speedLabel);

		pane.add(new JLabel(
				"Up: Accelerate, Down: Reverse, Left/Right: Steering, Space: Brakes"),
				BorderLayout.PAGE_START);
		pane.add(statusBar, BorderLayout.PAGE_END);

		window.addKeyListener(handler);
		window.setVisible(true);
	}

	/**
	 * Set the speed of the car that is displayed to the user.
	 * @param speed The speed to update the user display with.
	 */
	public void setSpeed(double speed) {
		this.speedLabel.setText(speed + "px/s");
	}

	/**
	 * Add a <code>JComponent</code> to this <code>GameInterface</code> object.
	 * @param c the <code>JComponent</code> object to add
	 * @param layout a valid <code>BorderLayout</code> region constant.
	 */
	public void add(JComponent c, Object layout) {
		this.pane.add(c, layout);
	}
}

/**
 * The main implementation of the car driving game.
 * @author Eric Coutu - ecoutu - 0523365
 *
 */
public class A3 {
	/**
	 * Creates a <code>Game</code> object. If no car image file is specified
	 * the program will search for it in the jar file.
	 * @param args No command line arguments accepted.
	 */
	public static void main (String args[]) {
		new Game("images/sudo.png");
	}
}
