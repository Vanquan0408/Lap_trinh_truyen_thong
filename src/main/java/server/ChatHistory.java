package server;

import java.io.FileWriter;

public class ChatHistory {

    public static void save(String msg){

        try{

            FileWriter fw = new FileWriter("chat_history.txt",true);

            fw.write(msg+"\n");

            fw.close();

        }catch(Exception e){}

    }

}