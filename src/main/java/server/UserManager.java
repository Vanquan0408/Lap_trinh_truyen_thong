package server;

import java.util.HashMap;

public class UserManager {

    private static HashMap<String,String> users = new HashMap<>();

    public static boolean register(String user,String pass){

        if(users.containsKey(user)){
            return false;
        }

        users.put(user, pass);
        return true;
    }

    public static boolean login(String user,String pass){

        if(!users.containsKey(user)){
            return false;
        }

        return users.get(user).equals(pass);
    }

}