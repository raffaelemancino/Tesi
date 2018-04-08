/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.processmining.models.cnet2ad.smv;

import java.util.ArrayList;

/**
 *
 * @author Raffaele Francesco Mancino
 */
public class State
{
    public int id;
    public ArrayList<State> next = new ArrayList<>();
    public ArrayList<Property> values = new ArrayList<>();
    
    public boolean equal(State s)
    {
        boolean test = true;
        for(int i=0; i<values.size(); i++)
        {
            if(values.get(i).value!=s.values.get(i).value)
            {
                test = false;
            }
        }
        
        //se sono tutti 0 deve comunque dare falso
        
        
        if(!this.isZeroState())
            return test;
        else
            return false;
    }
    
    public boolean isZeroState()
    {
        boolean test = true;
        for(int i=1; i<values.size(); i++)
        {
            if(values.get(i).value!=0)
            {
                test=false;
            }
        }
        return test;
    }
}
