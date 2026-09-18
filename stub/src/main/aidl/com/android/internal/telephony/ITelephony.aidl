package com.android.internal.telephony;

interface ITelephony {
    boolean isImsRegistered(int subId);
    void resetIms(int slotIndex);
}
