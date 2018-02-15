import java.io.*;
import java.lang.reflect.Array;
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
            pRight = Math.max(pRight, psy[i]);
        }

        final int inc = INC;
        pLeft = Math.max(pLeft - inc, 0);
        pRight = Math.min(pRight + inc, S - 1);
        pTop = Math.max(pTop - inc, 0);
        pBottom = Math.min(pBottom + inc, S - 1);
        pCornerY = new int[]{pTop, pTop, pBottom, pBottom};
        pCornerX = new int[]{pLeft, pRight, pRight, pLeft};
        cornerY = new int[]{0, 0, S, S};
        cornerX = new int[]{0, S, S, 0};
        cy = (int)(centerY / P);
        cx = (int)(centerX / P);

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
        try {
           rand = SecureRandom.getInstance("SHA1PRNG");
        } catch (Exception e) { return ""; }
        rand.setSeed(1234);
        t = -1;
        // all knights start in top left corner
        knight = new Knight[K];
        int groups = K / GROUP_SIZE;
        Pos[] targets = new Pos[groups];
        for (int i = 0; i < groups; i++) {
            targets[i] = new Pos(rand.nextInt(pBottom - pTop) + pTop,
                    rand.nextInt(pRight - pLeft) + pLeft);
        }
        for (int i = 0; i < K; i++) {
            knight[i] = new Knight(i, cornerY[initC], cornerX[initC]);
            knight[i].setTargetQueue(new Pos[]{
                    new Pos(pCornerY[initC], pCornerX[initC]),
                    targets[i % groups],
                    new Pos(pCornerY[(initC + 2) % 4], pCornerX[(initC + 2) % 4]),
                    new Pos(cornerY[(initC + 2) % 4], cornerX[(initC + 2) % 4]),
            });
            knight[i].updateTarget();
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
                knight[i].updateTarget();
            }
        }
        return new String(c);
    }
    int dist(int y1, int x1, int y2, int x2) {
        return Math.abs(y1 - y2) + Math.abs(x1 - x2);
    }
}

class Knight {
    int i;
    Pos p, t;
    Queue<Pos> targets;
    int updateCount = 0;

    Knight(int i, int y, int x) {
        p = new Pos(y, x);
        t = new Pos(0, 0);
        this.i = i;
    }

    void setTarget(int y, int x) {
        t.set(y, x);
    }

    void setTargetQueue(Pos[] ts) {
        targets = new ArrayDeque<>();
        targets.addAll(Arrays.asList(ts));
    }

    char move() {
        return move(t.y, t.x);
    }

    boolean goal() {
        return p.eq(t);
    }

    void updateTarget() {
        if(targets.isEmpty()) return;
        t = targets.poll();
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
