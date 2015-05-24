#!/bin/sh
date

sed -r '
s/(.+)\t(.+)/\2/
'  < /home/lera/Desktop/LAUREA/Training_pos_isst-paisa-devLeg.pos > /home/lera/Desktop/LAUREA/Training_pos_isst-paisa-devLeg_no_words.pos

date
