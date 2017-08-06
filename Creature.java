import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Scanner;


public class Creature extends GameObject {

    private double rot;

    private double[] xPoints;
    private double[] yPoints;

    private int width = 10;
    private int height = 20;

    private MersenneTwister rand;

    private final String[] INSTRUCTIONS = {"A ", "C ", "R1", "R2"};

    private int count;
    protected static int divideCount = 0;
    protected static int herbDivCount = 0;
    protected static double instCountinGene = 0;
    private int numAncestors;

    private final int ROTATE_SPEED = 10;
    private final int DEFAULT_DIVISION_THRESHOLD = 30000;
    //private final int DEFAULT_DIVISION_THRESHOLD = 10000;
    private final int START_MOVE_SPEED = 10;
    private final int START_EAT_SPEED = 100;
    private final int DEFAULT_FP = 3000;
    private final int DEFAULT_STARVING_RATE = 20;
    private final int VISION_DISTANCE = 100;

    // Default mutation rate is 10
    private final int MUTATION_RATE = 8;

    private int starvingRate = DEFAULT_STARVING_RATE;
    private int divisionThreshold = DEFAULT_DIVISION_THRESHOLD;

    // How far off the Creature can be from the center of a food source
    private final int MARGIN_OF_ERROR = 25;

    private Evol game;

    private int enviWidth, enviHeight;
    private Rectangle enviBounds;

    // This will hold the x/y coordinates of two points:
    // the x/y coordinate of the creature and the edge of
    // the window that the creature is looking at
    private int[][] vision;

    private int[] genes;
    private int stimuli = 0;

    private boolean stimulated;

    private final int STIM_FOOD = 1;
    private final int STIM_FLEE = 2;
    private final int STIM_FIGHT = 4;
    private final int ON_FOOD = 8;

    // NUM_INSTRUCTIONS * INSTRUCTION_LENGTH <= 32
    private final int NUM_INSTRUCTIONS = 15;
    private final int INSTRUCTION_LENGTH = 2;

    private int fleeing;
    private boolean dead;
    private boolean divided;

    private Controller enviObjects;

    private int foodPoints;

    private int eatSpeed;
    private int movementSpeed;

    private int species;

    private int instCount;

    private Color color;

    // This will get updated every move, so that it increases as time goes on
    private long timeToDivide;
    protected static final long EATEN = 100000;

    public Creature(int x, int y, double rot, int species, Evol game) {
        super(x, y);

        rand = new MersenneTwister();

        this.game = game;

        this.enviWidth = game.getEnviWidth();
        this.enviHeight = game.getEnviHeight();

        this.enviBounds = game.getEnviBounds();

        this.enviObjects = game.getController();

        this.numAncestors = 0;

        this.species = species;
        this.genes = new int[5];

        this.vision = new int[2][2];

        if(species == 0) {
            this.color = Color.BLUE;
        } else {
            this.color = Color.RED;
        }

        this.xPoints = new double[3];
        this.yPoints = new double[3];

        this.rot = 0;

        this.stimulated = false;

        // Set random behavior for genes
        for(int i = 0; i < genes.length; i++) {
            genes[i] = rand.nextInt();

            // Set some instructions in gene relating to predator stimulus
            if(i == 1) {
                genes[i] |= 2;
                genes[i] &= (~1);
            }
        }


        this.fleeing = 0;
        this.dead = false;
	this.divided = false;

        this.foodPoints = DEFAULT_FP * (species + 1) + (2 * species * DEFAULT_FP);
        this.movementSpeed = START_MOVE_SPEED;
        this.eatSpeed = START_EAT_SPEED;

        this.starvingRate *= (2 * species + 1);

        this.divisionThreshold *= (4 * species) + 1;

        xPoints[0] = x;
        xPoints[1] = x - width / 2;
        xPoints[2] = x + width / 2;

        yPoints[0] = y - 3 * height / 4;
        yPoints[1] = yPoints[2] = y + height / 4;

        setSize(foodPoints);

        rotate(rot);

        count = 0;
        instCount = 0;
	this.timeToDivide = 0;
    }

    public void setGene(int geneNum, int gene) { genes[geneNum] = gene; }
    public int getGene(int geneNum) { return this.genes[geneNum]; }

