package com.noam.ftcscouting.utils;

import java.util.ArrayList;
import java.util.HashMap;

public class StaticSync {
    private static long id = 0;
    private static HashMap<Long, Notifiable> objects = new HashMap<>();
    private static ArrayList<Object> queue = new ArrayList<>();

    public static synchronized void send(final Object message){
        for (Long id: objects.keySet()){
            final Notifiable object = objects.get(id);
            new Thread(() -> object.onNotified(message)).start();
        }
    }

    public static void queue(Object message){
        queue.add(message);
    }

    public static synchronized void sync(){
        for (Object message: queue){
            send(message);
        }
        queue.clear();
    }

    public static long register(Notifiable object){
        objects.put(++id, object);
        return id;
    }

    public static void unregister(Long id){
        objects.remove(id);
    }

    public static void unregisterAll(){
        objects.clear();
        queue.clear();
        id = 0;
    }

    public interface Notifiable {
       void  onNotified(Object message);
    }
}
