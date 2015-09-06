package score;

import algorithms.Queue;
import features.NGramCollector;
import features.TagDictionary;

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
    public void add(NGramCollector nc,  NGramCollector tnc, TagDictionary td)
    {
        if (tokens.size() == 0) throw new IllegalArgumentException("Empty tokens");
        if (tags.size() == 0) throw new IllegalArgumentException("Empty tags");
        enqueue("STOP");
        nc.addPhrase(tokens);
        tnc.addPhrase(tags);
        td.addTaggedPhrase(tokens, tags);
    }

    /**
     * Score
     *
     * @param nc Token collector
     * @param tnc Token collector
     */
    public void score(NGramCollector nc,  NGramCollector tnc, TagDictionary td)
    {
        if (tokens.size() == 0) throw new IllegalArgumentException("Empty tokens");
        if (tags.size() == 0) throw new IllegalArgumentException("Empty tags");

        enqueue("STOP");

        score += score(nc, tokens);
        score += score(tnc, tags);
        score += scoreMarkov(tnc, td);
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
     * Score Markov
     *
     * @param tnc Tag n-gram collector
     * @param td Tag dictionary
     * @return score
     */
    public double scoreMarkov(NGramCollector tnc, TagDictionary td)
    {
        Queue<String> tokensCopy = new Queue<>();
        Queue<String> tagsCopy = new Queue<>();
        for(String token: tokens) {
            tokensCopy.enqueue(token);
        }
        for(String tag: tags) {
            tagsCopy.enqueue(tag);
        }
        return scoreFirstGram(tnc, td, tokens, tags, tnc.N - 1) + scorePhrase(tnc, td, tokensCopy, tagsCopy);
    }

    /**
     * Count N-Gram score
     *
     * @param nc Token n-gram collector
     * @param tokens Token queue
     * @return score
     */
    protected double scorePhrase(NGramCollector nc, Queue<String> tokens)
    {
        if(tokens.size() < nc.N) throw new IllegalArgumentException();

        double ph_sc = nc.scorePhaseLength(tokens) / (tokens.size() - nc.N + 1);

        double score = scoreFirstGram(nc, tokens, nc.N);
        while(tokens.size() > nc.N) {
            tokens.dequeue();
            score += scoreFirstGram(nc, tokens, nc.N);
        }

        return ph_sc * score;
    }

    /**
     * Count N-Gram score
     *
     * @param tnc Tag n-gram collector
     * @param td Tag dictionary
     * @param tokens Token queue
     * @param tags Tag queue
     * @return score
     */
    protected double scorePhrase(NGramCollector tnc, TagDictionary td, Queue<String> tokens, Queue<String> tags)
    {
        if(tags.size() < tnc.N) throw new IllegalArgumentException();

        double ph_sc = tnc.scorePhaseLength(tokens) / (tags.size() - tnc.N + 1);

        double score = scoreFirstGram(tnc, td, tokens, tags, tnc.N);
        while(tags.size() > tnc.N) {
            tokens.dequeue();
            tags.dequeue();
            score += scoreFirstGram(tnc, td, tokens, tags, tnc.N);
        }

        return ph_sc * score;
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

    /**
     * Score first gram
     *
     * @param tnc Tag collector
     * @param td Tag dictionary
     * @param tokens Token queue
     * @param tags Tag queue
     * @param n nGram rang
     * @return score
     */
    protected double scoreFirstGram(NGramCollector tnc, TagDictionary td, Queue<String> tokens, Queue<String> tags, int n) {
        if (tokens.size() < n) throw new IllegalArgumentException();

        Queue<String> tokensCopy = new Queue<>();
        for(String token: tokens) {
            tokensCopy.enqueue(token);
        }

        double tdScore = 0.0;
        String token;
        Queue<String> q = new Queue<>();
        for (String tag : tags) {
            token = tokensCopy.dequeue();
            if (q.size() == n - 1) {
                int count = tnc.count(tag);
                if (count == 0) {
                    return 0.0;
                }
                tdScore = (double) td.count(token, tag) / (double) count;
            }
            q.enqueue(tag);

            if (q.size() == n) {
                break;
            }
        }

        return tnc.scoreGram(q, n)*tdScore;
    }
}
