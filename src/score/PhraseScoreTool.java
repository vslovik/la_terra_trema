package score;

import algorithms.Queue;

public interface PhraseScoreTool {

    /**
     * Calculate phrase score
     *
     * @param tokens Phrase tokens
     * @return score
     */
    public double score(Queue<String> tokens);
}