import java.io.*;
import java.security.*;
import java.util.*;

public class PrincessesAndMonsters {

    // ---CUT START---
    public static void main(String[] args) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            PrincessesAndMonsters pam = new PrincessesAndMonsters();

            int S = Integer.parseInt(br.readLine());
            int P = Integer.parseInt(br.readLine());
            int[] princesses = new int[P];
            for (int i = 0; i < P; ++i) {
                princesses[i] = Integer.parseInt(br.readLine());
            }
            int M = Integer.parseInt(br.readLine());
            int[] monsters = new int[M];
            for (int i = 0; i < M; ++i) {
                monsters[i] = Integer.parseInt(br.readLine());
            }
            int K = Integer.parseInt(br.readLine());

            String retInit = pam.initialize(S, princesses, monsters, K);
            System.out.println(retInit);
            System.out.flush();

            for (int st = 0; ; ++st) {
                int nK = Integer.parseInt(br.readLine());
                int[] status = new int[nK];
                for (int i = 0; i < nK; ++i) {
                    status[i] = Integer.parseInt(br.readLine());
                }

                int nP = Integer.parseInt(br.readLine());
                int nM = Integer.parseInt(br.readLine());
                int timeLeft = Integer.parseInt(br.readLine());

                String ret = pam.move(status, nP, nM, timeLeft);
                System.out.println(ret);
                System.out.flush();
            }
        }
        catch (Exception e) {}
    }
    // ---CUT END---

    SecureRandom rand;
    String dir = "NEWS";
    char[] c;
    int t;
    int S, P, M, K;
    int[] psx, psy;
    int[] msx, msy;
    Knight[] knight;
    int cy, cx;
    int pLeft, pRight, pTop, pBottom;
    int[] pCornerY, pCornerX;
    int[] cornerY, cornerX;
    int initC;

    final int INC = 2;
    final int GROUP_SIZE = 4;

    public String initialize(int S, int[] princesses, int[] monsters, int K) {
        try {
            rand = SecureRandom.getInstance("SHA1PRNG");
        } catch (Exception e) { return ""; }
        rand.setSeed(1234);
        Knight.rand = rand;
        t = -1;

        this.S = S;
        this.P = princesses.length / 2;
        this.M = monsters.length / 2;
        this.K = K;
        psy = new int[P];
        psx = new int[P];
        msy = new int[M];
        msx = new int[M];
        long centerY = 0;
        long centerX = 0;
        pLeft = S;
        pTop = S;
        for (int i = 0; i < P; i++) {
            psy[i] = princesses[i * 2];
            psx[i] = princesses[i * 2 + 1];
            centerY += psy[i];
            centerX += psx[i];
            pTop = Math.min(pTop, psy[i]);
            pLeft = Math.min(pLeft, psx[i]);
            pBottom = Math.max(pBottom, psy[i]);
            pRight = Math.max(pRight, psx[i]);
        }
        cy = (int)(centerY / P);
        cx = (int)(centerX / P);

        int widMax = Math.max(pBottom - pTop, pRight - pLeft);
        widMax = Math.max(widMax, K / 3) / 2;
//        widMax = K / 2;
        pLeft = Math.max(cx - widMax, 1);
        pRight = Math.min(cx + widMax, S - 2);
        pTop = Math.max(cy - widMax, 1);
        pBottom = Math.min(cy + widMax, S - 2);
//        pLeft = Math.max(pLeft - inc, 0);
//        pRight = Math.min(pRight + inc, S - 1);
//        pTop = Math.max(pTop - inc, 0);
//        pBottom = Math.min(pBottom + inc, S - 1);
        pCornerY = new int[]{pTop, pTop, pBottom, pBottom};
        pCornerX = new int[]{pLeft, pRight, pRight, pLeft};
        cornerY = new int[]{0, 0, S, S};
        cornerX = new int[]{0, S, S, 0};

        initC = -1;
        int near = S * S;
        for (int i = 0; i < 4; i++) {
            final int d = dist(cornerY[i], cornerX[i], pCornerY[i], pCornerX[i]);
            if(d < near) {
                near = d;
                initC = i;
            }
        }

        for (int i = 0; i < M; i++) {
            msy[i] = monsters[i * 2];
            msx[i] = monsters[i * 2 + 1];
        }
        knight = new Knight[K];
        final int baseY = pCornerY[(initC + 1) % 4];
        final int baseX = pCornerX[(initC + 1) % 4];
        for (int i = 0; i < K; i++) {
            final double vy = - baseY + pCornerY[(initC + 3) % 4];
            final double vx = - baseX + pCornerX[(initC + 3) % 4];
            final double t = sigmoid(3, rand.nextDouble() * 2 - 1);
            Pos tPos = new Pos(
                    (int)(baseY + vy * t),
                    (int)(baseX + vx * t)
            );
            knight[i] = new Knight(i, cornerY[initC], cornerX[initC]);
            knight[i].setTargetQueue(new Pos[]{
                    new Pos(pCornerY[initC], pCornerX[initC]),
                    tPos,
                    new Pos(pCornerY[(initC + 2) % 4], pCornerX[(initC + 2) % 4]),
                    new Pos(cornerY[(initC + 2) % 4], cornerX[(initC + 2) % 4]),
            });
            knight[i].setSpeedQueue(new double[]{
                    1, 0.5, 1, 1,
            });
            knight[i].update();
        }
        ay = 0;
        ax = 0;
        c = new char[K];
        Arrays.fill(c, (char)('0' + initC));
        return new String(c);
    }
    int ax, ay;
    public String move(int[] status, int P, int M, int timeLeft) {
        t++;
        for (int i = 0; i < K; i++) {
            c[i] = knight[i].move();
            if(knight[i].goal()) {
                knight[i].update();
            }
        }
        return new String(c);
    }
    int dist(int y1, int x1, int y2, int x2) {
        return Math.abs(y1 - y2) + Math.abs(x1 - x2);
    }
    double sigmoid(double a, double x) {
        return 1 / (1 + Math.pow(Math.E, -a * x));
    }
}

