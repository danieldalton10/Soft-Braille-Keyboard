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
# Scan a liblouis translation table and print out the Braille codes with
# duplicates.
# Eg. Where there are Braille combinations with > 1 character assigned
# causing conflicts during backtranslation.
# This is pretty hacky though.
# python duplication.py /path/toloui_table
# and if you don't want to recurse i.e. just consider this input file
# not other files that it also imports collectively
# python duplication.py /path/toloui_table -n
# Infact this is so hacky the last argument can be whatever you want; it
# just must be present.
# This is designed for ad-hoc usage and you should use at your own risk.

import sys

def scanFile (file, result):
    #if "loweredDigits6Dots.uti" in file or "chardefs.cti" in file:
#        return
    f=open(file)
    for line in f.readlines():
        line = line.split() # splits on whitespace
        if len(line) <= 0 or line[0].startswith("#"):
            continue
        if line[0].strip().startswith ("include"):
            if len(sys.argv) <= 2:
                scanFile(line[1].strip(), result)
        elif len(line) >= 3:
            if line[2].strip() not in result.keys():
                result[line[2].strip()]=[{"file":file, "character":line[1].strip()}]
            else:
                result[line[2].strip()].append({"file":file, "character":line[1].strip()})
    f.close()
    
result = {}            
scanFile(sys.argv[1], result)

for key in result.keys():
    if len(result[key]) > 1:
        symbols = ""
        for symbolDict in result[key]:
            try:
                symbol = symbolDict['character']
                if len(symbols) > 0:
                    symbols += " "
                if symbol.lower().startswith("\\x"):
                    list = symbol.lower().split("\\x")[1:]
                    symbol = ""
                    for item in list:
                        symbol += ("%c" % (int ("0x"+item, 16)))
                symbols += symbol + " " + symbolDict["file"]
            except:
                print ("error")
        print ("%s\t%s" % (key, symbols))
