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
    ArrayList<State> next;
    ArrayList<Property> values;

    public ArrayList<State> getNext()
    {
        return next;
    }

    public State setNext(ArrayList<State> next)
    {
        this.next = next;
        return this;
    }

    public ArrayList<Property> getValues()
    {
        return values;
    }

    public State setValues(ArrayList<Property> values)
    {
        this.values = values;
        return this;
    }
}
