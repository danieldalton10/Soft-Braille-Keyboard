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

import android.content.Context;
import android.content.Intent;

import com.googlecode.eyesfree.braille.translate.TranslatorClient;

// TODO(ddalton): Find a way to use the service directly without this class.
public class MyTranslatorClient extends TranslatorClient {
    private Intent mServiceIntent;

    public MyTranslatorClient(Context context, OnInitListener onInitListener) {
        mContext = context;
        mOnInitListener = onInitListener;
        mServiceIntent = new Intent(
                context,
                com.googlecode.eyesfree.braille.service.translate.TranslatorService.class);
        doBindService();
    }

    private void doBindService() {
        Connection localConnection = new Connection();
        if (!mContext.bindService(mServiceIntent, localConnection,
                Context.BIND_AUTO_CREATE)) {
            mHandler.scheduleRebind();
            return;
        }
        mConnection = localConnection;
    }
}
