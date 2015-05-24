package score;

import algorithms.Queue;

public class ScoredTaggedPhrase implements Comparable<ScoredTaggedPhrase> {

    protected Double score;
    protected Queue<String> tokens;
    protected Queue<String> tags;

    public ScoredTaggedPhrase()
    {
        this.tokens = new Queue<String>();
        this.tags = new Queue<String>();
        this.score = 0.0;
    }

    public ScoredTaggedPhrase(Queue<String> tokens, Queue<String> tags, double score)
    {
        this.tokens = tokens;
        this.tags = tags;
        this.score = score;
    }

    public int compareTo(ScoredTaggedPhrase that)
    {
        if (that == null) {
            throw new NullPointerException();
        }
        return score.compareTo(that.score);
    }
}
