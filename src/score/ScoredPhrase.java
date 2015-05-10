package score;

import algorithms.Queue;
import com.sun.istack.internal.NotNull;

public class ScoredPhrase implements Comparable<ScoredPhrase> {

    protected Integer score;
    protected Queue<String> lines;

    public ScoredPhrase(Queue<String> lines, int score)
    {
        this.lines = lines;
        this.score = score;
    }

    public int compareTo(ScoredPhrase that)
    {
        if (that == null) {
            throw new NullPointerException();
        }
        return score.compareTo(that.score);
    }
}
