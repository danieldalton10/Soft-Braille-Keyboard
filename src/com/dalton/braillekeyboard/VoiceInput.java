/*
 * Copyright (C) 2016 The Soft Braille Keyboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dalton.braillekeyboard;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;

/**
 * Acts as a layer of abstraction between the Android speech to text engine.
 * 
 * You should instantiate this class and call the start method to begin input.
 * Any required setup is performed the first time start is called. Once you are
 * finished it is important to call destroy() to release resources.
 * 
 * The TextReadyListener is a simplified callback for VoiceInput and will
 * deliver the best result back to the caller as a String.
 */
public class VoiceInput {

    /**
     * Callback for interractions with VoiceInput.
     * 
     * Implement these callbacks which will e invoked by the VoiceInput.
     */
    public interface TextReadyListener {
        /**
         * Called when the Voice engine successfully decodes speech to text.
         * 
         * @param text
         *            The best textual match for the speech.
         */
        void onTextReady(String text);

        /**
         * Invoked when an error with the voice input engine occurs.
         * 
         * @param error
         *            The error code for this error. See the error codes in
         *            SpeechRecognizer.
         */
        void onError(int error);
    }

    // Recognition listener for Android's SpeechRecognizer.
    private final RecognitionListener listener = new RecognitionListener() {

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> resultsList = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            // Send the best match to our callback, or the empty string if there
            // is none.
            textReadyListener.onTextReady(resultsList.size() > 0 ? resultsList
                    .get(0) : "");
            isListening = false; // update state
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }

        @Override
        public void onError(int error) {
            // There seems to be a bug where this gets called with
            // SpeechRecognizer.ERROR_NO_MATCH every time, but the very first
            // time voice input is done.
            // However the results seem to be delivered to us just fine. For now
            // ignore this error, but it needs further investigation and a
            // better solution for example when it is actually a legitimate
            // problem.
            if (error != SpeechRecognizer.ERROR_NO_MATCH) {
                textReadyListener.onError(error);
                isListening = false;
            }
        }

        @Override
        public void onEndOfSpeech() {
            isListening = false;
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onBeginningOfSpeech() {
        }
    };

    private SpeechRecognizer recognizer;
    private boolean isListening;
    private TextReadyListener textReadyListener;

    // Prepares the Android SpeechRecognizer if necessary. Returns true if the
    // engine can be used otherwise false.
    private boolean prepareIfNecessary(Context context) {
        if (recognizer == null) {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(context);
                recognizer.setRecognitionListener(listener);
            } else {
                recognizer = null;
            }
        }
        return recognizer != null;
    }

    /**
     * Start listening for voice dictation and return the speech as text in the
     * callback see TextReadyListener. Subsequent calls to this method while
     * listening is in progress will have no effect.
     * 
     * @param context
     *            The application context.
     * @param textReadyListener
     *            The callback implementation which will be invoked when the
     *            results are ready or an error occurs.
     * @return true if voice input has been started or is in progress or false
     *         if voice input can't be started or isn't available.
     */
    public boolean start(Context context, TextReadyListener textReadyListener) {
        if (prepareIfNecessary(context)) {
            if (!isListening) {
                this.textReadyListener = textReadyListener;
                recognizer.startListening(new Intent());
                isListening = true;
            }
            return true;
        }
        return false;
    }

    /**
     * Return the state of the VoiceInput instance.
     * 
     * @return true if listening is in progress otherwise false.
     */
    public boolean isListening() {
        return isListening;
    }

    /**
     * Release system resources held by this instance. You should always call
     * this when you have finished with the VoiceInput instance.
     */
    public void destroy() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
    }
}
