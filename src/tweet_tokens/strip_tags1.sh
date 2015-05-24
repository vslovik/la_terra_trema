#!/bin/sh
date

sed -r '
s/(.+)\t(.+)/\1/
'  < /home/lera/Desktop/LAUREA/Training_pos_isst-paisa-devLeg.pos > /home/lera/Desktop/LAUREA/Training_pos_isst-paisa-devLeg_no_tags.pos

date
