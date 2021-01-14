#!/usr/bin/env bash

# Based off of the instructions for Ubuntu on <https://github.com/openstreetmap/osm2pgsql>

sudo apt-get install make cmake g++ libboost-dev libboost-system-dev \
  libboost-filesystem-dev libexpat1-dev zlib1g-dev \
  libbz2-dev libpq-dev libproj-dev lua5.3 liblua5.3-dev pandoc \
  postgresql-server-dev-13 # not mentioned in instructions, got from <https://github.com/minetest/minetest/issues/4293> after googling error message

# From <https://github.com/openstreetmap/osm2pgsql#luajit-support>
sudo apt-get install libluajit-5.1-dev

cd osm2pgsql || exit

mkdir -p build
cd build || exit

cmake -D WITH_LUAJIT=ON ..
make
sudo make install