    public void setStimFood() { this.stimuli |= STIM_FOOD; }
    public boolean getStimFood() { return (this.stimuli& STIM_FOOD) != 0; }

    public void setStimFlee() { this.stimuli |= STIM_FLEE; }
    public boolean getStimFlee() { return (this.stimuli & STIM_FLEE) != 0; }

    public void setStimFight() { this.stimuli |= STIM_FIGHT; }
    public boolean getStimFight() { return (this.stimuli & STIM_FIGHT) != 0; }

    public void setOnFood() { this.stimuli |= ON_FOOD; }
    public boolean getOnFood() { return (this.stimuli & ON_FOOD) != 0; }

    public int getNumAncestors() { return this.numAncestors; }
    public void setNumAncestors(int ancestors) { this.numAncestors = ancestors; }

    public void setColor(Color c) { this.color = c; }
    public Color getColor() { return this.color; }

    public long getTimeToDivide() { return this.timeToDivide; }
    public void setTimeToDivide(long ttd) { this.timeToDivide = ttd; }

    public boolean isDead() { return this.dead; }
    public void setDead(boolean d) { this.dead = d; }

    public boolean divided() { return this.divided; }
    public void setDivided(boolean d) { this.divided = d; }
    
    public int getStarvingRate() {
        return starvingRate;
    }

    public void setStarvingRate(int starvingRate) {
        this.starvingRate = starvingRate;
    }

    public int getSpecies() {
        return species;
    }

    public void setSpecies(int species) {
        this.species = species;
    }

    public int getFoodPoints() { return this.foodPoints; }

    public void setFoodPoints(int points) {
        this.foodPoints = points;
    }

    public void incX(int increment) {
        for(int i = 0; i < 3; i++) {
            xPoints[i] += increment;
        }

        setX(getX() + increment);
    }

    public void incY(int increment) {
        for(int i = 0; i < 3; i++) {
            yPoints[i] += increment;
        }

        setY(getY() + increment);
    }

    public int[][] getVision() {
        return this.vision;
    }

    public void setVision(double rot) {

        vision[0][0] = getX();
        vision[0][1] = getY();

        if (rot == 0) {
            // Facing up
            vision[1][0] = vision[0][0];
            vision[1][1] = Math.max(0, vision[0][1] - VISION_DISTANCE);
        } else if (rot == Math.PI / 2) {
            // Facing left
            vision[1][0] = Math.max(0, vision[0][0] - VISION_DISTANCE);
            vision[1][1] = vision[0][1];
        } else if (rot == Math.PI) {
            // Facing down
            vision[1][0] = vision[0][0];
            vision[1][1] = Math.min(enviHeight, vision[0][1] + VISION_DISTANCE);
        } else {
            // Facing right
            vision[1][0] = Math.min(enviWidth, vision[0][0] + VISION_DISTANCE);
            vision[1][1] = vision[0][1];
        }
    }


