package uk.co.boconi.emil.obd2aa;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.MenuAdapter;
import com.google.android.apps.auto.sdk.MenuItem;
import com.google.android.apps.auto.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class AAMenu extends MenuAdapter{


    private CarActivity mActivity;
    private AAMenu.a b;
    private List<MenuItem> c = new ArrayList();

    public void update_mActivity(CarActivity caractivity)
    {
        this.mActivity=caractivity;

    }


    public void a(AAMenu.a parama)
    {
        this.b = parama;
    }

    public void a(Map<String, String> paramMap)
    {
        if ((paramMap != null) && (!paramMap.isEmpty()))
        {
            this.c = new ArrayList(paramMap.size());
            Iterator<Entry<String, String>> mparamMap = paramMap.entrySet().iterator();
            while (mparamMap.hasNext())
            {
                Object localObject = (Map.Entry)mparamMap.next();
                Bundle localBundle = new Bundle();

                localObject = new MenuItem.Builder()
                        .setType(0)
                        .setTitle((CharSequence)((Map.Entry)localObject).getValue())
                        .build();
                this.c.add((MenuItem) localObject);
            }
        }
    }

    public void b()
    {
        this.b = null;
    }

    public MenuItem getMenuItem(int paramInt)
    {
        return (MenuItem)this.c.get(paramInt);
    }

    public int getMenuItemCount()
    {
        return this.c.size();
    }

    public void onMenuItemClicked(int paramInt)
    {
        Intent intent = null;
      if(getMenuItem(paramInt).getTitle().toString().equalsIgnoreCase("TPMS"))
           intent=new Intent(mActivity.getApplicationContext(),MyTpmsService.class);
        else
          intent = new Intent(mActivity.getApplicationContext(),MyOdbService.class);
        mActivity.startCarActivity(intent);

    }

    public static abstract interface a
    {
        public abstract void a(String paramString);
    }
}
