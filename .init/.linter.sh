#!/bin/bash
cd /home/kavia/workspace/code-generation/food-delivery-browser-303763-303772/android_frontend
./gradlew lint
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

