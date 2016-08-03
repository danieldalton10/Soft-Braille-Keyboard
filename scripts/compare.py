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
# Compare two string.xml files
# Display the list of strings that are not commonly defined between the
# two files.
# Useful for syncing translation strings.xml files, but we should
# automate this further.
# Usage: python compare.py strings.xml1 strings.xml2
# This is designed for ad-hoc usage and you should use at your own risk.

from sys import argv

def getName(s):
    start = s.index("name=\"")
    start = s.index("\"", start)+1
    end = s.index("\"", start)
    return s[start:end]
    

f1=open(argv[1])
f2=open(argv[2])
f1Dict = {}
f2Dict = {}

i = 0
for line in f1:
    i+=1
    if line.strip().startswith("<string name="):
        f1Dict[getName(line)]=i

i = 0
for line in f2:
    i+=1
    if line.strip().startswith("<string name="):
        f2Dict[getName(line)]=i

f1.close()
f2.close()
results={}
print ("The following strings are only contained in %s." % (argv[1]))
for key in f1Dict.keys():
    if not f2Dict.has_key(key):
        results[f1Dict[key]]=key
keys=results.keys()
keys.sort()
for key in keys:
    print ("%s on line %d" % (results[key], key))
    
print ("The following strings are only contained in %s." % (argv[2]))
results.clear()
for key in f2Dict.keys():
    if not f1Dict.has_key(key):
        results[f2Dict[key]]=key

keys=results.keys()
keys.sort()
for key in keys:
    print ("%s on line %d" % (results[key], key))
    
