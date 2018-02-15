import java.io.*;
import java.security.*;
import java.util.*;

public class PrincessesAndMonsters {
    SecureRandom r1;
    String dir = "NEWS";
    char[] c;
    int t;
    public String initialize(int S, int[] princesses, int[] monsters, int K) {
        try {
           r1 = SecureRandom.getInstance("SHA1PRNG");
        } catch (Exception e) { return ""; }
        r1.setSeed(1234);
        t = -1;
        // all knights start in top left corner
        c = new char[K];
        Arrays.fill(c, '0');
        return new String(c);
    }
    public String move(int[] status, int P, int M, int timeLeft) {
        t++;
        Arrays.fill(c, t < 3 ? 'E' : t < 8 ? 'S' : dir.charAt(r1.nextInt(4)));
        return new String(c);
    }
    // -------8<------- end of solution submitted to the website -------8<-------
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
}

