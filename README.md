**score.TopScored** takes as input a file that contains a set of tagged sentences to learn statistics from and
another file with tagged phrases to score.

It outputs N best-scored sentences.

Phrases are in TSV format:
* one token/tag pair separated by `\t` (tab) symbol per line,
* empty line (`\n`) is used as phrase delimiter.

The quality score (QS) for the tagged sentence is computed as a product of the local probabilities
to have the class *a* in the local context *b* in a form used for TnT pos-tagger with trigram counts
smoothing and suffix analysis as a method of handling unknown words.

#####Execution#####
`java score.TopScored [file to extract features from] [file with phrases to score]`

