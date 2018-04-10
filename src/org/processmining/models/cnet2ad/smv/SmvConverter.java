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
    /**
     * Grafo da convertire.
     */
    private final ADgraph adgraph;
    
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
        smv.append("\t\t" + "TRUE : s0;" + "\n");
        smv.append("\t\t" + "esac;" + "\n");
        
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
    
    /**
     * Restituisce il la matrice dei successivi cammini contemporanei. Ogni or
     *  produce un nuovo cammino possibile e quindi una nuova riga della matrice.
     *  Se ci sono solo and deve restituire una matrice formata da una sola riga.
     * @param oldedges vettore de
     * @return 
     */
    private ArrayList<ArrayList<ADedge>> nextEdge(ArrayList<ADedge> oldedges)
    {
        ArrayList<ArrayList<ADedge>> newFlow = new ArrayList<ArrayList<ADedge>>();
        
        //fork edges conterrà tutte le operazioni avvenute in contemporanea
        ArrayList<ADedge> forkEdges = new ArrayList<>();
        if(this.closingJoin(oldedges))
        {
            for(ADedge oldedge : oldedges)
            {
                if(!oldedge.end().isType(ADnode.BranchNode))
                {
                    forkEdges.addAll(this.nextForkEdge(oldedge));
                }
            }
        }else{
            for(ADedge oldedge : oldedges)
            {
                if(!oldedge.end().isType(ADnode.BranchNode) && !oldedge.end().isType(ADnode.JoinNode))
                {
                    forkEdges.addAll(this.nextForkEdge(oldedge));
                }else if(oldedge.end().isType(ADnode.JoinNode))
                {
                    forkEdges.add(oldedge);
                }
            }
        }
        
        //popolo una matrice di nodi uscenti dai branch contemporanei
        ArrayList<ArrayList<ADedge>> branchOut = new ArrayList<>();
        if(this.closingJoin(oldedges))
        {
            for(ADedge oldedge : oldedges)
            {
                if(oldedge.end().isType(ADnode.BranchNode))
                {
                    branchOut.add(this.nextBranchEdge(oldedge));
                }else{
                    for(ADedge edge : this.adgraph.edges())
                    {
                        if (oldedge.end().id == edge.begin().id )
                        {
                            if (edge.end().isType(ADnode.BranchNode))
                            {
                                branchOut.add(this.nextBranchEdge(edge));
                            }
                        }
                    }
                }
            }
        }else{
            for(ADedge oldedge : oldedges)
            {
                if(oldedge.end().isType(ADnode.BranchNode))
                {
                    branchOut.add(this.nextBranchEdge(oldedge));
                }else if(!oldedge.end().isType(ADnode.JoinNode)){
                    for(ADedge edge : this.adgraph.edges())
                    {
                        if (oldedge.end().id == edge.begin().id )
                        {
                            if (edge.end().isType(ADnode.BranchNode))
                            {
                                branchOut.add(this.nextBranchEdge(edge));
                            }
                        }
                    }
                }
            }
        }
        
        
        //costruisco un vettore di coicidenze tra branch
        ArrayList<ArrayList<ADedge>> coincidence = new ArrayList<>();
        ArrayList<ArrayList<ADedge>> oldCoincidence = new ArrayList<>();
        if(branchOut.size() > 1)
        {
            for(int i=0; i<branchOut.get(0).size(); i++)
            {
                for(int j=0; j<branchOut.get(1).size(); j++)
                {
                    coincidence.add(new ArrayList<>());
                    coincidence.get(coincidence.size()-1).add(branchOut.get(0).get(i));
                    coincidence.get(coincidence.size()-1).add(branchOut.get(1).get(j));
                }
            }

            for(int r=2; r<branchOut.size(); r++)
            {
                oldCoincidence = coincidence;
                coincidence = new ArrayList<>();
                for(int c=0; c<branchOut.get(r).size(); c++)
                {
                    for(int vr=0; vr<oldCoincidence.size(); vr++)
                    {
                        coincidence.add(new ArrayList<>());
                        coincidence.get(coincidence.size()-1).addAll(oldCoincidence.get(vr));
                        coincidence.get(coincidence.size()-1).add(branchOut.get(r).get(c));
                    }
                }
            }

            //concatena ad ogni coincidenza branch un flusso contemporaneo se coincidence è vuoto inserisce solo il flusso contemporaneo
            for(ArrayList<ADedge> c : coincidence)
            {
                newFlow.add(new ArrayList<>());
                newFlow.get(newFlow.size()-1).addAll(c);
                newFlow.get(newFlow.size()-1).addAll(forkEdges);
            }
        }else if(branchOut.size()==1)
        {
            for(ADedge c : branchOut.get(0))
            {
                newFlow.add(new ArrayList<>());
                newFlow.get(newFlow.size()-1).add(c);
                newFlow.get(newFlow.size() - 1).addAll(forkEdges);
            }
        }else if(branchOut.size()==0)
        {
            newFlow.add(forkEdges);
        }
        
        return newFlow;
    }
    
    /**
     * Dato un edge restituisce gli edge del passo successivo. In caso di fork o 
     * branch, salta il successivo e va al secondo successivo.
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
                    
                }else if(edge.end().isType(ADnode.FinalNode)){
                    //non lo aggiunge
                }else{
                    list.add(edge);
                }
            }
        }
        return list;
    }
    
    /**
     * Restituisce il vettore dei nodi uscenti da un branch
     * @param beforeEdge arco precedente
     * @return ArrayList
     */
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
        ArrayList<ArrayList<ADedge>> flows = new ArrayList<>();
        ArrayList<State> parents = new ArrayList<>();
        flows.add(this.firstEdge());
        for(ArrayList<ADedge> flow : flows)
        {
            State s = this.getStateFromEdges(flow);
            result.add(s);
            parents.add(s);
        }
        
        boolean loop = true;
        while (loop)
        {
            ArrayList<ArrayList<ADedge>> newFlows = new ArrayList<>();
            ArrayList<State> newParents = new ArrayList<>();
            for(int j=0; j<flows.size(); j++)
            {
                ArrayList<ArrayList<ADedge>> next = this.nextEdge(flows.get(j));
                
                for(int k=0; k<next.size(); k++)
                {
                    State s = this.getStateFromEdges(next.get(k));
                    State copy = this.stateExist(s, result);
                    if(copy != null)
                    {
                        parents.get(j).next.add(copy);
                    }else{
                        parents.get(j).next.add(s);
                        if(!next.get(0).isEmpty())
                        {
                            newFlows.add(next.get(k));
                            newParents.add(s);
                        }
                        result.add(s);
                    }
                }
            }
            parents = newParents;
            flows = newFlows;
            
            if(flows.size()==0)
                loop=false;
        }
        
        //attribuisce id
        for(int j=0; j<result.size(); j++)
        {
            result.get(j).id=j;
        }
        
        result = this.compressGraph(result);
        
        result = this.cleanNext(result);
        
        //attribuisce id per grafo compresso
        for(int j=0; j<result.size(); j++)
        {
            result.get(j).id=j;
        }
        
        return result;
    }
    
    private State stateExist(State s, ArrayList<State> listS)
    {
        for(State state : listS)
        {
            if(state.equal(s))
            {
                return state;
            }
        }
        return null;
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
     * controlla se tutti gli edge passati in input hanno raggiunto il join di
     * chiusura. Restituisce vero se sono tutti o nessuno join node.
     * @param edges edge da testare
     * @return boolean
     */
    private boolean closingJoin(ArrayList<ADedge> edges)
    {
        ADnode tester = edges.get(0).end();
        for(int i=1; i<edges.size(); i++)
        {
            ADedge edge = edges.get(i);
            if(tester.isType(ADnode.JoinNode))
            {
                if(!edge.end().isType(ADnode.JoinNode))
                {
                    return false;
                }
            }else{
                if(edge.end().isType(ADnode.JoinNode))
                {
                    return false;
                }
            }
        }
        return true;
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
    
    @Deprecated
    private int countZeroState(ArrayList<State> list)
    {
        int count = 0;
        for(State s : list)
        {
            if(s.isZeroState())
            {
                count++;
            }
        }
        return count;
    }
    
    private ArrayList<State> compressGraph(ArrayList<State> list)
    {
        ArrayList<State> ret = new ArrayList<>();
        for (int i=0; i<list.size(); i++)
        {
            State state = list.get(i);
            if(!state.isZeroState())
            {
                for(int j=state.next.size()-1; j>=0; j--)
                {
                    if(state.next.get(j).isZeroState())
                    {
                        state.next.addAll(state.next.get(j).next);
                        state.next.remove(j);
                    }
                }
                ret.add(state);
            }
        }
        return ret;
    }
    
    /**
     * restituisce la posizione dello stato nel vettore, se non è presente restituisce -1
     * @param id id cercato
     * @param list lista di stati
     * @return int posizione
     */
    private int getState_i(int id, ArrayList<State> list)
    {
        for(int i=0; i<list.size(); i++)
        {
            State s = list.get(i);
            if(s.id==id)
            {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Only for debug
     * @param list
     * @deprecated
     */
    @Deprecated
    private void printEdges(ArrayList<ArrayList<ADedge>> list)
    {
        for(ArrayList<ADedge> a : list)
        {
            for(ADedge b : a)
            {
                System.out.println(b.end().name + " => " + b.end().getType());
            }
            System.out.println("----------------------------");
        }
        System.out.println("|~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~|");
    }
    
    /**
     * Elimina eventuali ripetizioni nel vettore next. Da eseguire dopo 
     * l'attribuzione degli ID!!.
     * @param list lista degli stati da alleggerire
     * @return stati alleggeriti
     */
    private ArrayList<State> cleanNext(ArrayList<State> list)
    {
        for(int i=0; i<list.size(); i++)
        {
            ArrayList<State> newNext = new ArrayList<State>();
            State state = list.get(i);
            for(int j=0; j<state.next.size(); j++)
            {
                State stateNext = state.next.get(j);
                if(!this.alreadyExist(stateNext, newNext))
                {
                    newNext.add(stateNext);
                }
            }
            state.next = newNext;
        }
        return list;
    }
    
    /**
     * Controlla se lo stato è gia presente nel vettore
     * @return boolean
     */
    private boolean alreadyExist(State state, ArrayList<State> list)
    {
        for(State s : list)
        {
            if(s.id == state.id)
            {
                return true;
            }
        }
        return false;
    }
    
    private String statePropertyToString(State s)
    {
        String string = new String("");
        for(Property p : s.values)
        {
            if(p.value==1)
            {
                string += p.name;
                string += " -> ";
            }
        }
        return string;
    }
}
