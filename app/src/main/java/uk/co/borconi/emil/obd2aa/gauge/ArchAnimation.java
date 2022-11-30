package uk.co.borconi.emil.obd2aa.gauge;

import android.view.animation.Animation;
import android.view.animation.Transformation;

import uk.co.borconi.emil.obd2aa.ArcProgress;

public class ArchAnimation extends Animation {

    private final ArcProgress circle;

    private final float oldAngle;
    private final float newAngle;

    public ArchAnimation(ArcProgress circle, float newAngle) {
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
