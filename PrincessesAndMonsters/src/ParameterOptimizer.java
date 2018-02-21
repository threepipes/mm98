import java.io.*;
import java.util.*;

public class ParameterOptimizer {
    public static void main(String[] args) throws IOException {
//        evalCSV("best.csv");
        simulatedAnnealing();
    }

    static void simulatedAnnealing() throws IOException {
        SimulatedAnnealing sa = new SimulatedAnnealing();
        TreeSet<Ev> tree = sa.sa();
        EVAL_SIZE = 5000;
        for(Ev ev: tree) {
            ev.evaluate();
            System.out.println(ev);
        }
    }

    static int EVAL_SIZE = 1000;
    static double evaluate(int idx, boolean output) {
        PrincessesAndMonstersVis.TL = 2000;
        double score = 0;
        double raw = 0;
        int start = 2001, end = start + EVAL_SIZE - 1;
        List<Generator> list = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            list.add(new Generator(i + ""));
        }
        list.parallelStream().forEach(Generator::gen);
        for (Generator g: list) {
            Result res = g.f.result;
            score += res.score / res.P;
            raw += res.score;
        }
        if(output) System.out.printf("%3d score: %f, raw: %f\n",
                idx, score, raw);
        return score;
    }

    void setParam(double... param) {
        PrincessesAndMonsters.SIGMOID_A = param[0];
    }

    void optimizer() {
        PrincessesAndMonstersVis.TL = 20;
        PrincessesAndMonsters.RAND_SEED = 2233;
        int maxId = -1;
        double max = Integer.MIN_VALUE;
        for (int i = 30; i <= 45; i++) {
            double a = i / 10.0;
            setParam(a);
            double res = evaluate(i, true);
            if(res > max) {
                max = res;
                maxId = i;
            }
        }
        System.out.println(max + " " + maxId);
    }

    static void evalCSV(String filepath) throws IOException {
        EVAL_SIZE = 5000;
        Scanner in = new Scanner(new File(filepath));

        while(in.hasNextLine()) {
            String line = in.nextLine();
            int[] params = new int[Ev.PARAM_SIZE];
            String[] data = line.split(",");
            for (int i = 0; i < params.length; i++) {
                params[i] = Integer.parseInt(data[i]);
            }
            Ev ev = new Ev(params);
            ev.evaluate();
            System.out.println(ev);
        }
    }
}


class SimulatedAnnealing {
    Ev initialEv = new Ev(Ev.PARAM_INI.clone());
    final double TEMPER = 0.01;
    final double TOP = 10;

    TreeSet<Ev> sa() throws IOException {
        TreeSet<Ev> tree = new TreeSet<>();
        Ev ev = initialEv;
        final int WARMING = 5;
        System.out.println("Start evaluate v1.0.");
        long time = System.currentTimeMillis();
        for (int i = 0; i < WARMING; i++) {
            ev.evaluate();
        }
        double score = ev.score;
        time = (System.currentTimeMillis() - time) / WARMING;
        tree.add(ev);
        System.out.println("Initial score: " + score);
        Ev best = ev;
        double bestScore = score;
        final long HOUR_10 = 8 * 3600 * 1000;
        final int MAX_ITER = (int)(HOUR_10 / time);
        System.out.println("Iter Num: " + MAX_ITER);
        Random rand = new Random(0);
        PrintWriter pw = new PrintWriter(
                new BufferedWriter(new FileWriter("log.txt")));
        pw.println(ev.csv());
        for(int i = 0; i < MAX_ITER; i++) {
            double degree = ((double)MAX_ITER - i) / MAX_ITER;
            Ev next = ev.generateNext(degree);
            double nextScore = next.evaluate();
            tree.add(next);
            if(tree.size() > TOP) tree.pollLast();
            pw.println(next.csv());
            if((i + 1) % 10 == 0) pw.flush();
            String output = String.format("Iter %d/%d: score=%f ", i, MAX_ITER, nextScore);
            if(nextScore > bestScore) {
                best = next;
                bestScore = nextScore;
                output += "Update! ";
            }
            double temperature = temper((double)i / MAX_ITER);
            double prob = prob(score, nextScore, temperature);
            output += String.format("T:%f P:%f ", temperature, prob);
            if(rand.nextDouble() <= prob) {
                ev = next;
                score = nextScore;
                output += "swap ";
            }
            output += String.format("Now: %s, Best: %s", ev, best);
            System.out.println(output);
        }
        pw.close();
        System.out.printf("Best: %s\n", best);
        return tree;
    }

    double temper(double r) {
        return Math.pow(TEMPER, r);
    }

    double prob(double curScore, double nextScore, double temper) {
        if(curScore <= nextScore) return 1;
        else return Math.pow(Math.E, (nextScore - curScore) / (temper * 1000));
    }
}

class Ev implements Comparable<Ev> {
    static Random rand = new Random(0);
    double score;
    int[] params;
//    public static final int[] PARAM_INI = {15, 40, 10,  4,  4,  0,  0, 30};
    public static final int[] PARAM_INI =   {21,  8,  6,  5,  3, 12, 16, 23};
//    public static final int[] PARAM_INI = {48, 39, 20,  2,  2, 47, 13,  27};
//    public static final int[] PARAM_INI = {15, 25,  4,    2,  1, 30};
    public static final int[] PARAM_MAX =   {50, 50, 50, 10, 10,200, 30, 60};
    public static final int[] PARAM_BASE=   {10, 10, 10,  1,  0,  0,  0,-30};
    public static final int PARAM_SIZE = 8;
    Ev(int[] params) {
        this.params = params;
    }

    Ev generateNext(double degree) {
        int i = rand.nextInt(PARAM_SIZE);
        int[] next = params.clone();
        next[i] += (int)Math.max(1, rand.nextDouble() * degree * (PARAM_MAX[i])) * (rand.nextBoolean() ? -1 : 1);
        next[i] = (next[i] + PARAM_MAX[i]) % PARAM_MAX[i];
        return new Ev(next);
    }

    double evaluate() {
        PrincessesAndMonsters.C_MEANDIST = (double) (params[0] + PARAM_BASE[0]) / 100;
        PrincessesAndMonsters.C_MIN_WID = (double) (params[1] + PARAM_BASE[1]) / 100;
        PrincessesAndMonsters.C_MAX_WID = (double) (params[2] + PARAM_BASE[2]) / 100;
        PrincessesAndMonsters.DECIDE_DIST = params[3] + PARAM_BASE[3];
        PrincessesAndMonsters.P_REVERSE_LIMIT = params[4] + PARAM_BASE[4];
        PrincessesAndMonsters.P_GRAV_COEF = (double) (params[5] + PARAM_BASE[5]) / 10;
        PrincessesAndMonsters.C_P_WIDMAX = (double) (params[6] + PARAM_BASE[6]) / 100;
        PrincessesAndMonsters.C_K_WIDMAX = (double) (params[7] + PARAM_BASE[7]) / 100;
//        PrincessesAndMonsters.RETURN_LIMIT = params[3] + PARAM_BASE[3];
        score = ParameterOptimizer.evaluate(0, false);
        return score;
    }

    @Override
    public int compareTo(Ev o) {
        if(score != o.score) return Double.compare(o.score, score);
        return rand.nextInt();
    }

    public String csv() {
        String s = "";
        for(int i = 0; i < params.length; i++) s += params[i] + ",";
        s += score;
        return s;
    }

    @Override
    public String toString() {
        return Arrays.toString(params) + ":" + score;
    }
}
