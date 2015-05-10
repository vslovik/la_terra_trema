#!/bin/sh
sed -r '
s/^(\w)\1+/\1/g
s/(\w)\1+$/\1/g
s/(\w{2})\1+$/\1\1/g
s/^(\w{2})\1+/\1\1/g
s/(\w)\1{2}\1+/\1\1\1/g'  </home/lera/Desktop/LAUREA/corpus_annotato_automaticamente.pos > /home/lera/Desktop/LAUREA/corpus_annotato_automaticamente_cleaned.pos

date
