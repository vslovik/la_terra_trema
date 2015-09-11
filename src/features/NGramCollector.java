package features;

import algorithms.ST;
import algorithms.Queue;
import algorithms.TST;
import utils.Utils;

/**
 * The <tt>NGramCollector</tt> class
 *
 * @author Valeriya Slovikovskaya vslovik@gmail.com
 */
public class NGramCollector {

    public int N = 3;
    protected TST<Node> tokenIndex;
    protected ST<Integer, String> tokenIndexR;
    protected ST<Integer, Integer> phraseLengths;

    protected TST<Node> suffixIndex;
    protected ST<Integer, String> suffixIndexR;
    protected int suffixThreshold = 5;
    protected int maxSuffixLength = 4;

    protected double[] lambda = new double[N];

    class Node
    {
        protected int freq;
        protected int index;
        protected ST<Integer, Node> neighbors = new ST<>();
    }

    /**
     * N-gram collector constructor
     */
    public NGramCollector() {
        tokenIndex = new TST<>();
        tokenIndexR = new ST<>();
        suffixIndex = new TST<>();
        suffixIndexR = new ST<>();
        phraseLengths = new ST<>();
        for(int i = 0; i < N; i++) {
            lambda[i] = 0.0;
        }
    }

    /**
     * Smooth trigram counts
     */
    public void smoothTrigramCounts() {
        for (String token : tokenIndex.keys()) {
            Node node = tokenIndex.get(token);
            if (N == 3) { // Implemented only for N = 3
                smoothingLambda(node, null, node);
            }
        }

        normalizeLambdas();
    }

    /**
     * Smoothes lambda
     *
     * @param first First node
     * @param prev Second node
     * @param node Last node
     */
    protected void smoothingLambda(Node first, Node prev, Node node)
    {
        Node n;
        if (node.neighbors.size() == 0 && node.freq > 0 && prev != null) {

            double uni = (double) (tokenIndex.get(tokenIndexR.get(node.index)).freq - 1) / (double) (tokenIndex.size() - 1);
            double bi  = (double) (prev.freq - 1) / (double) (tokenIndex.get(tokenIndexR.get(prev.index)).freq - 1);
            double tri = 0.0;
            for(int index: first.neighbors.keys()) {
                if (prev.index == index) {
                    tri = (double) (node.freq - 1) / (double) (first.neighbors.get(index).freq - 1);
                }
            }

            if (uni > bi && uni > tri) {
                lambda[0] += node.freq;
            } else if (bi > uni && bi > tri) {
                lambda[1] += node.freq;
            } else if (tri > uni && tri > bi) {
                lambda[2] += node.freq;
            }

        } else {
            for(int index: node.neighbors.keys()) {
                n = node.neighbors.get(index);
                smoothingLambda(first, node, n);
            }
        }
    }

    /**
     * Gets smoothed trigram score
     *
     * @param index Index
     * @param indexR Reverse index
     * @param first First node
     * @param prev Prev node
     * @param node Last node
     * @return score
     */
    protected double getSmoothedTrigramScore(TST<Node> index, ST<Integer, String> indexR, Node first, Node prev, Node node) {
        double uni = (double) index.get(indexR.get(node.index)).freq / (double) index.size();
        double bi  = (double) prev.freq / (double) index.get(indexR.get(prev.index)).freq;

        double tri = 0.0;
            for (int i : first.neighbors.keys()) {
                if (prev.index == i) {
                    tri = (double) node.freq / (double) first.neighbors.get(i).freq;
                }
            }

        return lambda[0]*uni + lambda[1]*bi + lambda[2]*tri;
    }

    /**
     * Gets smoothed trigram score
     *
     * @param index Index
     * @param indexR Reverse index
     * @param prev Prev node
     * @param token Last trigram token
     * @return score
     */
    protected double getSmoothedTrigramScore(TST<Node> index, ST<Integer, String> indexR, Node prev, String token) {
        Node n = index.get(token);
        double uni = (n == null ? 0.0 : n.freq) / (double) index.size();
        double bi  = (double) prev.freq / (double) index.get(indexR.get(prev.index)).freq;

        return lambda[0]*uni + lambda[1]*bi;
    }

    /**
     * Normalizes lambdas - trigram counts smoothing factors
     */
    protected void normalizeLambdas() {
        double sum = 0.0;
        for (double l: lambda) {
            sum += l;
        }
        if (sum > 0) {
            for (int i = 0; i < N; i++) {
                lambda[i] = lambda[i] / sum;
            }
        }
    }

    /**
     * Builds suffix index
     */
    public void buildSuffixIndex()
    {
        Queue<String> q;
        for (String token: tokenIndex.keys()) {
            Node node = tokenIndex.get(token);
            if (node.freq < suffixThreshold) {
                q = new Queue<>();
                q.enqueue(token);
                addSuffixGrams(q, node);
            }
        }
    }

