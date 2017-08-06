/*
<html>
<applet code="Evol.class" width="200" height="100"></applet>
</html>
*/

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.awt.Color;
import java.awt.Graphics;
import java.util.Vector;

public class Evol extends JApplet implements Runnable {


    /************************************/
    // VARIABLES USED TO MAINTAIN APP CONSTANTS
    /************************************/


    private boolean running;
    private Thread ticker;

    // Width should be 1000 and height should be 700
    private final int WINDOW_WIDTH    = 1400, WINDOW_HEIGHT = 800;
    private final int MENU_WIDTH      = 300, MENU_HEIGHT    = WINDOW_HEIGHT;
    private final int CONSOLE_WIDTH   = WINDOW_WIDTH - MENU_WIDTH, CONSOLE_HEIGHT = 150;
    private final int SLIDER_X_OFFSET = 20, SLIDER_Y_OFFSET = 50;
    private final int SLIDER_WIDTH    = MENU_WIDTH - (2 * SLIDER_X_OFFSET);
    private final int SLIDER_HEIGHT   = 50;

    // Food offset
    private final int OFFSET = 100;

    // Default FPS is 30
    private final int FPS     = 30;
    private int movesPerFrame = 10;
    private int moveCount;
    private boolean firstCheck;

    private Creature mostDeveloped;
    private int mostAncestors;

    private int enviWidth = WINDOW_WIDTH - MENU_WIDTH,
    enviHeight = WINDOW_HEIGHT - CONSOLE_HEIGHT;

    private final boolean GRAPHICS = true;

    private KeyHandler keyHandler;

    private JTextPane txt = new JTextPane();
    private JScrollPane jsp;

    // Used to select the number of moves per frame
    private JSlider mpfSlider;

    private StringBuffer consoleSB = new StringBuffer();


    /************************************/
    // VARIABLES USED TO MAINTAIN GAME OBJECTS
    /************************************/


    // Holds all GameObjects currently being taken into account by game
    private Controller controller;

    // This is a Vector such that every Vector of longs within it contains
    // the times of all creatures that have divided from that generation.
    // For example generationDivisionTimes[0] is a Vector that holds all
    // the division times for the first generation of creatures, initially
    // spawned when the applet initializes
    public static Vector<Vector<Long>> generationDivisionTimes;

    // Used to generate random values
    private MersenneTwister r;

    private int herbCount;


    /************************************/
    // STARTING CONDITIONS
    /************************************/

    //private final int HERB_START_COUNT = 300;
    //private final int PRED_START_COUNT = 3;
    //private final int MAX_FOOD         = 300;

    private final int HERB_START_COUNT = 1000;
    private final int PRED_START_COUNT = 30;
    private final int MAX_FOOD         = 200;
    private final int MAX_FOOD_AMOUNT  = 10000;
    private final int FOOD_START_COUNT = MAX_FOOD;
    private final boolean PREDS_ON     = true;

    // Number of food sources generated per frame rendering * 1000
    // Default is 2000
    private final int FOOD_GEN_RATE = 5000;

    public void setEnviWidth(int width) { this.enviWidth = width; }
    public int getEnviWidth() { return this.enviWidth; }

    public void setEnviHeight(int height) { this.enviHeight = height; }
    public int getEnviHeight() { return this.enviHeight; }


    public Rectangle getEnviBounds() { return new Rectangle(0, 0, enviWidth, enviHeight); }

    // Bookkeeping for starting main game thread
    public void start() {

        if (ticker == null || !ticker.isAlive()) {
            running = true;
            ticker = new Thread(this);
            ticker.setPriority(Thread.MIN_PRIORITY);
            ticker.start();
        }

        resize(WINDOW_WIDTH, WINDOW_HEIGHT);

        requestFocusInWindow();
    }


