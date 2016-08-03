INTRODUCTION

This is an Android input method which displays a virtual on screen
Braille keyboard for use by a blind person. This facilitates much
faster and more comfortable input for the blind on Android with a
variety of powerful editing commands and support for a number of
different languages.

Please see https://goo.gl/lD1v49 for more details on the project.

If you are interested in becoming involved with any development or have questions please
contact Daniel Dalton <daniel.dalton10@gmail.com>

Please see the TODO file in this directory for a list of things that
need attention.

This application is licensed under the Apache License, Version 2.0 (the "License");
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0

BUILDING THE APP

You can checkout sbk as follows:
git clone git@bitbucket.org:daniel_dalton10/soft-braille-keyboard.git

To build the app you will need a few things. 
* Checkout Android Brailleback
git clone http://github.com/google/brailleback
* See brailleback/README for setup instructions. Follow these and make
sure you can at least ant build the service and client packages
each. Don't worry if brailleback isn't building we do not use this.
Note: The liblouis and brltty patches wouldn't apply for me in the
brailleback project. Solution was to manually enter the path to each
file for each patch when applying.
* Update the soft braille keyboard project.properties to use the right
target and point to the required libraries:
cd /path/to/soft-braille-keyboard
android update project -p ./ --target <target_id> --library /path/to/brailleback/braille/service/
android update project -p ./ --target <target_id> --library /path/to/brailleback/braille/client/
* ant debug
* If this all goes fine you should do the following:
  - Checkout latest liblouis somewhere else and copy the pl-pl-comp8.ctb
  file into the
  brailleback/braille/service/jni/liblouiswrapper/liblouis/tables/
  directory.
  - Copy *.utb *.ctb to this same directory from sbk/patches:
  cd /path/to/sbk_checkout
  cp patches/*.utb /path/to/brailleback/braille/service/jni/liblouiswrapper/liblouis/tables/
  cp patches/*.ctb /path/to/brailleback/braille/service/jni/liblouiswrapper/liblouis/tables/
  cd /path/to/brailleback/braille/service/jni/liblouiswrapper/liblouis/
  patch -p 0 < /path/to/sbk/patches/tablechanges.patch
  cd /path/to/brailleback
  patch -p 0 < /path/to/sbk/patches/tablelist-brailleback.patch
    Update the TranslatorClient:
    patch -p 1 < /path/to/soft-braille-keyboard/patches/TranslatorClient.patch
  And finally update the Braille service to use the newly included
  tables. If you make any changes to the liblouis tables or add
  additional tables you should do this last step:
  cd braille/service
  ./tables/mktranslationtables

If this all works fine it is time to configure eclipse:
1. Open eclipse. 
2. File -> import -> existing android code 
3. Browse and import the service package. 
4. Select just the service package and finish.
5. File -> import -> existing android code 
6. Browse and import the client package. 
7. Select just the client package and
finish. 
8. File -> import -> existing android code 
9. Browse to the Soft braille keyboard package and import it. 
10. Click finish. 
11. The projects should now build right click on the sbk project and
select run as -. android application 

ADDING A BRAILLE TABLE

- add the new table in the
brailleback/braille/service/jni/liblouiswrapper/liblouis/tables/
directory.
- Update brailleback/braille/service/res/xml/tables.xml.
- Run brailleback/braille/service/tables/mktranslationtables from the
brailleback/braille/service dir.
- Update sbk/res/values/arrays.xml to also include the id of the newly
defined table you created for the braille service.
- Clean build everything in eclipse or whatever and test.
- create the diffs:
  + cd /path/to/brailleback
  git diff braille/service/res/xml/tablelist.xml >/path/to/sbk/patches/tablelist-brailleback.patch
  + Copy the new table into the patches dir for now, so others can use
  it or otherwise get it included in liblouis and update the readme to
  include this like the pl-comp8 table.
- If you modify an existing table
  + run mktranslationtables as described.
  + Create the tablelist.xml diff as above if you changed that file. 
  + cd /path/to/brailleback/braille/service/jni/liblouiswrapper/liblouis/
  + git diff tables/ >/path/to/sbk/patches/tablechanges.patch
