package uk.co.boconi.emil.obd2aa;

/**
 * Created by Emil on 18/09/2017.
 */

public class UnitConvertHelper {
    public static float ConvertValue (float inputval, String unit)
    {
        if (unit.equalsIgnoreCase("km/h"))
            inputval = (float) (inputval / 1.60);
        else if (unit.equalsIgnoreCase("km"))
            inputval = (float) (inputval / 1.60);
        else if (unit.equalsIgnoreCase("l"))
            inputval = (float) (inputval * 0.219969);
        else if (unit.equalsIgnoreCase("°C"))
            inputval = (float) ((inputval * 1.8) + 32);
        else if (unit.equalsIgnoreCase("Kg"))
            inputval = (float) (inputval * 2.20462);
        else if (unit.equalsIgnoreCase("m"))
            inputval = (float) (inputval * 3.28084);
        else if (unit.equalsIgnoreCase("psi"))
            inputval = (float) (inputval * 0.0689476);
        else if (unit.equalsIgnoreCase("ft-lb"))
            inputval = (float) (inputval * 1.35581795);
        else if (unit.equalsIgnoreCase("kPa"))
            inputval = (float) (inputval * 0.145038);
        else if (unit.equalsIgnoreCase("l/hr"))
            inputval=(float) (inputval * 0.219969);
        return inputval;
    }
}
