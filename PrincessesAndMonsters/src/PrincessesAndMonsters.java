import java.security.*;
import java.util.*;

public class PrincessesAndMonsters {
    static boolean debug = false;

    final int[] diay = {1, 1, -1, -1};
    final int[] diax = {1, -1, -1, 1};

    SecureRandom rand;
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
    double meanDist;
    double cyf, cxf;

    static double SIGMOID_A = 3;
    static double C_MEANDIST = 0.31;
    static double C_MIN_WID = 0.18;
    static double C_MAX_WID = 0.16;
    static double C_P_WIDMAX = 0.16;
    static double C_K_WIDMAX =-0.07;
    static long RAND_SEED = 1234;

    public String initialize(int S, int[] princesses, int[] monsters, int K) {
        try {
            rand = SecureRandom.getInstance("SHA1PRNG");
        } catch (Exception e) { return ""; }
        rand.setSeed(RAND_SEED);
//        Knight.rand = rand;
        t = -1;

        this.S = S;
        this.P = princesses.length / 2;
        this.M = monsters.length / 2;
        this.K = K;
        psy = new int[P];
        psx = new int[P];
        msy = new int[M];
        msx = new int[M];
        int centerY = 0;
        int centerX = 0;
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

        for (int i = 0; i < M; i++) {
            msy[i] = monsters[i * 2];
            msx[i] = monsters[i * 2 + 1];
        }

        cyf = (double) centerY / P;
        cxf = (double) centerX / P;
        cy = (int) cyf;
        cx = (int) cxf;
        meanDist = 0;
        for (int i = 0; i < P; i++) {
            for (int j = 0; j < M; j++) {
                meanDist += Math.abs(msy[i] - psy[i]) + Math.abs(msx[i] - psx[i]);
            }
        }
        meanDist /= P * M;

        int widMax = (int) (meanDist * C_MEANDIST + P * C_P_WIDMAX + K * C_K_WIDMAX);
        widMax = Math.max(widMax, 3);
        widMax = Math.min(widMax, (int)(K * C_MIN_WID + widMax) / 2);
        widMax = Math.max(widMax, (int)(K * C_MAX_WID + widMax) / 2);
        widMax = Math.min(widMax, Math.min(cx, S - cx) - 1);
        widMax = Math.min(widMax, Math.min(cy, S - cy) - 1);
        pLeft = Math.max(cx - widMax, 1);
        pRight = Math.min(cx + widMax, S - 2);
        pTop = Math.max(cy - widMax, 1);
        pBottom = Math.min(cy + widMax, S - 2);
        pCornerY = new int[]{pTop, pTop, pBottom, pBottom};
        pCornerX = new int[]{pLeft, pRight, pRight, pLeft};
        cornerY = new int[]{0, 0, S - 1, S - 1};
        cornerX = new int[]{0, S - 1, S - 1, 0};

        initC = -1;
        int near = S * S;
        for (int i = 0; i < 4; i++) {
            final int d = dist(cornerY[i], cornerX[i], pCornerY[i], pCornerX[i]);
            if(d < near) {
                near = d;
                initC = i;
            }
        }

        int[][] diags = {diay, diax};
        final int w = Math.min(K / 4, widMax);

        knight = new Knight[K];
        final int diag = (initC + 2) % 4;
        int group = 1;
        Loop[] loops = new Loop[group];
        for (int i = 0; i < group; i++) {
            loops[i] = new Loop(rand.nextBoolean() ? 1 : -1, rand.nextInt(S / 3) + 1, (initC + 2) % 4);
        }
        for (int i = 0; i < K; i++) {
            final int a = i % 2;
            final int wx = i / 2;
            int[] first = {pCornerY[initC], pCornerX[initC]};
            first[a] += diags[a][initC] * (widMax + w - wx % (w * 2));
            int[] second = first.clone();
            second[a^1] += diags[a^1][initC] * widMax * 2;
            Command secCom = a == 0 ? Command.SkipOne : Command.Diagonal;

            knight[i] = new Knight(i, cornerY[initC], cornerX[initC], this.rand);
            knight[i].setOrderQueue(new Order[]{
                    new Order(Command.Corner, new Pos(pCornerY[initC], pCornerX[initC]), 1),
                    new Order(Command.Diagonal, new Pos(first[0], first[1]), 1),
                    new Order(secCom, new Pos(second[0], second[1]), 1),
                    new Order(Command.Branch, new Pos(pCornerY[diag], pCornerX[diag]), 1),
                    new Order(Command.Finish, new Pos(cornerY[diag], cornerX[diag]), 1),
            });
            knight[i].setOrderSub(new Order[]{
                    new Order(Command.Diagonal, new Pos(second[0], second[1]), 1),
                    new Order(Command.Diagonal, new Pos(first[0], first[1]), 1),
                    new Order(Command.Gather, new Pos(pCornerY[initC], pCornerX[initC]), 1),
                    new Order(Command.Finish, new Pos(cornerY[initC], cornerX[initC]), 1),
            });
            knight[i].setLoop(loops[i % group].copy());
            knight[i].update();
        }
        c = new char[K];
        Arrays.fill(c, (char)('0' + initC));
        return new String(c);
    }

