package score;

import algorithms.Queue;
import features.NGramCollector;
import features.TagDictionary;
import utils.Utils;

/**
 * The <tt>ScoredTaggedPhrase</tt> class represents a tagged sentence to score
 *
 * @author Valeriya Slovikovskaya vslovik@gmail.com
 */
public class ScoredTaggedPhrase implements Comparable<ScoredTaggedPhrase>{

    protected Double score;
    protected Queue<String> tokens;
    protected Queue<String> tags;

    /**
     * Class constructor
     */
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
     * Adds next tagged token to tagged phrase
     *
     * @param line token\ttag line
     */
    public void enqueue(String line) // ToDo: find a better name
    {
        if (line.equals("START") || line.equals("STOP")) {
            tokens.enqueue(line);
            tags.enqueue(line);

            return;
        }

        String[] arr = line.split("\t");
        if (arr.length < 2) throw new IllegalArgumentException("Invalid token\\ttag line");

        tokens.enqueue(arr[0]);
        tags.enqueue(arr[1]);
    }

    /**
     * Adds phrase <em>N</em>-grams into token and tags collectors
     *
     * @param tnc Tag collector
     */
    public void add(NGramCollector tnc, TagDictionary td) // ToDo: find a better name
    {
        if (tokens.size() == 0) throw new IllegalArgumentException("Empty tokens");
        if (tags.size() == 0) throw new IllegalArgumentException("Empty tags");
        enqueue("STOP");
        tnc.addPhrase(tags);
        td.addTaggedPhrase(tokens, tags);
    }

    /**
     * Scores tagged phrase
     *
     * @param tnc Tag collector
     */
    public void score(NGramCollector tnc, TagDictionary td)
    {
        if (tokens.size() == 0) throw new IllegalArgumentException("Empty tokens");
        if (tags.size() == 0) throw new IllegalArgumentException("Empty tags");

        enqueue("STOP");

        score = scoreMarkov(tnc, td);
    }

    /**
     * Calculate "Markov model" score
     *
     * @param tnc Tag n-gram collector
     * @param td Tag dictionary
     * @return score
     */
    public double scoreMarkov(NGramCollector tnc, TagDictionary td)
    {
        return scoreFirstGram(tnc, td, tokens, tags, tnc.N - 1)*scorePhrase(tnc, td, Utils.copy(tokens), Utils.copy(tags));
    }

    /**
     * Scores tagged phrase
     *
     * @param tnc Tag n-gram collector
     * @param td Tag dictionary
     * @param tokens Token queue
     * @param tags Tag queue
     * @return score
     */
    protected double scorePhrase(NGramCollector tnc, TagDictionary td, Queue<String> tokens, Queue<String> tags)
    {
        if(tags.size() < tnc.N) throw new IllegalArgumentException(tags.toString());

        double ph_sc = 0.1 / (tags.size() - tnc.N + 1);

        double score = scoreFirstGram(tnc, td, tokens, tags, tnc.N);
        while(tags.size() > tnc.N) {
            tokens.dequeue();
            tags.dequeue();
            score *= scoreFirstGram(tnc, td, tokens, tags, tnc.N);
        }

        return ph_sc * score;
    }

    /**
     * Scores first gram
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

        Queue<String> tokensCopy = Utils.copy(tokens);

        double tdScore = 0.0;
        String token;
        Queue<String> q = new Queue<>();
        for (String tag : tags) {
            token = tokensCopy.dequeue();
            if (q.size() == n - 1) {
                tdScore = tdScore(tnc, td, token, tag);
            }
            q.enqueue(tag);

            if (q.size() == n) {
                break;
            }
        }

        return tnc.scoreGram(q, n)*tdScore;
    }

    /**
     * Prints scored phrase
     */
    public void print()
    {
        String token, tag;
        while (tokens.size() > 0) {
            token = tokens.dequeue();
            tag = tags.dequeue();
            if (!token.equals("START") && !token.equals("STOP")) {
                System.out.println(token + "\t" + tag);
            }
        }
    }

    /**
     * Gets output probability score
     *
     * @param tnc NGramCollector
     * @param td TagDictionary
     * @param token String
     * @param tag String
     * @return output probability score
     */
    protected double tdScore(NGramCollector tnc, TagDictionary td, String token, String tag)
    {
        int count = tnc.count(tag);
        if (count == 0) {
            return 0.0;
        }

        double tdCount = td.count(token, tag);
        if (tdCount == 0) {
            return td.suffixCount(token, tag) / (double) count;
        }

        return td.count(token, tag) / (double) count;
    }

}