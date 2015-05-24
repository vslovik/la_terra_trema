#!/bin/sh
date

sed -r '
s/(.+)\t(.+)/\2/
'  < /home/lera/Desktop/LAUREA/corpus_annotato_automaticamente.pos > /home/lera/Desktop/LAUREA/corpus_annotato_automaticamente_cleaned_no_words.pos

date