    // Initialize Random object and Controller object
    public void init() {
        //resize(WINDOW_WIDTH, WINDOW_HEIGHT);

        setFocusable(true);

        // Set title of applet
        Frame title = (Frame)this.getParent().getParent();
        title.setTitle("EVOL: A Simulator of Darwinian Evolution");

        keyHandler = new KeyHandler();
        this.addKeyListener(keyHandler);

        //txt.setText("This is the JSP");
        //txt.setEditable(false);
        txt.setEditable(true);

        txt.setLocation(0, enviHeight);
        txt.setSize(CONSOLE_WIDTH, CONSOLE_HEIGHT);

        jsp = new JScrollPane(txt);
        this.add(jsp);

        mpfSlider = new JSlider(1, 100);
        mpfSlider.setLocation(enviWidth + SLIDER_X_OFFSET, SLIDER_Y_OFFSET);
        mpfSlider.setSize(SLIDER_WIDTH, SLIDER_HEIGHT);
        mpfSlider.setValue(1);
        this.add(mpfSlider);

        r = new MersenneTwister();

        controller = new Controller();
	generationDivisionTimes = new Vector<Vector<Long>>();

        moveCount = 0;
        mostAncestors = 0;
        firstCheck = true;

        // Generate FOOD_START_COUNT random Food sources within reasonable bounds
        for(int i = 0; i < FOOD_START_COUNT; i++) {
            controller.add(new Food(r.nextInt(enviWidth - OFFSET), // x coordinate
            r.nextInt(enviHeight - OFFSET), // y coordinate
            r.nextInt(MAX_FOOD_AMOUNT))); // size
        }

        // Generate HERB_START_COUNT random Creatures within reasonable bounds
        for(int i = 0; i < HERB_START_COUNT; i++) {
            controller.add(new Creature(r.nextInt(enviWidth - OFFSET), // x coordinate
            r.nextInt(enviHeight - OFFSET), // y coordinate
            0, 0, this));
        }

        herbCount = HERB_START_COUNT;

        // Generate PRED_START_COUNT random predator creatures within reasonable bounds
        if(PREDS_ON) {
            for(int i = 0; i < PRED_START_COUNT; i++) {
                Creature enemy = new Creature(3 * enviWidth / 4 + r.nextInt(enviWidth / 4 - OFFSET), // x coordinate
                3 * enviHeight / 4 + r.nextInt(enviHeight / 4 - OFFSET), // y coordinate
                0, 1, this);
                controller.add(enemy);
            }
        }



    }