class Knight {
    static SecureRandom rand;
    int i;
    Pos p, t;
    Queue<Pos> targets;
    Queue<Double> speedQueue;
    int updateCount = 0;
    double speed = 1;
    boolean stop = false;

    Knight(int i, int y, int x) {
        p = new Pos(y, x);
        t = new Pos(0, 0);
        this.i = i;
    }

    void stop() {
        stop = true;
    }

    void start() {
        stop = false;
    }

    void setTarget(int y, int x) {
        t.set(y, x);
    }

    void setSpeedQueue(double[] ss) {
        speedQueue = new ArrayDeque<>();
        for(double s: ss) speedQueue.add(s);
    }

    void setTargetQueue(Pos[] ts) {
        targets = new ArrayDeque<>();
        targets.addAll(Arrays.asList(ts));
    }

    char move() {
        if(stop || rand.nextDouble() > speed) return 'T';
        return move(t.y, t.x);
    }

    boolean goal() {
        return p.eq(t);
    }

    void update() {
        if(targets.isEmpty()) return;
        t = targets.poll();
        speed = speedQueue.poll();
        updateCount++;
    }

    char move(int ty, int tx) {
        final int my = ty - p.y;
        final int mx = tx - p.x;
        if(Math.abs(my) > Math.abs(mx)) {
            if(my < 0) {
                p.y--;
                return 'N';
            } else {
                p.y++;
                return 'S';
            }
        } else {
            if(mx < 0) {
                p.x--;
                return 'W';
            } else if(mx > 0) {
                p.x++;
                return 'E';
            } else return 'T';
        }
    }
}

class Pos {
    int y, x;
    Pos(int y, int x) {
        set(y, x);
    }

    void set(int y, int x) {
        this.y = y;
        this.x = x;
    }

    boolean eq(Pos p) {
        return y == p.y && x == p.x;
    }
}
