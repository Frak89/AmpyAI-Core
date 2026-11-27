package com.ampyai.core;

import static org.junit.Assert.*;
import android.graphics.*;
import android.os.Looper;
import android.view.View;
import java.io.*;
import java.time.Duration;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=37,qualifiers="w412dp-h850dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PortraitRenderTest {
    @Test(timeout=90000) public void drawsTalkingPortrait() throws Exception {
        ActivityController<OrbActivity> controller=Robolectric.buildActivity(OrbActivity.class).create();
        OrbActivity activity=controller.get();
        View root=activity.findViewById(android.R.id.content);
        int w=1080,h=2240;
        root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));
        root.layout(0,0,w,h);
        activity.getSharedPreferences("orb",0).edit().putBoolean("speech",false).commit();
        Method speak=OrbActivity.class.getDeclaredMethod("present",String.class);speak.setAccessible(true);
        speak.invoke(activity,"Sono qui, Davide. Da quale dispositivo vuoi iniziare?");
        Bitmap bmp=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bmp);
        java.lang.reflect.Field ticker=OrbActivity.class.getDeclaredField("type");ticker.setAccessible(true);
        Runnable nextLetter=(Runnable)ticker.get(activity);
        for(int i=0;i<132;i++){
            org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofMillis(13));
            nextLetter.run();
        }
        root.draw(c);
        int emerald=0,gold=0;
        for(int y=80;y<h/2;y+=5)for(int x=0;x<w;x+=5){
            int col=bmp.getPixel(x,y),r=Color.red(col),g=Color.green(col),b=Color.blue(col);
            if(g>75&&g>r*1.3&&b<g*1.1)emerald++;
            if(r>125&&g>66&&b<g*.65&&r>g)gold++;
        }
        assertTrue("Visible emerald bust",emerald>900);assertTrue("Visible amber eyes",gold>70);
        File file=new File("build/reports/portrait/Frag-Orbita.png");file.getParentFile().mkdirs();
        try(FileOutputStream stream=new FileOutputStream(file)){assertTrue(bmp.compress(Bitmap.CompressFormat.PNG,100,stream));}
        controller.destroy();
    }
}