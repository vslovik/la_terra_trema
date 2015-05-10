package score;

public class ScoredToken implements Comparable<ScoredToken> {

    protected Integer score;
    protected String token;

    public ScoredToken(String token, int score)
    {
        this.token = token;
        this.score = score;
    }

    public int getScore()
    {
        return score;
    }

    public String getToken()
    {
        return token;
    }

    public int compareTo(ScoredToken that)
    {
        if (that == null) {
            throw new NullPointerException();
        }
        return score.compareTo(that.score);
    }
}
