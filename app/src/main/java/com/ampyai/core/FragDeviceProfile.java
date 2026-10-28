package com.ampyai.core;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Build;
import android.util.DisplayMetrics;
import java.util.Locale;

/** Only Android system metadata, never reads contacts, location or other private app data. */
public final class FragDeviceProfile {
    public static final class Snapshot {
        public final String maker,device,language;
        public final int sdk,charge,width,height;
        public Snapshot(String maker,String device,String language,int sdk,
                        int charge,int width,int height) {
            this.maker=maker;this.device=device;this.language=language;
            this.sdk=sdk;this.charge=charge;this.width=width;this.height=height;
        }
        public String describe(){
            return "Produttore: "+maker+"\nModello: "+device+"\nAndroid API: "+sdk+
                    "\nBatteria: "+(charge<0?"non disponibile":charge+"%")+
                    "\nLingua del sistema: "+language+
                    "\nArea schermo approssimativa: "+width+"×"+height+" px";
        }
        public boolean pixel7Pro(){
            return "google".equalsIgnoreCase(maker) &&
                    ("Pixel 7 Pro".equalsIgnoreCase(device)||"cheetah".equalsIgnoreCase(device));
        }
    }
    public static Snapshot collect(Context ctx) {
        BatteryManager battery=(BatteryManager)ctx.getSystemService(Context.BATTERY_SERVICE);
        int charge=battery==null?-1:
                battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        DisplayMetrics m=ctx.getResources().getDisplayMetrics();
        return new Snapshot(Build.MANUFACTURER,Build.MODEL,
                Locale.getDefault().toLanguageTag(),Build.VERSION.SDK_INT,
                charge,m.widthPixels,m.heightPixels);
    }
    private FragDeviceProfile(){}
}