    // The result is that when the creature is facing "towards"
    // an object, it is stimulated
    public void checkStimulus() {

        int numObjects = enviObjects.size();

        stimulated = false;
        //stimFood = false;
        //stimFlee = false;
        //stimFight = false;

        // Sets the first 4 bits to 0
        stimuli &= (~15);

        for (int i = 0; i < numObjects; i++) {
            GameObject obj = enviObjects.elementAt(i);

            int objX = obj.getX();
            int objY = obj.getY();

            // Check to see if Creature is on a food source
            if (Math.abs(vision[0][0] - objX) < MARGIN_OF_ERROR
            && Math.abs(vision[0][1] - objY) < MARGIN_OF_ERROR) {

                // Make separate if check to check if food in case add more
                // objects later
                if (obj instanceof Food && species < 1 && fleeing <= 0) {
                    //eating = true;
                    setOnFood();

                    eat((Food) obj);

                    break;
                } else if (obj instanceof Creature) {

                    // Creature is on a prey
                    if (((Creature) obj).getSpecies() < species) {
                        kill((Creature) obj);
                    }
                }

            }

            if (vision[1][1] - vision[0][1] < 0 && objY - vision[0][1] < 0
            && objY - vision[0][1] > -VISION_DISTANCE
            && Math.abs(objX - vision[0][0]) < MARGIN_OF_ERROR) {

                // Creature is facing up and sees an object
                stimulated = true;

                if (obj instanceof Food && species < 1) {
                    //stimFood = true;
                    setStimFood();
                } else if (obj instanceof Creature) {
                    if (((Creature) obj).getSpecies() > species) {
                        //stimFlee = true; // die() if too close;
                        setStimFlee();
                    } else if (((Creature) obj).getSpecies() < species) {
                        //stimFight = true; // kill((Creature)obj) if close
                        // enough;
                        setStimFight();
                    }
                }

            } else if (vision[1][0] - vision[0][0] < 0 && objX - vision[0][0] < 0
            && objX - vision[0][0] > -VISION_DISTANCE
            && Math.abs(objY - vision[0][1]) < MARGIN_OF_ERROR) {

                // Creature is facing left and sees an object
                stimulated = true;

                if (obj instanceof Food && species < 1) {
                    //stimFood = true;
                    setStimFood();
                } else if (obj instanceof Creature) {
                    if (((Creature) obj).getSpecies() > species) {
                        //stimFlee = true; // die() if too close;
                        setStimFlee();
                    } else if (((Creature) obj).getSpecies() < species) {
                        //stimFight = true; // kill((Creature)obj) if close
                        // enough;
                        setStimFight();
                    }
                }

            } else if (vision[1][1] - vision[0][1] > 0 && objY - vision[0][1] > 0
            && objY - vision[0][1] < VISION_DISTANCE
            && Math.abs(objX - vision[0][0]) < MARGIN_OF_ERROR) {

                // Creature is facing down and sees an object
                stimulated = true;

                if (obj instanceof Food && species < 1) {
                    //stimFood = true;
                    setStimFood();
                } else if (obj instanceof Creature) {
                    if (((Creature) obj).getSpecies() > species) {
                        //stimFlee = true; // die() if too close;
                        setStimFlee();
                    } else if (((Creature) obj).getSpecies() < species) {
                        //stimFight = true; // kill((Creature)obj) if close
                        // enough;
                        setStimFight();
                    }
                }

            } else if (vision[1][0] - vision[0][0] > 0 && objX - vision[0][0] > 0
            && objX - vision[0][0] < VISION_DISTANCE
            && Math.abs(objY - vision[0][1]) < MARGIN_OF_ERROR) {

                // Creature is facing right and sees an object
                stimulated = true;

                if (obj instanceof Food && species < 1) {
                    //stimFood = true;
                    setStimFood();
                } else if (obj instanceof Creature) {
                    if (((Creature) obj).getSpecies() > species) {
                        //stimFlee = true; // die() if too close;
                        setStimFlee();
                    } else if (((Creature) obj).getSpecies() < species) {
                        //stimFight = true; // kill((Creature)obj) if close
                        // enough;
                        setStimFight();
                    }
                }

            }

            //eating = false;
            stimuli &= (~ON_FOOD);

        }

    }

    public void die() {
        this.foodPoints = 0;

	if(!divided){
	    this.timeToDivide = EATEN;
	}

        dead = true;
    }

    public void kill(Creature enemy) {
        foodPoints += enemy.getFoodPoints();
        enemy.die();
    }

    public void eat(Food food) {

        int foodAmount = food.getAmount();

        if (eatSpeed <= foodAmount) {
            // There is enough for the Creature to have a full bite
            foodPoints += eatSpeed;
            food.setAmount(foodAmount - eatSpeed);
        } else {
            // Not enough food for the Creature to have a full bite
            foodPoints += foodAmount;
            food.setAmount(0);
        }

        setSize(foodPoints);
    }

    public void setSize(int size) {
        width = ((int) Math.pow(size, 0.25));
        height = 2 * width;

        xPoints[0] = getX();
        xPoints[1] = getX() - width / 2;
        xPoints[2] = getX() + width / 2;

        yPoints[0] = getY() - 3 * height / 4;
        yPoints[1] = yPoints[2] = getY() + height / 4;

        for (int i = 0; i < 3; i++) {
            double xTemp = xPoints[i];
            double yTemp = yPoints[i];

            xPoints[i] = (Math.cos(rot) * (xTemp - getX())
            + Math.sin(rot) * (yTemp - getY())) + getX();
            yPoints[i] = (-Math.sin(rot) * (xTemp - getX())
            + Math.cos(rot) * (yTemp - getY())) + getY();

        }

        int dec = 6000 * (species + 1);

	// Change the movement speed here
        /*
	  if((START_MOVE_SPEED - (foodPoints / dec)) / (species + 1) > 0) {
            movementSpeed = (START_MOVE_SPEED - (foodPoints / dec)) / (species + 1);
	  }
	*/

    }

