package edu.buffalo.cse.cse486586.groupmessenger2;

import android.util.Log;

import java.util.Comparator;

/**
 * Created by junaid on 3/25/17.
 */
public class CompareClass implements Comparator<String> {

    @Override
    public int compare(String x, String y) {
        // TODO Auto-generated method stub
        if(Integer.parseInt(x.split(":")[0])<Integer.parseInt(y.split(":")[0]))
            return -1;
        else if(Integer.parseInt(x.split(":")[0])>Integer.parseInt(y.split(":")[0]))
            return 1;
        else{
            Log.d("Com", x+" : "+y);
            if(Integer.parseInt(x.split(":")[1])>Integer.parseInt(y.split(":")[1]))
                return -1;

            else if(Integer.parseInt(x.split(":")[1])>Integer.parseInt(y.split(":")[1]))
                return 1;

            else
                return 0;
        }
    }
}