    /**
    * Main method executing game logic
    * Main Thread sleeps for 1000 / FPS millis between each call to run()
    */
    public void run() {
        while(running) {

            requestFocusInWindow();

            if(!keyHandler.getPaused()) {

                // Keep track of number of GameObjects, predators, food sources,
                // and herbivores
                int contSize = controller.size();
                int predCount = 0;
                int vegCount = 0;
                int foodCount = 0;

                firstCheck = true;
                moveCount++;
                mostAncestors = 0;

                // Get correct count for foods, herbivores, and preds
                for(int i = 0; i < contSize; i++) {
                    // Loop through all current GameObjects
                    GameObject obj = controller.elementAt(i);


                    if(obj instanceof Creature) {
                        // GameObject is a Creature

			Creature creat = (Creature)obj;			

                        if(creat.getSpecies() == 1) {
                            // Creature is a predator
                            predCount++;
                        } else {
                            // Creature is a herbivore
                            vegCount++;
                            creat.setColor(Color.BLUE);
                        }


			// Move the creature. If the return value is greater than 0,
			// this means the creature has divided
			long divideTime = creat.move();

                        if(divideTime > 0) {

			    int creatNumAncestors = creat.getNumAncestors();

			    if(creatNumAncestors >= generationDivisionTimes.size()) {
				generationDivisionTimes.add(new Vector<Long>());
			    }

			    Vector<Long> creatureGen = generationDivisionTimes.elementAt(creatNumAncestors);
			    
			    int creatureGenSize = creatureGen.size();
			    int index = 0;

			    // Get the index where we need to put divideTime to get a sorted vector
			    while(index < creatureGenSize) {

				if(divideTime <= creatureGen.elementAt(index)) {
				    break;
				}

				index++;
			    }

			    creatureGen.insertElementAt(divideTime, index);
			    //generationDivisionTimes.elementAt(creatNumAncestors).add(divideTime);
			
			}

			if(creat.getTimeToDivide() < Creature.EATEN) {
			    creat.setTimeToDivide( creat.getTimeToDivide() + 1 );
			}

                        if(creat.getNumAncestors() > mostAncestors) {
                            mostAncestors = creat.getNumAncestors();
                            mostDeveloped = creat;
                        }

                    } else if(obj instanceof Food) {

                        // GameObject is Food
                        foodCount++;
                    }
                }

                herbCount = vegCount;

                // Controller removes all dead creatures and consumed food sources
                controller.testObjects();

                // Generate food according to if there is enough space for it (foodCount < MAX_FOOD)
                // and according to how quickly it should be generated. On each frame, there is a
                // (FOOD_GEN_RATE / 1000) probability of a new food source being randomly generated
                // in the game
                if(foodCount < MAX_FOOD && r.nextInt(1000) < FOOD_GEN_RATE) {
                    //controller.add(new Food(r.nextInt(enviWidth - OFFSET),
                    //r.nextInt(enviHeight - OFFSET),
                    //r.nextInt(MAX_FOOD_AMOUNT)));
		    //int xRange = enviWidth / 2 + (2 * r.nextInt(2) - 1) * (r.nextInt(enviWidth / 4) + enviWidth / 4 - OFFSET);
		    //int yRange = enviHeight / 2 + (2 * r.nextInt(2) - 1) * (r.nextInt(enviHeight / 4) + enviHeight / 4 - OFFSET);
		    int xRange = r.nextInt(enviWidth - OFFSET);
		    int yRange = r.nextInt(enviHeight - OFFSET);;

		    controller.add(new Food(xRange, yRange, r.nextInt(MAX_FOOD_AMOUNT)));
                }


                // Repopulate the game with more predators if they all die out
                if(predCount < 1 && r.nextInt(10) < 4 && PREDS_ON) {

                    // Create new preadtor Creature with random x,y coordinates
                    Creature newPred = new Creature(r.nextInt(enviWidth - OFFSET),
                    r.nextInt(enviHeight - OFFSET),
                    0, 1, this);

                    // Experimental code. Assume that number of herbivores will be very high after 2000 divisions
                    // and so help predator evolution by making more of them
                    if(Creature.divideCount < 2000) {
                        controller.add(newPred);
                    } else {
                        for(int i = 0; i < 3; i++) {
                            newPred = new Creature(r.nextInt(enviWidth - OFFSET),
                            r.nextInt(enviHeight - OFFSET),
                            0, 1, this);
                            controller.add(newPred);
                        }
                    }
                }


                // Code to repopulate game with herbivores. Commented out but could be used if needed.
                /*
                if(vegCount < 1 && r.nextInt(10) < 4) {
                Creature newVeg = new Creature(r.nextInt(450), r.nextInt(450), 0, 0, this);
                controller.add(newVeg);
            }
            */

            if(vegCount == 0) {
                consoleSB.append("\n\n----------ALL HERBIVORES DIED--------------\n\n");
		repaint();
                stop();
            }





            // Sleep to allow user's eyes to actually process what is going
            // on.
            // If only we could see more frames per second...

        }

        if(mostDeveloped != null) {
            mostDeveloped.setColor(Color.GREEN);
        }

        /*
	  if(keyHandler.getPaused() && firstCheck && Creature.divideCount > 0) {
            consoleSB.append("\n\nMost developed creature's genome: \n");
            consoleSB.append(mostDeveloped.getGenome());
            firstCheck = false;
        }
	*/

	// We either want to print something to the console or write it to a file
	if(keyHandler.getPrintTimes() || keyHandler.getWriteToFile()) {
	    keyHandler.setPrintTimes(false);
	    
	    consoleSB.append(Creature.divideCount + " divisions have occured\n");
	    consoleSB.append("Division times for each generation:\n\n");
	    
	    int gdtSize = generationDivisionTimes.size();

	    consoleSB.append(mostDeveloped.getGenome());
	    
	    for(int i = 0; i < gdtSize; i++) {

		Vector<Long> genTimes = generationDivisionTimes.elementAt(i);
		/*
		if(genTimes.size() > 30) {
		    consoleSB.append(i + ": ");
		    
		    
		    int sum = 0;
		    
		    
		    for(long l: genTimes) {
			//consoleSB.append(l + ", ");
			if(l < 100000) {
			    //sum += l;
			    sum += 1;
			}
		    }
		    
		    consoleSB.append((double)sum / (double)genTimes.size());
		    

		    //consoleSB.append(genTimes.elementAt(genTimes.size() / 4));

		    //if(i == 0) {
		    consoleSB.append(" num creatures: " + genTimes.size());
		    //}
		    
		    consoleSB.append("\n");
		}
		*/
	    }
	    

	    consoleSB.append("\n");

	    System.out.print(consoleSB.toString());

	}

	if(keyHandler.getWriteToFile()) {

	    keyHandler.setWriteToFile(false);

	    try{
		
		File f = new File("test.txt");

		if(f.createNewFile()) {
		    System.out.println("file created");
		}

		f.setWritable(true);
		BufferedWriter out = new BufferedWriter(new FileWriter(f, true));
		out.write(consoleSB.toString());
		out.close();
		

		/*
		PrintWriter pw = new PrintWriter("text.txt", "UTF-8");
		pw.print(consoleSB.toString());
		pw.close();
		*/

	    } catch(Exception e) { 
		System.out.println("ERROR: Could not write to file."); 
		e.printStackTrace();
	    }
	}

        if(GRAPHICS && moveCount >= movesPerFrame) {

            moveCount %= movesPerFrame;

            // Call draw for all GameObjects in our controller
            repaint();

            try {
                Thread.sleep(1000 / FPS);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        movesPerFrame = mpfSlider.getValue();

    }
}


/**
* Draw all GameObjects that are currently alive and not consumed
*/
public void paint(Graphics g) {

    /********************* Draw background **********************/


    g.setColor(Color.LIGHT_GRAY);
    g.fillRect(0, 0, enviWidth, enviHeight);




    /********** Draw GameObjects in front of background *********/

    if(GRAPHICS) {
        controller.draw(g);
    }


    /*********** Draw additional components *********************/


    g.setColor(Color.BLACK);

    jsp.getVerticalScrollBar().setEnabled(false);

    txt.setFont(new Font("Monospaced", 1, 12));
    txt.setText(consoleSB.toString());
    txt.setEditable(true);

    jsp.setLocation(0, enviHeight);
    jsp.setSize(CONSOLE_WIDTH, CONSOLE_HEIGHT - 1);
    jsp.getVerticalScrollBar().setEnabled(true);

    g.setFont(new Font("Times", 1, 12));
    String mpf = "Moves per Frame";
    FontMetrics fm = g.getFontMetrics();
    int fontHeight = fm.getHeight();


    mpfSlider.setLocation(enviWidth + SLIDER_X_OFFSET, SLIDER_Y_OFFSET);
    mpfSlider.setSize(SLIDER_WIDTH, SLIDER_HEIGHT);

    mpfSlider.repaint();

    g.setColor(Color.WHITE);
    g.fillRect((int)mpfSlider.getX(), (int)mpfSlider.getY() - 5 - fontHeight,
    mpfSlider.getWidth(), fontHeight);

    g.setColor(Color.BLACK);
    g.drawString(mpf + ": " + mpfSlider.getValue(),
    (int)mpfSlider.getX(),
    (int)mpfSlider.getY() - 5);


    g.setColor(Color.WHITE);
    g.fillRect(enviWidth + SLIDER_X_OFFSET, MENU_HEIGHT - 40 - fontHeight,
    SLIDER_WIDTH, fontHeight);

    g.setColor(Color.BLACK);
    g.drawString("Number of divisions: " + Creature.divideCount,
    enviWidth + SLIDER_X_OFFSET, MENU_HEIGHT - 40);


    g.setColor(Color.WHITE);
    g.fillRect(enviWidth + SLIDER_X_OFFSET, MENU_HEIGHT - 60 - fontHeight,
    SLIDER_WIDTH, fontHeight);

    g.setColor(Color.BLACK);
    g.drawString("Most ancestors: " + mostAncestors,
    enviWidth + SLIDER_X_OFFSET, MENU_HEIGHT - 60);

    g.setColor(Color.WHITE);
    g.fillRect(enviWidth + SLIDER_X_OFFSET, MENU_HEIGHT - 80 - fontHeight,
    SLIDER_WIDTH, fontHeight);

    g.setColor(Color.BLACK);
    g.drawString("Number of Prey: " + herbCount,
    enviWidth + SLIDER_X_OFFSET, MENU_HEIGHT - 80);


    /****************** Draw Borders ***************************/


    g.drawRect(enviWidth, 0, MENU_WIDTH, MENU_HEIGHT);
    g.drawRect(0, enviHeight, CONSOLE_WIDTH, CONSOLE_HEIGHT);



}


// App should stop running
public void stop() {
    running = false;
}


// Returns the controller object containing all of the GameObjects
// currently alive and not consumed
public Controller getController() {
    return this.controller;
}
}
