# Copyright 2016 Daniel Dalton
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Supply a Braille combination and a loui translation table.
# Print out the list of files that map a character to this translation.
# Useful for finding out what includes map to a specific Braille
# combinations for resolving conflicts.
# Usage: python find_combination.py code starting_file
# This is designed for ad-hoc usage and you should use at your own risk.

import sys

def scan (file):
    f=open(file)
    lineNumber = 0
    for line in f.readlines():
        lineNumber+=1
        line = line.split() # splits on whitespace
        if len(line) <= 0 or line[0].startswith("#"):
            continue
        if line[0].strip().startswith ("include"):
            scan(line[1].strip())
        elif len(line) > 1:
            if line[1].strip() == sys.argv[1]:
                return file + " " + str(lineNumber)
            elif len(line) > 2:
                if line[2].strip() == sys.argv[1]:
                    return file + " " + str(lineNumber)
    return ""
print (scan(sys.argv[2]))
