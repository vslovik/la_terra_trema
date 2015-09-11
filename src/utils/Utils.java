package utils;

import algorithms.Queue;

/**
 *  The <tt>Utils</tt> class is a collection of reusable functions
 *
 *  @author Valeriya Slovikovskaya vslovik@gmail.com
 */
public class Utils {

    /**
     * Gets shortened suffix
     *
     * @param suffix Suffix
     * @return shortened suffix
     */
    public static String cutSuffix(String suffix)
    {
        return suffix.length() == 1 ? "" : suffix.substring(1, suffix.length());
    }

    /**
     * Copy queue
     *
     * @param q Token/suffix queue
     * @return queue copy
     */
    public static Queue<String> copy(Queue<String> q) {
        Queue<String> copy = new Queue<String>();
        for(String s: q) {
            copy.enqueue(s);
        }

        return copy;
    }

}
