package uk.co.boconi.emil.obd2aa;

import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Created by Emil on 18/08/2017.
 */

public class ArchAnimaton extends Animation {

    private ArcProgress circle;

    private float oldAngle;
    private float newAngle;

    public ArchAnimaton(ArcProgress circle, float newAngle) {
        this.oldAngle = circle.getProgress();
        this.newAngle = newAngle;
        this.circle = circle;

    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation transformation) {
        float angle = oldAngle + ((newAngle - oldAngle) * interpolatedTime);

        circle.setProgress(angle);

    }
}
