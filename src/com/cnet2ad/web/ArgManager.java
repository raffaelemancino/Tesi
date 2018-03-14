package com.cnet2ad.web;


import java.util.ArrayList;

/*
    Questa classe si occupa di gestire gli argomenti passati in input
    all'applicazione
 */
public class ArgManager {

    private ArrayList<String> arguments;

    public ArgManager(String[] args){
        this.arguments = new ArrayList<>();
        for(int i = 0; i < args.length; i++)
            this.arguments.add(args[i]);
    }

    public String log(){
        boolean isFlag = false;
        for(int i = 0; i < this.arguments.size(); i++){
            String argument = this.arguments.get(i);

            if(argument.equals("-json"))
                continue;

            if(isFlag){
                isFlag = false;
                continue;
            }

            if(argument.startsWith("-")){
                isFlag = true;
            }
            else return argument;
        }
        return "";
    }

    public String param(String f){
        for(int i = 0; i < this.arguments.size(); i++){
            String argument = this.arguments.get(i);

            if(argument.equals(f)){
                if(i + 1 < this.arguments.size()){
                    String value = this.arguments.get(i+1);

                    if(value.startsWith("-") == false)
                        return value;
                }
                return "";
            }
        }
        return "";
    }

    public boolean flag(String f){
        for(int i = 0; i < this.arguments.size(); i++){
            String argument = this.arguments.get(i);

            if(argument.equals(f)){
                return true;
            }
        }
        return false;
    }

}