    public void rotate(double angle) {

        for (int i = 0; i < 3; i++) {
            double xTemp = xPoints[i];
            double yTemp = yPoints[i];

            xPoints[i] = (Math.cos(angle) * (xTemp - getX())
            + Math.sin(angle) * (yTemp - getY())) + getX();
            yPoints[i] = (-Math.sin(angle) * (xTemp - getX())
            + Math.cos(angle) * (yTemp - getY())) + getY();

        }

        rot += angle;
        rot %= (2 * Math.PI);

        setVision(rot);

    }

    public long move() {
        if (!dead) {

            if (species < 2) {
                foodPoints -= starvingRate;
                setSize(foodPoints);

                // 32 bits in an int - 2 bits per instruction = 16 instructions max
                if (getStimFlee()) {

                    // Spotted a predator
                    execute(genes[1], instCount);

                } else if (getOnFood()) {

                    // On a food source
                    execute(genes[0], instCount);

                } else if (getStimFight()) {

                    // Spotted prey
                    execute(genes[2], instCount);

                } else if (getStimFood()) {

                    // Spotted food
                    execute(genes[3], instCount);

                } else {

                    // Unstimulated
                    execute(genes[4], instCount);

                }

                instCount++;
                instCount %= NUM_INSTRUCTIONS;

                if (checkDivide()) {
                    return divide();
                }
		
            }
        }

        return -1;
	
    }
    
    // Will execute the moves corresponding to the sequence of moves
    // starting at the indicated start bit in the gene
    public void execute(int gene, int inst) {
        // 111111111111 = 2^13 - 1 = 4095

        //long moveSeq = (((4095 << start) & gene) >> (64 - start));

        int move = (int)((gene >> (INSTRUCTION_LENGTH * inst)) & 3);

        if(move == 3) {
            rotate(Math.PI / 2);
        } else if(move == 2) {
            rotate(3 * Math.PI / 2);
        } else if(move == 1) {
            checkStimulus();
        } else {
            advance(enviBounds);
        }
    }

    public void advance(Rectangle bounds) {

        // The tip of the creature
        int point = 3 * height / 4;

        if (rot == 0 && getY() - movementSpeed > point) {
            // Facing up
            incY(-movementSpeed);
        } else if (rot == Math.PI / 2 && getX() - movementSpeed > point) {
            // Facing left
            incX(-movementSpeed);
        } else if (rot == Math.PI && getY() + movementSpeed < bounds.getHeight() - point) {
            // Facing down
            incY(movementSpeed);
        } else if (rot == 3 * Math.PI / 2 && getX() + movementSpeed < bounds.getWidth() - point) {
            // Facing right
            incX(movementSpeed);
        } else {
            //rotate((Math.PI / 2) * (2 * rand.nextInt(2) - 1));
            rotate(Math.PI);
        }

        setVision(rot);
    }

    public boolean checkDivide() {
        return foodPoints > divisionThreshold;
    }

    public long divide() {
        divideCount++;

        String genome = "";

        if(divideCount % 10 == 0) {
            //printGenome();
            genome = getGenome();

            // Indexing of genes starts at 1

            // Needs fixing!!!!!!!
            if(species == 0) {
                //herbDivCount++;
                //instCountinGene += (double)getInstCountinGene(1, "C");
                //System.out.println(instCountinGene / (double)(herbDivCount));
                //System.out.println(getInstCountinGene(1, "C"));
            }
        }

        Rectangle bounds = game.getBounds();

        //foodPoints = DEFAULT_FP;
        //setSize(foodPoints);

        //rotate(-rot);

        for(int i = 0; i < 4 - (2 * species); i++) {
            Creature child = new Creature(getX(), getY(), (Math.PI / 2) * (i + 1), species, game);

            child.setNumAncestors(getNumAncestors() + 1);

            for(int geneNum = 0; geneNum < genes.length; geneNum++) {

                // Copy parent's genome into its children's
                child.setGene(geneNum, this.getGene(geneNum));

            }

            int numMutations = rand.nextInt(MUTATION_RATE);

            for(int j = 0; j < numMutations; j++) {
                child.mutate();
            }

            enviObjects.add(child);
            child.advance(bounds);
        }

	this.divided = true;
	die();


        // enviObjects.printCreatureNum();

        return timeToDivide;
    }

