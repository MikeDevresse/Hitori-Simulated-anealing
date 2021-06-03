import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Hitori est une classe permétant la résolution d'une grille du jeu Hitori
 *
 * @author Mike Devresse
 */
public class Hitori {
    /** Variables utilisées pour le rendu */
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED_BACKGROUND = "\u001B[41m";

    /**
     * Determine la rapidité de diminution de la température, la température serras multiplié par ce nombre à chaque fois.
     * Toujours < 1 et proche de 1 (Ex: 0.992)
     */
    private final double decay;

    /**
     * Température initiale, plus elle est élevé, plus les solutions seront variées en début de résolution.
     */
    private final double temperature;

    /**
     * Nombre d'itérations à effectuer pour chaque température obtenue
     */
    private final int timesPerTemperature;

    /**
     * Nombre à partir de laquelle nous allons réinitialiser la température par la valeur resetTemp
     * @see Hitori#resetTemp
     */
    private final double resetUnder;

    /**
     * Lorsque la température resetUnder est atteinte, nous réinitialisons la température à celle indiqué par cette variable.
     * @see Hitori#resetUnder
     */
    private final double resetTemp;

    /**
     * Grille de jeu courante à partir de laquelle nous allons régénérer une nouvelle grille
     */
    private int[][] grid;

    /**
     * Liste de coordonnées sur lesquels nous avons détecter qu'un même nombre se trouve sur la même ligne ou colonne.
     * @see Hitori#hasCommon(int[][], int, int)
     * @see Hitori#setCommons()
     */
    private ArrayList<int[]> commons;

    /**
     * Temps à partir duquel la température à été réinitialiser la dernière fois
     */
    public long start;

    /**
     * Nouveau random qui va être utilisé pour choisir un nombre et pour juger notre solution dans la méthode solve
     * @see Hitori#solve()
     * @see Hitori#randomize(int[][], ArrayList)
     */
    public static Random r = new Random();

    /**
     * Constructeur de la classe Hitori permettant de choisir tous les paramètres affectant la résolution de la grille
     * @param fileName nom du fichier à partir duquel charger la grille
     * @param size taille de la grille
     * @param temperature température initiale
     * @param decay facteur de diminution de la température
     * @param timesPerTemperature itération par température
     * @param resetUnder seuil bas de température après laquelle on remet à resetTemp la température
     * @param resetTemp valeur à laquelle on remet la température une fois le seuil atteint
     */
    public Hitori(String fileName, int size, double temperature, double decay, int timesPerTemperature, double resetUnder, double resetTemp) {
        start = System.currentTimeMillis();

        this.temperature = temperature;
        this.decay = decay;
        this.timesPerTemperature = timesPerTemperature;
        this.resetUnder = resetUnder;
        this.resetTemp = resetTemp;

        this.grid = loadFromFile(fileName,size);
        this.setCommons();
    }

