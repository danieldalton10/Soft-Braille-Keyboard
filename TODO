For lack of a better bug tracking system here are a list of things that should be fixed / added in the future.

BUGS

* There is a minor bug where a misspelled word doesn’t say
  “misspelled” if it is at the end of a sentence. (eg. after a period).
* Switching Braille tables / grades on the fly in the middle of a word
  has unpredictable results. If I begin typing in computer Braille and
  then switch to literary and continue typing the remainder of the
  word and press space the dot patterns are converted to a word as if
  the whole thing was typed in literary Braille. The first segment of
  the word should honour the rules of computer Braille and the second
  segment where I changed grade should honour the literary rules in
  this case.
* A word is duplicated if I type a word, shrink the keyboard, clear
  the input (eg. press application "send" btn), expand it and press
  space.

TODO

* Migrate to the new Android gradle build system / Android studio.
* Find a common java code style template so we can format all files
  with the same conventions.
* Currently we hack our way around the Braille service
  TranslatorClient rather than using it directly. Ideally we would like
  to use TranslatorClient directly without our own MyTranslatorClient
  eg. like Brailleback does. Unfortunately, I couldn’t get this to work.
* In Text To Speech engine there should be an option “auto - use
  system default” which will always honour the default system tts
  engine.
* Russian support.
* Do spell check with hunspell to support wider variety of devices.
* Be smarter with contracted Braille. Make delete character remove the
  last Braille pattern that was typed and update the text
  appropriately. Currently it just clears the dot list and deletes the
  last char. If you start typing in the middle of a word improve
  performance by creating a dot list up until the cursor which newly
  typed symbols can be appended. So if you type “mor”, delete the r
  and type another r you get “mor” rather than “morather”.
* Option to play a sound for misspellings rather than saying “misspelled”.
* Emojis support.
* More TTS options eg. engine / voice for specific languages.
* More control over keyboard sounds.
* Control of “closing keyboard message”.
* Internationalisation
* Automated testing - unit / integration tests. Consider code refactor
  using more dependency injection to facilitate mocking out parts of
  the system. Ultimately, this should help prevent breaking existing
  features of the app as we develop.