    int[][] knightMap;
    int state = 0;
    int changeOrder = 0;
    int deadKnight = 0;
    int escorted = 0;
    int gy, gx;

    static int RETURN_LIMIT = 6;
    static int DECIDE_DIST = 6;
    static int P_REVERSE_LIMIT = 3;
    static double K_REVERSE_LIMIT = 0.3;
    static double P_GRAV_COEF = 1.2;

    public String move(int[] status, int P, int M, int timeLeft) {
        t++;
        boolean stopAll = true;
        int captured = 0;
        int aliveKnight = 0;
        deadKnight = 0;
        int gravX = 0;
        int gravY = 0;
        knightMap = new int[S][S];
        for (int i = 0; i < K; i++) {
            Pos p = knight[i].p;
            if(status[i] > 0) {
                captured += status[i];
                gravY += p.y * P_GRAV_COEF;
                gravX += p.x * P_GRAV_COEF;
            }
            if(status[i] == -1) {
                deadKnight++;
            }
            if(status[i] >= 0) {
                knightMap[p.y][p.x]++;
                aliveKnight++;
                gravY += p.y;
                gravX += p.x;
            }
        }

        gravX = (int)(gravX / (captured * P_GRAV_COEF + aliveKnight));
        gravY = (int)(gravY / (captured * P_GRAV_COEF + aliveKnight));
        for (int i = 0; i < K; i++) {
            if(status[i] < 0) continue;
            Knight kn = knight[i];
            kn.delay = 1;
            final int distNext = kn.distNext();
            boolean skip = false;
            if(changeOrder == 0 && kn.updateCount == Command.Branch && distNext < DECIDE_DIST) {
                gy = gravY;
                gx = gravX;
                if(P - captured > P_REVERSE_LIMIT && deadKnight < K * K_REVERSE_LIMIT)
                    changeOrder = 1;
                else
                    changeOrder = -1;
            }
            if(kn.updateCount == Command.Branch) {
                if(changeOrder == -1) {
                    kn.updateCount = Command.Gather;
                    kn.setTarget(gy, gx);
                } else if(changeOrder == 1) {
                    kn.changeOrderSet();
                    kn.update();
                }
            }
            if(state == 1 && kn.stop) kn.start();
//            if(state == 2 && kn.updateCount == Command.Finish && distNext == 1) {
//                if(status[i] == 0) kn.update();
//                else escorted += status[i];
//            }
            final int nc = getNearCorner(kn);
            if(nc >= 0) kn.setTarget(cornerY[nc], cornerX[nc]);
            else if(M == 0 && P == 0
                    || timeLeft < 500
                    || kn.inLoop() && get(knightMap, kn.p) <= RETURN_LIMIT)
                kn.setTarget(cornerY[- nc - 1], cornerX[- nc - 1]);
            if(skip) c[i] = 'T';
            else c[i] = kn.move();
            if(kn.updateCount == Command.SkipOne) kn.speed = 1;
            if(state == 0 && kn.updateCount == Command.Gather && kn.distNext() == 0)
                kn.stop();
            if(kn.goal() && !kn.stop) kn.update();
            if(!kn.stop) stopAll = false;
        }
        if(stopAll)
            state++;
        else if(state == 1) state++;
        return new String(c);
    }

