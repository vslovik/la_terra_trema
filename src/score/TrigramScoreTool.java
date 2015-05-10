package score;

import algorithms.Queue;

public interface TrigramScoreTool {

    public int score(Queue<String> trigram);

    public int score(String token);
}