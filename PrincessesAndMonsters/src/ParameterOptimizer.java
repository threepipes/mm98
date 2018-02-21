import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class ParameterOptimizer {
    public static void main(String[] args) throws IOException {
        SimulatedAnnealing sa = new SimulatedAnnealing();
        sa.sa();
    }

    static final int EVAL_SIZE = 100;
    static double evaluate(int idx, boolean output) {
        PrincessesAndMonstersVis.TL = 2000;
        double score = 0;
        double raw = 0;
        int start = 1001, end = start + EVAL_SIZE - 1;
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

}


class SimulatedAnnealing {
    Ev initialEv = new Ev(Ev.PARAM_INI.clone());
    final double TEMPER = 0.01;

    void sa() throws IOException {
        Ev Ev = initialEv;
        System.out.println("Start evaluate v1.0.");
        long time = System.currentTimeMillis();
        double score = Ev.evaluate();
        time = System.currentTimeMillis() - time;
        System.out.println("Initial score: " + score);
        Ev best = Ev;
        double bestScore = score;
        final long HOUR_10 = 10 * 3600 * 1000;
        final int MAX_ITER = (int)(HOUR_10 / time);
        System.out.println("Iter Num: " + MAX_ITER);
        Random rand = new Random(0);
        PrintWriter pw = new PrintWriter(
                new BufferedWriter(new FileWriter("log.txt")));
        pw.println(Ev.csv());
        for(int i = 0; i < MAX_ITER; i++) {
            double degree = ((double)MAX_ITER - i) / MAX_ITER;
            Ev next = Ev.generateNext(degree);
            double nextScore = next.evaluate();
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
                Ev = next;
                score = nextScore;
                output += "swap ";
            }
            output += String.format("Now: %s, Best: %s", Ev, best);
            System.out.println(output);
        }
        pw.close();
        System.out.printf("Best: %s\n", best);
    }

    double temper(double r) {
        return Math.pow(TEMPER, r);
    }

    double prob(double curScore, double nextScore, double temper) {
        if(curScore <= nextScore) return 1;
        else return Math.pow(Math.E, (nextScore - curScore) * 100 / temper);
    }
}

class Ev implements Comparable<Ev> {
    static Random rand = new Random(0);
    double score;
    int[] params;
    public static final int[] PARAM_INI = {15, 40, 10,  5,  4,  4, 20};
    public static final int[] PARAM_MAX = {50, 50, 50, 20, 10, 10, 50};
    public static final int[] PARAM_BASE= {10, 10, 10,  0,  1,  0,  0};
    public static final int PARAM_SIZE = 7;
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
        PrincessesAndMonsters.RETURN_LIMIT = params[3] + PARAM_BASE[3];
        PrincessesAndMonsters.DECIDE_DIST = params[4] + PARAM_BASE[4];
        PrincessesAndMonsters.P_REVERSE_LIMIT = params[5] + PARAM_BASE[5];
        PrincessesAndMonsters.K_REVERSE_LIMIT = (double) (params[6] + PARAM_BASE[6]) / 100;
        return ParameterOptimizer.evaluate(0, false);
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
