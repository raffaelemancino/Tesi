/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.processmining.models.cnet2ad.smv;

import java.util.ArrayList;
import org.processmining.models.cnet2ad.ADedge;
import org.processmining.models.cnet2ad.ADgraph;
import org.processmining.models.cnet2ad.ADnode;

/**
 *
 * @author Raffaele Francesco Mancino
 */
public class SmvConverter
{
    private ADgraph adgraph;
    private ArrayList<Property> activities;
    private ArrayList<State> states;
    public SmvConverter(ADgraph graph)
    {
        this.adgraph = graph;
        this.activities = this.getActivities();
        this.states = this.getStates(activities);
    }
    
    public String convert()
    {
        StringBuilder smv = new StringBuilder();
        smv.append("MODULE main" + "\n");
        smv.append("VAR" + "\n");
        
        
        int i=0;
        smv.append("\t" + "s : { ");
        for (State state : states)
        {
            smv.append("s" + state.id + ", ");
        }
        smv.append(" }" + "\n");
        
        for (i = 0; i < this.activities.size(); i++)
        {
            //smv.append("\t" + "a" + String.valueOf(i) + ": 0..1;" + "\n");
            smv.append("\t" + this.activities.get(i).getName() + " : 0..1;" + "\n");
        }
        return smv.toString();
    }
    
    private ArrayList<Property> getActivities()
    {
        ArrayList<Property> activities = new ArrayList();
        for (ADnode node : this.adgraph.nodes())
        {
            if (node.isType(ADnode.Node))
            {
                Property property = new Property();
                property.setName(node.name);
                activities.add(property);
            }
        }
        return activities;
    }
    
    private ArrayList<State> getStates(ArrayList<Property> activities)
    {
        ArrayList<State> s = new ArrayList<>();
        
        
        ArrayList<ADedge> currentedges = this.nextEdge();
        ADnode currentnode = currentedges.get(0).begin();
        s.add(new State().setValues(activities));
        
        
        
        //attribuisce in numero allo stato s(i)
        int i=0;
        for (State state : s)
        {
            state.id = i;
            i++;
        }
        
        return s;
    }
    
    private ArrayList<State> flow(ADedge e, ArrayList<Property> childActivity)
    {
        ArrayList<Property> ownActivity = childActivity;
        ArrayList<ADedge> edges = new ArrayList<>();
        edges.add(e);
        ArrayList<State> s = new ArrayList<>();
        
        for (ADedge edge : edges)
        {
            ADnode endNode = edge.end();
        
            if ( endNode.isType(ADnode.BranchNode) )
            {
                for ( ADedge ed : this.adgraph.edges())
                {
                    if (ed.begin().id == endNode.id)
                    {
                        s.addAll(this.flow(ed, ownActivity)); //non sono collegati da collegare
                    }
                }
            }
            if (endNode.isType(ADnode.Node))
            {
                State newState = new State().setValues(ownActivity);
                if (s.size()>1)
                {
                    s.get(s.size()-1).next.add(newState);
                }
                s.add(newState);
            }
            if(endNode.isType(ADnode.ForkNode))
            {
                State newState = new State().setValues(ownActivity);
                if (s.size()>1)
                {
                    s.get(s.size()-1).next.add(newState);
                }
                s.add(newState);
            }
        }
        
        
        return s;
    }
    
    private ArrayList<ADedge> nextEdge(ADedge oldedge)
    {
        ArrayList<ADedge> rest = new ArrayList<>();
        ADnode node = oldedge.end();
        for (ADedge edge : this.adgraph.edges() )
        {
            if(node.id==edge.begin().id)
            {
                rest.add(edge);
            }
        }
        return rest;
    }
    
    private ArrayList<ADedge> nextEdge()
    {
        ArrayList<ADedge> rest = new ArrayList<>();
        for (ADedge edge : this.adgraph.edges())
        {
            if (edge.begin().isType(ADnode.InitialNode))
            {
                rest.add(edge);
                return rest;
            }
        }
        return null;
    }
    
    @Deprecated
    private ArrayList<ADnode> getResources()
    {
        return null;
    }
}
