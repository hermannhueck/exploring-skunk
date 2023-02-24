#! /bin/sh

set -x
docker run -p5432:5432 -d tpolecat/skunk-world
