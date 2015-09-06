package score;

import algorithms.Queue;
import features.NGramCollector;

public class ScoredTaggedPhrase implements Comparable<ScoredTaggedPhrase> {

    protected Double score;
    protected Queue<String> tokens;
    protected Queue<String> tags;


    public ScoredTaggedPhrase()
    {
        this.tokens = new Queue<>();
        tokens.enqueue("START");

        this.tags = new Queue<>();
        tags.enqueue("START");

        this.score = 0.0;
    }

    /**
     * @param that ScoredTaggedPhrase
     * @return score
     */
    public int compareTo(ScoredTaggedPhrase that)
    {
        if (that == null) {
            throw new NullPointerException();
        }
        return score.compareTo(that.score);
    }

    /**
     * @param line token\ttag line
     */
    public void enqueue(String line)
    {
        if (line.equals("START") || line.equals("STOP")) {
            tokens.enqueue(line);
            tags.enqueue(line);

            return;
        }

        String[] arr = line.split("\t");
        if (arr.length < 2) throw new IllegalArgumentException("Invalid token\ttag line");

        tokens.enqueue(arr[0]);
        tags.enqueue(arr[1]);
    }

    /**
     * Add
     *
     * @param nc Token collector
     * @param tnc Token collector
     */
    public void add(NGramCollector nc,  NGramCollector tnc)
    {
        if (tokens.size() == 0) throw new IllegalArgumentException("Empty tokens");
        if (tags.size() == 0) throw new IllegalArgumentException("Empty tags");
        enqueue("STOP");
        nc.addPhrase(tokens);
        tnc.addPhrase(tags);
    }

    /**
     * Score
     *
     * @param nc Token collector
     * @param tnc Token collector
     */
    public void score(NGramCollector nc,  NGramCollector tnc)
    {
        if (tokens.size() == 0) throw new IllegalArgumentException("Empty tokens");
        if (tags.size() == 0) throw new IllegalArgumentException("Empty tags");

        tokens.enqueue("STOP");
        score += score(nc, tokens);

        tags.enqueue("STOP");
        score += score(tnc, tags);
    }

    /**
     * Calculate phrase score
     *
     * @param nc Token collector
     * @param tokens Phrase tokens
     * @return score
     */
    public double score(NGramCollector nc, Queue<String> tokens)
    {
        Queue<String> copy = new Queue<>();
        for(String token: tokens) {
            copy.enqueue(token);
        }
        return scoreFirstGram(nc, tokens, nc.N - 1) + scorePhrase(nc, copy);
    }

    /**
     * Count N-Gram score
     *
     * @param nc Token collector
     * @param tokens Token queue
     * @return score
     */
    protected double scorePhrase(NGramCollector nc, Queue<String> tokens)
    {
        if(tokens.size() < nc.N) throw new IllegalArgumentException();

        double ph_sc = nc.scorePhaseLength(tokens);

        double score = scoreFirstGram(nc, tokens, nc.N);
        while(tokens.size() > nc.N) {
            tokens.dequeue();
            score += scoreFirstGram(nc, tokens, nc.N);
        }

        return ph_sc * score / (tokens.size() - nc.N + 1);
    }

    /**
     * Score first gram
     *
     * @param nc Token collector
     * @param tokens Token queue
     * @param n nGram rang
     * @return score
     */
    protected double scoreFirstGram(NGramCollector nc, Queue<String> tokens, int n) {
        if (tokens.size() < n) throw new IllegalArgumentException();

        Queue<String> q = new Queue<>();
        for (String token : tokens) {
            q.enqueue(token);
            if (q.size() == n) {
                break;
            }
        }

        return nc.scoreGram(q, n);
    }
}