    /**
     * Adds suffix n-grams
     */
    protected void addSuffixGrams(Queue<String> q, Node node) {
        Node n;
        if (node.neighbors.size() == 0) {
            addSuffixGrams(q, new Queue<>());
        } else {
            for(int index: node.neighbors.keys()) {
                n = node.neighbors.get(index);
                q.enqueue(tokenIndexR.get(index));
                addSuffixGrams(q, n);
            }
        }
    }

    /**
     * Adds suffix n-grams
     *
     * @param t Tokens
     * @param s Suffix
     */
    protected void addSuffixGrams(Queue<String> t, Queue<String> s) {
        if (t.size() == 0) {

            Queue<String> copy = Utils.copy(s);
            for (String token: copy) {
                addSuffix(token);
            }
            while(copy.size() > 0) {
                addSuffixGram(copy);
                copy.dequeue();
            }

            return;
        }

        Queue<String> tCopy = Utils.copy(t), sCopy;
        String suffix = getSuffix(tCopy.dequeue());
        while (suffix.length() > 0) {
            sCopy = Utils.copy(s);
            sCopy.enqueue(suffix);
            addSuffixGrams(tCopy, sCopy);
            suffix = Utils.cutSuffix(suffix);
        }
    }

    /**
     * getSuffix
     *
     * @param token Token
     * @return Suffix
     */
    protected String getSuffix(String token)
    {
        int length = token.length();
        if (length == 0) throw new IllegalArgumentException("Empty token");
        if (length > maxSuffixLength) {
            return token.substring(length - maxSuffixLength, length - 1);
        } else {
            return token;
        }
    }

    /**
     * Print n-grams
     *
     * @param token Token
     */
    public void printNGrams(String token)
    {
        Node node = printUniGram(token);
        if (node == null) return;
        printNGrams(token, node.neighbors);
    }

    /**
     * Adds Node to the collector
     *
     * @param token Suffix
     */
    protected Node addNode(String token)
    {
        return addNode(tokenIndex, tokenIndexR, token);
    }

    /**
     * Adds suffix to the collector
     *
     * @param suffix Suffix
     */
    protected Node addSuffix(String suffix)
    {
        return addNode(suffixIndex, suffixIndexR, suffix);
    }

    /**
     * Add Node to the collector
     *
     * @param token Token
     * @return Node
     */
    protected Node addNode(TST<Node> index, ST<Integer, String> indexR, String token)
    {
        if (token.length() == 0) throw new IllegalArgumentException("Empty token.");

        Node node = index.get(token);
        if (node == null) {
            node = new Node();
            node.index = index.size();
            node.freq = 1;
            index.put(token, node);
            indexR.put(node.index, token);
        } else {
            node.freq += 1;
        }

        return node;
    }

    /**
     * Add N-gram to the collector
     *
     * @param tokens Tokens
     */
    protected void addFirstGram(Queue<String> tokens)
    {
        addGram(tokenIndex, tokens);
    }

    /**
     * Add N-gram to the collector
     *
     * @param tokens Tokens
     */
    protected void addSuffixGram(Queue<String> tokens)
    {
        addGram(suffixIndex, tokens);
    }

    /**
     * Add Gram
     *
     * @param index Index
     * @param tokens Tokens
     */
    protected void addGram(TST<Node> index, Queue<String> tokens) {
        if (tokens.size() == 0) throw new IllegalArgumentException("Empty tokens queue.");

        Node prev = null, node;
        int i = 0;
        for (String token: tokens) {
            node = index.get(token);
            if (prev != null) {
                node = addNeighbor(prev, node.index);
            }

            i++;
            if (i == N) {
                break;
            } else {
                prev = node;
            }
        }
    }

    /**
     * Adds neighbor to node
     *
     * @param prev Previous node
     * @param index Next node index
     *
     * @return Node
     */
    protected Node addNeighbor(Node prev, int index)
    {
        if (prev == null) throw new IllegalArgumentException("Invalid node.");
        Node neighbor = prev.neighbors.get(index);
        if (neighbor == null) {
            neighbor = new Node();
            neighbor.index = index;
            neighbor.freq = 1;
            prev.neighbors.put(neighbor.index, neighbor);
        } else {
            neighbor.freq += 1;
        }

        return neighbor;
    }

    /**
     * Scores token n-gram
     *
     * @param q n-gram queue
     * @param n n-gram rang
     * @return score
     */
    public double scoreGram(Queue<String> q, int n)
    {
        if (q.size() < n) throw new IllegalArgumentException();

        Queue<String> qCopy = Utils.copy(q);

        String token;
        Node prev = null, node = null, first = null;
        while (q.size() > 0) {
            token = q.dequeue();
            prev = node;
            if (prev == null) {
                node = tokenIndex.get(token);
                first = node;
            } else {
                node = takeNext(prev, token);
                if (node == null) if(n == 3 && first != prev) return getSmoothedTrigramScore(tokenIndex, tokenIndexR, prev, token);
            }
        }

        if (node == null) return scoreSuffixGram(qCopy, n);

        if (n == 1) return (double) node.freq / tokenIndex.get("START").freq;

        if (prev == null) return n == 3 ? lambda[0] * ((double) node.freq / (double) tokenIndex.size()) : 0.0;

        return n == 3 ? getSmoothedTrigramScore(tokenIndex, tokenIndexR, first, prev, node) : (double) node.freq / (double) prev.freq;

    }

