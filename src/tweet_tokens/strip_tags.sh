#!/bin/sh
date

sed -r '
s/(.+)\t(.+)/\1/
'  < /home/lera/Desktop/LAUREA/corpus_annotato_automaticamente_cleaned.pos > /home/lera/Desktop/LAUREA/corpus_annotato_automaticamente_cleaned_no_tags.pos

date
