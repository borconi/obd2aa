package uk.co.boconi.emil.obd2aa;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.io.File;

import static java.lang.Integer.parseInt;

/**
 * Created by Emil on 01/09/2017.
 */

public class DrawGauges {

        public void SetUpandDraw(Context mcontext, int totalwidth, int totalheight, TableLayout mywrapper, OBD2_Background mOBD2) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mcontext);
            int def_color_selector = prefs.getInt("def_color_selector", 0);
            int warn1_color_selector = prefs.getInt("def_warn1_selector", 0);
            int warn2_color_selector = prefs.getInt("def_warn2_selector", 0);
            int text_color = prefs.getInt("text_color", 0);
            int arch_width = parseInt(prefs.getString("arch_width", "3"));
            int gauge_number = prefs.getInt("gauge_number", 0);
            int layout_style=prefs.getInt("layout",0);

            Boolean isdebugging=prefs.getBoolean("debugging",false);

            Log.d("OBD2AA","Total Width:" + totalwidth + "Total height: "+totalheight);

            if (layout_style!=0) {
                LayoutInflater inflater = (LayoutInflater) mcontext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                int layoutid=mcontext.getResources().getIdentifier("gauge_layout_"+layout_style,"layout",mcontext.getPackageName());
                View child = inflater.inflate(layoutid, null);

                mywrapper.addView(child);



                View tempview =  mywrapper.findViewWithTag("wrapper_layout");
                if (tempview!=null)
                    tempview.setLayoutParams(new TableRow.LayoutParams(totalwidth, totalheight));

                for (int x = 1; x <= gauge_number; x++) {
                    ArcProgress newArc = (ArcProgress) child.findViewWithTag("gauge_" + x);

                    float warn1;
                    float warn2;
                    float maxlevel;
                    float minlevel;
                     try {
                         maxlevel=prefs.getFloat("maxval_" + x, 0);
                         }
                         catch (Exception E)
                         {
                             maxlevel=prefs.getInt("maxval_" + x, 0);
                         }
                    try {
                        minlevel=prefs.getFloat("minval_" + x, 0);
                        }
                        catch (Exception E)
                        {
                            minlevel=prefs.getInt("minval_" + x, 0);
                        }
                    if (prefs.getString("warn1level_" + x, "0").isEmpty())
                        warn1 = 100 * maxlevel / (float) 100;
                    else
                        warn1 = parseInt(prefs.getString("warn1level_" + x, "0")) * (maxlevel - minlevel) / (float) 100;
                    ;
                    if (prefs.getString("warn2level_" + x, "0").isEmpty())
                        warn2 = 100 * maxlevel / (float) 100;
                    else
                        warn2 = parseInt(prefs.getString("warn2level_" + x, "0")) * (maxlevel - minlevel) / (float) 100;
                    ;
                    if (isdebugging) {

                        Log.d("OBD2", "Max Level: " + maxlevel + "Min Level: " + minlevel + " Warning level 1: " + warn1 + "Warning level 2: " + warn2);
                        Log.d("OBD2", "Max Level: " + maxlevel + " Warning 1 stored value: " + prefs.getString("warn1level_" + x, "0") + " Warning 2 stored value: " + prefs.getString("warn2level_" + x, "0"));
                    }

                    newArc.setProgress(0);
                    // newArc.setId(x);
                    newArc.setBottomText(prefs.getString("gaugename_" + x, ""));
                    newArc.setisReverse(prefs.getBoolean("isreversed_" + x, false));
                    newArc.setFinishedStrokeColor(def_color_selector);
                    newArc.setwarn1color(warn1_color_selector);
                    newArc.setwarn2color(warn2_color_selector);
                    if (mOBD2 != null)
                        newArc.setSuffixText(mOBD2.getUnit(prefs.getString("gaugeunit_" + x, "")));
                    else
                        newArc.setSuffixText(prefs.getString("gaugeunit_" + x, ""));
                    newArc.setWarn1(warn1);
                    newArc.setWarn2(warn2);
                    newArc.setMax(maxlevel);
                    newArc.setMin(minlevel);
                    newArc.setGaugeStyle(prefs.getInt("gaugestyle_" + x, 0));
                    newArc.setTextColor(text_color);
                    newArc.setStrokeWidth(arch_width);

                    if (prefs.getInt("gaugestyle_" + x, 0) == 6) {
                        newArc.setBackgroundResource(R.drawable.bg1);
                        newArc.setShowNeedle(false);

                    } else if (prefs.getInt("gaugestyle_" + x, 0) == 7) {
                        newArc.setBackgroundResource(R.drawable.bg2);
                        newArc.setShowNeedle(false);

                    } else {
                        if (prefs.getBoolean("use_custom_bg_"+x,false))
                            {
                            Log.d("OBD2AA","We should use a custom background...");
                            File imgFile = new File(prefs.getString("custom_bg_path_"+x,""));
                            Log.d("OBD2AA","File path is: "+imgFile.toString());
                            if (imgFile.exists()) {
                                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                                Log.d("OBD2AA","Resizing image to: " + newArc.getHeight() + "width: " + newArc.getWidth());
                               // Bitmap resized = Bitmap.createScaledBitmap(myBitmap, newArc.getWidth(), newArc.getHeight(), true);
                                BitmapDrawable bitmapDrawable = new BitmapDrawable(mcontext.getResources(),myBitmap);
                                newArc.setBackground(bitmapDrawable);
                            }
                            newArc.setIndent(prefs.getInt("arch_indent_"+x,0));
                            newArc.setArcAngle(prefs.getInt("arch_length_"+x,288));
                            newArc.setStartposition(prefs.getInt("arch_startpos_"+x,270));
                        }
                        else
                            newArc.setArcAngle(360 * 0.8f);

                        newArc.setShowNeedle(prefs.getBoolean("showneedle_" + x, true));
                        if (prefs.getBoolean("use_custom_needle_"+x,false)) {
                            File imgFile = new File(prefs.getString("custom_needle_path_"+x,""));
                            if (imgFile.exists()) {
                                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                           //     myBitmap.setHasAlpha(true);
                              //  Bitmap resized = Bitmap.createScaledBitmap(myBitmap, newArc.getHeight(), newArc.getWidth(), false);
                          //      resized.setHasAlpha(true);
                                newArc.setNeedleBitmap(myBitmap);
                            }
                        }
                        else
                            newArc.setNeedlecolor(prefs.getInt("needle_color",-1));

                    }

                    newArc.setShowArc(prefs.getBoolean("showscale_" + x, true));
                    newArc.setShowText(prefs.getBoolean("showtext_" + x, true));
                    newArc.setUseGradientColor(prefs.getBoolean("usegradienttext_" + x, false));
                    newArc.setShowDecimal(prefs.getBoolean("showdecimal_" + x, false));
                    newArc.setShowUnit(prefs.getBoolean("showunit_" + x, false));

                }
            }
            else
            {
                int rownumber=0;
                int columnnumber=0;
            switch (gauge_number) {
                case 1:
                    rownumber = 1;
                    columnnumber = 1;

                    break;
                case 2:
                    rownumber = 1;
                    columnnumber = 2;

                    break;
                case 3:
                    rownumber = 1;
                    columnnumber = 3;

                    break;
                case 4:
                    rownumber = 2;
                    columnnumber = 2;
                    break;
                case 5:
                    rownumber = 2;
                    columnnumber = 3;
                    break;
                case 6:
                    rownumber = 2;
                    columnnumber = 3;
                    break;
                case 7:
                    rownumber = 3;
                    columnnumber = 3;
                    break;
                case 8:
                    rownumber = 2;
                    columnnumber = 4;
                    break;
                case 9:
                    rownumber = 3;
                    columnnumber = 3;
                    break;
                case 10:
                    rownumber = 3;
                    columnnumber = 4;
                    break;
                case 11:
                    rownumber = 3;
                    columnnumber = 4;
                    break;
                case 12:
                    rownumber = 3;
                    columnnumber = 4;
                    break;
                default:
                    rownumber = 3;
                    columnnumber = 5;
            }

            int x = 1;
            for (int r = 1; r <= rownumber; r++) {
                TableRow row = new TableRow(mcontext);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(totalwidth, Math.round(totalheight / rownumber));
                lp.weight = 1.0f;

                row.setLayoutParams(lp);
                mywrapper.addView(row);

                //Add X rows
                for (int c = 1; c <= columnnumber; c++) {
                    if (x<=gauge_number) {
                        //Add collumns for each row.
                        float warn1;
                        float warn2;
                        float maxlevel;
                        float minlevel;
                        try {
                            maxlevel=prefs.getFloat("maxval_" + x, 0);
                        }
                        catch (Exception E)
                        {
                            maxlevel=prefs.getInt("maxval_" + x, 0);
                        }
                        try {
                            minlevel=prefs.getFloat("minval_" + x, 0);
                        }
                        catch (Exception E)
                        {
                            minlevel=prefs.getInt("minval_" + x, 0);
                        }
                        if (prefs.getString("warn1level_"+x,"0").isEmpty())
                            warn1=100 * maxlevel / (float) 100;
                        else
                            warn1=parseInt(prefs.getString("warn1level_"+x,"0"))* (maxlevel-minlevel) / (float) 100;;
                        if (prefs.getString("warn2level_"+x,"0").isEmpty())
                            warn2=100* maxlevel / (float) 100;
                        else
                            warn2=parseInt(prefs.getString("warn2level_"+x,"0")) * (maxlevel-minlevel) / (float) 100;;
                        if (isdebugging) {
                            Log.d("OBD2", "Dynamic insert row: " + r + " column: " + c);
                            Log.d("OBD2", "Max Level: " + maxlevel + "Min Level: " + minlevel + " Warning level 1: " + warn1 + "Warning level 2: " + warn2);
                            Log.d("OBD2", "Max Level: " + maxlevel + " Warning 1 stored value: " + prefs.getString("warn1level_" + x, "0") + " Warning 2 stored value: " + prefs.getString("warn2level_" + x, "0"));
                        }
                        ArcProgress newArc = new ArcProgress(mcontext);

                        int itemwidth = Math.round(Math.min((totalwidth / columnnumber), (totalheight / rownumber)));
                        TableRow.LayoutParams arclayout = new TableRow.LayoutParams(itemwidth, itemwidth);
                        int margins=0;
                        if (r==rownumber)
                        {
                            int elementsinlastrow=(columnnumber-(rownumber*columnnumber-gauge_number));

                            margins= Math.round((totalwidth - elementsinlastrow * itemwidth) / (elementsinlastrow * 2));
                            if (isdebugging)
                                Log.d("OBD2AA","Total width: "+totalwidth + "Element width: "+itemwidth + " elements on row: " + elementsinlastrow+" margin: "+margins);
                        }
                        else
                            margins = Math.round((totalwidth - columnnumber * itemwidth) / (columnnumber * 2));


                        arclayout.setMargins(margins, 0, margins, 0);
                        newArc.setLayoutParams(arclayout);
                        newArc.setProgress(0);
                        // newArc.setId(x);
                        newArc.setBottomText(prefs.getString("gaugename_" + x, ""));
                        newArc.setisReverse(prefs.getBoolean("isreversed_" + x, false));
                        newArc.setFinishedStrokeColor(def_color_selector);
                        newArc.setwarn1color(warn1_color_selector);
                        newArc.setwarn2color(warn2_color_selector);
                        if (mOBD2!=null)
                            newArc.setSuffixText(mOBD2.getUnit(prefs.getString("gaugeunit_" + x, "")));
                        else
                            newArc.setSuffixText(prefs.getString("gaugeunit_" + x, ""));
                        newArc.setWarn1(warn1);
                        newArc.setWarn2(warn2);
                        newArc.setMax(maxlevel);
                        newArc.setMin(minlevel);
                        newArc.setGaugeStyle(prefs.getInt("gaugestyle_"+x,0));
                        newArc.setTextColor(text_color);
                        newArc.setStrokeWidth(arch_width);
                        newArc.setBottomTextSize((float) (itemwidth/9.333));
                        if (prefs.getInt("gaugestyle_"+x,0)==6)
                        {
                            newArc.setBackgroundResource(R.drawable.bg1);
                            newArc.setShowNeedle(false);
                            newArc.setTextSize((float) (itemwidth/6.1));
                        }
                       else if (prefs.getInt("gaugestyle_"+x,0)==7)
                        {
                            newArc.setBackgroundResource(R.drawable.bg2);
                            newArc.setShowNeedle(false);
                            newArc.setTextSize((float) (itemwidth/6.1));
                        }
                        else
                        {
                            if (prefs.getBoolean("use_custom_bg_"+x,false))
                            {
                                Log.d("OBD2AA","We should use a custom background...");
                                File imgFile = new File(prefs.getString("custom_bg_path_"+x,""));
                                Log.d("OBD2AA","File path is: "+imgFile.toString());
                                if (imgFile.exists()) {
                                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                                    Log.d("OBD2AA","Resizing image to: " +itemwidth + "width: " + itemwidth);
                                    Bitmap resized = Bitmap.createScaledBitmap(myBitmap, itemwidth, itemwidth, true);
                                    BitmapDrawable bitmapDrawable;
                                    if (mOBD2!=null)
                                        bitmapDrawable = new BitmapDrawable(mOBD2.getApplicationContext().getResources(),resized);
                                    else
                                        bitmapDrawable = new BitmapDrawable(mcontext.getResources(),resized);

                                    newArc.setBackground(bitmapDrawable);
                                }
                                newArc.setIndent(prefs.getInt("arch_indent_"+x,0));
                                newArc.setArcAngle(prefs.getInt("arch_length_"+x,288));
                                newArc.setStartposition(prefs.getInt("arch_startpos_"+x,270));
                             }
                             else
                                newArc.setArcAngle(360 * 0.8f);

                            newArc.setShowNeedle(prefs.getBoolean("showneedle_" + x, true));
                            if (prefs.getBoolean("use_custom_needle_"+x,false)) {
                                File imgFile = new File(prefs.getString("custom_needle_path_"+x,""));
                                if (imgFile.exists()) {
                                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

                                  //  Bitmap resized = Bitmap.createScaledBitmap(myBitmap, itemwidth, itemwidth, false);

                                    newArc.setNeedleBitmap(myBitmap);
                                }
                            }
                            else
                                newArc.setNeedlecolor(prefs.getInt("needle_color",-1));
                            newArc.setTextSize((float) (itemwidth/5.1));

                        }
                        newArc.setSuffixTextSize((float) (itemwidth/10.25));

                        newArc.setShowArc(prefs.getBoolean("showscale_" + x, true));
                        newArc.setShowText(prefs.getBoolean("showtext_" + x, true));
                        newArc.setUseGradientColor(prefs.getBoolean("usegradienttext_" + x, false));
                        newArc.setShowDecimal(prefs.getBoolean("showdecimal_" + x, false));
                        newArc.setShowUnit(prefs.getBoolean("showunit_" + x, false));
                        newArc.setTag("gauge_" + x);
                        row.addView(newArc);

                        x++;
                    }
                }

            }
            }

        }



}
