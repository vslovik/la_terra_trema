package features;

import algorithms.Queue;
import algorithms.ST;
import algorithms.TST;
import utils.Utils;

/**
 * The <tt>TagDictionary</tt> class represents token - tag dictionary
 *
 * @author Valeriya Slovikovskaya vslovik@gmail.com
 */
public class TagDictionary {

    protected TST<Node> tokenIndex;
    protected TST<Node> suffixIndex;
    protected int suffixThreshold = 5;
    protected int maxSuffixLength = 4;

    /**
     * Node to keep in index
     */
    class Node
    {
        protected int freq;
        protected String token;
        protected ST<String, Double> tags = new ST<>();
    }

    /**
     * Tag dictionary constructor
     */
    public TagDictionary() {
        tokenIndex = new TST<>();
        suffixIndex = new TST<>();
    }

    /**
     * Builds suffix index
     */
    public void buildSuffixIndex() {
        for (String token : tokenIndex.keys()) {
            addSuffixes(token);
        }
    }

    /**
     * Build suffix index with smoothing
     *
     * @param teta Smoothing factor
     */
    public void buildSuffixIndex(double teta)
    {
        buildSuffixIndex();
        smoothSuffixCounts(teta);
    }

    /**
     * Add suffixes of max to min length
     *
     * @param token Token
     */
    protected void addSuffixes(String token)
    {
        Node node = tokenIndex.get(token);
        if (node.freq >= suffixThreshold) return;

        for (String tag : node.tags.keys())
            addSuffixes(token, tag);
    }

    /**
     * Add suffixes for token and tag
     *
     * @param token Token
     * @param tag Tag
     */
    protected void addSuffixes(String token, String tag) {
        String s = getSuffix(token);;
        while (s.length() > 0) {
            addSuffix(s, tag);
            s = Utils.cutSuffix(s);
        }
    }

    /**
     * Gets token ending to keep in suffix index
     *
     * @param token Token
     * @return suffix
     */
    protected String getSuffix(String token)
    {
        int length = token.length();
        if (length == 0) throw new IllegalArgumentException("Empty token");
        return length > maxSuffixLength ? token.substring(length - maxSuffixLength, length) : token;
    }

    /**
     * Add Node to the token-tag dictionary
     *
     * @param token Token
     * @param tag Tag
     */
    protected void addNode(String token, String tag)
    {
        addNode(tokenIndex, token, tag);
    }

    /**
     * Add Node to the suffix-tag dictionary
     *
     * @param suffix Suffix
     * @param tag Tag
     */
    protected void addSuffix(String suffix, String tag)
    {
        addNode(suffixIndex, suffix, tag);
    }

    /**
     * Add Node to the collector
     *
     * @param token Token
     * @param tag Tag
     */
    protected void addNode(TST<Node> index, String token, String tag)
    {
        Node node;
        if (token.length() == 0) throw new IllegalArgumentException("Empty token.");
        if (tag.length() == 0) throw new IllegalArgumentException("Empty tag.");
        node = index.get(token);
        if (node == null) {
            node = new Node();
            node.freq = 1;
            node.token = token;
            node.tags = new ST<>();
            node.tags.put(tag, 1.0);
            index.put(token, node);
        } else {
            node.freq += 1;
            node.tags.put(tag, node.tags.contains(tag) ? node.tags.get(tag) + 1.0 : 1.0);
        }
    }

    /**
     * Add tagged phrase to the dictionary
     *
     * @param tokens Token queue
     * @param tags Tag queue
     */
    public void addTaggedPhrase(Queue<String> tokens, Queue<String> tags)
    {
        if (tokens.size() == 0) throw new IllegalArgumentException("Empty tokens");
        if (tags.size() == 0) throw new IllegalArgumentException("Empty tags");
        if (tags.size() != tokens.size()) throw new IllegalArgumentException("Invalid tokens/tags queues");
        String tag;
        Queue<String> tagsCopy = Utils.copy(tags);
        for (String token: tokens) {
            tag = tagsCopy.dequeue();
            addNode(token, tag);
        }
    }

    /**
     *  Size
     *
     * @return collection size
     */
    public int size()
    {
        return tokenIndex.size();
    }

    /**
     * Count
     *
     * @param token Token
     * @param tag Tag
     * @return count
     */
    public double count(String token, String tag) {

        Node node = tokenIndex.get(token);
        if (node == null) {
           return suffixCount(token, tag);
        }

        if (node.tags.size() == 0) {
            throw new IllegalArgumentException("Empty tags.");
        }

        return node.tags.contains(tag) ? node.tags.get(tag) : 0;
    }

    /**
     * Count
     *
     * @param token Token
     * @param tag Tag
     * @return count
     */
    public double suffixCount(String token, String tag) {

        String suffix = getSuffix(token);
        Node node = null;
        while (suffix.length() > 0) {
            node = suffixIndex.get(suffix);
            if (node != null) {
                break;
            }
            suffix = Utils.cutSuffix(suffix);
        }

        if (node == null || suffix.length() == 0) {
            return 0;
        }

        if (node.tags.size() == 0) {
            throw new IllegalArgumentException("Empty tags.");
        }

        return node.tags.contains(tag) ? node.tags.get(tag) : 0;
    }


    /**
     * Smooth suffix counts
     *
     * @param teta smoothing factor
     */
     public void smoothSuffixCounts(double teta)
     {
         Node node;
         for(String token: tokenIndex.keys()) {
             node = tokenIndex.get(token);
             if (node.freq < suffixThreshold) {
                 for(String tag: node.tags.keys())
                     smoothSuffix(getSuffix(token), tag, teta);
             }
         }
     }

    /**
     * Smoothes tag counts for tokens with suffixes
     *
     * @param suffix Suffix to smooth count for
     * @param tag Tag of token with suffix
     * @param teta Smoothing factor
     */
    protected void smoothSuffix(String suffix, String tag, double teta)
    {
        double freq;
        Node node, prev = null;
        while(suffix.length() > 0) {
            node = suffixIndex.get(suffix);
            if(node == null) {
                throw new IllegalArgumentException("Missing suffix: " + suffix);
            }
            if(node.tags.size() == 0) {
                throw new IllegalArgumentException("Empty tags for suffix: " + suffix);
            }
            if(!node.tags.contains(tag)) {
                throw new IllegalArgumentException("Missing tag: " + tag + " for suffix: " + suffix);
            }
            if (prev != null) {
                freq = (node.tags.get(tag) + teta * (prev.tags.contains(tag) ? prev.tags.get(tag) : 0)) / (1 + teta);
                node.tags.put(tag, freq);
            }
            suffix = Utils.cutSuffix(suffix);
            prev = node;
        }
    }

}