    int get(int[][] map, Pos p) {
        return map[p.y][p.x];
    }

    int getNearCorner(Knight kn) {
        int nearId = -1;
        int nearDist = S * S;
        for (int i = 0; i < 4; i++) {
            final int d = kn.dist(cornerY[i], cornerX[i]);
            if(d < nearDist) {
                nearDist = d;
                nearId = i;
            }
        }
        final int tl = turnLeft();
        return tl - 2 <= nearDist ? nearId : -nearId - 1;
    }

    int turnLeft() {
        return S * S * S - t;
    }

    int dist(int y1, int x1, int y2, int x2) {
        return Math.abs(y1 - y2) + Math.abs(x1 - x2);
    }

    class Loop {
        int cycle, inner, position;
        Loop(int cycle, int inner, int position) {
            this.cycle = cycle;
            this.inner = inner;
            this.position = position;
        }

        Pos next() {
            final int y = cornerY[position] + diay[position];
            final int x = cornerX[position] + diax[position];
            position = (position + cycle + 4) % 4;
            return new Pos(y, x);
        }

        Loop copy() {
            return new Loop(cycle, inner, position);
        }
    }
}

enum Command {
    Init,
    Corner,
    Diagonal,
    SkipOne,
    Branch,
    Gather,
    Finish,
}

class Order {
    Command command;
    Pos pos;
    double speed;
    Order(Command c, Pos p, double s) {
        command = c;
        pos = p;
        speed = s;
    }
}

class Knight {
    SecureRandom rand;
    int i;
    Pos p, t, d, pre;
    int orderId;
    List<Order> orderQueue;
    List<Order> subOrders;
    PrincessesAndMonsters.Loop loop;
    Command updateCount = Command.Init;
    double speed = 1;
    double delay = 1;
    boolean stop = false;

    Knight(int i, int y, int x, SecureRandom rand) {
        p = new Pos(y, x);
        t = new Pos(0, 0);
        pre = new Pos(y, x);
        this.i = i;
        orderId = 0;
        this.rand = rand;
    }

    void stop() {
        stop = true;
    }

    void start() {
        stop = false;
    }

    void setTarget(int y, int x) {
        t = new Pos(y, x);
        pre.set(0, 0);
    }

    void setLoop(PrincessesAndMonsters.Loop loop) {
        this.loop = loop;
    }

    void setOrderQueue(Order[] orders) {
        orderQueue = new ArrayList<>();
        orderQueue.addAll(Arrays.asList(orders));
    }

    void setOrderSub(Order[] orders) {
        subOrders = new ArrayList<>();
        subOrders.addAll(Arrays.asList(orders));
    }

    void changeOrderSet() {
        orderQueue = subOrders;
        orderId = 0;
    }

    boolean inLoop() {
        return orderId >= orderQueue.size();
    }

    char move() {
        if(stop || rand.nextDouble() > speed * delay) return 'T';
        return move(t.y, t.x);
    }

    boolean goal() {
        return p.eq(t);
    }

    void update() {
        if(orderId >= orderQueue.size()) t = loop.next();
        else {
            Order o = orderQueue.get(orderId++);
            t = o.pos;
            speed = o.speed;
            updateCount = o.command;
            if(updateCount == Command.SkipOne) speed = 0;
            delay = 1;
            pre.set(0, 0);
        }
        d = new Pos(Math.abs(p.y - t.y), Math.abs(p.x - t.x));
    }

    char move(int ty, int tx) {
        final int my = ty - p.y;
        final int mx = tx - p.x;
        pre.set(p.y, p.x);
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

    int dist(int y, int x) {
        return p.dist(y, x);
    }

    int distNext() {
        return p.dist(t);
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

    int dist(int y, int x) {
        return Math.abs(this.y - y) + Math.abs(this.x - x);
    }

    int dist(Pos p) {
        return Math.abs(p.y - y) + Math.abs(p.x - x);
    }

    boolean eq(Pos p) {
        return y == p.y && x == p.x;
    }
}
