import java.util.ArrayList;
import java.util.List;

public class ParameterOptimizer {
    public static void main(String[] args) {
        ParameterOptimizer opt = new ParameterOptimizer();
        opt.optimizer();
    }

    final int EVAL_SIZE = 50;
    double evaluate(int idx) {
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
        System.out.printf("%3d score: %f, raw: %f\n",
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
            double res = evaluate(i);
            if(res > max) {
                max = res;
                maxId = i;
            }
        }
        System.out.println(max + " " + maxId);
    }

}
