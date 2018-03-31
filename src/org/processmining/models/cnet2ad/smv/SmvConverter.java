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
    private ArrayList<Property> marks; //ogni fork node ha la sua activity comune a tutti i nodi successivi
    private ArrayList<State> states;
    
    public SmvConverter(ADgraph graph)
    {
        this.adgraph = graph;
        this.marks = this.getActivities();
        this.states = this.getStates();
    }
    
    public String convert()
    {
        StringBuilder smv = new StringBuilder();
        smv.append("MODULE main" + "\n");
        smv.append("VAR" + "\n");
        
        this.states = this.getStatesD();
        
        int i=0;
        //states s : { s0, s1, s2, }
        smv.append("\t" + "s : { ");
        
        smv.append("s" + this.states.get(0).id);
        for (i=1; i<this.states.size(); i++)
        {
            smv.append(", s" + this.states.get(i).id);
        }
        smv.append(" }" + "\n");
        
        //activities s : 0..1;
        for (i = 0; i < this.marks.size(); i++)
        {
            smv.append("\t" + this.marks.get(i).name + " : 0..1;" + "\n");
        }
        
        //assigment section
        smv.append("ASSIGN" + "\n");
        
        //states transation
        smv.append("\t" + "init(s) := s" + this.states.get(0).id + "\n");
        smv.append("\t" + "next(s) := case" + "\n");
        for(State s : this.states)
        {
            if(s.next.size()!=0)
            {
                smv.append("\t\t" + "s = s" + s.id + " : ");
                if(s.next.size()==1)
                {
                    smv.append("s" + s.next.get(0).id);
                }else if(s.next.size()>1)
                {
                    smv.append("{ ");
                    smv.append("s" + s.next.get(0).id);

                    for (int j=1; j<s.next.size(); j++)
                    {
                        smv.append(", s" + s.next.get(j).id);
                    }
                    smv.append(" }");
                }
                smv.append("\n");
            }
        }
        smv.append("\t" + "esac;");
        
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
                property.name = node.name;
                activities.add(property);
            }
        }
        return activities;
    }
        
    /**
     * Dato un edge restituisce gli edge del passo successivo (se sono presenti altre fork,
     * restituisce anche gli edge successivi a queste)
     * @param beforeEdge edge precedente alla nodo di fork
     * @return lista dei nodi successivi a una fork
     */
    public ArrayList<ADedge> nextForkEdge(ADedge beforeEdge)
    {
        ArrayList<ADedge> list = new ArrayList<>();
        for (ADedge edge : this.adgraph.edges())
        {
            if (beforeEdge.end().id == edge.begin().id)
            {
                if (edge.end().isType(ADnode.ForkNode))
                {
                    list.addAll(this.nextForkEdge(edge));
                }else if(edge.end().isType(ADnode.BranchNode)){
                    ArrayList<ADedge> temp = this.nextBranchEdge(edge);
                    if(temp.size()==1)
                    {
                        list.addAll(temp);
                    }
                }else{
                    list.add(edge);
                }
            }
        }
        return list;
    }
    
    public ArrayList<ADedge> nextBranchEdge(ADedge beforeEdge)
    {
        ArrayList<ADedge> list = new ArrayList<>();
        for (ADedge edge : this.adgraph.edges())
        {
            list.add(edge);
        }
        return list;
    }
    
    private ArrayList<ADedge> firstEdge()
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

    private ArrayList<State> getStates()
    {
        //vettore degli stati da restituire
        ArrayList<State> result = new ArrayList<>();
        //matrice dei possibili marks, ogni or produce un nuovo marks
        ArrayList<ArrayList<Property>> marksMatrix = new ArrayList<>();
        //siccome vi è un sono nodo iniziale inizializzo con un solo mark
        marksMatrix.add(this.marks);
        //vettore degli edges da attraversare simultaneamente ( prodotti da fork )
        ArrayList<ADedge> edges = new ArrayList<>();
        //primo edge per attraversare il grafo
        edges = this.firstEdge();
        
        for (ADedge edge : edges)
        {
            if (edge.end().isType(ADnode.ForkNode))
            {
                ArrayList<ADedge> newEdges = new ArrayList<>();
                newEdges.addAll(this.nextForkEdge(edge));
                for (ADedge newEdge : newEdges)
                {
                    
                }
            }
        }
        
        return result;
    }
    
    private ArrayList<State> getStatesD()
    {
        ArrayList<State> states = new ArrayList<>();
        State s0 = new State();
        s0.id=0;
        State s1 = new State();
        s1.id=1;
        State s2 = new State();
        s2.id=2;
        State s3 = new State();
        s3.id=3;
        s0.next.add(s1);
        s1.next.add(s2);
        s1.next.add(s3);
        s2.next.add(s3);
        s3.next.add(s0);
        
        states.add(s0);
        states.add(s1);
        states.add(s2);
        states.add(s3);
        
        return states;
    }
}