    /**
     * Méthode principale qui tant que nous n'avons pas trouver de grille valide, en génère une à partir de notre meilleure
     * solution et juge si oui ou non on doit garder cette solution pour la suite.
     * 
     * @see Hitori#randomize(int[][], ArrayList) 
     */
    public void solve() {
        double T = this.temperature;
        int currentScore = getScore(this.grid), states = 0, nextScore;
        int[][] nextGrid;

        /* On ne s'arrête pas tant que nous n'avons pas trouver de solution */
        while(currentScore > 0) {
            for(int i = 0 ; i < timesPerTemperature ; i++) {
                /* on copie la grille puis nous lui changeons une case et on récupère son score */
                nextGrid = copy(this.grid);
                randomize(nextGrid,this.commons);
                nextScore = getScore(nextGrid);

                /* Si son score est 0 alors on à notre solution on peut arrêter le programme */
                if(nextScore <= 0) {
                    this.grid = nextGrid;
                    return;
                }
                /*
                 * si le score est supérieur ou égale au score déjà enregistré ou si la méthode du recuit ne nous donne 
                 * pas une meilleure solution alors on passe à l'itération suivante 
                 */
                if(nextScore >= currentScore && !(r.nextInt(100) < 1.00/(1.00 + Math.exp((nextScore-currentScore)/T)))) continue;

                /* sinon on sauvegarde la grille, le score et on incrémente notre nombre d'état sauvegardé (à but statistique) */
                currentScore = nextScore;
                this.grid = nextGrid;
                states++;
            }
            
            /* on applique notre facteur de diminution de température */
            T *= this.decay;
            
            /* Si notre température à atteint le seuil bas alors on la remet à 0 (et on affiche des statistiques) */
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

    /**
     * Méthode permettant de déterminer les cases qui ont des valeurs communes et les ajoutes à l'ArrayList
     * @see Hitori#commons
     * @see Hitori#hasCommon(int[][], int, int) 
     */
    public void setCommons() {
        this.commons = new ArrayList<>();
        /* On parcours chaque ligne pour appliquer la méthode hasCommon pour voir si la case à des éléments en commun */
        for(int i = 0 ; i < this.grid.length ; i++) {
            for(int j = 0 ; j < this.grid[i].length ; j++) {
                if(hasCommon(grid,i,j)) {
                    int[] val = new int[2];
                    val[0] = i;
                    val[1] = j;
                    this.commons.add(val);
                }
            }
        }
    }

    /**
     * Méthode permettant de formaté la grille courrante
     * @return Un String contenant notre grille formaté avec en fond rouge les cases "noircis"
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("\n+" + "-----+".repeat(grid[0].length) + "\n");
        for (int[] row : grid) {
            str.append("| ")
                /* Si notre valeure est négative, alors on l'écrit en noir sur fond rouge */
                .append(Arrays.stream(row).mapToObj(
                        val -> val < 0 ?
                            ANSI_RED_BACKGROUND + ANSI_BLACK + String.format("%3d", val) + ANSI_RESET :
                            String.format("%3d", val)
                ).collect(Collectors.joining(" | ")))
                .append(" |")
                .append("\n+").append("-----+".repeat(grid[0].length)).append("\n");
        }

        return str.toString();
    }

    /**
     * Copie une grille à l'identique
     * @param grid grille à copié
     * @return Copie de la grille
     */
    public static int[][] copy(int[][] grid) {
        int[][] copy = new int[grid.length][grid.length];
        for(int i = 0; i < grid.length; i++){
            System.arraycopy(grid[i], 0, copy[i], 0, grid.length);
        }
        return copy;
    }

