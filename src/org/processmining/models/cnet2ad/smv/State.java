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
    ArrayList<State> next = new ArrayList<>();
    ArrayList<Property> values = new ArrayList<>();

}