    // Mutate one instruction in one gene
    public void mutate() {

        // Mutate an instruction randomly
        int mutIndex = rand.nextInt(4) << rand.nextInt(32);

        int geneNum = rand.nextInt(5);

        // Toggles the bit at the mutIndex index
        setGene(geneNum, genes[geneNum] ^= mutIndex);
    }

    public void printGenome() {
        System.out.print(divideCount + " divisions have occurred --- Species: " + species + "\n\n"
        + "Gene 1 (On Food)     : " + toInst(genes[0], NUM_INSTRUCTIONS) + "\n"
        + "Gene 2 (See Predator): " + toInst(genes[1], NUM_INSTRUCTIONS) + "\n"
        + "Gene 3 (See Prey)    : " + toInst(genes[2], NUM_INSTRUCTIONS) + "\n"
        + "Gene 4 (See Food)    : " + toInst(genes[3], NUM_INSTRUCTIONS) + "\n"
        + "Gene 5 (Unstimulated): " + toInst(genes[4], NUM_INSTRUCTIONS) + "\n\n"
        + "----------------------------------------------");
    }

    public String getGenome() {
        return "\n\n"    + divideCount + " divisions have occurred --- Species: " + species + "\n\n"
        + "Gene 1 (On Food)     : " + toInst(genes[0], NUM_INSTRUCTIONS) + "\n"
        + "Gene 2 (See Predator): " + toInst(genes[1], NUM_INSTRUCTIONS) + "\n"
        + "Gene 3 (See Prey)    : " + toInst(genes[2], NUM_INSTRUCTIONS) + "\n"
        + "Gene 4 (See Food)    : " + toInst(genes[3], NUM_INSTRUCTIONS) + "\n"
        + "Gene 5 (Unstimulated): " + toInst(genes[4], NUM_INSTRUCTIONS) + "\n\n"
        + "----------------------------------------------";

    }

    public void printGene(int geneNum) {
        System.out.println(toInst(genes[geneNum], NUM_INSTRUCTIONS));
    }

    public int getInstCountinGene(int geneNum, String inst) {

        boolean err = true;
        int count = 0;

        for(String s: INSTRUCTIONS) {
            if(inst.equals(s)) {
                err = false;
                break;
            }
        }

        if(err) {
            System.err.println("PrintInstCountinGene given invalid instruction String");
        }

        String instList = toInst(genes[geneNum - 1], NUM_INSTRUCTIONS);

        Scanner listScan = new Scanner(instList);

        String next = "";

        while(listScan.hasNext()) {
            next = listScan.next();
            if(next.equals(inst)) {
                count++;
            }
        }

        return count;
    }

    public String toInst(int gene, int numInst) {

        String result = "";

        for(int i = 0; i < numInst; i++) {
            //int pos = i * INSTRUCTION_LENGTH;

            int toConvert = gene >> (i * INSTRUCTION_LENGTH) & 3;

            String inst = INSTRUCTIONS[toConvert];

	    result += inst + " ";

	}

	return result;
    }

    @Override
	public void draw(Graphics g) {
	
	int[] approxX = new int[3];
	int[] approxY = new int[3];
	
	for (int i = 0; i < 3; i++) {
	    approxX[i] = (int) xPoints[i];
	    approxY[i] = (int) yPoints[i];
	}
	
	g.setColor(Color.BLACK);
	g.drawPolygon(approxX, approxY, 3);
	
	g.setColor(this.color);
	
	g.fillPolygon(approxX, approxY, 3);
	
	if (stimulated) {
	    if (getStimFlee()) {
		// Sees another Creature
		g.setColor(Color.RED);
	    } else if (getStimFood() || getStimFight()) {
		g.setColor(Color.GREEN);
	    } else {
		g.setColor(Color.ORANGE);
	    }
	} else {
	    g.setColor(Color.ORANGE);
	}
	
	g.drawLine(vision[0][0], vision[0][1], vision[1][0], vision[1][1]);
    }
}
