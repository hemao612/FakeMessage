/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.messaging.tests;

import android.Manifest;
import android.annotation.Nullable;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.GsmAlphabet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Activity to send test cell broadcast messages from GUI.
 */
public class SendTestMessageActivity extends Activity implements View.OnClickListener{

    private Button mButton;
    private TextView mSender;
    private TextView mMessage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.send_test_message_activity);
        mButton = (Button) findViewById(R.id.btn_click);
        mButton.setOnClickListener(this);

        mSender = (TextView) findViewById(R.id.sender);
        mMessage = (TextView) findViewById(R.id.message);
    }

    private static byte[] hexStringToByteArray(String hexString) {
        int length = hexString.length();
        byte[] buffer = new byte[length / 2];

        for (int i = 0 ; i < length ; i += 2) {
            buffer[i / 2] =
                    (byte)((toByte(hexString.charAt(i)) << 4) | toByte(hexString.charAt(i+1)));
        }

        return buffer;
    }

    private static int toByte(char c) {
        if (c >= '0' && c <= '9') return (c - '0');
        if (c >= 'A' && c <= 'F') return (c - 'A' + 10);
        if (c >= 'a' && c <= 'f') return (c - 'a' + 10);

        throw new RuntimeException ("Invalid hex char '" + c + "'");
    }

    @Override
    public void onClick(View v) {
//        String pduString = "07916164260220F0040B914151245584F600006060605130308A04D4F29C0E";
//        byte[] pdu = hexStringToByteArray(pduString);
        sendMessage();
    }

    private void sendMessage() {
        CharSequence sender = mSender.getText();
        CharSequence message = mMessage.getText();
        Log.d("Sms_he", "sendMessage");
        if (!TextUtils.isEmpty(sender) && !TextUtils.isEmpty(message)) {
            Log.d("Sms_he", "sending");
            byte[] pdu = createPdu(sender.toString(), message.toString());
            Object[] pdus = new Object[] {pdu};
            Intent intent = new Intent(Telephony.Sms.Intents.SMS_DELIVER_ACTION);
            intent.putExtra("pdus", pdus);
            intent.putExtra("format", "3gpp");
            sendOrderedBroadcastAsUser(intent, UserHandle.ALL,
                    Manifest.permission.RECEIVE_SMS,
                    AppOpsManager.OP_RECEIVE_SMS, null, null, Activity.RESULT_OK, null, null);
            Toast.makeText(this, "sending", Toast.LENGTH_SHORT);
        }
    }

    private static byte[] createPdu(String sender, String body) {
        byte[] pdu = null;
        byte[] scBytes = PhoneNumberUtils
                .networkPortionToCalledPartyBCD("0000000000");
        byte[] senderBytes = PhoneNumberUtils
                .networkPortionToCalledPartyBCD(sender);
        int lsmcs = scBytes.length;
        byte[] dateBytes = new byte[7];
        Calendar calendar = new GregorianCalendar();
        dateBytes[0] = reverseByte((byte) (calendar.get(Calendar.YEAR)));
        dateBytes[1] = reverseByte((byte) (calendar.get(Calendar.MONTH) + 1));
        dateBytes[2] = reverseByte((byte) (calendar.get(Calendar.DAY_OF_MONTH)));
        dateBytes[3] = reverseByte((byte) (calendar.get(Calendar.HOUR_OF_DAY)));
        dateBytes[4] = reverseByte((byte) (calendar.get(Calendar.MINUTE)));
        dateBytes[5] = reverseByte((byte) (calendar.get(Calendar.SECOND)));
        dateBytes[6] = reverseByte((byte) ((calendar.get(Calendar.ZONE_OFFSET) + calendar
                .get(Calendar.DST_OFFSET)) / (60 * 1000 * 15)));
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            bo.write(lsmcs);
            bo.write(scBytes);
            bo.write(0x04);
            bo.write((byte) sender.length());
            bo.write(senderBytes);
            bo.write(0x00);
            bo.write(0x00); // encoding: 0 for default 7bit
            bo.write(dateBytes);
            try {
                GsmAlphabet.stringToGsm7BitPacked(body);
                byte[] bodybytes = GsmAlphabet.stringToGsm7BitPacked(body);
                bo.write(bodybytes);
            } catch (Exception e) {
            }

            pdu = bo.toByteArray();
        } catch (IOException e) {
        }
        return pdu;
    }

    private static byte reverseByte(byte b) {
        return (byte) ((b & 0xF0) >> 4 | (b & 0x0F) << 4);
    }
}
