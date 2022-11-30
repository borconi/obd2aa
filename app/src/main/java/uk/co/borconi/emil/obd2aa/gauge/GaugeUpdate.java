package uk.co.borconi.emil.obd2aa.gauge;

public class GaugeUpdate {
    private int gauge;
    private Float val;
    private boolean max = false;
    private boolean min = false;

    public GaugeUpdate(int gauge, Float val, boolean max, boolean min) {
        this.gauge = gauge;
        this.val = val;
        this.max = max;
        this.min = min;
    }

    public GaugeUpdate(int gauge, Float val) {
        this.gauge = gauge;
        this.val = val;
    }

    public boolean isMin() {
        return min;
    }

    public void setMin(boolean min) {
        this.min = min;
    }

    public boolean isMax() {
        return max;
    }

    public void setMax(boolean max) {
        this.max = max;
    }

    public int getGauge() {
        return gauge;
    }

    public void setGauge(int gauge) {
        this.gauge = gauge;
    }

    public Float getVal() {
        return val;
    }

    public void setVal(Float val) {
        this.val = val;
    }
}

