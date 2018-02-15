import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.security.SecureRandom;
import java.util.*;
import javax.swing.*;

// --------------------------------------------------------
class State {
    public int r, c;    // row and column of the room in the dungeon
    public int s;       // status: 0 - alive, -1 - dead, -2 - outside, 1 (for a princess) - escorted, 1+ (for a knight) - escorts that many princesses
    public State(int nr, int nc) {
        r = nr;
        c = nc;
        s = 0;
    }
    public void follow(State st) {
        r = st.r;
        c = st.c;
    }
}

// --------------------------------------------------------
public class PrincessesAndMonstersVis {
    static final int minS = 10, maxS = 50;
    static final int minK = 20, maxK = 100;
    // movement directions: N E W S (any other character)
    static final int[] dr = {-1, 0,  0, 1, 0};
    static final int[] dc = { 0, 1, -1, 0, 0};

    static final int invalidScore = -1000000000;    // valid scores can be negative this time

    int S;                          // size of the dungeon
    int P, M, K;                    // numbers of: princesses, monsters, knights
    State[] state;                  // list of states (position + status) of all entities in the story
                                    // first P are princesses, next M are monsters, the rest are knights
    ArrayList<Integer> pInd, mInd, kInd;     // indices of live entities of each type in the states list

    int[] follow;                   // index of the knight which escorts i-th princess, if her status is 1
    ArrayList<Integer>[][] princesses, monsters, knights; // list of each type of entity in specific rooms

