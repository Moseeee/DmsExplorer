/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.android.transition;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Path;
import android.os.Build.VERSION_CODES;
import android.support.annotation.NonNull;
import android.transition.PathMotion;
import android.util.AttributeSet;

/**
 * マテリアルデザインにある下に凸な曲線のモーションを実現する。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
@TargetApi(VERSION_CODES.LOLLIPOP)
public class SmilingCurveMotion extends PathMotion {
    public SmilingCurveMotion() {
    }

    public SmilingCurveMotion(final @NonNull Context context, final @NonNull AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public Path getPath(final float startX, final float startY, final float endX, final float endY) {
        final Path path = new Path();
        path.moveTo(startX, startY);
        final float middleX;
        final float middleY;
        if (startY > endY) {
            middleX = (startX + endX * 3) / 4;
            middleY = (startY * 3 + endY) / 4;
        } else {
            middleX = (startX * 3 + endX) / 4;
            middleY = (startY + endY * 3) / 4;
        }
        path.quadTo(middleX, middleY, endX, endY);
        return path;
    }
}
