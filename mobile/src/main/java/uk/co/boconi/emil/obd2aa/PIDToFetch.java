package uk.co.boconi.emil.obd2aa;


/**
 * Created by Emil on 01/09/2017.
 */

public class PIDToFetch {
    public String PID;
    public Boolean isActive;
    public long LastFetch;
    public int gaugenumber;
    public boolean needsconverting;
    public String unit;
    public float maxvalue;
    public float minValue;
    public double lastvalue=0;

    public  PIDToFetch(String pid, Boolean ac, long lf, int g, String unit, boolean b, float MaxValue, float MinValue)
    {
        this.isActive=ac;
        this.LastFetch=lf;
        this.PID=pid;
        this.gaugenumber=g;
        this.unit=unit;
        this.needsconverting=b;
        this.maxvalue=MaxValue;
        this.minValue=MinValue;
    }

    public void setLastvalue(double lv) {this.lastvalue=lv;}
    public double getLastvalue() {return this.lastvalue;}
    public void putLastFetch(long lf){
        this.LastFetch=lf;
    }

    public void setMaxValue(float maxValue) {
        this.maxvalue = maxValue;
    }

    public String[] getPID(){
        String a[]={this.PID};
        return a;
    }
    public String getSinglePid() {
        return this.PID;
    }

    public boolean getActive(){return this.isActive;}
    public long getLastFetch(){return this.LastFetch;}
    public int getGaugeNumber(){return this.gaugenumber;}
    public boolean getNeedsConversion(){return this.needsconverting;}
    public String getUnit(){return this.unit;}
    public float getMaxValue() {return this.maxvalue;}


    public float getMinValue() {
        return this.minValue;
    }
}
