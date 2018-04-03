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
    
    public SmvConverter(ADgraph graph)
    {
        this.adgraph = graph;
    }
    
    /**
     * Avvia la conversione del ADgraph e lo serializza
     * @return File per NuXMV
     */
    public String convert()
    {
        StringBuilder smv = new StringBuilder();
        smv.append("MODULE main" + "\n");
        smv.append("VAR" + "\n");
        
        //ArrayList<State> states = this.getStatesD();
        ArrayList<State> states = this.getStates();
        ArrayList<Property> marks = this.getProperties();
        
        int i=0;
        //states s : { s0, s1, s2, }
        smv.append("\t" + "s : { ");
        
        smv.append("s" + states.get(0).id);
        for (i=1; i<states.size(); i++)
        {
            smv.append(", s" + states.get(i).id);
        }
        smv.append(" };" + "\n");
        
        //activities s : 0..1;
        for (i = 0; i < marks.size(); i++)
        {
            smv.append("\t" + marks.get(i).name + " : 0..1;" + "\n");
        }
        
        //assigment section
        smv.append("ASSIGN" + "\n");
        
        //states transation
        smv.append("\t" + "init(s) := s" + states.get(0).id + ";\n");
        smv.append("\t" + "next(s) := case" + "\n");
        for(State s : states)
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
                smv.append(";\n");
            }
        }
        smv.append("\t" + "esac;" + "\n");
        
        //activities value
        for(i=0; i < marks.size(); i++)
        {
            smv.append("\t" + marks.get(i).name + " := case" + "\n");
            for(State s : states)
            {
                if(s.values.get(i).value==1)
                {
                    smv.append("\t\t" + "s = s" + s.id + " : 1;" + "\n");
                }
            }
            smv.append("\t\t" + "TRUE : 0;" + "\n");
            smv.append("\t\t" + "esac;" + "\n");
        }
        
        return smv.toString();
    }
    
    /**
     * Restituisce tutte le attività dell'applicazione, con opportune modifiche
     * può restituire risorse e attività dell'applicazione
     * @return lista delle attività
     */
    private ArrayList<Property> getProperties()
    {
        ArrayList<Property> properties = new ArrayList();
        for (ADnode node : this.adgraph.nodes())
        {
            if (node.isType(ADnode.Node))
            {
                Property property = new Property();
                property.name = node.name;
                properties.add(property);
            }
        }
        return properties;
    }
    
    private ArrayList<ArrayList<ADedge>> nextEdge(ArrayList<ADedge> oldedges)
    {
        ArrayList<ArrayList<ADedge>> newFlow = new ArrayList<ArrayList<ADedge>>();
        //fork edges conterrà tutte le operazioni avvenute in contemporanea
        ArrayList<ADedge> forkEdges = new ArrayList<>();
        if(oldedges.size()==1)
        {
            for (ADedge edge : oldedges)
            {
                forkEdges.addAll(this.nextForkEdge(edge));
            }
        }else if(oldedges.size()>1)
        {
            for (int i=0; i<oldedges.size(); i++)
            {
                ADedge edge = oldedges.get(i);
                if(edge.end().isType(ADnode.JoinNode))
                {
                    forkEdges.add(edge);
                    oldedges.remove(i);
                }
            }
            for(ADedge edge : oldedges)
            {
                forkEdges.addAll(this.nextForkEdge(edge));
            }
        }
        
        //creerà una nuova riga della matrice per ogni brach
        boolean branch = false;
        for (ADedge edge : oldedges)
        {
            if (edge.end().isType(ADnode.BranchNode))
            {
                
            }
        }
        
        return newFlow;
    }
    
    /**
     * Dato un edge restituisce gli edge del passo successivo (se sono presenti altre fork,
     * restituisce anche gli edge successivi a queste)
     * @param beforeEdge edge precedente alla nodo di fork
     * @return lista dei nodi successivi a una fork
     */
    private ArrayList<ADedge> nextForkEdge(ADedge beforeEdge)
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
    
    private ArrayList<ADedge> nextBranchEdge(ADedge beforeEdge)
    {
        ArrayList<ADedge> list = new ArrayList<>();
        for (ADedge edge : this.adgraph.edges())
        {
            if(beforeEdge.end().id == edge.begin().id)
            {
                list.add(edge);
            }
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
        /**
         * vettore degli stati da restituire
         */
        ArrayList<State> result = new ArrayList<>();
        /**
         * matrice di edge, ogni riga rappresenta un possibile cammino percorribile
         * e dato in input alla funzione getStateFromEdges restituisce lo stato che li rappresenta
         */
        ArrayList<ArrayList<ADedge>> flow = new ArrayList<>();
        flow.add(this.firstEdge());
        
        boolean loop = true;
        while (loop)
        {
            loop = false;
        }
        
        for (ArrayList<ADedge> edges : flow)
        {
            
        }
        
        return result;
    }
    
    /**
     * Attraverso questa funzione tutti gli stati di un Activity Graph eseguiti 
     * in parallelo vengono tradotti in un stato per NuXMV e automaticamente,
     * per gli stati considerati, si settano a 1 le attività attive.
     * Modificando la funzione è possibile includere anche le risorse.
     * @param edges collegamenti uscenti eseguiti in parallelo
     * @return stato del grafo NuXMV
     */
    private State getStateFromEdges(ArrayList<ADedge> edges)
    {
        State state = new State();
        state.values = this.getProperties();
        for (ADedge edge : edges)
        {
            for (Property p : state.values)
            {
                if (p.name == edge.end().name)
                {
                    p.value = 1;
                }
            }
        }
        return state;
    }
    
    /**
     * Solo per debug
     * @deprecated
     */
    @Deprecated
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
        
        s0.values = this.getProperties();
        s1.values = this.getProperties();
        s2.values = this.getProperties();
        s3.values = this.getProperties();
        
        s0.values.get(0).value = 1;
        s1.values.get(1).value = 1;
        s1.values.get(2).value = 1;
        s2.values.get(2).value = 1;
        s3.values.get(3).value = 1;
        
        states.add(s0);
        states.add(s1);
        states.add(s2);
        states.add(s3);
        
        return states;
    }
}