    SecureRandom rnd;
    // ---------------------------------------------------
    boolean isInside(int r, int c) {
        return r >= 0 && c >= 0 && r < S && c < S;
    }
    // ---------------------------------------------------
    boolean isExit(State p) {
        return (p.r == 0 || p.r == S-1) && (p.c == 0 || p.c == S-1);
    }
    // ---------------------------------------------------
    int getMoveDir(char c) {
        if (c == 'N') return 0;
        if (c == 'E') return 1;
        if (c == 'W') return 2;
        if (c == 'S') return 3;
        return 4;
    }
    // ---------------------------------------------------
    void makeMove(int ind, int dir) {
        int r = state[ind].r + dr[dir], c = state[ind].c + dc[dir];
        if (isInside(r, c)) {
            state[ind].r = r;
            state[ind].c = c;
        }
    }
    // ---------------------------------------------------
    String generate(String seedStr) {
      try {
        rnd = SecureRandom.getInstance("SHA1PRNG");
        long seed = Long.parseLong(seedStr);
        rnd.setSeed(seed);

        S = rnd.nextInt(maxS - minS + 1) + minS;
        K = rnd.nextInt(maxK - minK + 1) + minK;
        if (seed == 1) {
            S = minS;
            K = minK;
        }
        else if (seed == 2) {
            S = maxS;
            K = maxK;
        }

        // princesses are rare
        int minP = S / 5, maxP = S;
        P = rnd.nextInt(maxP - minP + 1) + minP;
        // monsters are more frequent
        int minM = S, maxM = (S * S) / 5;
        M = rnd.nextInt(maxM - minM + 1) + minM;
        if (seed == 1) {
            P = maxP;
            M = minM;
        }
        else if (seed == 2) {
            P = maxP;
            M = maxM;
        }

        pInd = new ArrayList<Integer>();
        mInd = new ArrayList<Integer>();
        kInd = new ArrayList<Integer>();

        state = new State[P + M + K];

        follow = new int[P];
        Arrays.fill(follow, -1);
        // princesses start in random rooms in a circle in some place of the dungeon
        // monsters start in random rooms around that circle (far from both princesses and exits)
        int inR = rnd.nextInt(S/3) + S/3;
        int inC = rnd.nextInt(S/3) + S/3;
        int innerR = rnd.nextInt(S / 6) + Math.max(S / 6, 2);
        int nextR = rnd.nextInt(S / 6) + innerR + Math.max(S / 6, 3);
        int outerR = (S * 7) / 10;
        int r, c, rad, angle;
        for (int i = 0; i < P; ++i) {
            pInd.add(i);
            rad = rnd.nextInt(innerR);
            angle = rnd.nextInt(360);
            r = inR + (int)(rad * Math.sin(angle / Math.PI / 2));
            c = inC + (int)(rad * Math.cos(angle / Math.PI / 2));
            state[i] = new State(r, c);
        }

        for (int i = P; i < P + M; ++i) {
            mInd.add(i);
            do {
                rad = rnd.nextInt(outerR - nextR) + nextR;
                angle = rnd.nextInt(360);
                r = inR + (int)(rad * Math.sin(angle / Math.PI / 2));
                c = inC + (int)(rad * Math.cos(angle / Math.PI / 2));
            } while (!isInside(r, c));
            state[i] = new State(r, c);
        }

        // generate test representation
        StringBuffer sb = new StringBuffer();
        sb.append("Dungeon size = " + S + '\n');
        sb.append("Number of princesses = " + P + '\n');
        sb.append("Number of monsters = " + M + '\n');
        sb.append("Number of knights = " + K + '\n');
        return sb.toString();
      }
      catch (Exception e) {
        addFatalError("An exception occurred while generating test case.");
        e.printStackTrace();
        return "";
      }
    }
    // -----------------------------------------
    @SuppressWarnings("unchecked")
    void countEverybody() {
        if (princesses == null) {
            princesses = new ArrayList[S][S];
            monsters = new ArrayList[S][S];
            knights = new ArrayList[S][S];
            for (int i = 0; i < S; ++i)
            for (int j = 0; j < S; ++j) {
                princesses[i][j] = new ArrayList<Integer>();
                monsters[i][j] = new ArrayList<Integer>();
                knights[i][j] = new ArrayList<Integer>();
            }
        } else {
            for (int i = 0; i < S; ++i)
            for (int j = 0; j < S; ++j) {
                princesses[i][j].clear();
                monsters[i][j].clear();
                knights[i][j].clear();
            }
        }
        for (int i : pInd)
                princesses[state[i].r][state[i].c].add(i);
        for (int i : mInd)
                monsters[state[i].r][state[i].c].add(i);
        for (int i : kInd)
                knights[state[i].r][state[i].c].add(i);
    }
    // ---------------------------------------------------
    public double runTest(String seed) {
      try {
        String test = generate(seed);
        if (debug)
            System.out.println(test);
        SecureRandom rndMove = SecureRandom.getInstance("SHA1PRNG");
        rndMove.setSeed(rnd.nextLong());

        // convert princesses' and monsters' positions to int[]s
        int[] princessArg = new int[2 * P];
        for (int i = 0; i < P; ++i) {
            princessArg[2*i]   = state[i].r;
            princessArg[2*i+1] = state[i].c;
        }
        int[] monsterArg = new int[2 * M];
        for (int i = 0; i < M; ++i) {
            monsterArg[2*i]   = state[P + i].r;
            monsterArg[2*i+1] = state[P + i].c;
        }

        // initialize user's solution and place knights based on return
        int timeLeft = TL * 1000;
        long startTime;
        String init;
        try {
            startTime = System.currentTimeMillis();
            init = initialize(S, princessArg, monsterArg, K);
            timeLeft -= (int)(System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            addFatalError("Failed to get result from initialize.");
            System.out.println(e);
            return invalidScore;
        }
        if (timeLeft < 0) {
            addFatalError("Init: Time limit exceeded.");
            return invalidScore;
        }

        // place the knights
        if (init.length() != K) {
            addFatalError("Init: return must be exactly " + K + " characters long, it was " + init.length() + ".");
            return invalidScore;
        }

        for (int i = 0; i < K; ++i) {
            char c = init.charAt(i);
            if (c < '0' || c > '3') {
                addFatalError("Init: each character of return must be 0..3, your return contained '" + c + "'.");
                return invalidScore;
            }
            int ind = P + M + i;
            kInd.add(ind);
            // entrances are numbered in clockwise order, starting with top left corner
            if (c == '0')
                state[ind] = new State(0, 0);
            else
            if (c == '1')
                state[ind] = new State(0, S-1);
            else
            if (c == '2')
                state[ind] = new State(S-1, S-1);
            else
            if (c == '3')
                state[ind] = new State(S-1, 0);
        }

        countEverybody();

        if (vis) {
            // visualize initial state
            SZW = (S + 2) * SZ;
            SZH = (S + 2) * SZ;
            jf.setSize(SZW + 4, SZH + 26);
            jf.setVisible(true);
            draw();
        }

        // the simulation lasts while there are knights in the dungeon
        // and there is at least one monster or princess
        int score = 0, t;
        for (t = 0; kInd.size() > 0 && t < S * S * S; ++t) {
            // convert knight's statuses to argument
            int[] statusArg = new int[K];
            for (int i = 0; i < K; ++i)
                statusArg[i] = state[P + M + i].s;

            // call user's solution
            // get back list of knights' movements (as a string)
            String moves;
            try {
                startTime = System.currentTimeMillis();
                moves = move(statusArg, pInd.size(), mInd.size(), timeLeft);
                timeLeft -= (int)(System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                addFatalError("Step " + t + ": Failed to get result from move.");
                return invalidScore;
            }
            if (timeLeft < 0) {
                addFatalError("Step " + t + ": Time limit exceeded.");
                return invalidScore;
            }
            if (moves == null) {
                addFatalError("Step " + t + ": Null return");
                return invalidScore;
            }
            if (moves.length() != K) {
                addFatalError("Step " + t + ": Return from move must be exactly " + K + " characters long, it was " + moves.length() + ".");
                return invalidScore;
            }

//            if (debug)
//                System.out.println("Step " + t + ": " + moves);

            // move all knights per received instructions
            for (int i = 0; i < K; ++i) {
                int ind = P + M + i;
                if (state[ind].s < 0)
                    continue;
                int dir = getMoveDir(moves.charAt(i));
                makeMove(ind, dir);
                // if a knight tries to walk into a wall, just ignore this move, don't fail the solution
            }

            // move all live princesses: following knights or randomly
            // always consume P random numbers even if not needed
            for (int i = 0; i < P; ++i) {
                int dir = rndMove.nextInt(5);
                if (state[i].s < 0)
                    continue;
                if (state[i].s == 1) {
                    // escorted princess always follows the knight
                    state[i].follow(state[follow[i]]);
                    continue;
                }
                makeMove(i, dir);
            }

            // move all live monsters randomly
            // always consume M random numbers even if not needed
            for (int i = P; i < P + M; ++i) {
                int dir = rndMove.nextInt(5);
                if (state[i].s < 0)
                    continue;
                makeMove(i, dir);
            }

            // implement exits for knights and princesses (monsters never exit)
            for (int i = 0; i < kInd.size(); ) {
                Integer ind = kInd.get(i);
                if (isExit(state[ind])) {
                    state[ind].s = -2;
                    kInd.remove(ind);
                    if (debug)
                        System.out.println("Step " + t + ": knight " + (ind - P - M) + " has left the dungeon");
                } else {
                    ++i;
                }
            }
            for (int i = 0; i < pInd.size(); ) {
                Integer ind = pInd.get(i);
                if (isExit(state[ind])) {
                    // if princess was escorted, award points
                    if (state[ind].s == 1)
                        score += 100;
                    state[ind].s = -2;
                    pInd.remove(ind);
                    if (debug)
                        System.out.println("Step " + t + ": princess " + ind + " has left the dungeon");
                } else {
                    ++i;
                }
            }

            countEverybody();

            // interactions (everybody moved first and now interacts at the same time)
            for (int r = 0; r < S; ++r)
            for (int c = 0; c < S; ++c) {
                // 1. knights fight monsters
                while (monsters[r][c].size() > 0 && knights[r][c].size() > 0) {
                    int m = monsters[r][c].size(), k = knights[r][c].size();
                    int fall = rnd.nextInt(m + k);
                    if (fall < m) {
                        // last knight dies
                        Integer ind = knights[r][c].get(k - 1);
                        state[ind].s = -1;
                        kInd.remove(ind);
                        knights[r][c].remove(k - 1);
                        score -= 5;
                        if (debug)
                            System.out.println("Step " + t + ": knight " + (ind - P - M) + " died at (" + r + ", " + c + ")");
                    } else {
                        // last monster dies
                        Integer ind = monsters[r][c].get(m - 1);
                        state[ind].s = -1;
                        mInd.remove(ind);
                        monsters[r][c].remove(m - 1);
                        score++;
                        if (debug)
                            System.out.println("Step " + t + ": a monster died at (" + r + ", " + c + ")");
                    }
                }

                // 2. monsters kill princesses, if no knights are present
                // after fights monsters and knights can't be present in the rooom at the same time
                if (monsters[r][c].size() > 0 && princesses[r][c].size() > 0) {
                    for (Integer i : princesses[r][c]) {
                        state[i].s = -1;
                        pInd.remove(i);
                        if (debug) {
                            System.out.println("Step " + t + ": princess " + i + " died at (" + r + ", " + c + ")");
                        }
                    }
                    princesses[r][c].clear();
                }

                // 3. princesses follow the knight with the lowest index in the room
                if (knights[r][c].size() > 0 && princesses[r][c].size() > 0) {
                    int ind = knights[r][c].get(0);
                    for (Integer i : princesses[r][c]) {
                        if (state[i].s == 0) {
                            // start following new knight
                            state[i].s = 1;
                            follow[i] = ind;
                            state[ind].s++;
                            if (debug) {
                                System.out.println("Step " + t + ": princess " + i + " started following knight " + (ind - P - M));
                            }
                        } else if (state[i].s == 1 && follow[i] != ind) {
                            // if a princess was following someone but a lower index appeared, she switches
                            if (debug) {
                                System.out.println("Step " + t + ": princess " + i + " switched from following knight " + (follow[i] - P - M) + " to following knight " + (ind - P - M));
                            }
                            state[follow[i]].s--;
                            follow[i] = ind;
                            state[ind].s++;
                        }
                    }
                }
            }

            if (vis) {
                // visualize current state
                draw();
            }

/*            if (debug) {
                System.out.println("Princesses");
                for (int i = 0; i < P; ++i)
                    System.out.print(state[i].s + " ");
                System.out.println("");
                System.out.println("Monsters");
                for (int i = P; i < P + M; ++i)
                    System.out.print(state[i].s + " ");
                System.out.println("");
                System.out.println("Knights");
                for (int i = P + M; i < P + M + K; ++i)
                    System.out.print(state[i].s + " ");
                System.out.println("");
                System.out.println("");
            }*/
        }

        if (t == S * S * S && kInd.size() > 0) {
            // knights left in the dungeon after the limit die
            score -= kInd.size() * 5;
        }

        if (debug) {
            System.out.println("Steps taken: " + t);
            System.out.println("Princesses left in the dungeon: " + pInd.size());
            System.out.println("Monsters left in the dungeon: " + mInd.size());
            System.out.println("Knights left in the dungeon: " + kInd.size());
            System.out.println("Time taken (ms): " + (TL * 1000 - timeLeft));
        }

        return score;
      }
      catch (Exception e) {
        addFatalError("An exception occurred while trying to process your program's results.");
        e.printStackTrace();
        return invalidScore;
      }
    }
// ------------- visualization part ----------------------
    JFrame jf;
    Vis v;
    static int del;
    static int SZ, SZW, SZH;
    static int TL;
    static String exec;
    static boolean vis, debug;
    static Process proc;
    InputStream is;
    OutputStream os;
    BufferedReader br;
    // ---------------------------------------------------
    String initialize(int S, int[] princesses, int[] monsters, int K) throws IOException {
        if(solver != null) {
            return solver.initialize(S, princesses, monsters, K);
        }
        // pass the params to the solution and get the return
        StringBuffer sb = new StringBuffer();
        sb.append(S).append('\n');
        sb.append(princesses.length).append('\n');
        for (int i = 0; i < princesses.length; ++i)
            sb.append(princesses[i]).append('\n');
        sb.append(monsters.length).append('\n');
        for (int i = 0; i < monsters.length; ++i)
            sb.append(monsters[i]).append('\n');
        sb.append(K).append('\n');
        os.write(sb.toString().getBytes());
        os.flush();

        // get the return - a string
        String ret = br.readLine();
        return ret;
    }
    // ---------------------------------------------------
    String move(int[] status, int P, int M, int timeLeft) throws IOException {
        if(solver != null) {
            return solver.move(status, P, M, timeLeft);
        }
        // pass the params to the solution and get the return
        StringBuffer sb = new StringBuffer();
        sb.append(status.length).append('\n');
        for (int i = 0; i < status.length; ++i)
            sb.append(status[i]).append('\n');
        sb.append(P).append('\n');
        sb.append(M).append('\n');
        sb.append(timeLeft).append('\n');
        os.write(sb.toString().getBytes());
        os.flush();

        // get the return - a string
        String ret = br.readLine();
        return ret;
    }
    // -----------------------------------------
    void draw() {
        if (!vis) return;
        countEverybody();
        v.repaint();
        try { Thread.sleep(del); }
        catch (Exception e) { };
    }
    // -----------------------------------------
    public class Vis extends JPanel implements WindowListener {
        public void paint(Graphics g) {
            BufferedImage bi = new BufferedImage(SZW, SZH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = (Graphics2D)bi.getGraphics();
            char[] ch;
            // background
            g2.setColor(new Color(0xDDDDDD));
            g2.fillRect(0,0, SZW, SZH);
            g2.setColor(Color.WHITE);
            g2.fillRect(SZ, SZ, SZ * S, SZ * S);

            g2.setFont(new Font("Arial",Font.BOLD, SZ < 16 ? 10 : 12));
            FontMetrics fm = g2.getFontMetrics();

            // exits
            g2.setColor(new Color(0xB2FFBC));
            for (int r = 0; r < S; r += S-1)
            for (int c = 0; c < S; c += S-1)
                g2.fillRect(c * SZ + SZ, r * SZ + SZ, SZ, SZ);

            // cells are color-coded: gold for princess, blue for knight, gold framed in blue for both, 
            // black for monster, white for empty room
            Color[] cols = {Color.BLACK, new Color(0xFFD700), new Color(0x4682B4)};
            // on top of that numbers show how many of these are there (if more than 1)
            for (int r = 0; r < S; ++r)
            for (int c = 0; c < S; ++c) {
                int p = princesses[r][c].size(), 
                    m = monsters[r][c].size(), 
                    k = knights[r][c].size();
                if (m == 0 && p == 0 && k == 0)
                    continue;
                if (p > 0 && k > 0) {
                    // princesses escorted by knights
                    g2.setColor(cols[2]);
                    g2.fillRect(c * SZ + SZ, r * SZ + SZ, SZ, SZ);
                    g2.setColor(cols[1]);
                    g2.fillRect(c * SZ + SZ + 3, r * SZ + SZ + 3, SZ - 5, SZ - 5);
                } else {
                    int ind = (m > 0 ? 0 : p > 0 ? 1 : 2);
                    g2.setColor(cols[ind]);
                    g2.fillRect(c * SZ + SZ, r * SZ + SZ, SZ, SZ);
                }
                // add number (if both princesses and knights are there, number is for knights)
                int n = (m > 0 ? m : k > 0 ? k : p);
                if (n <= 1)
                    continue;
                g2.setColor(m > 0 ? Color.WHITE : Color.BLACK);
                ch = ("" + n).toCharArray();
                int h = r * SZ + SZ/2 + fm.getHeight()/2 - 2;
                g2.drawChars(ch, 0, ch.length, SZ + c * SZ + SZ/2 - fm.charWidth(ch[0])/2, SZ + h);
            }

            // lines between rooms
            g2.setColor(Color.BLACK);
            for (int i = 0; i <= S; i++) {
                g2.drawLine(0 + SZ, i*SZ + SZ, S*SZ + SZ, i*SZ + SZ);
                g2.drawLine(i*SZ + SZ, 0 + SZ, i*SZ + SZ, S*SZ + SZ);
            }

            g.drawImage(bi,0,0, SZW, SZH, null);
        }
        // -------------------------------------
        public Vis() {
            jf.addWindowListener(this);
        }
        // -------------------------------------
        // WindowListener
        public void windowClosing(WindowEvent e){ 
            if (proc != null)
                try { proc.destroy(); } 
                catch (Exception ex) { ex.printStackTrace(); }
            System.exit(0); 
        }
        public void windowActivated(WindowEvent e) { }
        public void windowDeactivated(WindowEvent e) { }
        public void windowOpened(WindowEvent e) { }
        public void windowClosed(WindowEvent e) { }
        public void windowIconified(WindowEvent e) { }
        public void windowDeiconified(WindowEvent e) { }
    }
    // ---------------------------------------------------
    PrincessesAndMonsters solver;
    public PrincessesAndMonstersVis(String seed) {
        // interface for runTest
        if (vis)
        {   jf = new JFrame();
            v = new Vis();
            jf.getContentPane().add(v);
        }
        Thread thread = null;
        if (exec != null) {
            try {
                Runtime rt = Runtime.getRuntime();
                proc = rt.exec(exec);
                os = proc.getOutputStream();
                is = proc.getInputStream();
                br = new BufferedReader(new InputStreamReader(is));
                thread = new ErrorReader(proc.getErrorStream());
                thread.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            solver = new PrincessesAndMonsters();
        }

        double s = runTest(seed);

        if (thread != null)
            try { thread.join(1000); } 
            catch (Exception e) { e.printStackTrace(); }

        System.out.println("Score = " + s);

        if (proc != null)
            try { proc.destroy(); }
            catch (Exception e) { e.printStackTrace(); }
    }
    // ---------------------------------------------------
    public static void main(String[] args) {
        args = new String[]{
                "-vis",
        };
        String seed = "1";
        vis = false;
        del = 100; // sleep time
        SZ = 15;
        TL = 20;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-seed"))
                seed = args[++i];
            if (args[i].equals("-exec"))
                exec = args[++i];
            if (args[i].equals("-vis"))
                vis = true;
            if (args[i].equals("-debug"))
                debug = true;
            if (args[i].equals("-delay"))
                del = Integer.parseInt(args[++i]);
            if (args[i].equals("-size"))
                SZ = Integer.parseInt(args[++i]);
            if (args[i].equals("-timelimit"))
                TL = Integer.parseInt(args[++i]);
        }
        for (int i = 11; i <= 30; i++) {
            seed = "" + i;
            PrincessesAndMonstersVis f = new PrincessesAndMonstersVis(seed);
        }
    }
    // ---------------------------------------------------
    void addFatalError(String message) {
        System.out.println(message);
    }
}

class ErrorReader extends Thread{
    InputStream error;
    public ErrorReader(InputStream is) {
        error = is;
    }
    public void run() {
        try {
            byte[] ch = new byte[50000];
            int read;
            while ((read = error.read(ch)) > 0)
            {   String s = new String(ch,0,read);
                System.out.print(s);
                System.out.flush();
            }
        } catch(Exception e) { }
    }
}