    /**
     * Calcul le score d'une grille, le score se calcul en comptant le nombre d'occurrence de chaque élément sur la
     * même ligne ou colonne. Plus le score est élevé, moins la solution est bonne.
     * @param grid Grille sur laquelle calculé le score
     * @return Entier représentant le score.
     */
    public static int getScore(int[][] grid) {
        int score = 0;
        for(int i = 0 ; i<grid.length ; i++) {
            boolean[] duplicatedI = new boolean[grid.length+1];
            boolean[] duplicatedJ = new boolean[grid.length+1];

            /*
             * Pour chaque ligne et colonne, si on à pas déjà trouver l'élément alors on marque comme quoi on l'a déjà
             * trouver, si on l'a déjà trouver alors on incrémente le score
             */
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

    /**
     * Permet de noircir une case aléatoire parmi les éléments communs de la grille et de vérifier que la case noirci
     * n'enfreint pas les règles du jeu à savoir être connecté et ne pas avoir deux cases noirci adjacentes
     * @param grid Grille sur laquelle changé l'élément
     * @param commons Elements communs de la grille
     *
     * @see Hitori#commons
     * @see Hitori#isValid(int[][])
     * @see Hitori#hasDarkNeighbors(int[][], int, int)
     */
    public static void randomize(int[][] grid, ArrayList<int[]> commons) {
        int i, j, k;
        boolean changed = false;
        do {
            /* choisis un élément aléatoire parmi les éléments communs */
            k = r.nextInt(commons.size());
            i = commons.get(k)[0];
            j = commons.get(k)[1];
            /*
             * Si la case est déjà noirci donc inférieur à 0 alors on peut la remettre dans son état d'origine sinon
             * on vérifie que les voisins ne sont pas noirci avant d'effectuer le changement
             */
            if(grid[i][j] < 0 || (!hasDarkNeighbors(grid,i,j))) {
                grid[i][j] = -grid[i][j];
                /* Si la grille n'est plus valide alors on revient en arrière sur nos changements */
                if(grid[i][j] < 0 && !isValid(grid)) grid[i][j] = -grid[i][j];
                /* Sinon on indique qu'on a trouver une solution */
                else changed = true;
            }
        } while(!changed);
    }

    /**
     * Vérifie la connectivité de tous les éléments dans la grille en éffectuant un parcours en profondeur récursivement
     * @param grid Grille sur laquelle vérifier la connectivité
     * @return vrai si la grille est connecté, faux sinon
     */
    public static boolean isValid(int[][] grid) {
        int exploreCount;
        boolean[][] exploreGrid = new boolean[grid.length][grid.length];

        /*
         * Si la case aux coordonnées 0,0 est noirci alors on commence le parcours sur la case 1,0 car on sait qu'elle
         * n'est pas noirci vu que deux cases adjacentes ne peuvent pas être noirci
         */
        if(grid[0][0] > 0) exploreCount = explore(grid,0,0,exploreGrid);
        else               exploreCount = explore(grid,1,0,exploreGrid);

        /* Pour vérifier la connectivité on vérifie que l'on a parcouru toutes les cases du tableau donc le carré de la taille */
        return exploreCount == grid.length * grid.length;
    }

    /**
     * Méthode récursive qui permet de parcourir toute la grille en profondeur et compte le nombre d'éléments parcourus
     * @param grid Grille sur laquelle parcourir
     * @param i index de la ligne que l'on parcours
     * @param j index de la colonne que l'on parcours
     * @param exploreGrid Tableau de booléen permettant de signaliser si on a parcours les cases selon les coordonnées
     * @return Un entier faisant la somme de la récursivité signifiant le nombre d'éléments parcourus
     */
    public static int explore(int[][] grid, int i, int j, boolean[][] exploreGrid) {
        /* On marque l'élément à nos coordonnées */
        exploreGrid[i][j] = true;

        /* On retourne au moins 1 */
        int cpt = 1;
        /* Si l'élément est noirci alors on ne propage pas l'exploration */
        if(grid[i][j] > 0) {
            /* sinon on vérifie si on est pas sur une bordure et si l'élément n'est pas marqué */
            if(i != 0                  && (!exploreGrid[i-1][j])) cpt += explore(grid,i-1,j,exploreGrid);
            if(i != grid.length - 1    && (!exploreGrid[i+1][j])) cpt += explore(grid,i+1,j,exploreGrid);
            if(j != 0                  && (!exploreGrid[i][j-1])) cpt += explore(grid,i,j-1,exploreGrid);
            if(j != grid[i].length - 1 && (!exploreGrid[i][j+1])) cpt += explore(grid,i,j+1,exploreGrid);
        }
        return cpt;
    }

    /**
     * Méthode permettant de savoir si un voisin de la case est noirici ou pas
     * @param grid Grille sur laquelle effectuer la recherche
     * @param i index de la ligne de notre case
     * @param j index de la colonne de notre case
     * @return Vrai si un voisin est noirci, non si aucun voisin est noirci
     */
    public static boolean hasDarkNeighbors(int[][] grid, int i,int j) {
        if(i != 0                && grid[i-1][j] < 0) return true;
        if(i != grid.length-1    && grid[i+1][j] < 0) return true;
        if(j != 0                && grid[i][j-1] < 0) return true;
        if(j != grid[i].length-1 && grid[i][j+1] < 0) return true;
        return false;
    }

    /**
     * Méthode permettant de savoir si la case spécifié possède un doublon sur la même ligne ou colonne
     * @param grid Grille sur laquelle effectuer la recherche
     * @param i index de la ligne de notre case
     * @param j index de la colonne de notre case
     * @return Vrai si la case possède un doublon, faux sinon
     */
    public static boolean hasCommon(int[][] grid, int i, int j) {
        for(int k = 0 ; k < grid.length ; k++) {
            if(i != k && grid[i][j] == grid[i][k]) return true;
            if(j != k && grid[i][j] == grid[k][j]) return true;
        }
        return false;
    }

    /**
     * Génère une grille selon un fichier et une taille
     * @param fileName Nom du fichier à générer
     * @param size Taille de la grille
     * @return Une grille remplis selon le fichier
     */
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

    public static void main(String[] args) {
        long firstStart = System.currentTimeMillis();
        Hitori h = new Hitori("15.txt",15,100,0.9992,200,0.000001, 50);
        h.solve();
        System.out.println(h);
        long timeElapsed = System.currentTimeMillis() - firstStart;
        System.out.println(timeElapsed + "ms");
    }
}
