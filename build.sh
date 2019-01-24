#!/usr/bin/env bash
./gradlew :player:assembleRelease
mv player/build/outputs/aar/player-release.aar ~/StudioProjects/ellabook/player/
