package uk.co.boconi.emil.obd2aa;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Emil on 31/08/2017.
 */

class PidList {

    public String longName;
    public String shortName;
    public String unit;
    public String maxValue;
    public String minValue;
    public String scale;
    public String pid;

    public PidList(String info) {
        String[] pidinfo = info.split(",");
        this.longName = pidinfo[0];
        this.shortName = pidinfo[1];
        this.unit = pidinfo[2];
        this.maxValue = pidinfo[3];
        this.minValue = pidinfo[4];
        this.scale = pidinfo[5];
        this.pid = pidinfo[6]+","+pidinfo[7];
    }
    public PidList(JSONObject data)
    {
        try {
            this.longName = data.getString("longName");
            this.shortName = data.getString("shortName");
            this.unit = data.getString("unit");
            this.maxValue = data.getString("maxValue");
            this.minValue = data.getString("minValue");
            this.scale = data.getString("scale");
            this.pid = data.getString("pid");
        }
        catch (Exception e)
        {

        }
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return this.longName.toString();
    }

    public String getPidName() {
        return this.longName;
    }

    public String getShortPidName()
    {
        return this.shortName;
    }

    public String getUnit()
    {
        return this.unit;
    }

    public String getMaxValue()
    {
        return this.maxValue;
    }

    public String getMinValue()
    {
        return this.minValue;
    }
    public String getScale()
    {
        return this.scale;
    }
    public String getPid()
    {
        return this.pid;
    }



}