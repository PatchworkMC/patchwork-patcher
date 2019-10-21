#!/usr/bin/env bash
find . -regex '.*\.\(java\)' -exec clang-format -style=file -i {} \;