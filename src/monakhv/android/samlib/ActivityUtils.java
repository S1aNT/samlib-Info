/*
 * Copyright 2013 Dmitry Monakhov.
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

package monakhv.android.samlib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.widget.SlidingPaneLayout;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ListView;

/**
 *
 * @author Dmitry Monakhov
 */
public class ActivityUtils {
    static Drawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0x3300FF00, 0xFF00FF00, 0xffffffff});
    public static final int FAIDING_COLOR=-858993460;//Color.GRAY;
    public static final int ACTIVE_COLOR=Color.BLACK;
    public static void setDivider(ListView listView){
        listView.setDivider(gradient);
        listView.setDividerHeight(1);
        
    }
    public static void setShadow(SlidingPaneLayout pane){
        pane.setShadowResource(R.drawable.slidingpane_shadow);
        
        pane.setHorizontalFadingEdgeEnabled(true);
        
        pane.setSliderFadeColor(FAIDING_COLOR);
    }
    public static void ImageViewAnimatedChange(Context c, final ImageView v, final Bitmap new_image) {
    final Animation anim_out = AnimationUtils.loadAnimation(c, android.R.anim.fade_out); 
    final Animation anim_in  = AnimationUtils.loadAnimation(c, android.R.anim.fade_in); 
    anim_out.setAnimationListener(new AnimationListener()
    {
        @Override public void onAnimationStart(Animation animation) {}
        @Override public void onAnimationRepeat(Animation animation) {}
        @Override public void onAnimationEnd(Animation animation)
        {
            v.setImageBitmap(new_image); 
            anim_in.setAnimationListener(new AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {}
                @Override public void onAnimationRepeat(Animation animation) {}
                @Override public void onAnimationEnd(Animation animation) {}
            });
            v.startAnimation(anim_in);
        }
    });
    v.startAnimation(anim_out);
}
}