    /**
     * Scores suffix n-gram
     *
     * @param q n-gram queue
     * @param n n-gram rang
     * @return score
     */
    public double scoreSuffixGram(Queue<String> q, int n)
    {
        if (q.size() < n) throw new IllegalArgumentException();

        String token;
        Node prev = null, node = null, first = null;
        while (q.size() > 0) {
            token = q.dequeue();
            prev = node;
            if (prev == null) {
                node = suffixNode(token);
                first = node;
            } else {
                node = takeNextSuffixNode(prev, token);
                if (node == null) {
                    if(n == 3 && first != prev) {
                        return getSmoothedTrigramScore(suffixIndex, suffixIndexR, prev, token);
                    }
                }
            }
        }

        if (node == null) {
            return 0.0;
        }

        if (n == 1) return (double) node.freq / tokenIndex.get("START").freq;

        if (prev == null) return n == 3 ? lambda[0] * (double) node.freq / (double) tokenIndex.size() : 0.0;
        return n == 3 ? getSmoothedTrigramScore(suffixIndex, suffixIndexR, first, prev, node) : (double) node.freq / (double) prev.freq;

    }

    /**
     * Gets longest suffix node by token
     *
     * @param token Token
     * @return node
     */
    protected Node suffixNode(String token)
    {
        Node node = null;
        String suffix = getSuffix(token);
        while(suffix.length() > 0) {
            node = suffixIndex.get(suffix);
            if (node != null) break;
            suffix = Utils.cutSuffix(suffix);
        }

        return node;
    }

    /**
     * Takes next node by token
     * 
     * @param prev Previous node
     * @param token Token
     * @return Node
     */
    protected Node takeNext(Node prev, String token)
    {
        for (int index: prev.neighbors.keys()) {
            if (tokenIndexR.get(index).equals(token)) {
                return prev.neighbors.get(index);
            }
        }
        
        return null;        
    }

    /**
     * Takes next suffix node by token
     *
     * @param prev Previous node
     * @param token Token
     * @return Node
     */
    protected Node takeNextSuffixNode(Node prev, String token)
    {
        String suffix = getSuffix(token);
        while(suffix.length() > 0) {
            for (int index : prev.neighbors.keys()) {
                if (suffixIndexR.get(index).equals(suffix)) {
                    return prev.neighbors.get(index);
                }
            }
            suffix = Utils.cutSuffix(suffix);
        }

        return null;
    }

    /**
     * Prints uni gram
     *
     * @param token Token
     * @return Node
     */
    protected Node printUniGram(String token)
    {
        Node node = tokenIndex.get(token);
        if(node == null) return null;
        System.out.println(token + " " + node.freq);

        return node;
    }

    /**
     * Prints n-grams
     *
     * @param history Backward history
     * @param neighbors Neighbors
     */
    protected void printNGrams(String history, ST<Integer, Node> neighbors)
    {
        if (neighbors.size() == 0) return;
        for (int index: neighbors.keys()) {
            Node node = neighbors.get(index);
            System.out.println(history + " " + tokenIndexR.get(index) +  " " + node.freq);
            printNGrams(history + " " + tokenIndexR.get(index), node.neighbors);
        }
    }

    /**
     * Add phrase to the collector
     *
     * @param tokens Token queue
     */
    public void addPhrase(Queue<String> tokens)
    {
        Queue<String> copy = Utils.copy(tokens);
        for (String token: copy) {
            addNode(token);
        }
         while(copy.size() > 0) {
            addFirstGram(copy);
            copy.dequeue();
        }
    }

    /**
     *  Gets token index size
     *
     * @return collection size
     */
    public int size()
    {
        return tokenIndex.size();
    }

    /**
     * Gets token count
     *
     * @param token Token
     * @return count
     */
    public int count(String token) {
        Node node = tokenIndex.get(token);
        if (node == null) {
            return 0;
        }

        return node.freq;
    }

    /**
     * Gets suffix smoothing factor
     *
     * @return factor
     */
    public double suffixSmoothingFactor()
    {
        int sum = 0;
        for(String token: tokenIndex.keys()) {
            sum += tokenIndex.get(token).freq;
        }
        double avg = (double) sum / (double) tokenIndex.size();

        double teta = 0.0;
        for(String token: tokenIndex.keys()) {
            teta += Math.pow(tokenIndex.get(token).freq - avg, 2);
        }

        return teta / (double) tokenIndex.size();
    }

}
