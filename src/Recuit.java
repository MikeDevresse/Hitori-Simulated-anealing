import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

public class Recuit {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED_BACKGROUND = "\u001B[41m";

    private final double decay;
    private final double temperature;
    private final int timesPerTemperature;
    private final double resetUnder;
    private final double resetTemp;

    public int[][] grid;
    private ArrayList<int[]> commons;
    public long start;
    public static Random r = new Random();

    public Recuit(String fileName, int size, double temperature, double decay, int timesPerTemperature, double resetUnder, double resetTemp) {
        start = System.currentTimeMillis();

        this.temperature = temperature;
        this.decay = decay;
        this.timesPerTemperature = timesPerTemperature;
        this.resetUnder = resetUnder;
        this.resetTemp = resetTemp;

        this.grid = loadFromFile(fileName,size);
        this.setCommons();
    }

    public void solve() {
        int currentScore = getScore(this.grid);
        double T = this.temperature;
        int states = 0, nextScore;
        int[][] nextGrid;
        while(currentScore > 0) {
            for(int i = 0 ; i < timesPerTemperature ; i++) {
                nextGrid = copy(this.grid);
                //nextGrid = this.grid;
                randomize(nextGrid,this.commons);
                nextScore = getScore(nextGrid);

                if(nextScore <= 0) {
                    this.grid = nextGrid;
                    return;
                }
                if(nextScore >= currentScore && !(r.nextInt(100) < 1.00/(1.00 + Math.exp((nextScore-currentScore)/T)))) continue;

                currentScore = nextScore;
                states++;
                this.grid = nextGrid;
            }
            T *= this.decay;
            if(T < resetUnder) {
                T = resetTemp;
                long timeElapsed = System.currentTimeMillis() - start;
                start = System.currentTimeMillis();
                System.out.printf(
                        "Score: %d ; States : %d ; time %dms ; rate: %d states per sec%n",
                        currentScore,
                        states,
                        timeElapsed,
                        Math.round(states/(timeElapsed/1000.))
                );
                states = 0;
            }
        }

    }

    public void setCommons() {
        this.commons = new ArrayList<>();
        for(int i = 0 ; i < this.grid.length ; i++) {
            for(int j = 0 ; j < this.grid[i].length ; j++) {
                if(this.hasCommon(i,j)) {
                    int[] val = new int[2];
                    val[0] = i;
                    val[1] = j;
                    this.commons.add(val);
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("\n+" + "-----+".repeat(grid[0].length) + "\n");
        for(int i = 0 ; i<grid.length ; i++) {
            str.append("| ").append(Arrays.stream(grid[i]).mapToObj(val -> val < 0 ? ANSI_RED_BACKGROUND + ANSI_BLACK + String.format("%3d", val) + ANSI_RESET : String.format("%3d", val)).collect(Collectors.joining(" | "))).append(" |");
            str.append("\n+").append("-----+".repeat(grid[0].length)).append("\n");
        }

        return str.toString();
    }

    public static int[][] copy(int[][] grid) {
        int[][] copy = new int[grid.length][grid.length];
        for(int i = 0; i < grid.length; i++){
            System.arraycopy(grid[i], 0, copy[i], 0, grid.length);
        }
        return copy;
    }

    public static int getScore(int[][] grid) {
        int score = 0;
        for(int i = 0 ; i<grid.length ; i++) {
            boolean[] duplicatedI = new boolean[grid.length];
            boolean[] duplicatedJ = new boolean[grid.length];

            for(int j = 0 ; j<grid[i].length ; j++) {
                int val;
                if((val = grid[i][j]) > 0) {
                    if(!duplicatedI[val-1]) duplicatedI[val-1] = true;
                    else score++;
                }
                if((val = grid[j][i]) > 0) {
                    if(!duplicatedJ[val-1]) duplicatedJ[val-1] = true;
                    else score++;
                }
            }
        }
        return score;
    }

    public static void randomize(int[][] grid, ArrayList<int[]> commons) {
        int i, j, k;
        boolean changed = false;
        do {
            k = r.nextInt(commons.size());
            i = commons.get(k)[0];
            j = commons.get(k)[1];
            if(grid[i][j] < 0 || (!hasDarkNeighbors(grid,i,j))) {
                grid[i][j] = -grid[i][j];
                if(!isValid(grid)) {
                    grid[i][j] = -grid[i][j];
                }
                else {
                    changed = true;
                }
            }
        } while(!changed);
    }

    public static boolean isValid(int[][] grid) {
        int exploreCount;
        boolean[][] exploreGrid = new boolean[grid.length][grid.length];

        if(grid[0][0] > 0) exploreCount = explore(grid,0,0,exploreGrid);
        else               exploreCount = explore(grid,1,0,exploreGrid);

        return exploreCount == grid.length * grid.length;
    }

    public static int explore(int[][] grid, int i, int j, boolean[][] exploreGrid) {
        exploreGrid[i][j] = true;
        int cpt = 1;
        if(grid[i][j] > 0) {
            if(i != 0                  && (!exploreGrid[i-1][j])) cpt += explore(grid,i-1,j,exploreGrid);
            if(i != grid.length - 1    && (!exploreGrid[i+1][j])) cpt += explore(grid,i+1,j,exploreGrid);
            if(j != 0                  && (!exploreGrid[i][j-1])) cpt += explore(grid,i,j-1,exploreGrid);
            if(j != grid[i].length - 1 && (!exploreGrid[i][j+1])) cpt += explore(grid,i,j+1,exploreGrid);
        }
        return cpt;
    }

    public static boolean hasDarkNeighbors(int[][] grid, int i,int j) {
        if(i != 0                && grid[i-1][j] < 0) return true;
        if(i != grid.length-1    && grid[i+1][j] < 0) return true;
        if(j != 0                && grid[i][j-1] < 0) return true;
        if(j != grid[i].length-1 && grid[i][j+1] < 0) return true;
        return false;
    }

    public boolean hasCommon(int i, int j) {
        for(int k = 0 ; k < this.grid.length ; k++) {
            if(i != k && this.grid[i][j] == this.grid[i][k]) return true;
            if(j != k && this.grid[i][j] == this.grid[k][j]) return true;
        }
        return false;
    }

    public static int[][] loadFromFile(String fileName, int size) {
        int[][] grid = new int[size][size];
        try {
            Scanner reader = new Scanner(new File(fileName));
            int i = 0;
            while(reader.hasNextLine()) {
                String[] lineSplited = reader.nextLine().split(" ");
                grid[i] = new int[size];
                int j = 0;
                for (String l: lineSplited) {
                    grid[i][j++] = Integer.parseInt(l);
                }
                i++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return grid;
    }
    public static int[][] loadInput() {
        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        int[][] grid = new int[n][n];
        if (in.hasNextLine()) {
            in.nextLine();
        }
        int i = 0;
        while(in.hasNextLine()) {
            String[] lineSplitted = in.nextLine().split("");
            grid[i] = new int[n];
            int j = 0;
            for (String l: lineSplitted) {
                int val = Integer.parseInt(l);
                if(val > 0)
                    grid[i][j++] = val;
            }
            i++;
        }
        in.close();
        return grid;
    }

    public static void main(String[] args) {
        long firstStart = System.currentTimeMillis();
        Recuit h = new Recuit("15.txt",15,100,0.9992,200,0.000001, 50);
        h.solve();
        System.out.println(h);
        long timeElapsed = System.currentTimeMillis() - firstStart;
        System.out.println(timeElapsed + "ms");
    }
}
